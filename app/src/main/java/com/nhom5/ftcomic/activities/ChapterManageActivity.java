package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ChapterManageAdapter;
import com.nhom5.ftcomic.models.Chapter;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.response.ChapterResponse;
import com.nhom5.ftcomic.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChapterManageActivity extends AppCompatActivity {

    private RecyclerView rvChapters;
    private View layoutEmpty;
    private ChapterManageAdapter adapter;
    private SessionManager sessionManager;

    private final List<Chapter> chapterList = new ArrayList<>();
    private int comicId = -1;
    private String comicName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chapter_manage);

        sessionManager = new SessionManager(this);
        readIntentData();

        if (!canManageChapters()) {
            Toast.makeText(this, "Bạn không có quyền quản lý chương", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);
                return insets;
            });
        }

        initViews();
        setupToolbar();
        setupCreateButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchChapters();
    }

    private void readIntentData() {
        comicId = getIntent().getIntExtra("COMIC_ID", -1);
        if (comicId <= 0) {
            String comicIdText = getIntent().getStringExtra("COMIC_ID");
            if (comicIdText != null) {
                try {
                    comicId = Integer.parseInt(comicIdText);
                } catch (NumberFormatException ignored) {
                    comicId = -1;
                }
            }
        }

        comicName = getIntent().getStringExtra("COMIC_NAME");
        if (comicName == null) {
            comicName = "";
        }
    }

    private boolean canManageChapters() {
        if (comicId <= 0 || sessionManager == null || !sessionManager.isLoggedIn()) {
            return false;
        }

        String role = sessionManager.getRole();
        return "admin".equals(role) || "translator".equals(role);
    }

    private void initViews() {
        rvChapters = findViewById(R.id.rvChapters);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        adapter = new ChapterManageAdapter(chapterList, comicId, comicName);
        rvChapters.setLayoutManager(new LinearLayoutManager(this));
        rvChapters.setAdapter(adapter);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
            if (!comicName.trim().isEmpty()) {
                toolbar.setSubtitle(comicName);
            }
        }
    }

    private void setupCreateButton() {
        ExtendedFloatingActionButton fabCreateChapter = findViewById(R.id.fabCreateChapter);
        if (fabCreateChapter != null) {
            fabCreateChapter.setOnClickListener(v -> {
                Intent intent = new Intent(ChapterManageActivity.this, UpChapterActivity.class);
                intent.putExtra("COMIC_ID", comicId);
                intent.putExtra("COMIC_NAME", comicName);
                startActivity(intent);
            });
        }
    }

    private void fetchChapters() {
        if (comicId <= 0) {
            showEmpty(true);
            return;
        }

        SupabaseClient.getApi(this)
                .getChaptersByComicId("eq." + comicId, "chapter_number.asc")
                .enqueue(new Callback<List<ChapterResponse>>() {
                    @Override
                    public void onResponse(Call<List<ChapterResponse>> call,
                                           Response<List<ChapterResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            chapterList.clear();
                            for (ChapterResponse item : response.body()) {
                                chapterList.add(new Chapter(
                                        item.getId(),
                                        item.getComicId(),
                                        item.getChapterNumber(),
                                        item.getChapterName(),
                                        item.getUpdatedAt(),
                                        item.isHidden()
                                ));
                            }
                            adapter.notifyDataSetChanged();
                            showEmpty(chapterList.isEmpty());
                        } else {
                            Toast.makeText(
                                    ChapterManageActivity.this,
                                    "Không thể tải danh sách chương: " + response.code(),
                                    Toast.LENGTH_SHORT
                            ).show();
                            showEmpty(chapterList.isEmpty());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ChapterResponse>> call, Throwable t) {
                        Toast.makeText(
                                ChapterManageActivity.this,
                                "Lỗi kết nối máy chủ",
                                Toast.LENGTH_SHORT
                        ).show();
                        showEmpty(chapterList.isEmpty());
                    }
                });
    }

    private void showEmpty(boolean isEmpty) {
        if (layoutEmpty != null) {
            layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (rvChapters != null) {
            rvChapters.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }
}
