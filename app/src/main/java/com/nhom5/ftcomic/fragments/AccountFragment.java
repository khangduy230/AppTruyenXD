package com.nhom5.ftcomic.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.activities.DownloadedActivity;
import com.nhom5.ftcomic.activities.SettingsActivity;
import com.nhom5.ftcomic.activities.ReadingHistoryActivity;
import com.nhom5.ftcomic.activities.UserProfileActivity;
import com.nhom5.ftcomic.utils.AuthHelper;
import com.nhom5.ftcomic.utils.SessionManager;
import com.nhom5.ftcomic.network.SupabaseConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AccountFragment extends Fragment {
    private View layoutUserInfo;
    private TextView tvUserName, tvUserEmail;
    private TextView btnLogin;
    private TextView btnLogout;
    private TextView btnDownload;
    private View btnSettings;
    private TextView btnHistory;
    private SessionManager sessionManager;
    private ShapeableImageView ivAvatar;

    public AccountFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_account, container, false);

        sessionManager = new SessionManager(requireContext());

        layoutUserInfo = view.findViewById(R.id.layout_user_info);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        btnLogin = view.findViewById(R.id.btn_login);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnDownload = view.findViewById(R.id.btn_download);
        btnSettings = view.findViewById(R.id.btn_settings);
        btnHistory = view.findViewById(R.id.btn_history);
        ivAvatar = view.findViewById(R.id.iv_avatar);

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        btnDownload.setOnClickListener(v -> {
            AuthHelper.KiemTraVaThucHien(getContext(), getParentFragmentManager(), () -> {
                Intent intent = new Intent(getActivity(), DownloadedActivity.class);
                startActivity(intent);
            });
        });

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ReadingHistoryActivity.class);
                startActivity(intent);
            });
        }

        btnLogin.setOnClickListener(v -> {
            LoginFragment loginFragment = new LoginFragment();
            loginFragment.show(getParentFragmentManager(), "LoginFragment");
        });

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Toast.makeText(getContext(), "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            KiemTraVaCapNhatUI();
        });

        layoutUserInfo.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), UserProfileActivity.class);
            startActivity(intent);
        });

        getParentFragmentManager().setFragmentResultListener("key_dang_nhap", getViewLifecycleOwner(), (requestKey, result) -> {
            KiemTraVaCapNhatUI();
            fetchProfileRoiCapNhatUI();
        });

        KiemTraVaCapNhatUI();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sessionManager != null) {
            KiemTraVaCapNhatUI();
            fetchProfileRoiCapNhatUI();
        }
    }

    private void fetchProfileRoiCapNhatUI() {
        if (!sessionManager.isLoggedIn()) return;

        String userId = sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "rest/v1/profiles?id=eq." + userId + "&select=username,avatar_url")
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;

                String json = response.body().string();
                if (json.isEmpty() || json.equals("[]")) return;

                try {
                    JSONArray array = new JSONArray(json);
                    if (array.length() == 0) return;

                    JSONObject profile = array.getJSONObject(0);

                    if (!profile.isNull("username")) {
                        sessionManager.saveUsername(profile.getString("username"));
                    }
                    if (!profile.isNull("avatar_url")) {
                        sessionManager.saveAvatarUri(profile.getString("avatar_url"));
                    }

                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> KiemTraVaCapNhatUI());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void KiemTraVaCapNhatUI() {
        boolean isLogged = sessionManager.isLoggedIn();

        if (isLogged) {
            if (layoutUserInfo != null) layoutUserInfo.setVisibility(View.VISIBLE);
            if (btnLogout != null) btnLogout.setVisibility(View.VISIBLE);
            if (btnLogin != null) btnLogin.setVisibility(View.GONE);

            if (tvUserEmail != null) {
                tvUserEmail.setText(sessionManager.getEmail());
            }

            if (tvUserName != null) {
                String savedUsername = sessionManager.getUsername();
                if (savedUsername != null && !savedUsername.isEmpty()) {
                    tvUserName.setText(savedUsername);
                } else {
                    String email = sessionManager.getEmail();
                    if (email != null && email.contains("@")) {
                        tvUserName.setText(email.substring(0, email.indexOf("@")));
                    } else {
                        tvUserName.setText("Người dùng");
                    }
                }
            }

            if (ivAvatar != null) {
                String savedUri = sessionManager.getAvatarUri();
                if (savedUri != null && !savedUri.isEmpty()) {
                    if (savedUri.startsWith("http")) {
                        Glide.with(this)
                                .load(savedUri)
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile)
                                .into(ivAvatar);
                    } else {
                        // URI local cũ
                        ivAvatar.setImageURI(Uri.parse(savedUri));
                    }
                    ivAvatar.setImageTintList(null);
                }
            }

        } else {
            if (layoutUserInfo != null) layoutUserInfo.setVisibility(View.GONE);
            if (btnLogout != null) btnLogout.setVisibility(View.GONE);
            if (btnLogin != null) btnLogin.setVisibility(View.VISIBLE);
            if (tvUserEmail != null) tvUserEmail.setText("");
            if (tvUserName != null) tvUserName.setText("");
        }
    }
}