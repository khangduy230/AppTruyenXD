package com.example.truyentranhthanhxuan;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class YouthComicApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        //khởi tạo SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettingPrefs", MODE_PRIVATE);

        //lấy trạng thái hiện tại (mặc định là false - Light Mode)
        boolean isNightMode = sharedPreferences.getBoolean("NightMode", false);

        //set chế độ hiện tại (Light/Dark)
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}