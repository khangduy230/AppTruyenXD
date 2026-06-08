package com.nhom5.ftcomic.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.network.SupabaseAuthClient;
import com.nhom5.ftcomic.network.request.AuthRequest;
import com.nhom5.ftcomic.network.response.AuthResponse;
import com.nhom5.ftcomic.utils.SessionManager;
import com.nhom5.ftcomic.network.SupabaseConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends BottomSheetDialogFragment {

    private boolean isLoginMode = true;

    private TextView tvTitle, tvSubtitle, tvSwitchPrompt, tvSwitchAction;
    private TextInputLayout layoutConfirmPassword;
    private TextInputEditText edtEmail, edtPassword, edtConfirmPassword;
    private MaterialButton btnSubmit;

    private SessionManager sessionManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login, container, false);

        sessionManager = new SessionManager(requireContext());

        bindViews(view);
        setupKeyboard();
        setupSwitchMode();
        setupSubmit();

        return view;
    }

    private void bindViews(View view) {
        tvTitle = view.findViewById(R.id.tvTitle);
        tvSubtitle = view.findViewById(R.id.tvSubtitle);
        layoutConfirmPassword = view.findViewById(R.id.layout_ConfirmPassword);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        tvSwitchPrompt = view.findViewById(R.id.tvSwitchPrompt);
        tvSwitchAction = view.findViewById(R.id.tvSwitchAction);

        edtEmail = view.findViewById(R.id.edtEmail);
        edtPassword = view.findViewById(R.id.edtPassword);
        edtConfirmPassword = view.findViewById(R.id.edtConfirmPassword);
    }

    private void setupKeyboard() {
        edtEmail.setOnClickListener(v -> {
            edtEmail.requestFocus();

            InputMethodManager imm = (InputMethodManager)
                    requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.showSoftInput(edtEmail, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void setupSwitchMode() {
        tvSwitchAction.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateModeUI();
        });
    }

    private void updateModeUI() {
        if (isLoginMode) {
            tvTitle.setText("Đăng nhập");
            tvSubtitle.setText("Để sử dụng tính năng");
            layoutConfirmPassword.setVisibility(View.GONE);
            btnSubmit.setText("Đăng nhập");
            tvSwitchPrompt.setText("Chưa có tài khoản?");
            tvSwitchAction.setText("Đăng ký ngay");
        } else {
            tvTitle.setText("Tạo tài khoản");
            tvSubtitle.setText("Đăng ký tài khoản mới");
            layoutConfirmPassword.setVisibility(View.VISIBLE);
            btnSubmit.setText("Đăng ký");
            tvSwitchPrompt.setText("Đã có tài khoản?");
            tvSwitchAction.setText("Đăng nhập");
        }
    }

    private void setupSubmit() {
        btnSubmit.setOnClickListener(v -> {
            if (isLoginMode) {
                loginWithSupabase();
            } else {
                registerWithSupabase();
            }
        });
    }

    private void loginWithSupabase() {
        String email = getText(edtEmail);
        String password = getText(edtPassword);

        if (!validateEmailPassword(email, password)) {
            return;
        }

        setLoading(true);

        AuthRequest request = new AuthRequest(email, password);

        SupabaseAuthClient.getApi()
                .login(request)
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        setLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            AuthResponse authResponse = response.body();

                            if (authResponse.getAccessToken() == null || authResponse.getUser() == null) {
                                Toast.makeText(getContext(),
                                        "Đăng nhập chưa hoàn tất. Hãy kiểm tra xác nhận email.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            sessionManager.saveSession(
                                    authResponse.getAccessToken(),
                                    authResponse.getRefreshToken(),
                                    authResponse.getUser().getId(),
                                    authResponse.getUser().getEmail()
                            );

                            // SỬA LỖI 4: Gọi fetchUserProfile để lấy thông tin (Username, Avatar) khi Đăng nhập
                            fetchUserProfile(authResponse.getUser().getId(), authResponse.getAccessToken());

                            Toast.makeText(getContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            notifyLoginSuccess();
                        } else {
                            Toast.makeText(getContext(), getErrorMessage(response), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void registerWithSupabase() {
        String email = getText(edtEmail);
        String password = getText(edtPassword);
        String confirmPassword = getText(edtConfirmPassword);

        if (!validateEmailPassword(email, password)) {
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            edtConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            return;
        }

        if (!password.equals(confirmPassword)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }

        setLoading(true);

        AuthRequest request = new AuthRequest(email, password);

        SupabaseAuthClient.getApi()
                .register(request)
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                        setLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            AuthResponse authResponse = response.body();

                            if (authResponse.getAccessToken() != null && authResponse.getUser() != null) {
                                sessionManager.saveSession(
                                        authResponse.getAccessToken(),
                                        authResponse.getRefreshToken(),
                                        authResponse.getUser().getId(),
                                        authResponse.getUser().getEmail()
                                ); // SỬA LỖI 1: Thiếu dấu chấm phẩy ở đây

                                fetchUserProfile(authResponse.getUser().getId(), authResponse.getAccessToken());

                                Toast.makeText(getContext(), "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                notifyLoginSuccess();
                            } else {
                                Toast.makeText(getContext(),
                                        "Đăng ký thành công. Hãy kiểm tra email để xác nhận tài khoản.",
                                        Toast.LENGTH_LONG).show();

                                isLoginMode = true;
                                updateModeUI();
                            }
                        } else {
                            Toast.makeText(getContext(), getErrorMessage(response), Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateEmailPassword(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            return false;
        }

        if (password.length() < 6) {
            edtPassword.setError("Mật khẩu tối thiểu 6 ký tự");
            return false;
        }

        return true;
    }

    private String getText(TextInputEditText editText) {
        if (editText.getText() == null) {
            return "";
        }

        return editText.getText().toString().trim();
    }

    private void setLoading(boolean loading) {
        btnSubmit.setEnabled(!loading);

        if (loading) {
            btnSubmit.setText(isLoginMode ? "Đang đăng nhập..." : "Đang đăng ký...");
        } else {
            btnSubmit.setText(isLoginMode ? "Đăng nhập" : "Đăng ký");
        }
    }

    private void notifyLoginSuccess() {
        Bundle result = new Bundle();
        result.putBoolean("logged_in", true);
        getParentFragmentManager().setFragmentResult("key_dang_nhap", result);
        dismiss();
    }

    private void fetchUserProfile(String userId, String accessToken) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "rest/v1/profiles?id=eq." + userId + "&select=username,avatar_url")
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        // SỬA LỖI 2: Chỉ định rõ okhttp3.Call, okhttp3.Callback, okhttp3.Response để không bị nhầm với Retrofit
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) { }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String json = response.body().string();
                        JSONArray array = new JSONArray(json);

                        if (array.length() > 0) {
                            JSONObject profile = array.getJSONObject(0);

                            // ✅ Lưu vào SharedPreferences để dùng offline
                            if (!profile.isNull("username")) {
                                sessionManager.saveUsername(profile.getString("username"));
                            }
                            if (!profile.isNull("avatar_url")) {
                                sessionManager.saveAvatarUri(profile.getString("avatar_url"));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private String getErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String error = response.errorBody().string();

                JSONObject jsonObject = new JSONObject(error);

                if (jsonObject.has("message")) {
                    return jsonObject.getString("message");
                }

                if (jsonObject.has("msg")) {
                    return jsonObject.getString("msg");
                }

                if (jsonObject.has("error_description")) {
                    return jsonObject.getString("error_description");
                }

                return error;
            }
        } catch (Exception e) {
            return "Thao tác thất bại";
        }

        return "Thao tác thất bại";
    }

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }
}