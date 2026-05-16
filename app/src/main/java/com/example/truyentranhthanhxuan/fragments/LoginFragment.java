package com.example.truyentranhthanhxuan.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.truyentranhthanhxuan.R;
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
            String email = edtEmail.getText().toString();
            String pass = edtPassword.getText().toString();
            if(isLoginMode) {
                Toast.makeText(getContext(), "Đang đăng nhập...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Đang đăng ký...", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}