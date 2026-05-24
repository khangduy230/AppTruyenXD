package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ChapterAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.Favorite;
import com.nhom5.ftcomic.repository.ComicRepository;

import java.util.ArrayList;

public class DetailComicActivity extends AppCompatActivity {

    private ImageView imgCover;
    private TextView tvTitle, tvAuthor, tvDescription;
    private TextView tvLikeCount, tvRating, tvCommentCount;
    private Button btnSave, btnReadFirstChapter;
    private RecyclerView recyclerViewChapters;

    private AppDatabase appDatabase;
    private ComicRepository comicRepository;
    private ChapterAdapter chapterAdapter;

    private int comicId = -1;
    private boolean isFavorite = false;
    private int firstChapterId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_comic);

        comicId = getIntent().getIntExtra("COMIC_ID", -1);

        appDatabase = AppDatabase.getInstance(this);
        comicRepository = new ComicRepository(this);

        bindViews();
        setupChapterRecyclerView();
        observeComicDetail();
        observeChapters();
        observeFavoriteStatus();
        setupButtons();

        // Sync chapters từ Supabase về Room
        comicRepository.syncChaptersByComicId(comicId);
    }

    private void bindViews() {
        imgCover = findViewById(R.id.imgCover);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvDescription = findViewById(R.id.tvDescription);

        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvRating = findViewById(R.id.tvRating);
        tvCommentCount = findViewById(R.id.tvCommentCount);

        btnSave = findViewById(R.id.btnSave);
        btnReadFirstChapter = findViewById(R.id.btnReadFirstChapter);

        recyclerViewChapters = findViewById(R.id.recyclerViewChapters);
    }

    private void setupChapterRecyclerView() {
        chapterAdapter = new ChapterAdapter(new ArrayList<>(), chapter -> {
            openReaderActivity(chapter.getId());
        });

        recyclerViewChapters.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChapters.setNestedScrollingEnabled(false);
        recyclerViewChapters.setAdapter(chapterAdapter);
    }

    private void observeComicDetail() {
        comicRepository.getComicByIdLive(comicId).observe(this, comic -> {
            if (comic == null) {
                return;
            }

            if (comic.getCoverUrl() != null && !comic.getCoverUrl().isEmpty()) {
                Glide.with(this)
                        .load(comic.getCoverUrl())
                        .placeholder(comic.getImage())
                        .error(comic.getImage())
                        .into(imgCover);
            } else {
                imgCover.setImageResource(comic.getImage());
            }

            tvTitle.setText(comic.getName());
            tvAuthor.setText(comic.getAuthor());
            tvDescription.setText(comic.getDescription());

            tvLikeCount.setText(String.valueOf(comic.getLikeCount()));
            tvRating.setText(comic.getRating() + "*");
            tvCommentCount.setText(String.valueOf(comic.getCommentCount()));
        });
    }

    private void observeChapters() {
        comicRepository.getChaptersByComicId(comicId).observe(this, chapters -> {
            chapterAdapter.setChapterList(chapters);

            if (chapters != null && !chapters.isEmpty()) {
                firstChapterId = chapters.get(0).getId();
            }
        });
    }

    private void observeFavoriteStatus() {
        appDatabase.favoriteDao().isFavoriteLive(comicId).observe(this, count -> {
            isFavorite = count != null && count > 0;

            if (isFavorite) {
                btnSave.setText("Đã lưu");
            } else {
                btnSave.setText("Lưu vào thư viện");
            }
        });
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> toggleFavorite());

        btnReadFirstChapter.setOnClickListener(v -> {
            if (firstChapterId != -1) {
                openReaderActivity(firstChapterId);
            } else {
                Toast.makeText(this, "Truyện chưa có chương", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleFavorite() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (isFavorite) {
                appDatabase.favoriteDao().deleteFavoriteByComicId(comicId);
            } else {
                Favorite favorite = new Favorite(comicId, System.currentTimeMillis());
                appDatabase.favoriteDao().insertFavorite(favorite);
            }
        });
    }

    private void openReaderActivity(int chapterId) {
        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra("COMIC_ID", comicId);
        intent.putExtra("CHAPTER_ID", chapterId);
        startActivity(intent);
    }
}