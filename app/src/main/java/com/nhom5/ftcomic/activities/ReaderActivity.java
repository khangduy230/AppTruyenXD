package com.nhom5.ftcomic.activities;

import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import com.nhom5.ftcomic.network.SupabaseApi;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.request.ReadingHistoryRequest;
import com.nhom5.ftcomic.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    private int currentPageNumber = 1;
    private int startPageNumber = 1;

    private final Handler historyHandler = new Handler(Looper.getMainLooper());
    private Runnable historyRunnable;
    private SessionManager sessionManager;
    private RecyclerView recyclerViewPages;
    private TextView tvEmptyState;
    private Button btnDownloadChapter;
    private Button btnPreviousChapter;
    private LinearLayout layoutDownloadProgress;
    private TextView tvDownloadProgress;
    private ProgressBar progressDownload;

    private AppBarLayout appBarLayout;
    private LinearLayout bottomBar;
    private boolean isUiVisible = true; // Cờ theo dõi trạng thái ẩn/hiện

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

        sessionManager = new SessionManager(this);

        comicId = getIntent().getIntExtra("COMIC_ID", -1);
        chapterId = getIntent().getIntExtra("CHAPTER_ID", -1);

        startPageNumber = getIntent().getIntExtra("PAGE_NUMBER", 1);
        currentPageNumber = Math.max(1, startPageNumber);

        Log.d("READER_DEBUG", "comicId = " + comicId);
        Log.d("READER_DEBUG", "chapterId = " + chapterId);
        Log.d("READER_DEBUG", "startPageNumber = " + startPageNumber);

        if (comicId <= 0 || chapterId <= 0) {
            Toast.makeText(
                    this,
                    "Không lấy được COMIC_ID hoặc CHAPTER_ID",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setNavigationOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        appDatabase = AppDatabase.getInstance(this);
        comicRepository = new ComicRepository(this);

        bindViews();
        setupRecyclerView();

        comicRepository.syncChaptersByComicId(comicId);
        comicRepository.syncPagesByChapterId(chapterId);

        observeChapterPages();
        observeDownloadedStatus();

        saveReadingHistory();

        comicRepository.getComicByIdLive(comicId).observe(this, comic -> {
            if (comic != null) {
                topAppBar.setTitle(comic.getName());
            }
        });

        comicRepository.getChaptersByComicId(comicId).observe(this, chapters -> {
            if (chapters != null && !chapters.isEmpty()) {
                allChaptersInComic = chapters;

                for (Chapter ch : chapters) {
                    if (ch.getId() == chapterId) {
                        currentChapterNumber = ch.getChapterNumber();
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

        appBarLayout.bringToFront();
        bottomBar.bringToFront();
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

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                if (layoutManager != null) {
                    int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();

                    if (firstVisiblePosition != RecyclerView.NO_POSITION) {
                        int newPageNumber = firstVisiblePosition + 1;

                        if (newPageNumber != currentPageNumber) {
                            currentPageNumber = newPageNumber;
                            scheduleSaveReadingHistory();
                        }
                    }
                }
            }
        });

        // 2. XỬ LÝ HIỆN/ẨN KHI BẤM VÀO MÀN HÌNH (Dùng GestureDetector)
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // Chỉ bắt sự kiện chạm 1 lần dứt khoát (không phải vuốt)
                toggleSystemUI();
                return super.onSingleTapConfirmed(e);
            }
        });

        recyclerViewPages.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // Bắt buộc trả về false để RecyclerView vẫn nhận được sự kiện cuộn (scroll)
        });
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
        if (allChaptersInComic == null || allChaptersInComic.isEmpty()) {
            Toast.makeText(this, "Chưa tải được danh sách chương", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Chapter ch : allChaptersInComic) {
            if (ch.getChapterNumber() == targetChapterNumber) {
                openChapter(ch.getId(), 1);
                return;
            }
        }

        Toast.makeText(this, "Không tìm thấy chương " + targetChapterNumber, Toast.LENGTH_SHORT).show();
    }

    private void hideSystemUI() {
        if (!isUiVisible) return; // Nếu đang ẩn rồi thì bỏ qua
        isUiVisible = false;

        // Tạo hiệu ứng trượt LÊN cho thanh Top
        Slide slideTop = new Slide(Gravity.TOP);
        slideTop.addTarget(appBarLayout);
        slideTop.setDuration(150); // Tốc độ trượt (ms)

        // Tạo hiệu ứng trượt XUỐNG cho thanh Bottom
        Slide slideBottom = new Slide(Gravity.BOTTOM);
        slideBottom.addTarget(bottomBar);
        slideBottom.setDuration(150);

        // Gộp 2 hiệu ứng lại
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(slideTop);
        transitionSet.addTransition(slideBottom);

        // Áp dụng hiệu ứng
        TransitionManager.beginDelayedTransition((ViewGroup) findViewById(android.R.id.content), transitionSet);
        appBarLayout.setVisibility(View.GONE);
        bottomBar.setVisibility(View.GONE);
    }

    private void showSystemUI() {
        if (isUiVisible) return; // Nếu đang hiện rồi thì bỏ qua
        isUiVisible = true;

        // Tương tự, tạo hiệu ứng trượt LÊN cho thanh Top khi xuất hiện
        Slide slideTop = new Slide(Gravity.TOP);
        slideTop.addTarget(appBarLayout);
        slideTop.setDuration(150);

        // Tạo hiệu ứng trượt XUỐNG cho thanh Bottom khi xuất hiện
        Slide slideBottom = new Slide(Gravity.BOTTOM);
        slideBottom.addTarget(bottomBar);
        slideBottom.setDuration(150);

        // Gộp 2 hiệu ứng lại
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(slideTop);
        transitionSet.addTransition(slideBottom);

        // Áp dụng hiệu ứng
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

                    Log.d("READER_DEBUG", "chapterId đang đọc = " + chapterId);
                    Log.d("READER_DEBUG", "Số page trong Room = " + totalPages);

                    if (pages == null || pages.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Chương này chưa có trang truyện");
                        recyclerViewPages.setVisibility(View.GONE);
                        return;
                    }

                    tvEmptyState.setVisibility(View.GONE);
                    recyclerViewPages.setVisibility(View.VISIBLE);

                    readerPageAdapter.setPageList(pages);

                    if (startPageNumber > 1 && startPageNumber <= pages.size()) {
                        recyclerViewPages.post(() -> {
                            LinearLayoutManager layoutManager =
                                    (LinearLayoutManager) recyclerViewPages.getLayoutManager();

                            if (layoutManager != null) {
                                layoutManager.scrollToPositionWithOffset(startPageNumber - 1, 0);
                            }
                        });
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
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        int safePageNumber = Math.max(1, currentPageNumber);

        ReadingHistoryRequest request = new ReadingHistoryRequest(
                userId,
                comicId,
                chapterId,
                safePageNumber
        );

        SupabaseApi api = SupabaseClient.getApi(this);

        api.saveReadingHistory(
                "resolution=merge-duplicates,return=minimal",
                "user_id,comic_id",
                request
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        ReadingHistory history = new ReadingHistory(
                                userId,
                                comicId,
                                chapterId,
                                safePageNumber,
                                System.currentTimeMillis()
                        );

                        appDatabase.readingHistoryDao().insertOrUpdateHistory(history);
                    });
                } else {
                    Log.e("READING_HISTORY", "Save failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("READING_HISTORY", "Network error: " + t.getMessage());
            }
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

            bottomSheetDialog.dismiss();

            if (selectedChapter == null || selectedChapter.getId() <= 0) {
                Toast.makeText(this, "Không tìm thấy CHAPTER_ID", Toast.LENGTH_SHORT).show();
                return;
            }

            openChapter(selectedChapter.getId(), 1);
        });

        bottomSheetDialog.show();
    }

    private void scheduleSaveReadingHistory() {
        if (historyRunnable != null) {
            historyHandler.removeCallbacks(historyRunnable);
        }

        historyRunnable = this::saveReadingHistory;
        historyHandler.postDelayed(historyRunnable, 800);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveReadingHistory();
    }

    @Override
    protected void onDestroy() {
        if (historyRunnable != null) {
            historyHandler.removeCallbacks(historyRunnable);
        }

        super.onDestroy();
    }

    private void openChapter(int targetChapterId, int targetPageNumber) {
        if (comicId <= 0) {
            Toast.makeText(this, "Không tìm thấy COMIC_ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (targetChapterId <= 0) {
            Toast.makeText(this, "Không tìm thấy CHAPTER_ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra("COMIC_ID", comicId);
        intent.putExtra("CHAPTER_ID", targetChapterId);
        intent.putExtra("PAGE_NUMBER", Math.max(1, targetPageNumber));

        startActivity(intent);
        finish();
    }
}