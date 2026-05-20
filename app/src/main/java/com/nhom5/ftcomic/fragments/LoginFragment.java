package com.nhom5.ftcomic.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.nhom5.ftcomic.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginFragment extends BottomSheetDialogFragment {
    private boolean isLoginMode = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Ánh xạ View
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextInputLayout layoutConfirmPassword = view.findViewById(R.id.layout_ConfirmPassword);
        MaterialButton btnSubmit = view.findViewById(R.id.btnSubmit);
        TextView tvSwitchPrompt = view.findViewById(R.id.tvSwitchPrompt);
        TextView tvSwitchAction = view.findViewById(R.id.tvSwitchAction);

        TextInputEditText edtEmail = view.findViewById(R.id.edtEmail);
        TextInputEditText edtPassword = view.findViewById(R.id.edtPassword);
        TextInputEditText edtConfirmPassword = view.findViewById(R.id.edtConfirmPassword);

        edtEmail.setOnClickListener(v -> {
            edtEmail.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(edtEmail, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // Nút chuyển đổi (Text dưới cùng)
        tvSwitchAction.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            if (isLoginMode) {
                tvTitle.setText("Đăng nhập");
                layoutConfirmPassword.setVisibility(View.GONE);
                btnSubmit.setText("Đăng nhập");
                tvSwitchPrompt.setText("Chưa có tài khoản?");
                tvSwitchAction.setText("Đăng ký ngay");
            } else {
                tvTitle.setText("Tạo tài khoản");
                layoutConfirmPassword.setVisibility(View.VISIBLE);
                btnSubmit.setText("Đăng ký");
                tvSwitchPrompt.setText("Đã có tài khoản?");
                tvSwitchAction.setText("Đăng nhập");
            }
        });

        // Nút Xác nhận
        btnSubmit.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPassword.getText().toString().trim();
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isLoginMode) {
                LuuTrangThaiDangNhap();
                Toast.makeText(getContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().setFragmentResult("key_dang_nhap", new Bundle());
                dismiss();

            } else {
                String confirmPass = edtConfirmPassword.getText().toString().trim();
                if (!pass.equals(confirmPass)) {
                    Toast.makeText(getContext(), "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                    return;
                }

                LuuTrangThaiDangNhap();
                Toast.makeText(getContext(), "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().setFragmentResult("key_dang_nhap", new Bundle());
                dismiss(); // Đóng popup
            }
        });

        return view;
    }

    private void LuuTrangThaiDangNhap() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("my_application", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("DaDangNhap", true);
        editor.apply();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }
}