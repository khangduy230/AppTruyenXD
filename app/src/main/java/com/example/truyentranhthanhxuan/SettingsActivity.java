package com.example.truyentranhthanhxuan;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {
    //khai báo các biến cần thiết
    private Switch switchDarkMode;
    //SharedPreferences dùng để lưu lại các cài đặt khi tắt ứng dụng
    //và mở lại thì các thiết lập không mất đi.
    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //ánh xạ các biến
        switchDarkMode = findViewById(R.id.switchDarkMode);

        //khởi tạo SharedPreferences để lưu cấu hình
        sharedPreferences = getSharedPreferences("AppSettingPrefs", MODE_PRIVATE);

        //lấy trạng thái hiện tại (mặc định là false - Light Mode)
        boolean isNightMode = sharedPreferences.getBoolean("NightMode", false);

        //set trạng thái cho Switch
        switchDarkMode.setChecked(isNightMode);

        //xử lý sự kiện khi gạt Switch
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            if (isChecked) {
                //bật Dark Mode
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                editor.putBoolean("NightMode", true);
            } else {
                //tắt Dark Mode (về Light Mode)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                editor.putBoolean("NightMode", false);
            }
            editor.apply(); // lưu lại thiết lập
        });

        //tìm view theo id
        ImageView btnBack = findViewById(R.id.btnBack);

        //bắt sự kiện OnClickListener (khi click vào btnBack)
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //lệnh finish() sẽ đóng màn hình Settings này lại
                //và tự động quay về màn hình trước đó
                finish();
            }
        });
    }
}