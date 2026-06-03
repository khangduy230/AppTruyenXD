package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.DownloadedAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.Comic;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class DownloadedActivity extends AppCompatActivity {
    // Khai báo các thành phần giao diện
    private MaterialToolbar topAppBar;
    private RecyclerView recyclerViewDownloaded;
    private TextView tvEmptyDownloaded;
    private Button btnDelete;

    // Các thành phần hiển thị dung lượng bộ nhớ
    private TextView tvStorageUsedValue;
    private TextView tvStorageTotal;
    private LinearProgressIndicator storageProgressBar;

    private DownloadedAdapter downloadedAdapter;
    private AppDatabase appDatabase;

    // Biến trạng thái để kiểm soát chế độ chọn/xoá truyện
    private boolean isEditMode = false;
    private OnBackPressedCallback onBackPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloaded);

        appDatabase = AppDatabase.getInstance(this);

        // Khởi tạo và thiết lập các thành phần
        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupDeleteButton();
        observeDownloadedComics(); // Theo dõi dữ liệu từ Database
        setupBackCallback();       // Xử lý nút quay lại hệ thống

        // Load dung lượng bộ nhớ lần đầu
        updateStorageInfo();
    }

    // Ánh xạ các View từ Layout XML
    private void bindViews() {
        topAppBar = findViewById(R.id.topAppBar);
        recyclerViewDownloaded = findViewById(R.id.recyclerViewDownloaded);
        tvEmptyDownloaded = findViewById(R.id.tvEmptyDownloaded);
        btnDelete = findViewById(R.id.btnDelete);

        tvStorageUsedValue = findViewById(R.id.tvStorageUsedValue);
        tvStorageTotal = findViewById(R.id.tvStorageTotal);
        storageProgressBar = findViewById(R.id.storageProgressBar);
    }

    /**
     * Cập nhật thông tin dung lượng trong giao diện.
     * Tính toán kích thước thực tế của thư mục chứa truyện đã tải.
     */
    private void updateStorageInfo() {
        // Chạy trong luồng nền để tránh gây lag giao diện (ANR)
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                long usedBytes = 0;
                // Quét dung lượng tại bộ nhớ trong của ứng dụng
                usedBytes += getFolderSize(new File(getFilesDir(), "downloads"));
                // Quét dung lượng tại bộ nhớ ngoài (nếu có)
                usedBytes += getFolderSize(new File(getExternalFilesDir(null), "downloads"));

                // Lấy thông tin tổng dung lượng bộ nhớ máy
                File path = Environment.getDataDirectory();
                StatFs stat = new StatFs(path.getPath());
                long totalBytes = stat.getBlockCountLong() * stat.getBlockSizeLong();

                final long finalUsedBytes = usedBytes;
                // Cập nhật lại giao diện trên Main Thread
                runOnUiThread(() -> {
                    int comicCount = downloadedAdapter.getItemCount();

                    // NẾU DANH SÁCH TRỐNG: Ép dung lượng về 0 (tránh sai số nhỏ từ file hệ thống)
                    long displayBytes = (comicCount == 0) ? 0 : finalUsedBytes;

                    // Định dạng hiển thị MB hoặc GB tùy độ lớn
                    if (displayBytes < 1024 * 1024 * 1024) {
                        double usedMB = displayBytes / (1024.0 * 1024.0);
                        tvStorageUsedValue.setText(String.format(Locale.getDefault(), "%.1f MB", usedMB));
                    } else {
                        double usedGB = displayBytes / (1024.0 * 1024.0 * 1024.0);
                        tvStorageUsedValue.setText(String.format(Locale.getDefault(), "%.2f", usedGB));
                    }

                    // Hiển thị tổng dung lượng máy (có xử lý cho máy ảo bộ nhớ thấp)
                    double totalGB = totalBytes / (1024.0 * 1024.0 * 1024.0);
                    if (totalGB < 1.0) {
                        double totalMB = totalBytes / (1024.0 * 1024.0);
                        tvStorageTotal.setText(String.format(Locale.getDefault(), "%.0f MB tổng", totalMB));
                    } else {
                        tvStorageTotal.setText(String.format(Locale.getDefault(), "%.0f GB tổng", totalGB));
                    }

                    // Cập nhật Progress Bar (tỷ lệ phần trăm)
                    int progress = (int) ((displayBytes * 100.0) / totalBytes);
                    if (comicCount > 0 && progress == 0) progress = 1; // Luôn hiện một chút nếu có truyện
                    storageProgressBar.setProgress(Math.min(progress, 100));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Hàm đệ quy tính tổng dung lượng của một thư mục (Byte)
    private long getFolderSize(File folder) {
        long length = 0;
        if (folder == null || !folder.exists()) return 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) length += file.length();
                else length += getFolderSize(file);
            }
        }
        return length;
    }

    // Xử lý khi bấm nút Back của hệ thống
    private void setupBackCallback() {
        onBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                // Nếu đang ở chế độ xoá thì thoát chế độ đó, nếu không thì quay lại màn trước
                if (isEditMode) exitEditMode();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    private void setupToolbar() {
        topAppBar.setNavigationOnClickListener(v -> {
            if (isEditMode) exitEditMode();
            else finish();
        });
    }

    private void setupRecyclerView() {
        downloadedAdapter = new DownloadedAdapter(new ArrayList<>(), new DownloadedAdapter.OnComicClickListener() {
            @Override
            public void onComicClick(Comic comic) {
                openDetailComic(comic);
            }

            @Override
            public void onSelectionChanged(int count) {
                // Cập nhật text trên nút xoá khi người dùng chọn/bỏ chọn truyện
                if (isEditMode) {
                    btnDelete.setText("Xoá (" + count + ")");
                    btnDelete.setEnabled(count > 0);
                }
            }
        });

        recyclerViewDownloaded.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDownloaded.setAdapter(downloadedAdapter);
    }

    private void setupDeleteButton() {
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                // Lần nhấn đầu: vào chế độ chọn. Lần nhấn sau: thực hiện xoá.
                if (!isEditMode) enterEditMode();
                else performDelete();
            });
        }
    }

    // Kích hoạt chế độ chọn truyện để xoá
    private void enterEditMode() {
        isEditMode = true;
        if (onBackPressedCallback != null) onBackPressedCallback.setEnabled(true);
        downloadedAdapter.setSelectionMode(true);
        if (btnDelete != null) {
            btnDelete.setText("Xoá (0)");
            btnDelete.setEnabled(false);
        }
    }

    // Thoát chế độ chọn truyện
    private void exitEditMode() {
        isEditMode = false;
        if (onBackPressedCallback != null) onBackPressedCallback.setEnabled(false);
        downloadedAdapter.setSelectionMode(false);
        if (btnDelete != null) {
            btnDelete.setText("Xoá truyện");
            btnDelete.setEnabled(true);
        }
    }

    // Logic thực hiện xoá truyện
    private void performDelete() {
        Set<Integer> selectedIds = downloadedAdapter.getSelectedIds();
        if (selectedIds.isEmpty()) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            for (Integer comicId : selectedIds) {
                // 1. Xóa bản ghi các chương đã tải trong Database
                appDatabase.downloadedChapterDao().deleteChaptersByComicId(comicId);
                // 2. Xóa các tệp hình ảnh thực tế trên bộ nhớ máy
                deleteComicFiles(comicId);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã xoá " + selectedIds.size() + " truyện", Toast.LENGTH_SHORT).show();
                exitEditMode();
                // updateStorageInfo() sẽ tự động gọi lại thông qua LiveData Observer ở dưới
            });
        });
    }

    // Tìm và xoá thư mục truyện theo ID
    private void deleteComicFiles(int comicId) {
        String idStr = "comic_" + comicId;
        deleteRecursive(new File(getFilesDir(), "downloads/" + idStr));
        deleteRecursive(new File(getExternalFilesDir(null), "downloads/" + idStr));
    }

    // Hàm đệ quy để xoá sạch thư mục và các file con bên trong
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory != null && fileOrDirectory.exists()) {
            if (fileOrDirectory.isDirectory()) {
                File[] children = fileOrDirectory.listFiles();
                if (children != null) {
                    for (File child : children) deleteRecursive(child);
                }
            }
            fileOrDirectory.delete();
        }
    }

    /**
     * Theo dõi sự thay đổi của danh sách truyện đã tải từ Room Database.
     * Tự động cập nhật giao diện mỗi khi có thay đổi (thêm/xoá).
     */
    private void observeDownloadedComics() {
        appDatabase.downloadedChapterDao().getDownloadedComics()
                .observe(this, comics -> {
                    if (comics == null || comics.isEmpty()) {
                        tvEmptyDownloaded.setVisibility(View.VISIBLE);
                        recyclerViewDownloaded.setVisibility(View.GONE);
                        if (btnDelete != null) btnDelete.setVisibility(View.GONE);
                        downloadedAdapter.setComicList(new ArrayList<>());
                    } else {
                        tvEmptyDownloaded.setVisibility(View.GONE);
                        recyclerViewDownloaded.setVisibility(View.VISIBLE);
                        if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
                        downloadedAdapter.setComicList(new ArrayList<>(comics));
                    }
                    // Mỗi khi danh sách thay đổi, tính toán lại dung lượng bộ nhớ
                    updateStorageInfo();
                });
    }

    private void openDetailComic(Comic comic) {
        Intent intent = new Intent(this, DetailComicActivity.class);
        intent.putExtra("COMIC_ID", comic.getId());
        intent.putExtra("COMIC_COVER_URL", comic.getCoverUrl()); // Thêm dòng này
        startActivity(intent);
    }
}