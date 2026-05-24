package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ComicAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.google.android.material.appbar.MaterialToolbar;
import com.nhom5.ftcomic.models.Comic;

import java.util.ArrayList;

public class ReadingHistoryActivity extends AppCompatActivity {

    private RecyclerView rcvHistory;
    private ComicAdapter comicAdapter;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reading_history);

        // Giữ nguyên quản lý viền hệ thống tránh lỗi hiển thị tràn viền
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Cấu hình nút quay lại trên Toolbar góc trái
        toolbar = findViewById(R.id.toolbarHistory);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 2. Cấu hình RecyclerView hiển thị danh sách truyện
        rcvHistory = findViewById(R.id.rcv_history);
        rcvHistory.setLayoutManager(new LinearLayoutManager(this));

// Khởi tạo Adapter (Thêm rõ ràng chữ <Comic> vào sau ArrayList)
        comicAdapter = new ComicAdapter(new ArrayList<Comic>(), comic -> {
            // Khi click vào cuốn truyện trong lịch sử, tự động mở màn hình chi tiết
            Intent intent = new Intent(ReadingHistoryActivity.this, DetailComicActivity.class);
            intent.putExtra("comic_id", comic.getId());
            startActivity(intent);
        });
        rcvHistory.setAdapter(comicAdapter);

        // 3. Sử dụng LiveData lắng nghe Database lịch sử đọc truyện
        AppDatabase.getInstance(this).readingHistoryDao().getHistoryComics()
                .observe(this, listComics -> {
                    if (listComics != null && !listComics.isEmpty()) {
                        // SỬA TỪ .setData THÀNH .setComicList chuẩn theo file ComicAdapter của bạn
                        comicAdapter.setComicList(listComics);
                    } else {
                        Toast.makeText(ReadingHistoryActivity.this, "Lịch sử đọc của bạn đang trống!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}