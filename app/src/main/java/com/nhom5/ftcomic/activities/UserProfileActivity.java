package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.utils.SessionManager;

public class UserProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextInputEditText etUsername, etEmail;
    private MaterialButton btnSave;
    private TextInputLayout tilUsername;
    private TextView tvDisplayName, tvEmailSub;

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

        // Hiển thị thông tin hiện tại
        String email = sessionManager.getEmail();
        etEmail.setText(email);

        loadUserInfo();

        String savedUsername = sessionManager.getUsername();
        if (savedUsername != null && !savedUsername.isEmpty()) {
            etUsername.setText(savedUsername);
        } else if (email != null && email.contains("@")) {
            etUsername.setText(email.substring(0, email.indexOf("@")));
        }

        btnSave.setOnClickListener(v -> {
            String newUsername = etUsername.getText() != null
                    ? etUsername.getText().toString().trim() : "";

            if (newUsername.isEmpty()) {
                tilUsername.setError("Tên hiển thị không được để trống!");
                return;
            }

            tilUsername.setError(null);
            sessionManager.saveUsername(newUsername);

            // ✅ Cập nhật display name ngay lập tức không cần reload
            if (tvDisplayName != null) tvDisplayName.setText(newUsername);

            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
        });
        CardView cardChangePassword = findViewById(R.id.card_change_password);
        cardChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            startActivity(intent);
        });
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
}