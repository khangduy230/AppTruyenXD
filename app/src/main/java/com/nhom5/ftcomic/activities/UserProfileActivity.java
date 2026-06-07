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

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

public class UserProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextInputEditText etUsername, etEmail;
    private MaterialButton btnSave;
    private TextInputLayout tilUsername;
    private TextView tvDisplayName, tvEmailSub;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ShapeableImageView ivAvatar;
    private Uri selectedImageUri = null;

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
                        // Hiển thị ảnh ngay lập tức
                        ivAvatar.setImageURI(uri);
                        // Xoá tint vì giờ là ảnh thật
                        ivAvatar.setImageTintList(null);
                    }
                }
        );

        // 3. Xử lý lưu thông tin
        btnSave.setOnClickListener(v -> {
            String newUsername = etUsername.getText() != null
                    ? etUsername.getText().toString().trim() : "";

            if (newUsername.isEmpty()) {
                tilUsername.setError("Tên hiển thị không được để trống!");
                return;
            }

            tilUsername.setError(null);
            saveUsernameToSupabase(newUsername);

            if (selectedImageUri != null) {
                uploadAvatarToSupabase(selectedImageUri);
            }
        });

        // 4. Các nút click khác
        CardView cardChangePassword = findViewById(R.id.card_change_password);
        cardChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        MaterialButton btnChangeAvatar = findViewById(R.id.btn_change_avatar);
        btnChangeAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        ivAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }
    private void saveUsernameToSupabase(String username) {
        String userId = sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();

        try {
            JSONObject body = new JSONObject();
            body.put("id", userId);
            body.put("username", username);

            OkHttpClient client = new OkHttpClient();
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
                    runOnUiThread(() ->
                            Toast.makeText(UserProfileActivity.this,
                                    "Lưu cục bộ, chưa đồng bộ!", Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // ✅ Lưu ngay khi Supabase thành công, không cần parse body
                        sessionManager.saveUsername(username);
                        runOnUiThread(() -> {
                            if (tvDisplayName != null) tvDisplayName.setText(username);
                            Toast.makeText(UserProfileActivity.this,
                                    "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(UserProfileActivity.this,
                                        "Lỗi cập nhật: " + response.code(), Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void uploadAvatarToSupabase(Uri imageUri) {
        String userId = sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();

        try {
            // Đọc bytes từ URI
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] imageBytes = buffer.toByteArray();
            inputStream.close();

            String fileName = userId + ".jpg"; // mỗi user 1 file, tự ghi đè

            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = RequestBody.create(
                    imageBytes,
                    MediaType.parse("image/jpeg")
            );

            // Upload lên Supabase Storage bucket "avatars"
            Request request = new Request.Builder()
                    .url(SupabaseConfig.PROJECT_URL + "/storage/v1/object/avatars/" + fileName)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("apikey", SupabaseConfig.API_KEY)
                    .addHeader("Content-Type", "image/jpeg")
                    .addHeader("x-upsert", "true") // ✅ ghi đè nếu đã có
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(UserProfileActivity.this,
                                    "Upload ảnh thất bại!", Toast.LENGTH_SHORT).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // URL public của ảnh
                        String avatarUrl = SupabaseConfig.PROJECT_URL + "/storage/v1/object/public/avatars/" + fileName;

                        // Lưu URL vào bảng profiles
                        saveAvatarUrlToProfile(avatarUrl);

                        // Lưu local để dùng offline
                        sessionManager.saveAvatarUri(avatarUrl);
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đọc ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAvatarUrlToProfile(String avatarUrl) {
        String userId = sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();

        try {
            JSONObject body = new JSONObject();
            body.put("id", userId);
            body.put("avatar_url", avatarUrl);

            OkHttpClient client = new OkHttpClient();
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
                public void onFailure(Call call, IOException e) { }

                @Override
                public void onResponse(Call call, Response response) throws IOException { }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadUserInfo() {
        String email = sessionManager.getEmail();
        String savedUsername = sessionManager.getUsername();

        // Hiển thị email
        etEmail.setText(email);
        if (tvEmailSub != null) tvEmailSub.setText(email);

        // Hiển thị tên
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
            // Nếu là URL http → dùng Glide
            if (savedUri.startsWith("http")) {
                Glide.with(this)
                        .load(savedUri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile)
                        .into(ivAvatar);
                ivAvatar.setImageTintList(null);
            } else {
                // URI local cũ
                ivAvatar.setImageURI(Uri.parse(savedUri));
                ivAvatar.setImageTintList(null);
            }
        }
    }
}