package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.utils.SessionManager;
import com.nhom5.ftcomic.network.SupabaseConfig;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UserProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextInputEditText etUsername, etEmail;
    private MaterialButton btnSave;
    private TextInputLayout tilUsername;
    private TextView tvDisplayName, tvEmailSub;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ShapeableImageView ivAvatar;
    private Uri selectedImageUri = null;

    // Tối ưu: Chỉ dùng 1 OkHttpClient cho toàn bộ Activity
    private final OkHttpClient client = new OkHttpClient();
    // Tối ưu: Dùng 1 Toast duy nhất để tránh bị dồn ứ (queue)
    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilUsername = findViewById(R.id.til_username);
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        btnSave = findViewById(R.id.btn_save);
        tvDisplayName = findViewById(R.id.tv_display_name);
        tvEmailSub = findViewById(R.id.tv_email_sub);
        ivAvatar = findViewById(R.id.iv_avatar);

        // 1. Hiển thị Tên, Email và Ảnh
        loadUserInfo();
        loadSavedAvatar();

        // 2. Cài đặt chọn ảnh
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        ivAvatar.setImageURI(uri);
                        ivAvatar.setImageTintList(null);
                    }
                }
        );

        // 3. Xử lý lưu thông tin
        btnSave.setOnClickListener(v -> {
            String newUsername = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";

            if (newUsername.isEmpty()) {
                tilUsername.setError("Tên hiển thị không được để trống!");
                return;
            }

            tilUsername.setError(null);

            // Khóa UI để tránh bấm nhiều lần
            setLoadingState(true);

            // Kiểm tra có up ảnh mới không
            if (selectedImageUri != null) {
                // Up ảnh trước -> Nếu thành công thì mới lưu Profile
                uploadAvatarAndSaveProfile(selectedImageUri, newUsername);
            } else {
                // Không có ảnh mới -> Chỉ lưu Username
                saveProfileData(newUsername, null);
            }
        });

        // 4. Các nút click khác
        CardView cardChangePassword = findViewById(R.id.card_change_password);
        cardChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_change_avatar).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        ivAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    /**
     * Tối ưu: Trình quản lý Toast chống spam.
     * Hủy Toast cũ ngay lập tức trước khi hiện cái mới.
     */
    private void showToast(String message) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (mToast != null) mToast.cancel();
            mToast = Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_SHORT);
            mToast.show();
        });
    }

    /**
     * Tối ưu: Khóa nút bấm khi đang thao tác mạng
     */
    private void setLoadingState(boolean isLoading) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            btnSave.setEnabled(!isLoading);
            btnSave.setText(isLoading ? "Đang xử lý..." : "Lưu thông tin");
        });
    }

    /**
     * Tối ưu: Gộp chung việc lưu Username và Avatar vào 1 API duy nhất
     */
    private void saveProfileData(String username, String avatarUrl) {
        String userId = sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();

        try {
            JSONObject body = new JSONObject();
            body.put("id", userId);
            body.put("username", username);
            if (avatarUrl != null) {
                body.put("avatar_url", avatarUrl);
            }

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(SupabaseConfig.PROJECT_URL + "/rest/v1/profiles")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("apikey", SupabaseConfig.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    setLoadingState(false);
                    showToast("Lỗi kết nối khi cập nhật thông tin!");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    setLoadingState(false);
                    if (response.isSuccessful()) {
                        // Cập nhật lại Session cục bộ
                        sessionManager.saveUsername(username);
                        if (avatarUrl != null) {
                            sessionManager.saveAvatarUri(avatarUrl);
                        }

                        runOnUiThread(() -> {
                            if (tvDisplayName != null) tvDisplayName.setText(username);
                            // Reset lại uri đang chọn sau khi đã thành công
                            selectedImageUri = null;
                        });

                        showToast("Cập nhật thành công!");
                    } else {
                        showToast("Lỗi cập nhật dữ liệu: " + response.code());
                    }
                }
            });

        } catch (Exception e) {
            setLoadingState(false);
            showToast("Lỗi: " + e.getMessage());
        }
    }

    /**
     * Upload ảnh lên Storage, nếu thành công sẽ tiếp tục gọi hàm lưu Profile
     */
    private void uploadAvatarAndSaveProfile(Uri imageUri, String username) {
        String userId = sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] imageBytes = buffer.toByteArray();
            inputStream.close();

            String fileName = userId + ".jpg";

            RequestBody requestBody = RequestBody.create(
                    imageBytes,
                    MediaType.parse("image/jpeg")
            );
            Request request = new Request.Builder()
                    .url(SupabaseConfig.PROJECT_URL + "/storage/v1/object/avatars/" + fileName)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("apikey", SupabaseConfig.API_KEY)
                    .addHeader("Content-Type", "image/jpeg")
                    .addHeader("x-upsert", "true")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    setLoadingState(false);
                    showToast("Upload ảnh thất bại, vui lòng thử lại!");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String avatarUrl = SupabaseConfig.PROJECT_URL
                                + "/storage/v1/object/public/avatars/" + fileName;

                        // Đã up ảnh xong -> Tiến hành lưu Username và Link ảnh vào bảng Profile
                        saveProfileData(username, avatarUrl);
                    } else {
                        setLoadingState(false);
                        showToast("Upload ảnh thất bại: " + response.code());
                    }
                }
            });

        } catch (Exception e) {
            setLoadingState(false);
            showToast("Lỗi đọc ảnh: " + e.getMessage());
        }
    }

    private void loadUserInfo() {
        String email = sessionManager.getEmail();
        String savedUsername = sessionManager.getUsername();

        etEmail.setText(email);
        if (tvEmailSub != null) tvEmailSub.setText(email);

        String displayName;
        if (savedUsername != null && !savedUsername.isEmpty()) {
            displayName = savedUsername;
        } else if (email != null && email.contains("@")) {
            displayName = email.substring(0, email.indexOf("@"));
        } else {
            displayName = "Người dùng";
        }
        etUsername.setText(displayName);
        if (tvDisplayName != null) tvDisplayName.setText(displayName);
    }

    private void loadSavedAvatar() {
        String savedUri = sessionManager.getAvatarUri();
        if (savedUri != null) {
            if (savedUri.startsWith("http")) {
                Glide.with(this)
                        .load(savedUri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .into(ivAvatar);
                ivAvatar.setImageTintList(null);
            } else {
                ivAvatar.setImageURI(Uri.parse(savedUri));
                ivAvatar.setImageTintList(null);
            }
        }
    }
}