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
import com.nhom5.ftcomic.models.Chapter;
import com.nhom5.ftcomic.models.DownloadedChapter;
import com.nhom5.ftcomic.models.ReadingHistory;
import com.nhom5.ftcomic.repository.ComicRepository;
import com.nhom5.ftcomic.utils.OfflineDownloadManager;

import java.util.ArrayList;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPages;
    private TextView tvEmptyState;
    private Button btnDownloadChapter;
    private Button btnPreviousChapter;
    private LinearLayout layoutDownloadProgress;
    private TextView tvDownloadProgress;
    private ProgressBar progressDownload;

    private ReaderPageAdapter readerPageAdapter;

    private AppDatabase appDatabase;
    private ComicRepository comicRepository;

    private int comicId = -1;
    private int chapterId = -1;
    private int totalPages = 0;

    private List<Chapter> allChaptersInComic = new ArrayList<>();
    private int currentChapterNumber = 1;

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
        // setupDownloadButton();

        observeChapterPages();
        observeDownloadedStatus();
        saveReadingHistory();

        comicRepository.syncPagesByChapterId(chapterId);

        // Lấy và quan sát toàn bộ chương của bộ truyện này từ SQLite (Room)
        comicRepository.getChaptersByComicId(comicId).observe(this, chapters -> {
            if (chapters != null && !chapters.isEmpty()) {
                allChaptersInComic = chapters; // Lưu danh sách chương vào biến toàn cục

                // Tìm số chương (chapterNumber) của chapterId hiện tại
                for (Chapter ch : chapters) {
                    if (ch.getId() == chapterId) {
                        currentChapterNumber = ch.getChapterNumber();
                        topAppBar.setSubtitle("Chương " + currentChapterNumber);
                        break;
                    }
                }

                // Logic ẩn/hiện nút "Chương trước" và "Chương sau" dựa trên số chương thực tế
                if (currentChapterNumber <= 1) {
                    btnPreviousChapter.setVisibility(View.GONE);
                } else {
                    btnPreviousChapter.setVisibility(View.VISIBLE);
                }

                // Tìm nút chương sau và ẩn đi nếu đang ở chương cuối cùng
                Button btnNextChapter = findViewById(R.id.btnNextChapter);
                if (currentChapterNumber >= chapters.size()) {
                    btnNextChapter.setVisibility(View.GONE);
                } else {
                    btnNextChapter.setVisibility(View.VISIBLE);
                    // Hiển thị text động theo số chương tiếp theo
                    btnNextChapter.setText("Chương " + (currentChapterNumber + 1));
                }
            }
        });

        // Thiết lập sự kiện click cho toàn bộ thanh điều hướng dưới
        setupBottomBarNavigation();


    }

    private void bindViews() {
        recyclerViewPages = findViewById(R.id.recyclerViewPages);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnDownloadChapter = findViewById(R.id.btnDownloadChapter);
        btnPreviousChapter = findViewById(R.id.btnPreviousChapter);

        layoutDownloadProgress = findViewById(R.id.layoutDownloadProgress);
        tvDownloadProgress = findViewById(R.id.tvDownloadProgress);
        progressDownload = findViewById(R.id.progressDownload);
    }

    private void setupRecyclerView() {
        readerPageAdapter = new ReaderPageAdapter(new ArrayList<>());
        recyclerViewPages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPages.setAdapter(readerPageAdapter);
    }

    private void setupBottomBarNavigation() {
        // 1. Nút tải chương cũ của bạn
        btnDownloadChapter.setOnClickListener(v -> downloadCurrentChapter());

        // 2. Xử lý nút Menu Danh sách chương
        findViewById(R.id.btnChapterList).setOnClickListener(v -> showChapterListBottomSheet());

        // 3. Xử lý nút bấm quay lại Chương trước
        btnPreviousChapter.setOnClickListener(v -> {
            int targetChapterNumber = currentChapterNumber - 1;
            navigateToChapterByNumber(targetChapterNumber);
        });

        // 4. Xử lý nút bấm tiến tới Chương sau
        findViewById(R.id.btnNextChapter).setOnClickListener(v -> {
            int targetChapterNumber = currentChapterNumber + 1;
            navigateToChapterByNumber(targetChapterNumber);
        });
    }

    // Hàm phụ trợ tìm ra ID của chương dựa trên số chương người dùng muốn tới để chuyển màn hình
    private void navigateToChapterByNumber(int targetChapterNumber) {
        for (Chapter ch : allChaptersInComic) {
            if (ch.getChapterNumber() == targetChapterNumber) {
                getIntent().putExtra("CHAPTER_ID", ch.getId());
                recreate(); // Khởi động lại màn hình đọc với ID chương mới
                break;
            }
        }
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
    private void showChapterListBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);

        View bottomSheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_chapters, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        android.widget.ListView listView = bottomSheetView.findViewById(R.id.listViewChapters);

        // Chuyển danh sách đối tượng Chapter từ DB thành chuỗi ký tự hiển thị lên giao diện
        ArrayList<String> displayNames = new ArrayList<>();
        for (Chapter ch : allChaptersInComic) {
            displayNames.add("Chương " + ch.getChapterNumber() + ": " + ch.getChapterName());
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, displayNames);
        listView.setAdapter(adapter);

        // Đẩy danh sách tự động cuộn đến vị trí chương hiện tại đang đọc cho tiện
        listView.setSelection(currentChapterNumber - 1);

        // Khi bấm vào một chương bất kỳ trong danh sách
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Chapter selectedChapter = allChaptersInComic.get(position);
            getIntent().putExtra("CHAPTER_ID", selectedChapter.getId());
            bottomSheetDialog.dismiss();
            recreate(); // Reload lại trang truyện theo chương vừa click
        });

        bottomSheetDialog.show();
    }
}