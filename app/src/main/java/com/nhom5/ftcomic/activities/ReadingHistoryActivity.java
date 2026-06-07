package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ComicAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.Comic;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import androidx.recyclerview.widget.GridLayoutManager;

public class ReadingHistoryActivity extends AppCompatActivity {

    private MaterialToolbar toolbarHistory;
    private RecyclerView rcvHistory;
    private TextView tvEmptyHistory; // Khai báo thêm TextView báo trống
    private ComicAdapter comicAdapter;
    private AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reading_history);

        // Quản lý viền hệ thống (Giữ nguyên cấu trúc của bạn)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        appDatabase = AppDatabase.getInstance(this);

        initViews();
        setupRecyclerView();
        observeReadingHistory();
        setupToolbar();


    }

    private void initViews() {
        rcvHistory = findViewById(R.id.rcv_history);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory); // Ánh xạ TextView báo trống
        toolbarHistory = findViewById(R.id.toolbarHistory); // Ánh xạ vào biến thành viên
    }

    private void setupToolbar() {
        if (toolbarHistory != null) {
            setSupportActionBar(toolbarHistory); // Nếu bạn muốn dùng menu
            toolbarHistory.setNavigationOnClickListener(v -> {
                getOnBackPressedDispatcher().onBackPressed();
            });
        }
    }
    private void setupRecyclerView() {
        rcvHistory.setLayoutManager(new GridLayoutManager(this, 3));

        comicAdapter = new ComicAdapter(new ArrayList<Comic>(), comic -> {
            // Sử dụng Executor chạy ngầm để truy vấn nhanh chapterId đọc dở từ Room Database
            AppDatabase.databaseWriteExecutor.execute(() -> {
                com.nhom5.ftcomic.models.ReadingHistory historyRecord =
                        appDatabase.readingHistoryDao().getHistoryByComicIdSync(comic.getId());

                runOnUiThread(() -> {
                    if (historyRecord != null && historyRecord.getChapterId() > 0) {
                        Intent intent = new Intent(ReadingHistoryActivity.this, ReaderActivity.class);
                        intent.putExtra("COMIC_ID", comic.getId());
                        intent.putExtra("CHAPTER_ID", historyRecord.getChapterId()); // Nạp ID chương chuẩn xác
                        startActivity(intent);
                    } else {
                        // Nếu dính lỗi mất liên kết chương, ta đẩy sang trang Chi tiết truyện
                        Toast.makeText(ReadingHistoryActivity.this,
                                "Không tìm thấy chương đọc dở, đang mở trang chi tiết!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(ReadingHistoryActivity.this, DetailComicActivity.class);
                        intent.putExtra("COMIC_ID", comic.getId());
                        startActivity(intent);
                    }
                });
            });
        });

        rcvHistory.setAdapter(comicAdapter);
    }

    private void observeReadingHistory() {
        if (appDatabase != null && appDatabase.readingHistoryDao() != null) {
            appDatabase.readingHistoryDao().getHistoryComics()
                    .observe(this, listComics -> {
                        if (listComics != null && !listComics.isEmpty()) {
                            // Có dữ liệu: Hiện danh sách, ẩn chữ báo trống
                            rcvHistory.setVisibility(View.VISIBLE);
                            tvEmptyHistory.setVisibility(View.GONE);
                            comicAdapter.setComicList(listComics);
                        } else {
                            // Không có dữ liệu: Ẩn danh sách, hiện chữ báo trống lên giữa màn hình
                            comicAdapter.setComicList(new ArrayList<>());
                            rcvHistory.setVisibility(View.GONE);
                            tvEmptyHistory.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }
}