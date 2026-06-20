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

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ReaderPageAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.Chapter;
import com.nhom5.ftcomic.models.DownloadedChapter;
import com.nhom5.ftcomic.models.ReadingHistory;
import com.nhom5.ftcomic.repository.ComicRepository;
import com.nhom5.ftcomic.utils.OfflineDownloadManager;
import com.nhom5.ftcomic.utils.SessionManager;
import androidx.transition.TransitionManager;
import android.view.ViewGroup;
import android.view.GestureDetector;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.List;
import android.view.Gravity;
import androidx.transition.Slide;
import androidx.transition.TransitionSet;

public class ReaderActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPages;
    private TextView tvEmptyState;
    private Button btnDownloadChapter;
    private Button btnPreviousChapter;
    private LinearLayout layoutDownloadProgress;
    private TextView tvDownloadProgress;
    private ProgressBar progressDownload;

    // Nút "X" dùng để huỷ bỏ tiến trình tải chương
    private View btnCancelDownload;

    private AppBarLayout appBarLayout;
    private LinearLayout bottomBar;
    private boolean isUiVisible = true; // Cờ theo dõi trạng thái ẩn/hiện

    private ReaderPageAdapter readerPageAdapter;

    private AppDatabase appDatabase;
    private ComicRepository comicRepository;
    private SessionManager sessionManager;

    private int comicId = -1;
    private int chapterId = -1;
    private int totalPages = 0;

    private List<Chapter> allChaptersInComic = new ArrayList<>();
    private int currentChapterNumber = 1;
    // ✅ THÊM BIẾN LƯU TÊN CHƯƠNG ĐỂ CHUYỂN SANG ACTIVITY BÌNH LUẬN
    private String currentChapterName = "";

    private boolean isDownloading = false;
    // Quản lý tải xuống offline
    private OfflineDownloadManager downloadManager;

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

        if (chapterId == -1 || comicId == -1) {
            Toast.makeText(this, "Không lấy được thông tin truyện/chương", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        appDatabase = AppDatabase.getInstance(this);
        comicRepository = new ComicRepository(this);
        sessionManager = new SessionManager(this);

        bindViews();
        setupRecyclerView();

        observeChapterPages();
        observeDownloadedStatus();
        saveReadingHistory();

        comicRepository.syncPagesByChapterId(chapterId);

        // Lấy tên truyện gắn lên thanh toolbar
        comicRepository.getComicByIdLive(comicId).observe(this, comic -> {
            if (comic != null) {
                topAppBar.setTitle(comic.getName());
            }
        });

        // Lấy danh sách chương để xử lý logic chuyển chương
        comicRepository.getChaptersByComicId(comicId).observe(this, chapters -> {
            if (chapters != null && !chapters.isEmpty()) {
                allChaptersInComic = chapters;

                for (Chapter ch : chapters) {
                    if (ch.getId() == chapterId) {
                        currentChapterNumber = ch.getChapterNumber();
                        // ✅ LƯU LẠI TÊN CHƯƠNG HIỆN TẠI TỪ DATABASE
                        currentChapterName = ch.getChapterName();
                        topAppBar.setSubtitle("Chương " + currentChapterNumber);
                        break;
                    }
                }

                if (currentChapterNumber <= 1) {
                    btnPreviousChapter.setVisibility(View.GONE);
                } else {
                    btnPreviousChapter.setVisibility(View.VISIBLE);
                }

                Button btnNextChapter = findViewById(R.id.btnNextChapter);
                if (currentChapterNumber >= chapters.size()) {
                    btnNextChapter.setVisibility(View.GONE);
                } else {
                    btnNextChapter.setVisibility(View.VISIBLE);
                }
            }
        });

        setupBottomBarNavigation();
    }

    private void bindViews() {
        appBarLayout = findViewById(R.id.appBarLayout);
        bottomBar = findViewById(R.id.bottomBar);
        recyclerViewPages = findViewById(R.id.recyclerViewPages);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnDownloadChapter = findViewById(R.id.btnDownloadChapter);
        btnPreviousChapter = findViewById(R.id.btnPreviousChapter);

        layoutDownloadProgress = findViewById(R.id.layoutDownloadProgress);
        tvDownloadProgress = findViewById(R.id.tvDownloadProgress);
        progressDownload = findViewById(R.id.progressDownload);

        // Ánh xạ nút Huỷ tải từ layout
        btnCancelDownload = findViewById(R.id.btnCancelDownload);

        appBarLayout.bringToFront();
        bottomBar.bringToFront();

        // Gán sự kiện click cho nút huỷ tải
        if (btnCancelDownload != null) {
            btnCancelDownload.setOnClickListener(v -> {
                if (downloadManager != null) {
                    // Gọi lệnh dừng tải trong OfflineDownloadManager
                    downloadManager.cancel();
                }
            });
        }
    }

    private void setupRecyclerView() {
        readerPageAdapter = new ReaderPageAdapter(new ArrayList<>());
        recyclerViewPages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPages.setAdapter(readerPageAdapter);
        recyclerViewPages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (Math.abs(dy) > 5) {
                    hideSystemUI();
                }
            }
        });

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                toggleSystemUI();
                return super.onSingleTapConfirmed(e);
            }
        });

        recyclerViewPages.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
    }

    private void setupBottomBarNavigation() {
        btnDownloadChapter.setOnClickListener(v -> downloadCurrentChapter());
        findViewById(R.id.btnChapterList).setOnClickListener(v -> showChapterListBottomSheet());

        // ✅ THÊM SỰ KIỆN CLICK CHO NÚT BÌNH LUẬN TRONG BOTTOM BAR
        View btnCommentInChap = findViewById(R.id.btnCommentInChap); // Hãy chắc chắn ID này khớp với ID bạn đặt trong file XML activity_reader
        if (btnCommentInChap != null) {
            btnCommentInChap.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(ReaderActivity.this, CommentsActivity.class);
                intent.putExtra("COMIC_ID", comicId);
                intent.putExtra("CHAPTER_ID", chapterId);
                // Gửi text hiển thị dạng "Chương 1" hoặc tên cụ thể để gắn tag
                intent.putExtra("CHAPTER_NAME", "Chương " + currentChapterNumber);
                startActivity(intent);
            });
        }

        btnPreviousChapter.setOnClickListener(v -> {
            int targetChapterNumber = currentChapterNumber - 1;
            navigateToChapterByNumber(targetChapterNumber);
        });
        findViewById(R.id.btnNextChapter).setOnClickListener(v -> {
            int targetChapterNumber = currentChapterNumber + 1;
            navigateToChapterByNumber(targetChapterNumber);
        });
    }

    private void navigateToChapterByNumber(int targetChapterNumber) {
        for (Chapter ch : allChaptersInComic) {
            if (ch.getChapterNumber() == targetChapterNumber) {
                getIntent().putExtra("CHAPTER_ID", ch.getId());
                getIntent().putExtra("COMIC_ID", comicId);
                recreate();
                break;
            }
        }
    }

    private void hideSystemUI() {
        if (!isUiVisible) return;
        isUiVisible = false;

        Slide slideTop = new Slide(Gravity.TOP);
        slideTop.addTarget(appBarLayout);
        slideTop.setDuration(150);

        Slide slideBottom = new Slide(Gravity.BOTTOM);
        slideBottom.addTarget(bottomBar);
        slideBottom.setDuration(150);

        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(slideTop);
        transitionSet.addTransition(slideBottom);

        TransitionManager.beginDelayedTransition((ViewGroup) findViewById(android.R.id.content), transitionSet);
        appBarLayout.setVisibility(View.GONE);
        bottomBar.setVisibility(View.GONE);
    }

    private void showSystemUI() {
        if (isUiVisible) return;
        isUiVisible = true;

        Slide slideTop = new Slide(Gravity.TOP);
        slideTop.addTarget(appBarLayout);
        slideTop.setDuration(150);

        Slide slideBottom = new Slide(Gravity.BOTTOM);
        slideBottom.addTarget(bottomBar);
        slideBottom.setDuration(150);

        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(slideTop);
        transitionSet.addTransition(slideBottom);

        TransitionManager.beginDelayedTransition((ViewGroup) findViewById(android.R.id.content), transitionSet);
        appBarLayout.setVisibility(View.VISIBLE);
        bottomBar.setVisibility(View.VISIBLE);
    }

    private void toggleSystemUI() {
        if (isUiVisible) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
    }
    private void observeChapterPages() {
        comicRepository.getPagesByChapterId(chapterId)
                .observe(this, pages -> {
                    totalPages = pages == null ? 0 : pages.size();
                    Log.d("READER_DEBUG", "Total pages observed: " + totalPages);
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

    /**
     * Thực hiện tải chương hiện tại về máy để xem offline
     */
    private void downloadCurrentChapter() {
        if (comicId == -1 || chapterId == -1) {
            Toast.makeText(this, "Không đủ dữ liệu để tải chương", Toast.LENGTH_SHORT).show();
            return;
        }

        if (totalPages <= 0) {
            Toast.makeText(this, "Chưa có dữ liệu trang để tải", Toast.LENGTH_SHORT).show();
            return;
        }

        downloadManager = new OfflineDownloadManager(this);

        downloadManager.downloadChapter(comicId, chapterId, new OfflineDownloadManager.DownloadCallback() {
            @Override
            public void onStart(int totalPages) {
                isDownloading = true;

                btnDownloadChapter.setEnabled(false);
                btnDownloadChapter.setText("Đang tải...");

                // Hiển thị thanh tiến trình tải
                layoutDownloadProgress.setVisibility(View.VISIBLE);
                progressDownload.setMax(totalPages);
                progressDownload.setProgress(0);
                tvDownloadProgress.setText("Đang tải 0/" + totalPages);
            }

            @Override
            public void onProgress(int currentPage, int totalPages) {
                // Cập nhật số lượng trang đã tải xong
                progressDownload.setProgress(currentPage);
                tvDownloadProgress.setText("Đang tải " + currentPage + "/" + totalPages);
            }

            @Override
            public void onSuccess(int totalPages) {
                isDownloading = false;

                // Ẩn thanh tiến trình khi hoàn tất
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

            /**
             * Callback được gọi khi người dùng chủ động nhấn nút huỷ (X)
             */
            @Override
            public void onCancel() {
                isDownloading = false;
                // Ẩn giao diện tải và khôi phục nút Tải chương
                layoutDownloadProgress.setVisibility(View.GONE);
                btnDownloadChapter.setText("Tải chương này");
                btnDownloadChapter.setEnabled(true);
                Toast.makeText(ReaderActivity.this, "Đã huỷ tải", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveReadingHistory() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            userId = "guest";
        }
        final String finalUserId = userId;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ReadingHistory history = new ReadingHistory(
                    finalUserId,
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

        ArrayList<String> displayNames = new ArrayList<>();
        for (Chapter ch : allChaptersInComic) {
            displayNames.add("Chương " + ch.getChapterNumber() + ": " + ch.getChapterName());
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, displayNames);
        listView.setAdapter(adapter);
        listView.setSelection(currentChapterNumber - 1);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Chapter selectedChapter = allChaptersInComic.get(position);
            getIntent().putExtra("CHAPTER_ID", selectedChapter.getId());
            getIntent().putExtra("COMIC_ID", comicId);
            bottomSheetDialog.dismiss();
            recreate();
        });

        bottomSheetDialog.show();
    }
}