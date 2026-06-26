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
    private TextInputEditText      etUsername, etEmail;
    private MaterialButton         btnSave;
    private TextInputLayout        tilUsername;
    private TextView               tvDisplayName, tvEmailSub;
    private ShapeableImageView     ivAvatar;
    private ActivityResultLauncher<String> pickImageLauncher;
    private Uri                    selectedImageUri = null;

    private String targetUserId = null;
    private boolean isAdminMode = false;

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
    }

    private void launchImagePicker() {
        pickImageLauncher.launch("image/*");
    }

    private void onSaveClicked() {
        String newUsername = etUsername.getText() != null
                ? etUsername.getText().toString().trim()
                : "";

        if (newUsername.isEmpty()) {
            tilUsername.setError("Tên hiển thị không được để trống!");
            return;
        }
        tilUsername.setError(null);
        setLoadingState(true);

        if (selectedImageUri != null) {
            uploadAvatarAndSaveProfile(selectedImageUri, newUsername);
        } else {
            saveProfileData(newUsername, null);
        }
    }

    private void uploadAvatarAndSaveProfile(Uri imageUri, String username) {
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

        if (userId == null || userId.trim().isEmpty()
                || accessToken == null || accessToken.trim().isEmpty()) {
            setLoadingState(false);
            showToast("Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại!");
            return;
        }

        String fileName = userId + ".jpg";

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL
                        + "/storage/v1/object/"
                        + SupabaseConfig.AVATARS_BUCKET
                        + "/"
                        + fileName)
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
                    saveProfileData(username, avatarUrl);
                } else {
                    setLoadingState(false);
                    showToast("Upload ảnh thất bại (" + response.code() + "): " + body);
                }
            }
        });
    }

    private void saveProfileData(String username, String avatarUrl) {
        String userId = isAdminMode ? targetUserId : sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();

        if (userId == null || userId.trim().isEmpty()
                || accessToken == null || accessToken.trim().isEmpty()) {
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
        } catch (JSONException e) {
            setLoadingState(false);
            showToast("Lỗi tạo dữ liệu: " + e.getMessage());
            return;
        }

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL
                        + "/rest/v1/profiles?id=eq."
                        + userId)
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
                    });

                    showToast("Cập nhật thành công!");
                } else {
                    showToast("Lỗi cập nhật dữ liệu (" + response.code() + "): " + respBody);
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
                setLoadingState(false);
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