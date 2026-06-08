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

    // 1 client dùng chung cho toàn bộ Activity
    private final OkHttpClient client = new OkHttpClient();
    // Toast duy nhất, tránh queue spam
    private Toast mToast;

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        sessionManager = new SessionManager(this);

        initViews();
        loadUserInfo();
        loadSavedAvatar();
        registerImagePicker();
        setupClickListeners();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

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
        cardChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class))
        );

        findViewById(R.id.btn_change_avatar).setOnClickListener(v -> launchImagePicker());
        ivAvatar.setOnClickListener(v -> launchImagePicker());
    }

    private void launchImagePicker() {
        pickImageLauncher.launch("image/*");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Save flow
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // Network: Upload avatar
    // ─────────────────────────────────────────────────────────────────────────

    private void uploadAvatarAndSaveProfile(Uri imageUri, String username) {
        byte[] imageBytes;
        try {
            imageBytes = readBytes(imageUri);
        } catch (IOException e) {
            setLoadingState(false);
            showToast("Lỗi đọc ảnh: " + e.getMessage());
            return;
        }

        String userId      = sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();
        String fileName    = userId + ".jpg";

        Request request = new Request.Builder()
                .url(SupabaseConfig.PROJECT_URL + "/storage/v1/object/avatars/" + fileName)
                .post(RequestBody.create(imageBytes, MEDIA_TYPE_IMAGE))
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey",        SupabaseConfig.API_KEY)
                .addHeader("Content-Type",  "image/jpeg")
                .addHeader("x-upsert",      "true")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                setLoadingState(false);
                showToast("Upload ảnh thất bại, vui lòng thử lại!");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Luôn đọc body để tránh leak connection
                String body = readBody(response);
                if (response.isSuccessful()) {
                    // URL lưu vào DB — không có timestamp
                    String avatarUrl = SupabaseConfig.PROJECT_URL
                            + "/storage/v1/object/public/avatars/" + fileName;
                    saveProfileData(username, avatarUrl);
                } else {
                    setLoadingState(false);
                    showToast("Upload ảnh thất bại (" + response.code() + "): " + body);
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Network: Save profile
    // ─────────────────────────────────────────────────────────────────────────

    private void saveProfileData(String username, String avatarUrl) {
        String userId      = sessionManager.getUserId();
        String accessToken = sessionManager.getAccessToken();

        JSONObject body = new JSONObject();
        try {
            body.put("id",       userId);
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
                .url(SupabaseConfig.PROJECT_URL + "/rest/v1/profiles")
                .post(RequestBody.create(body.toString(), MEDIA_TYPE_JSON))
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("apikey",        SupabaseConfig.API_KEY)
                .addHeader("Content-Type",  "application/json")
                .addHeader("Prefer",        "resolution=merge-duplicates")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                setLoadingState(false);
                showToast("Lỗi kết nối khi cập nhật thông tin!");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = readBody(response);
                setLoadingState(false);

                if (response.isSuccessful()) {
                    // Cập nhật session local
                    sessionManager.saveUsername(username);
                    if (avatarUrl != null) {
                        sessionManager.saveAvatarUri(avatarUrl);
                    }

                    runOnUiThread(() -> {
                        if (tvDisplayName != null) tvDisplayName.setText(username);
                        // Reload avatar với cache-bust để hiển thị ảnh mới ngay lập tức
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

    // ─────────────────────────────────────────────────────────────────────────
    // Load data
    // ─────────────────────────────────────────────────────────────────────────

    private void loadUserInfo() {
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

    private void loadSavedAvatar() {
        String savedUri = sessionManager.getAvatarUri();
        if (savedUri == null) return;

        if (savedUri.startsWith("http")) {
            loadAvatarFromUrl(savedUri);
        } else {
            ivAvatar.setImageURI(Uri.parse(savedUri));
            ivAvatar.setImageTintList(null);
        }
    }

    /**
     * Load ảnh từ URL. Thêm ?t=timestamp để bust Glide cache khi cùng URL
     * nhưng file đã thay đổi trên Storage (upload đè cùng tên).
     */
    private void loadAvatarFromUrl(String url) {
        if (isFinishing() || isDestroyed()) return;
        // Xóa timestamp cũ nếu có, rồi thêm mới
        String baseUrl      = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        String cacheBustUrl = baseUrl + "?t=" + System.currentTimeMillis();

        Glide.with(this)
                .load(cacheBustUrl)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)   // không cache trên disk
                .skipMemoryCache(true)                        // không cache trên memory
                .placeholder(R.drawable.ic_profile)
                .into(ivAvatar);

        ivAvatar.setImageTintList(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Đọc toàn bộ bytes từ Uri (dùng cho upload ảnh). */
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

    /** Đọc body string từ OkHttp Response và đóng body lại (tránh leak). */
    private String readBody(Response response) {
        try (ResponseBody rb = response.body()) {
            return rb != null ? rb.string() : "";
        } catch (IOException e) {
            return "";
        }
    }

    /** Toast chống spam — hủy cái cũ trước khi show cái mới. */
    private void showToast(String message) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (mToast != null) mToast.cancel();
            mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
            mToast.show();
        });
    }

    /** Khóa/mở nút Save khi đang gọi mạng. */
    private void setLoadingState(boolean isLoading) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            btnSave.setEnabled(!isLoading);
            btnSave.setText(isLoading ? "Đang xử lý..." : "Lưu thông tin");
        });
    }
}