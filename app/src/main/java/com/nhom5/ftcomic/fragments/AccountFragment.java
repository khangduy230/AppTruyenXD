package com.nhom5.ftcomic.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.activities.DownloadedActivity;
import com.nhom5.ftcomic.activities.SettingsActivity;
import com.nhom5.ftcomic.utils.AuthHelper;

public class AccountFragment extends Fragment {
    private View layoutUserInfo;
    private TextView btnLogin;
    private TextView btnLogout;
    private TextView btnDownload;
    private View btnSettings;

    private TextView btnNotifications;
    private TextView btnManage;
    private TextView btnHistory;

    public AccountFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        layoutUserInfo = view.findViewById(R.id.layout_user_info);
        btnLogin = view.findViewById(R.id.btn_login);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnDownload = view.findViewById(R.id.btn_download);
        btnSettings = view.findViewById(R.id.btn_settings);

        btnNotifications = view.findViewById(R.id.btn_notifications);
        btnManage = view.findViewById(R.id.btn_manage);
        btnHistory = view.findViewById(R.id.btn_history);

        //nút cài đặt
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });
        //nút truyện đã tải
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
        //nút đăng nhập
        btnLogin.setOnClickListener(v -> {
            LoginFragment loginFragment = new LoginFragment();
            loginFragment.show(getParentFragmentManager(), loginFragment.getTag());
        });
        //nút đăng xuất
        btnLogout.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("my_application", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean("DaDangNhap", false);
            editor.apply();

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
        KiemTraVaCapNhatUI();
    }

    private void KiemTraVaCapNhatUI() {
        // Mở sổ ra kiểm tra
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("my_application", Context.MODE_PRIVATE);
        boolean isLogged = sharedPreferences.getBoolean("DaDangNhap", false);

        if (isLogged) {
            if (layoutUserInfo != null) layoutUserInfo.setVisibility(View.VISIBLE);
            if (btnLogout != null) btnLogout.setVisibility(View.VISIBLE);
            if (btnLogin != null) btnLogin.setVisibility(View.GONE);
        }
        else {
            if (layoutUserInfo != null) layoutUserInfo.setVisibility(View.GONE);
            if (btnLogout != null) btnLogout.setVisibility(View.GONE);
            if (btnLogin != null) btnLogin.setVisibility(View.VISIBLE);
        }
    }
}