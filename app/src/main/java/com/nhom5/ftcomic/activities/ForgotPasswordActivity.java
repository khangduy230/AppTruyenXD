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
import com.nhom5.ftcomic.network.SupabaseClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etLoginInput, etAnswer, etNewPassword;
    private TextInputLayout layoutAnswer, layoutNewPassword;
    private MaterialButton btnAction;
    private boolean isQuestionFetched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etLoginInput = findViewById(R.id.etLoginInput);
        etAnswer = findViewById(R.id.etAnswer);
        etNewPassword = findViewById(R.id.etNewPassword);
        layoutAnswer = findViewById(R.id.layoutAnswer);
        layoutNewPassword = findViewById(R.id.layoutNewPassword);
        btnAction = findViewById(R.id.btnAction);

        btnAction.setOnClickListener(v -> {
            if (!isQuestionFetched) {
                checkUserAndFetchQuestion();
            } else {
                verifyAnswerAndResetPassword();
            }
        });
    }

    private void checkUserAndFetchQuestion() {
        String loginStr = etLoginInput.getText() != null ? etLoginInput.getText().toString().trim() : "";
        if (loginStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập Email hoặc Tên tài khoản!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAction.setEnabled(false);
        btnAction.setText("Đang kiểm tra...");

        Map<String, String> body = new HashMap<>();
        body.put("p_login", loginStr);

        SupabaseClient.getApi(this).getUserSecurityQuestion(body).enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(Call<List<Map<String, String>>> call, Response<List<Map<String, String>>> response) {
                btnAction.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    String question = response.body().get(0).get("question");
                    if (question == null || question.isEmpty() || question.equals("null")) {
                        btnAction.setText("Tìm câu hỏi bảo mật");
                        Toast.makeText(ForgotPasswordActivity.this, "Tài khoản chưa cài đặt câu hỏi bảo mật!", Toast.LENGTH_LONG).show();
                    } else {
                        isQuestionFetched = true;
                        etLoginInput.setEnabled(false);
                        layoutAnswer.setVisibility(View.VISIBLE);
                        layoutNewPassword.setVisibility(View.VISIBLE);
                        layoutAnswer.setHint("Câu hỏi: " + question);
                        btnAction.setText("Xác nhận đổi mật khẩu");
                    }
                } else {
                    btnAction.setText("Tìm câu hỏi bảo mật");
                    Toast.makeText(ForgotPasswordActivity.this, "Không tìm thấy thông tin tài khoản!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, String>>> call, Throwable t) {
                btnAction.setEnabled(true);
                btnAction.setText("Tìm câu hỏi bảo mật");
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyAnswerAndResetPassword() {
        String loginStr = etLoginInput.getText() != null ? etLoginInput.getText().toString().trim() : "";
        String answerStr = etAnswer.getText() != null ? etAnswer.getText().toString().trim() : "";
        String newPasswordStr = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";

        if (answerStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập câu trả lời bảo mật!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPasswordStr.isEmpty() || newPasswordStr.length() < 6) {
            Toast.makeText(this, "Mật khẩu mới phải từ 6 ký tự trở lên!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAction.setEnabled(false);
        btnAction.setText("Đang xử lý...");

        Map<String, Object> body = new HashMap<>();
        body.put("p_login", loginStr);
        body.put("p_answer", answerStr.toLowerCase()); // Chuẩn hóa chữ thường khi đối chiếu
        body.put("p_new_password", newPasswordStr);

        SupabaseClient.getApi(this).resetPasswordViaQuestion(body).enqueue(new Callback<List<Map<String, Boolean>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Boolean>>> call, Response<List<Map<String, Boolean>>> response) {
                btnAction.setEnabled(true);
                btnAction.setText("Xác nhận đổi mật khẩu");
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Boolean isSuccess = response.body().get(0).get("success");
                    if (isSuccess != null && isSuccess) {
                        Toast.makeText(ForgotPasswordActivity.this, "Đổi mật khẩu thành công! Hãy đăng nhập lại.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Câu trả lời bảo mật không chính xác!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "Lỗi thực thi máy chủ", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Boolean>>> call, Throwable t) {
                btnAction.setEnabled(true);
                btnAction.setText("Xác nhận đổi mật khẩu");
                Toast.makeText(ForgotPasswordActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}