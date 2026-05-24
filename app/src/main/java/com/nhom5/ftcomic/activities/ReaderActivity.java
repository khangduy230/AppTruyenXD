package com.nhom5.ftcomic.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ReaderPageAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.DownloadedChapter;
import com.nhom5.ftcomic.models.ReadingHistory;
import com.nhom5.ftcomic.repository.ComicRepository;
import com.nhom5.ftcomic.utils.OfflineDownloadManager;

import java.util.ArrayList;

public class ReaderActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPages;
    private TextView tvEmptyState;
    private Button btnDownloadChapter;

    private LinearLayout layoutDownloadProgress;
    private TextView tvDownloadProgress;
    private ProgressBar progressDownload;

    private ReaderPageAdapter readerPageAdapter;

    private AppDatabase appDatabase;
    private ComicRepository comicRepository;

    private int comicId = -1;
    private int chapterId = -1;
    private int totalPages = 0;

    private boolean isDownloading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

        topAppBar.setNavigationOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();


        });

        comicId = getIntent().getIntExtra("COMIC_ID", -1);
        chapterId = getIntent().getIntExtra("CHAPTER_ID", -1);

        Log.d("READER_DEBUG", "comicId = " + comicId);
        Log.d("READER_DEBUG", "chapterId = " + chapterId);

        if (chapterId == -1) {
            Toast.makeText(this, "Không lấy được CHAPTER_ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        appDatabase = AppDatabase.getInstance(this);
        comicRepository = new ComicRepository(this);

        bindViews();
        setupRecyclerView();
        setupDownloadButton();

        observeChapterPages();
        observeDownloadedStatus();
        saveReadingHistory();

        comicRepository.syncPagesByChapterId(chapterId);
    }

    private void bindViews() {
        recyclerViewPages = findViewById(R.id.recyclerViewPages);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnDownloadChapter = findViewById(R.id.btnDownloadChapter);

        layoutDownloadProgress = findViewById(R.id.layoutDownloadProgress);
        tvDownloadProgress = findViewById(R.id.tvDownloadProgress);
        progressDownload = findViewById(R.id.progressDownload);
    }

    private void setupRecyclerView() {
        readerPageAdapter = new ReaderPageAdapter(new ArrayList<>());
        recyclerViewPages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPages.setAdapter(readerPageAdapter);
    }

    private void setupDownloadButton() {
        btnDownloadChapter.setOnClickListener(v -> downloadCurrentChapter());
    }

    private void observeChapterPages() {
        comicRepository.getPagesByChapterId(chapterId)
                .observe(this, pages -> {
                    totalPages = pages == null ? 0 : pages.size();

                    Log.d("READER_DEBUG", "Số page trong Room = " + totalPages);

                    if (pages == null || pages.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        recyclerViewPages.setVisibility(View.GONE);
                    } else {
                        tvEmptyState.setVisibility(View.GONE);
                        recyclerViewPages.setVisibility(View.VISIBLE);
                        readerPageAdapter.setPageList(pages);
                    }
                });
    }

    private void observeDownloadedStatus() {
        appDatabase.downloadedChapterDao().getDownloadedChapter(chapterId)
                .observe(this, downloadedChapter -> {
                    if (isDownloading) {
                        return;
                    }

                    if (downloadedChapter == null) {
                        btnDownloadChapter.setText("Tải chương này");
                        btnDownloadChapter.setEnabled(true);
                    } else {
                        btnDownloadChapter.setText("Đã tải");
                        btnDownloadChapter.setEnabled(false);
                    }
                });
    }

    private void downloadCurrentChapter() {
        if (comicId == -1 || chapterId == -1) {
            Toast.makeText(this, "Không đủ dữ liệu để tải chương", Toast.LENGTH_SHORT).show();
            return;
        }

        if (totalPages <= 0) {
            Toast.makeText(this, "Chưa có dữ liệu trang để tải", Toast.LENGTH_SHORT).show();
            return;
        }

        OfflineDownloadManager downloadManager = new OfflineDownloadManager(this);

        downloadManager.downloadChapter(comicId, chapterId, new OfflineDownloadManager.DownloadCallback() {
            @Override
            public void onStart(int totalPages) {
                isDownloading = true;

                btnDownloadChapter.setEnabled(false);
                btnDownloadChapter.setText("Đang tải...");

                layoutDownloadProgress.setVisibility(View.VISIBLE);
                progressDownload.setMax(totalPages);
                progressDownload.setProgress(0);
                tvDownloadProgress.setText("Đang tải 0/" + totalPages);
            }

            @Override
            public void onProgress(int currentPage, int totalPages) {
                progressDownload.setProgress(currentPage);
                tvDownloadProgress.setText("Đang tải " + currentPage + "/" + totalPages);
            }

            @Override
            public void onSuccess(int totalPages) {
                isDownloading = false;

                layoutDownloadProgress.setVisibility(View.GONE);
                btnDownloadChapter.setText("Đã tải");
                btnDownloadChapter.setEnabled(false);

                Toast.makeText(ReaderActivity.this,
                        "Đã tải xong " + totalPages + " trang",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                isDownloading = false;

                layoutDownloadProgress.setVisibility(View.GONE);
                btnDownloadChapter.setText("Tải chương này");
                btnDownloadChapter.setEnabled(true);

                Toast.makeText(ReaderActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveReadingHistory() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ReadingHistory history = new ReadingHistory(
                    comicId,
                    chapterId,
                    1,
                    System.currentTimeMillis()
            );

            appDatabase.readingHistoryDao().insertOrUpdateHistory(history);
        });
    }
}