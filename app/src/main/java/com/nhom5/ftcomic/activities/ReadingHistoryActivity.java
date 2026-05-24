package com.nhom5.ftcomic.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ComicAdapter;
import com.nhom5.ftcomic.database.AppDatabase;

public class ReadingHistoryActivity extends AppCompatActivity {

    private RecyclerView rcvHistory;
    private ComicAdapter comicAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_history); // Tên file XML tương ứng của bạn

        rcvHistory = findViewById(R.id.rcv_history); // Đặt ID này trong file XML của activity này
        rcvHistory.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo adapter hiển thị danh sách truyện
        comicAdapter = new ComicAdapter(this);
        rcvHistory.setAdapter(comicAdapter);

        // Gọi LiveData từ Dao lên để lấy danh sách truyện đã đọc
        AppDatabase.getInstance(this).readingHistoryDao().getHistoryComics()
                .observe(this, listComics -> {
                    if (listComics != null && !listComics.isEmpty()) {
                        comicAdapter.setData(listComics); // Đổ dữ liệu vào Adapter để hiển thị công khai
                    } else {
                        Toast.makeText(ReadingHistoryActivity.this, "Lịch sử đọc trống!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}