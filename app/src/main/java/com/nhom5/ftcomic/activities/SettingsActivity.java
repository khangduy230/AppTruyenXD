package com.nhom5.ftcomic.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.nhom5.ftcomic.R;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {
    //khai báo các biến
    private Switch switchDarkMode;
    private SharedPreferences sharedPreferences;
    private LinearLayout layouttansuat;
    private TextView edttanxuat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Ánh xạ các biến
        switchDarkMode = findViewById(R.id.switchDarkMode);
        layouttansuat = findViewById(R.id.layouttansuat);
        edttanxuat = findViewById(R.id.edttanxuat);
        //khởi tạo SharedPreferences để lưu cấu hình
        sharedPreferences = getSharedPreferences("AppSettingPrefs", MODE_PRIVATE);


        boolean isNightMode = sharedPreferences.getBoolean("NightMode", false);
        switchDarkMode.setChecked(isNightMode);

        layouttansuat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFeedbackDialog(Gravity.CENTER);
            }
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
        // Bỏ chữ "view." ở đằng trước đi
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

        topAppBar.setNavigationOnClickListener(v -> {
            // Trong Activity, bạn chỉ cần gọi trực tiếp như thế này:
            getOnBackPressedDispatcher().onBackPressed();

            // Hoặc dùng một lệnh cực kỳ ngắn gọn và phổ biến để đóng Activity hiện tại:
            // finish();
        });
    }

    private void openFeedbackDialog(int gravity) {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_dialog_tanxuat);

        Window window = dialog.getWindow();
        if (window == null) return;

        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams windowAttributes = window.getAttributes();
        windowAttributes.gravity = gravity;
        window.setAttributes(windowAttributes);

        if (Gravity.CENTER == gravity) {
            //Khi click ra bên ngoài thì tắt
            dialog.setCancelable(true);
        }else
        {
            dialog.setCancelable(false);
        }
        //ánh xạ id các thành phần trong dialog
        RadioGroup grouptansuat = dialog.findViewById(R.id.grouptansuat);
        Button btnback = dialog.findViewById(R.id.btnback);
        Button btnconfirm = dialog.findViewById(R.id.btnconfirm);
        //xử lý sự kiện khi click vào btnback
        btnback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        //Xử lí sự kiện khi click vào btnconfirm
        btnconfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedId = grouptansuat.getCheckedRadioButtonId();
                RadioButton radioButton = dialog.findViewById(selectedId);
                String selectedText = radioButton.getText().toString();
                if (edttanxuat != null) {
                    edttanxuat.setText(selectedText);
                }
                dialog.dismiss();

            }
        });

        dialog.show();
    }
}