package com.nhom5.ftcomic.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.activities.DownloadedActivity;
import com.nhom5.ftcomic.activities.ManageComicActivity;
import com.nhom5.ftcomic.activities.NotificationsActivity;
import com.nhom5.ftcomic.activities.ReadingHistoryActivity;
import com.nhom5.ftcomic.activities.SettingsActivity;
import com.nhom5.ftcomic.utils.AuthHelper;
import com.nhom5.ftcomic.utils.SessionManager;

public class AccountFragment extends Fragment {

    private View layoutUserInfo;
    private TextView tvUserName, tvUserEmail;
    private TextView btnLogin;
    private TextView btnLogout;
    private TextView btnDownload;
    private View btnSettings;
    private TextView btnNotifications;
    private TextView btnManage;
    private TextView btnHistory;

    private SessionManager sessionManager;

    public AccountFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_account, container, false);

        sessionManager = new SessionManager(requireContext());

        layoutUserInfo = view.findViewById(R.id.layout_user_info);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);

        btnLogin = view.findViewById(R.id.btn_login);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnDownload = view.findViewById(R.id.btn_download);
        btnSettings = view.findViewById(R.id.btn_settings);

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        btnDownload.setOnClickListener(v -> {
            AuthHelper.KiemTraVaThucHien(getContext(), getParentFragmentManager(), () -> {
                Intent intent = new Intent(getActivity(), DownloadedActivity.class);
                startActivity(intent);
            });
        });
        //nút thông báo
        btnNotifications.setOnClickListener(v -> {
            AuthHelper.KiemTraVaThucHien(getContext(), getParentFragmentManager(), () -> {
                Intent intent = new Intent(getActivity(), NotificationsActivity.class);
                startActivity(intent);
            });
        });

        //nút Quản lý truyện
        btnManage.setOnClickListener(v -> {
            AuthHelper.KiemTraVaThucHien(getContext(), getParentFragmentManager(), () -> {
                Intent intent = new Intent(getActivity(), ManageComicActivity.class);
                startActivity(intent);
            });
        });

        //nút Lịch sử đọc
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ReadingHistoryActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            LoginFragment loginFragment = new LoginFragment();
            loginFragment.show(getParentFragmentManager(), "LoginFragment");
        });

        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Toast.makeText(getContext(), "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            KiemTraVaCapNhatUI();
        });

        getParentFragmentManager().setFragmentResultListener("key_dang_nhap", getViewLifecycleOwner(), (requestKey, result) -> {
            KiemTraVaCapNhatUI();
        });

        KiemTraVaCapNhatUI();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (sessionManager != null) {
            KiemTraVaCapNhatUI();
        }
    }

    private void KiemTraVaCapNhatUI() {
        boolean isLogged = sessionManager.isLoggedIn();

        if (isLogged) {
            if (layoutUserInfo != null) {
                layoutUserInfo.setVisibility(View.VISIBLE);
            }

            if (btnLogout != null) {
                btnLogout.setVisibility(View.VISIBLE);
            }

            if (btnLogin != null) {
                btnLogin.setVisibility(View.GONE);
            }

            if (tvUserEmail != null) {
                tvUserEmail.setText(sessionManager.getEmail());
            }

            if (tvUserName != null) {
                String email = sessionManager.getEmail();

                if (email != null && email.contains("@")) {
                    tvUserName.setText(email.substring(0, email.indexOf("@")));
                } else {
                    tvUserName.setText("Người dùng");
                }
            }

        } else {
            if (layoutUserInfo != null) {
                layoutUserInfo.setVisibility(View.GONE);
            }

            if (btnLogout != null) {
                btnLogout.setVisibility(View.GONE);
            }

            if (btnLogin != null) {
                btnLogin.setVisibility(View.VISIBLE);
            }

            if (tvUserEmail != null) {
                tvUserEmail.setText("");
            }

            if (tvUserName != null) {
                tvUserName.setText("");
            }
        }
    }
}