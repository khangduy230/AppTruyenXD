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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ComicAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.models.Chapter;
import com.nhom5.ftcomic.models.ReadingHistory;
import com.nhom5.ftcomic.network.SupabaseApi;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.response.ReadingHistoryResponse;
import com.nhom5.ftcomic.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReadingHistoryActivity extends AppCompatActivity {

    private MaterialToolbar toolbarHistory;
    private RecyclerView rcvHistory;
    private TextView tvEmptyHistory;
    private ComicAdapter comicAdapter;
    private AppDatabase appDatabase;
    private SessionManager sessionManager;
    private String currentUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reading_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        appDatabase = AppDatabase.getInstance(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        observeReadingHistory();
    }

    private void initViews() {
        rcvHistory = findViewById(R.id.rcv_history);
        tvEmptyHistory = findViewById(R.id.tvEmptyHistory);
        toolbarHistory = findViewById(R.id.toolbarHistory);
    }

    private void setupToolbar() {
        if (toolbarHistory != null) {
            setSupportActionBar(toolbarHistory);
            toolbarHistory.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }
    }

    private void setupRecyclerView() {
        rcvHistory.setLayoutManager(new GridLayoutManager(this, 3));
        comicAdapter = new ComicAdapter(new ArrayList<>(), comic -> {
            if (!sessionManager.isLoggedIn()) {
                Toast.makeText(ReadingHistoryActivity.this, "Bạn cần đăng nhập để xem lịch sử đọc", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = sessionManager.getUserId();
            if (userId == null || userId.trim().isEmpty()) {
                Toast.makeText(ReadingHistoryActivity.this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
                return;
            }

            AppDatabase.databaseWriteExecutor.execute(() -> {
                ReadingHistory historyRecord = appDatabase.readingHistoryDao().getHistoryByComicIdSync(userId, comic.getId());
                runOnUiThread(() -> {
                    if (historyRecord != null && historyRecord.getChapterId() > 0) {
                        Intent intent = new Intent(ReadingHistoryActivity.this, ReaderActivity.class);
                        intent.putExtra("COMIC_ID", comic.getId());
                        intent.putExtra("CHAPTER_ID", historyRecord.getChapterId());
                        intent.putExtra("PAGE_NUMBER", historyRecord.getPageNumber());
                        startActivity(intent);
                    } else {
                        Toast.makeText(ReadingHistoryActivity.this, "Không tìm thấy chương đọc dở, đang mở trang chi tiết!", Toast.LENGTH_SHORT).show();
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
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            showEmpty("Bạn cần đăng nhập để xem lịch sử đọc");
            return;
        }

        currentUserId = sessionManager.getUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            showEmpty("Không tìm thấy thông tin người dùng");
            return;
        }

        syncReadingHistoryFromSupabase(currentUserId);

        appDatabase.readingHistoryDao().getHistoryComics(currentUserId)
                .observe(this, listComics -> {
                    if (listComics != null && !listComics.isEmpty()) {
                        rcvHistory.setVisibility(View.VISIBLE);
                        tvEmptyHistory.setVisibility(View.GONE);

                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            Map<Integer, String> temporaryChapterMap = new HashMap<>();

                            for (Comic comic : listComics) {
                                ReadingHistory historyRecord = appDatabase.readingHistoryDao().getHistoryByComicIdSync(currentUserId, comic.getId());
                                if (historyRecord != null) {
                                    Chapter chapter = appDatabase.chapterDao().getChapterById((int) historyRecord.getChapterId());
                                    if (chapter != null) {
                                        temporaryChapterMap.put(comic.getId(), "Đang đọc: Tập " + chapter.getChapterNumber());
                                    } else {
                                        temporaryChapterMap.put(comic.getId(), "Đang đọc: Tập " + historyRecord.getChapterId());
                                    }
                                }
                            }

                            runOnUiThread(() -> {
                                comicAdapter.setHistoryChapterMap(temporaryChapterMap);
                                comicAdapter.setComicList(listComics);
                            });
                        });
                    } else {
                        comicAdapter.setComicList(new ArrayList<>());
                        showEmpty("Bạn chưa đọc truyện nào");
                    }
                });
    }

    private void syncReadingHistoryFromSupabase(String userId) {
        SupabaseApi api = SupabaseClient.getApi(this);
        api.getMyReadingHistory("eq." + userId, "last_read_at.desc").enqueue(new Callback<List<ReadingHistoryResponse>>() {
            @Override
            public void onResponse(Call<List<ReadingHistoryResponse>> call, Response<List<ReadingHistoryResponse>> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(ReadingHistoryActivity.this, "Không tải được lịch sử đọc: " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                List<ReadingHistoryResponse> remoteHistories = response.body();
                if (remoteHistories == null) {
                    remoteHistories = new ArrayList<>();
                }

                List<ReadingHistory> localHistories = new ArrayList<>();
                for (ReadingHistoryResponse item : remoteHistories) {
                    localHistories.add(new ReadingHistory(userId, item.getComicId(), item.getChapterId(), item.getPageNumber(), item.getLastReadAtMillis()));
                }

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    appDatabase.readingHistoryDao().deleteHistoriesByUser(userId);
                    appDatabase.readingHistoryDao().insertHistories(localHistories);
                });
            }

            @Override
            public void onFailure(Call<List<ReadingHistoryResponse>> call, Throwable t) {
                Toast.makeText(ReadingHistoryActivity.this, "Lỗi mạng khi tải lịch sử đọc", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmpty(String message) {
        if (comicAdapter != null) {
            comicAdapter.setComicList(new ArrayList<>());
        }
        if (rcvHistory != null) {
            rcvHistory.setVisibility(View.GONE);
        }
        if (tvEmptyHistory != null) {
            tvEmptyHistory.setText(message);
            tvEmptyHistory.setVisibility(View.VISIBLE);
        }
    }
}