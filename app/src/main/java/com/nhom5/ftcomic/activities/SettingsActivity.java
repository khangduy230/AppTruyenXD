package com.nhom5.ftcomic.activities;

import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.network.SupabaseApi;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.utils.AppSettings;
import com.nhom5.ftcomic.utils.SessionManager;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private LinearLayout layout_clear_downloads;
    private LinearLayout layout_clear_history;
    private LinearLayout layout_clear_image_cache;

    private AppDatabase appDatabase;
    private SessionManager sessionManager;
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

        appDatabase = AppDatabase.getInstance(this);
        sessionManager = new SessionManager(this);


        boolean isNightMode = sharedPreferences.getBoolean("NightMode", false);
        switchDarkMode.setChecked(isNightMode);


        // Dialog các thứ
        layout_tan_suat.setOnClickListener(v -> {
            String[] options = {"Thủ công", "Mỗi ngày", "Mỗi 2 ngày", "Mỗi 3 ngày", "Mỗi tuần"};
            showSelectionDialog("Tần suất cập nhật", options, edttanxuat, AppSettings.KEY_FREQUENCY_POS);
        });

        layout_mang_update.setOnClickListener(v -> {
            String[] options = {"Wifi và Mạng di động", "Chỉ Wifi"};
            showSelectionDialog("Mạng cập nhật", options, edtmangcapnhat, AppSettings.KEY_NETWORK_POS);
        });

        layout_auto_delete.setOnClickListener( v -> {
            String[] options = {"Không tự động xoá", "Sau khi đọc", "Sau 3 ngày","Sau 1 tuần", "Sau 2 tuần"};
            showSelectionDialog("Tự động xoá chương", options, edtautodelete, AppSettings.KEY_AUTO_DELETE_POS);
        });

        layout_clear_downloads = findViewById(R.id.layout_clear_downloads);
        layout_clear_history = findViewById(R.id.layout_clear_history);
        layout_clear_image_cache = findViewById(R.id.layout_clear_image_cache);


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


        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

        topAppBar.setNavigationOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();

        });

        restoreSettingLabels();
        setupRealSettingActions();

    }

    private void restoreSettingLabels() {
        String[] frequencyOptions = {"Thủ công", "Mỗi ngày", "Mỗi 2 ngày", "Mỗi 3 ngày", "Mỗi tuần"};
        String[] networkOptions = {"Wifi và Mạng di động", "Chỉ Wifi"};
        String[] autoDeleteOptions = {"Không tự động xoá", "Sau khi đọc", "Sau 3 ngày", "Sau 1 tuần", "Sau 2 tuần"};

        int frequencyPos = sharedPreferences.getInt(AppSettings.KEY_FREQUENCY_POS, 0);
        int networkPos = sharedPreferences.getInt(AppSettings.KEY_NETWORK_POS, 0);
        int autoDeletePos = sharedPreferences.getInt(AppSettings.KEY_AUTO_DELETE_POS, 0);

        if (frequencyPos >= 0 && frequencyPos < frequencyOptions.length) {
            edttanxuat.setText(frequencyOptions[frequencyPos]);
        }

        if (networkPos >= 0 && networkPos < networkOptions.length) {
            edtmangcapnhat.setText(networkOptions[networkPos]);
        }

        if (autoDeletePos >= 0 && autoDeletePos < autoDeleteOptions.length) {
            edtautodelete.setText(autoDeleteOptions[autoDeletePos]);
        }
    }

    private void setupRealSettingActions() {
        if (layout_clear_downloads != null) {
            layout_clear_downloads.setOnClickListener(v -> confirmClearDownloads());
        }

        if (layout_clear_history != null) {
            layout_clear_history.setOnClickListener(v -> confirmClearReadingHistory());
        }

        if (layout_clear_image_cache != null) {
            layout_clear_image_cache.setOnClickListener(v -> confirmClearImageCache());
        }
    }

    private void confirmClearDownloads() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa truyện đã tải?")
                .setMessage("Toàn bộ chương đã tải offline sẽ bị xóa khỏi máy.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> clearDownloadedChapters())
                .show();
    }

    private void clearDownloadedChapters() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            File downloadDir = new File(getFilesDir(), "downloads");
            deleteFileOrDirectory(downloadDir);

            appDatabase.downloadedChapterDao().deleteAllDownloadedChapters();
            appDatabase.chapterPageDao().clearAllLocalFilePaths();

            runOnUiThread(() -> {
                Toast.makeText(this, "Đã xóa truyện tải xuống", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void confirmClearReadingHistory() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Bạn cần đăng nhập để xóa lịch sử đọc", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa lịch sử đọc?")
                .setMessage("Lịch sử đọc của tài khoản hiện tại sẽ bị xóa khỏi máy và Supabase.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> clearReadingHistory())
                .show();
    }

    private void clearReadingHistory() {
        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        SupabaseApi api = SupabaseClient.getApi(this);

        api.deleteMyReadingHistory("eq." + userId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        appDatabase.readingHistoryDao().deleteHistoriesByUser(userId);

                        runOnUiThread(() -> {
                            Toast.makeText(
                                    SettingsActivity.this,
                                    "Đã xóa lịch sử đọc",
                                    Toast.LENGTH_SHORT
                            ).show();
                        });
                    });
                } else {
                    Toast.makeText(
                            SettingsActivity.this,
                            "Xóa lịch sử thất bại: " + response.code(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(
                        SettingsActivity.this,
                        "Lỗi mạng khi xóa lịch sử",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void confirmClearImageCache() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa cache ảnh?")
                .setMessage("Ảnh đã cache bởi Glide sẽ bị xóa. App sẽ tải lại ảnh khi cần.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> clearImageCache())
                .show();
    }

    private void clearImageCache() {
        Glide.get(this).clearMemory();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Glide.get(getApplicationContext()).clearDiskCache();

            runOnUiThread(() -> {
                Toast.makeText(this, "Đã xóa cache ảnh", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void deleteFileOrDirectory(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();

            if (children != null) {
                for (File child : children) {
                    deleteFileOrDirectory(child);
                }
            }
        }

        file.delete();
    }
}