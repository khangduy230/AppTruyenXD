package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ComicAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.Comic;

import java.util.ArrayList;

public class DownloadedActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;
    private RecyclerView recyclerViewDownloaded;
    private TextView tvEmptyDownloaded;

    private ComicAdapter comicAdapter;
    private AppDatabase appDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloaded);

        appDatabase = AppDatabase.getInstance(this);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        observeDownloadedComics();
    }

    private void bindViews() {
        topAppBar = findViewById(R.id.topAppBar);
        recyclerViewDownloaded = findViewById(R.id.recyclerViewDownloaded);
        tvEmptyDownloaded = findViewById(R.id.tvEmptyDownloaded);
    }

    private void setupToolbar() {
        topAppBar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        comicAdapter = new ComicAdapter(new ArrayList<>(), comic -> openDetailComic(comic));

        recyclerViewDownloaded.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerViewDownloaded.setAdapter(comicAdapter);
    }

    private void observeDownloadedComics() {
        appDatabase.downloadedChapterDao().getDownloadedComics()
                .observe(this, comics -> {
                    if (comics == null || comics.isEmpty()) {
                        tvEmptyDownloaded.setVisibility(View.VISIBLE);
                        recyclerViewDownloaded.setVisibility(View.GONE);
                        comicAdapter.setComicList(new ArrayList<>());
                    } else {
                        tvEmptyDownloaded.setVisibility(View.GONE);
                        recyclerViewDownloaded.setVisibility(View.VISIBLE);
                        comicAdapter.setComicList(comics);
                    }
                });
    }

    private void openDetailComic(Comic comic) {
        Intent intent = new Intent(this, DetailComicActivity.class);
        intent.putExtra("COMIC_ID", comic.getId());
        startActivity(intent);
    }
}