package com.nhom5.ftcomic;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.nhom5.ftcomic.database.AppDatabase;

public class YouthComicApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Khởi tạo SharedPreferences quản lý giao diện (Code cũ của bạn giữ nguyên)
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettingPrefs", MODE_PRIVATE);
        boolean isNightMode = sharedPreferences.getBoolean("NightMode", false);

        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // 2. Ép hệ thống nạp trước Room Database ở Luồng ngầm (Background Thread) ngay khi mở app
        // Giúp tránh việc Main Thread bị quá tải gây sập app khi mở các trang Lịch sử/Quản lý
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase.getInstance(YouthComicApp.this);
        });
    }
}