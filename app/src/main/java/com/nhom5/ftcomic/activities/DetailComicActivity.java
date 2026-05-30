package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ChapterAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.Category;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.models.Favorite;
import com.nhom5.ftcomic.models.Rating;
import com.nhom5.ftcomic.repository.ComicRepository;

import java.util.ArrayList;

public class DetailComicActivity extends AppCompatActivity {

    private LinearLayout layoutRating;
    private Comic currentComic;

    private TextView tvLikeCount, tvRating, tvRatingCount, tvCommentCount;
    private TextView tvTitle, tvAuthor, tvDescription;

    private ImageView imgCover;
    private Button btnSave, btnReadFirstChapter;
    private RecyclerView recyclerViewChapters;
    private ChipGroup chipGroupCategories;

    private AppDatabase appDatabase;
    private ComicRepository comicRepository;
    private ChapterAdapter chapterAdapter;

    private Rating myOldRating = null;

    private int comicId = -1;
    private boolean isFavorite = false;
    private int firstChapterId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_comic);

        comicId = getIntent().getIntExtra("COMIC_ID", -1);

        if (comicId == -1) {
            Toast.makeText(this, "Không lấy được COMIC_ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        appDatabase = AppDatabase.getInstance(this);
        comicRepository = new ComicRepository(this);

        bindViews();
        setupBackButton();
        setupCommentButton();
        setupChapterRecyclerView();
        setupButtons();

        observeComicDetail();
        observeChapters();
        observeCategories();
        observeFavoriteStatus();
        observeUserRating();

        comicRepository.syncChaptersByComicId(comicId);
        comicRepository.syncCategoriesByComicId(comicId);
    }

    private void bindViews() {
        imgCover = findViewById(R.id.imgCover);

        layoutRating = findViewById(R.id.layoutRating);

        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvDescription = findViewById(R.id.tvDescription);

        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvRating = findViewById(R.id.tvRating);
        tvRatingCount = findViewById(R.id.tvRatingCount);
        tvCommentCount = findViewById(R.id.tvCommentCount);

        btnSave = findViewById(R.id.btnSave);
        btnReadFirstChapter = findViewById(R.id.btnReadFirstChapter);

        recyclerViewChapters = findViewById(R.id.recyclerViewChapters);

        // Quan trọng: thiếu dòng này là nguyên nhân crash
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
    }

    private void setupBackButton() {
        MaterialButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void setupCommentButton() {
        MaterialCardView cardComments = findViewById(R.id.cardComments);

        cardComments.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommentsActivity.class);
            intent.putExtra("COMIC_ID", comicId);
            startActivity(intent);
        });
    }

    private void setupChapterRecyclerView() {
        chapterAdapter = new ChapterAdapter(new ArrayList<>(), chapter -> {
            openReaderActivity(chapter.getId());
        });

        recyclerViewChapters.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChapters.setNestedScrollingEnabled(false);
        recyclerViewChapters.setAdapter(chapterAdapter);
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> toggleFavorite());

        layoutRating.setOnClickListener(v -> {
            if (currentComic != null) {
                showRatingDialog();
            }
        });

        btnReadFirstChapter.setOnClickListener(v -> {
            if (firstChapterId != -1) {
                openReaderActivity(firstChapterId);
            } else {
                Toast.makeText(this, "Truyện chưa có chương", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeComicDetail() {
        comicRepository.getComicByIdLive(comicId).observe(this, comic -> {
            if (comic == null) {
                return;
            }

            currentComic = comic;

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
            tvCommentCount.setText(String.valueOf(comic.getCommentCount()));

            tvRating.setText(comic.getRating() + "*");
            tvRatingCount.setText(comic.getRatingCount() + " đánh giá");
        });
    }

    private void observeChapters() {
        comicRepository.getChaptersByComicId(comicId).observe(this, chapters -> {
            if (chapters == null) {
                chapterAdapter.setChapterList(new ArrayList<>());
                firstChapterId = -1;
                return;
            }

            chapterAdapter.setChapterList(chapters);

            if (!chapters.isEmpty()) {
                firstChapterId = chapters.get(0).getId();
            } else {
                firstChapterId = -1;
            }
        });
    }

    private void observeCategories() {
        comicRepository.getCategoriesByComicId(comicId).observe(this, categories -> {
            if (chipGroupCategories == null) {
                return;
            }

            chipGroupCategories.removeAllViews();

            if (categories == null || categories.isEmpty()) {
                Chip chip = new Chip(this);
                chip.setText("Đang cập nhật");
                chip.setClickable(false);
                chip.setCheckable(false);
                chipGroupCategories.addView(chip);
                return;
            }

            for (Category category : categories) {
                Chip chip = new Chip(this);
                chip.setText(category.getName());
                chip.setClickable(false);
                chip.setCheckable(false);
                chipGroupCategories.addView(chip);
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

    private void observeUserRating() {
        appDatabase.ratingDao().getUserRatingLive(comicId).observe(this, rating -> {
            myOldRating = rating;
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

    private void showRatingDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);

        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);

        if (myOldRating != null) {
            ratingBar.setRating(myOldRating.getUserStars());
        } else {
            ratingBar.setRating(0);
        }

        builder.setView(dialogView);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            if (currentComic == null) {
                return;
            }

            float newUserStars = ratingBar.getRating();

            if (newUserStars == 0) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 sao!", Toast.LENGTH_SHORT).show();
                return;
            }

            float currentRatingAverage = currentComic.getRating();
            int currentTotalRatings = currentComic.getRatingCount();

            int finalTotalRatings;
            float finalRatingAverage;

            if (myOldRating == null) {
                finalTotalRatings = currentTotalRatings + 1;

                float totalStars =
                        (currentRatingAverage * (float) currentTotalRatings) + newUserStars;

                finalRatingAverage = totalStars / (float) finalTotalRatings;

                Toast.makeText(this,
                        "Cảm ơn bạn đã đánh giá " + (int) newUserStars + " sao!",
                        Toast.LENGTH_SHORT).show();

            } else {
                finalTotalRatings = currentTotalRatings;

                float oldUserStars = myOldRating.getUserStars();

                float totalStars =
                        (currentRatingAverage * (float) currentTotalRatings)
                                - oldUserStars
                                + newUserStars;

                finalRatingAverage = totalStars / (float) finalTotalRatings;

                Toast.makeText(this,
                        "Đã cập nhật đánh giá thành " + (int) newUserStars + " sao!",
                        Toast.LENGTH_SHORT).show();
            }

            if (finalTotalRatings > 0) {
                finalRatingAverage = Math.round(finalRatingAverage * 10.0f) / 10.0f;
            } else {
                finalRatingAverage = 0.0f;
            }

            currentComic.setRating(finalRatingAverage);
            currentComic.setRatingCount(finalTotalRatings);

            AppDatabase.databaseWriteExecutor.execute(() -> {
                appDatabase.comicDao().updateComic(currentComic);
                appDatabase.ratingDao().insertOrUpdateRating(new Rating(comicId, newUserStars));
            });
        });

        if (myOldRating != null) {
            builder.setNeutralButton("Xóa đánh giá", (dialog, which) -> {
                if (currentComic == null) {
                    return;
                }

                float currentRatingAverage = currentComic.getRating();
                int currentTotalRatings = currentComic.getRatingCount();

                int finalTotalRatings = currentTotalRatings - 1;
                float finalRatingAverage = 0.0f;

                if (finalTotalRatings > 0) {
                    float oldUserStars = myOldRating.getUserStars();

                    float totalStars =
                            (currentRatingAverage * (float) currentTotalRatings) - oldUserStars;

                    finalRatingAverage = totalStars / (float) finalTotalRatings;
                    finalRatingAverage = Math.round(finalRatingAverage * 10.0f) / 10.0f;
                }

                currentComic.setRating(finalRatingAverage);
                currentComic.setRatingCount(finalTotalRatings);

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    appDatabase.comicDao().updateComic(currentComic);
                    appDatabase.ratingDao().deleteRatingByComicId(comicId);
                });

                Toast.makeText(this, "Đã hủy đánh giá của bạn!", Toast.LENGTH_SHORT).show();
            });
        }

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}