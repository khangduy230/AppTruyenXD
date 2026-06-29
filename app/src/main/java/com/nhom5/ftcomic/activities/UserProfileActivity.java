package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.utils.SessionManager;
import com.nhom5.ftcomic.network.SupabaseConfig;
import com.nhom5.ftcomic.network.SupabaseAuthClient;
import com.nhom5.ftcomic.network.request.AuthRequest;
import com.nhom5.ftcomic.network.response.AuthResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UserProfileActivity extends AppCompatActivity {

    private static final MediaType MEDIA_TYPE_JSON  = MediaType.parse("application/json");
    private static final MediaType MEDIA_TYPE_IMAGE = MediaType.parse("image/jpeg");
    private static final int      BUFFER_SIZE       = 16384;

    private SessionManager         sessionManager;
    private TextInputEditText      etUsername, etEmail, etSecurityQuestion, etSecurityAnswer;
    private MaterialButton         btnSave;
    private TextInputLayout        tilUsername, tilSecurityQuestion, tilSecurityAnswer;
    private TextView               tvDisplayName, tvEmailSub;
    private ShapeableImageView     ivAvatar;
    private ActivityResultLauncher<String> pickImageLauncher;
    private Uri                    selectedImageUri = null;

    private String targetUserId = null;
    private boolean isAdminMode = false;

    // ĐÃ THÊM: Các biến theo dõi trạng thái thay đổi và xác thực bảo mật
    private String loadedQuestion = "";
    private String loadedAnswer = "";
    private boolean isPasswordVerified = false;

    private final OkHttpClient client = new OkHttpClient();
    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        sessionManager = new SessionManager(this);

        if (getIntent().hasExtra("USER_ID")) {
            targetUserId = getIntent().getStringExtra("USER_ID");
            isAdminMode = true;
        }

        initViews();
        loadUserInfo();
        loadSavedAvatar();
        registerImagePicker();
        setupClickListeners();

        fetchSecurityQuestionFromDb();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilUsername  = findViewById(R.id.til_username);
        etUsername   = findViewById(R.id.et_username);
        etEmail      = findViewById(R.id.et_email);
        btnSave      = findViewById(R.id.btn_save);
        tvDisplayName = findViewById(R.id.tv_display_name);
        tvEmailSub   = findViewById(R.id.tv_email_sub);
        ivAvatar     = findViewById(R.id.iv_avatar);

        tilSecurityQuestion = findViewById(R.id.til_security_question);
        tilSecurityAnswer   = findViewById(R.id.til_security_answer);
        etSecurityQuestion  = findViewById(R.id.et_security_question);
        etSecurityAnswer    = findViewById(R.id.et_security_answer);
    }

    private void registerImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    selectedImageUri = uri;
                    ivAvatar.setImageURI(uri);
                    ivAvatar.setImageTintList(null);
                }
        );
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> onSaveClicked());

        CardView cardChangePassword = findViewById(R.id.card_change_password);
        cardChangePassword.setOnClickListener(v -> {
            if (isAdminMode) {
                showAdminChangePasswordDialog();
            } else {
                startActivity(new Intent(this, ChangePasswordActivity.class));
            }
        });

        findViewById(R.id.btn_change_avatar).setOnClickListener(v -> launchImagePicker());
        ivAvatar.setOnClickListener(v -> launchImagePicker());

        // ĐÃ THÊM: Bắt sự kiện click vào icon con mắt để check mật khẩu trước khi xem câu trả lời
        tilSecurityAnswer.setEndIconOnClickListener(v -> {
            if (isPasswordVerified) {
                toggleAnswerVisibility();
            } else {
                showPasswordVerificationDialog(this::toggleAnswerVisibility);
            }
        });
    }

    // Hàm thực hiện chuyển đổi qua lại giữa ẩn và hiện chữ câu trả lời bảo mật
    private void toggleAnswerVisibility() {
        int inputType = etSecurityAnswer.getInputType();
        if (inputType == (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            etSecurityAnswer.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            etSecurityAnswer.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        // Đẩy con trỏ văn bản về cuối dòng tránh lỗi vị trí nhập
        if (etSecurityAnswer.getText() != null) {
            etSecurityAnswer.setSelection(etSecurityAnswer.getText().length());
        }
    }

    // ĐÃ THÊM: Hộp thoại Dialog bắt nhập mật khẩu xác thực danh tính
    private void showPasswordVerificationDialog(Runnable onSuccessAction) {
        android.widget.EditText etPassword = new android.widget.EditText(this);
        etPassword.setHint("Nhập mật khẩu tài khoản");
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        container.addView(etPassword);
        container.setPadding(padding, padding / 2, padding, 0);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Xác thực bảo mật")
                .setMessage("Vui lòng xác nhận mật khẩu hiện tại để thực hiện thao tác này.")
                .setView(container)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String password = etPassword.getText().toString().trim();
                    if (password.isEmpty()) {
                        showToast("Mật khẩu không được để trống!");
                        return;
                    }
                    verifyPasswordRemote(password, onSuccessAction);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // ĐÃ THÊM: Gửi chuỗi xác thực mật khẩu qua API Login của Supabase Auth
    private void verifyPasswordRemote(String password, Runnable onSuccessAction) {
        setLoadingState(true);
        String email = sessionManager.getEmail();

        if (email == null || email.isEmpty()) {
            setLoadingState(false);
            showToast("Không tìm thấy thông tin phiên đăng nhập email!");
            return;
        }

        AuthRequest authRequest = new AuthRequest(email, password);

        SupabaseAuthClient.getApi()
                .login(authRequest)
                .enqueue(new retrofit2.Callback<AuthResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<AuthResponse> call, retrofit2.Response<AuthResponse> response) {
                        setLoadingState(false);
                        if (response.isSuccessful() && response.body() != null) {
                            isPasswordVerified = true;
                            runOnUiThread(onSuccessAction);
                        } else {
                            showToast("Mật khẩu không chính xác! Vui lòng thử lại.");
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<AuthResponse> call, Throwable t) {
                        setLoadingState(false);
                        showToast("Lỗi kết nối hệ thống: " + t.getMessage());
                    }
                });
    }

    private void launchImagePicker() {
        pickImageLauncher.launch("image/*");
    }

    private void onSaveClicked() {
        String newUsername = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String question = etSecurityQuestion.getText() != null ? etSecurityQuestion.getText().toString().trim() : "";
        String answer = etSecurityAnswer.getText() != null ? etSecurityAnswer.getText().toString().trim() : "";

        if (newUsername.isEmpty()) {
            tilUsername.setError("Tên hiển thị không được để trống!");
            return;
        }
        tilUsername.setError(null);

        if (!question.isEmpty() && answer.isEmpty()) {
            tilSecurityAnswer.setError("Vui lòng nhập câu trả lời cho câu hỏi bảo mật!");
            return;
        }
        tilSecurityAnswer.setError(null);

        // ĐÃ SỬA: Kiểm tra nếu có thay đổi thông tin bảo mật và chưa nhập pass
        boolean isSecurityInfoChanged = !question.equals(loadedQuestion) || !answer.equals(loadedAnswer);

        if (isSecurityInfoChanged && !isPasswordVerified) {
            showPasswordVerificationDialog(() -> proceedToSaveProfile(newUsername, question, answer));
        } else {
            proceedToSaveProfile(newUsername, question, answer);
        }
    }

    private void proceedToSaveProfile(String username, String question, String answer) {
        setLoadingState(true);
        if (selectedImageUri != null) {
            uploadAvatarAndSaveProfile(selectedImageUri, username, question, answer);
        } else {
            saveProfileData(username, null, question, answer);
        }
    }

    private void uploadAvatarAndSaveProfile(Uri imageUri, String username, String question, String answer) {
        byte[] imageBytes;
        try {
            imageBytes = readBytes(imageUri);
        } catch (IOException e) {
            setLoadingState(false);
            showToast("Lỗi đọc ảnh: " + e.getMessage());
            return;
        }

        String userId = isAdminMode ? targetUserId : sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();

        if (userId == null || userId.trim().isEmpty() || accessToken == null || accessToken.trim().isEmpty()) {
            setLoadingState(false);
            showToast("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại!");
            return;
        }

        String fileName = userId + ".jpg";

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/storage/v1/object/" + SupabaseConfig.AVATARS_BUCKET + "/" + fileName)
                .post(RequestBody.create(imageBytes, MEDIA_TYPE_IMAGE))
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "image/jpeg")
                .addHeader("x-upsert", "true")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                setLoadingState(false);
                showToast("Upload ảnh thất bại: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                String body = readBody(response);
                if (response.isSuccessful()) {
                    String avatarUrl = SupabaseConfig.getAvatarPublicUrl(fileName);
                    saveProfileData(username, avatarUrl, question, answer);
                } else {
                    setLoadingState(false);
                    showToast("Upload ảnh thất bại (" + response.code() + "): " + body);
                }
            }
        });
    }

    private void saveProfileData(String username, String avatarUrl, String question, String answer) {
        String userId = isAdminMode ? targetUserId : sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();

        if (userId == null || userId.trim().isEmpty() || accessToken == null || accessToken.trim().isEmpty()) {
            setLoadingState(false);
            showToast("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại!");
            return;
        }

        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
            if (avatarUrl != null) {
                body.put("avatar_url", avatarUrl);
            }
            body.put("security_question", question);
            body.put("security_answer", answer.toLowerCase());
        } catch (JSONException e) {
            setLoadingState(false);
            showToast("Lỗi tạo dữ liệu: " + e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/profiles?id=eq." + userId)
                .patch(RequestBody.create(body.toString(), MEDIA_TYPE_JSON))
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                setLoadingState(false);
                showToast("Lỗi kết nối khi cập nhật thông tin: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                String respBody = readBody(response);
                setLoadingState(false);

                if (response.isSuccessful()) {
                    if (!isAdminMode) {
                        sessionManager.saveUsername(username);
                        if (avatarUrl != null) {
                            sessionManager.saveAvatarUri(avatarUrl);
                        }
                    }

                    runOnUiThread(() -> {
                        if (tvDisplayName != null) tvDisplayName.setText(username);
                        if (avatarUrl != null) {
                            loadAvatarFromUrl(avatarUrl);
                        }
                        selectedImageUri = null;

                        // ĐÃ THÊM: Đồng bộ và reset lại chốt chặn kiểm tra bảo mật
                        loadedQuestion = question;
                        loadedAnswer = answer;
                        isPasswordVerified = false;
                    });

                    showToast("Cập nhật thành công!");
                } else {
                    showToast("Lỗi cập nhật dữ liệu (" + response.code() + "): " + respBody);
                }
            }
        });
    }

    // ĐÃ SỬA: Tải đồng thời cả Câu hỏi và Câu trả lời bảo mật cũ từ server về máy ảo
    private void fetchSecurityQuestionFromDb() {
        String userId = isAdminMode ? targetUserId : sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();
        if (userId == null) return;

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/profiles?id=eq." + userId + "&select=security_question,security_answer")
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONArray array = new JSONArray(response.body().string());
                        if (array.length() > 0) {
                            JSONObject jsonObject = array.getJSONObject(0);
                            String savedQuestion = jsonObject.optString("security_question", "");
                            String savedAnswer = jsonObject.optString("security_answer", "");

                            runOnUiThread(() -> {
                                if (!savedQuestion.equals("null") && !savedQuestion.isEmpty()) {
                                    etSecurityQuestion.setText(savedQuestion);
                                    loadedQuestion = savedQuestion;
                                }
                                if (!savedAnswer.equals("null") && !savedAnswer.isEmpty()) {
                                    etSecurityAnswer.setText(savedAnswer);
                                    loadedAnswer = savedAnswer;
                                }
                            });
                        }
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void loadUserInfo() {
        if (isAdminMode) {
            String targetUserName = getIntent().getStringExtra("USER_NAME");
            String fakeEmail = "";
            if (targetUserName != null && !targetUserName.trim().isEmpty()) {
                fakeEmail = targetUserName.replaceAll("\\s+", "").toLowerCase() + "@gmail.com";
            }

            etEmail.setText(fakeEmail);
            if (tvEmailSub != null) tvEmailSub.setText(fakeEmail);
            etUsername.setText(targetUserName);
            if (tvDisplayName != null) tvDisplayName.setText(targetUserName);
        } else {
            String email         = sessionManager.getEmail();
            String savedUsername = sessionManager.getUsername();

            etEmail.setText(email);
            if (tvEmailSub != null) tvEmailSub.setText(email);

            String displayName;
            if (savedUsername != null && !savedUsername.isEmpty()) {
                displayName = savedUsername;
            } else if (email != null && email.contains("@")) {
                displayName = email.substring(0, email.indexOf('@'));
            } else {
                displayName = "Người dùng";
            }

            etUsername.setText(displayName);
            if (tvDisplayName != null) tvDisplayName.setText(displayName);
        }
    }

    private void loadSavedAvatar() {
        if (isAdminMode) {
            String avatarUrl = SupabaseConfig.getAvatarPublicUrl(targetUserId + ".jpg");
            loadAvatarFromUrl(avatarUrl);
        } else {
            String savedUri = sessionManager.getAvatarUri();
            if (savedUri == null) return;

            if (savedUri.startsWith("http")) {
                loadAvatarFromUrl(savedUri);
            } else {
                ivAvatar.setImageURI(Uri.parse(savedUri));
                ivAvatar.setImageTintList(null);
            }
        }
    }

    private void loadAvatarFromUrl(String url) {
        if (isFinishing() || isDestroyed()) return;
        String baseUrl      = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        String cacheBustUrl = baseUrl + "?t=" + System.currentTimeMillis();

        Glide.with(this)
                .load(cacheBustUrl)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.ic_profile)
                .into(ivAvatar);

        ivAvatar.setImageTintList(null);
    }

    private byte[] readBytes(Uri uri) throws IOException {
        try (InputStream is = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (is == null) throw new IOException("Cannot open URI");
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = is.read(buf, 0, buf.length)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        }
    }

    private String readBody(Response response) {
        try (ResponseBody rb = response.body()) {
            return rb != null ? rb.string() : "";
        } catch (IOException e) {
            return "";
        }
    }

    private void showToast(String message) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (mToast != null) mToast.cancel();
            mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
            mToast.show();
        });
    }

    private void setLoadingState(boolean isLoading) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            btnSave.setEnabled(!isLoading);
            btnSave.setText(isLoading ? "Đang xử lý..." : "Lưu thông tin");
        });
    }

    private void showAdminChangePasswordDialog() {
        android.widget.EditText etNewPassword = new android.widget.EditText(this);
        etNewPassword.setHint("Nhập mật khẩu mới");
        etNewPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        container.addView(etNewPassword);
        container.setPadding(padding, padding / 2, padding, 0);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Đổi mật khẩu thành viên")
                .setMessage("Nhập mật khẩu mới cho tài khoản " + etUsername.getText().toString())
                .setView(container)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String newPassword = etNewPassword.getText().toString().trim();
                    if (newPassword.isEmpty() || newPassword.length() < 6) {
                        showToast("Mật khẩu phải có ít nhất 6 ký tự!");
                        return;
                    }
                    adminResetPasswordRemote(newPassword);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void adminResetPasswordRemote(String newPassword) {
        setLoadingState(true);
        String accessToken = sessionManager.getAccessToken();

        JSONObject bodyJson = new JSONObject();
        try {
            bodyJson.put("target_user_id", targetUserId);
            bodyJson.put("new_password", newPassword);
        } catch (JSONException e) {
            setLoadingState(false);
            showToast("Lỗi đóng gói dữ liệu");
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/rpc/admin_reset_password")
                .post(RequestBody.create(bodyJson.toString(), MEDIA_TYPE_JSON))
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                setLoadingState(true);
                showToast("Lỗi kết nối server: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                setLoadingState(false);
                if (response.isSuccessful()) {
                    showToast("Đã đổi mật khẩu thành công!");
                } else {
                    showToast("Đổi mật khẩu thất bại, mã lỗi: " + response.code());
                }
            }
        });
    }
}