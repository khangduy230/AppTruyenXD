package com.nhom5.ftcomic.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.network.SupabaseConfig;
import com.nhom5.ftcomic.utils.SessionManager;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextInputLayout tilNewPassword, tilConfirmPassword;
    private TextInputEditText etNewPassword, etConfirmPassword;
    private MaterialButton btnSave;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        sessionManager = new SessionManager(this);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilNewPassword = findViewById(R.id.til_new_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSave = findViewById(R.id.btn_save);
        progressBar = findViewById(R.id.progress_bar);

        btnSave.setOnClickListener(v -> doChangePassword());
    }

    private void doChangePassword() {
        String newPassword = etNewPassword.getText() != null
                ? etNewPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null
                ? etConfirmPassword.getText().toString().trim() : "";

        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);

        if (newPassword.isEmpty()) {
            tilNewPassword.setError("Vui lòng nhập mật khẩu mới!");
            return;
        }
        if (newPassword.length() < 6) {
            tilNewPassword.setError("Mật khẩu phải ít nhất 6 ký tự!");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp!");
            return;
        }

        setLoading(true);

        try {
            String accessToken = sessionManager.getAccessToken();

            JSONObject body = new JSONObject();
            body.put("password", newPassword);

            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(SupabaseConfig.AUTH_BASE_URL + "user")
                    .put(requestBody)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .addHeader("apikey", SupabaseConfig.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(ChangePasswordActivity.this,
                                "Lỗi kết nối: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null
                            ? response.body().string() : "";

                    runOnUiThread(() -> {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(ChangePasswordActivity.this,
                                    "Đổi mật khẩu thành công!",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            // ✅ Hiện lỗi cụ thể từ Supabase
                            String errorMsg = "Đổi mật khẩu thất bại!";
                            try {
                                JSONObject json = new JSONObject(responseBody);
                                if (json.has("message")) {
                                    errorMsg = json.getString("message");
                                }
                            } catch (Exception ignored) {}

                            Toast.makeText(ChangePasswordActivity.this,
                                    errorMsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            setLoading(false);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean isLoading) {
        btnSave.setEnabled(!isLoading);
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }
}