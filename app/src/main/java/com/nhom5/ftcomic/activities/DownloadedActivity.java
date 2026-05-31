package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.DownloadedAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.Comic;

import java.util.ArrayList;
import java.util.Set;

public class DownloadedActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;
    private RecyclerView recyclerViewDownloaded;
    private TextView tvEmptyDownloaded;
    private Button btnDelete;

    private DownloadedAdapter downloadedAdapter;
    private AppDatabase appDatabase;
    private boolean isEditMode = false;
    private OnBackPressedCallback onBackPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloaded);

        appDatabase = AppDatabase.getInstance(this);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupDeleteButton();
        observeDownloadedComics();
        setupBackCallback();
    }

    private void setupBackCallback() {
        onBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (isEditMode) {
                    exitEditMode();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    private void bindViews() {
        topAppBar = findViewById(R.id.topAppBar);
        recyclerViewDownloaded = findViewById(R.id.recyclerViewDownloaded);
        tvEmptyDownloaded = findViewById(R.id.tvEmptyDownloaded);
        btnDelete = findViewById(R.id.btnDelete);
    }

    private void setupToolbar() {
        topAppBar.setNavigationOnClickListener(v -> {
            if (isEditMode) {
                exitEditMode();
            } else {
                finish();
            }
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
                if (isEditMode) {
                    btnDelete.setText("Xoá (" + count + ")");
                    btnDelete.setEnabled(count > 0);
                }
            }
        });

        // Sử dụng LinearLayoutManager vì item_download.xml hiển thị theo dạng danh sách dòng
        recyclerViewDownloaded.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDownloaded.setAdapter(downloadedAdapter);
    }

    private void setupDeleteButton() {
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                if (!isEditMode) {
                    enterEditMode();
                } else {
                    performDelete();
                }
            });
        }
    }

    private void enterEditMode() {
        isEditMode = true;
        if (onBackPressedCallback != null) {
            onBackPressedCallback.setEnabled(true);
        }
        downloadedAdapter.setSelectionMode(true);
        if (btnDelete != null) {
            btnDelete.setText("Xoá (0)");
            btnDelete.setEnabled(false);
        }
    }

    private void exitEditMode() {
        isEditMode = false;
        if (onBackPressedCallback != null) {
            onBackPressedCallback.setEnabled(false);
        }
        downloadedAdapter.setSelectionMode(false);
        if (btnDelete != null) {
            btnDelete.setText("Xoá truyện");
            btnDelete.setEnabled(true);
        }
    }

    private void performDelete() {
        Set<Integer> selectedIds = downloadedAdapter.getSelectedIds();
        if (selectedIds.isEmpty()) return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            for (Integer comicId : selectedIds) {
                appDatabase.downloadedChapterDao().deleteChaptersByComicId(comicId);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Đã xoá " + selectedIds.size() + " truyện", Toast.LENGTH_SHORT).show();
                exitEditMode();
            });
        });
    }

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
                        downloadedAdapter.setComicList(comics);
                    }
                });
    }

    private void openDetailComic(Comic comic) {
        Intent intent = new Intent(this, DetailComicActivity.class);
        intent.putExtra("COMIC_ID", comic.getId());
        startActivity(intent);
    }
}
