package com.nhom5.ftcomic.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import com.google.android.material.materialswitch.MaterialSwitch;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.nhom5.ftcomic.R;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {
    //khai báo các biến
    private MaterialSwitch switchDarkMode;
    private SharedPreferences sharedPreferences;
    private LinearLayout layout_tan_suat;
    private LinearLayout layout_mang_update;
    private LinearLayout layout_auto_delete;
    private TextView edttanxuat;
    private TextView edtmangcapnhat;
    private TextView edtautodelete;

    // Hàm dùng chung cho Dialog
    private void showSelectionDialog(java.lang.String title, java.lang.String[] options, android.widget.TextView targetTextView, java.lang.String prefsKey) {
        int checkedItem = sharedPreferences.getInt(prefsKey, 0);

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    targetTextView.setText(options[which]);
                    sharedPreferences.edit().putInt(prefsKey, which).apply();
                    dialog.dismiss();
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);




        // Ánh xạ các biến
        switchDarkMode = findViewById(R.id.switchDarkMode);
        layout_tan_suat = findViewById(R.id.layout_tan_suat);
        layout_mang_update = findViewById(R.id.layout_mang_update);
        layout_auto_delete = findViewById(R.id.layout_auto_delete);
        edttanxuat = findViewById(R.id.edttanxuat);
        edtmangcapnhat = findViewById(R.id.edtmangcapnhat);
        edtautodelete = findViewById(R.id.edtautodelete);
        //khởi tạo SharedPreferences để lưu cấu hình
        sharedPreferences = getSharedPreferences("AppSettingPrefs", MODE_PRIVATE);


        boolean isNightMode = sharedPreferences.getBoolean("NightMode", false);
        switchDarkMode.setChecked(isNightMode);


        // Dialog các thứ
        layout_tan_suat.setOnClickListener(v -> {
            String[] options = {"Thủ công", "Mỗi ngày", "Mỗi 2 ngày", "Mỗi 3 ngày", "Mỗi tuần"};
            showSelectionDialog("Tần suất cập nhật", options, edttanxuat, "frequency_pos");
        });

        layout_mang_update.setOnClickListener(v -> {
            String[] options = {"Wifi và Mạng di động", "Chỉ Wifi"};
            showSelectionDialog("Mạng cập nhật", options, edtmangcapnhat, "network_pos");
        });

        layout_auto_delete.setOnClickListener( v -> {
            String[] options = {"Không tự động xoá", "Sau khi đọc", "Sau 3 ngày","Sau 1 tuần", "Sau 2 tuần"};
            showSelectionDialog("Tự động xoá chương", options, edtautodelete, "auto_delete_pos");
        });



        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                editor.putBoolean("NightMode", true);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                editor.putBoolean("NightMode", false);
            }

            editor.apply();
        });

        //tìm view theo id
//        ImageView btnBack = findViewById(R.id.btnBack);
//
//        //bắt sự kiện OnClickListener (khi click vào btnBack)
//        btnBack.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //lệnh finish() sẽ đóng màn hình Settings này lại
//                //và tự động quay về màn hình trước đó
//                finish();
//            }
//        });
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

        topAppBar.setNavigationOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();


        });


    }

}