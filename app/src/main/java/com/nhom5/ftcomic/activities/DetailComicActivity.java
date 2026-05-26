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
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ChapterAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.models.Favorite;
import com.nhom5.ftcomic.models.Rating;
import com.nhom5.ftcomic.repository.ComicRepository;

import java.util.ArrayList;

public class DetailComicActivity extends AppCompatActivity {
    private LinearLayout layoutRating;
    private Comic currentComic;
    private TextView tvLikeCount, tvRating, tvRatingCount, tvCommentCount;
    private Rating myOldRating = null;
    private ImageView imgCover;
    private TextView tvTitle, tvAuthor, tvDescription;
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
        MaterialButton btnBack = findViewById(R.id.btnBack);

        MaterialCardView cardComments = findViewById(R.id.cardComments);
        cardComments.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommentsActivity.class);

            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

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

        appDatabase.ratingDao().getUserRatingLive(comicId).observe(this, rating -> {
            myOldRating = rating;
        });
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

            this.currentComic = comic;

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

            // Số lượng bình luận đứng im độc lập, không bị ảnh hưởng khi đánh giá sao
            tvCommentCount.setText(String.valueOf(comic.getCommentCount()));

            tvRating.setText(comic.getRating() + "*");
            tvRatingCount.setText(comic.getRatingCount() + " đánh giá");
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

        // NÚT GỬI / CẬP NHẬT ĐÁNH GIÁ
        builder.setPositiveButton("Gửi", (dialog, which) -> {
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
                // TH 1: Đánh giá lần đầu -> Tăng số lượng đánh giá tổng lên 1
                finalTotalRatings = currentTotalRatings + 1;
                float totalStars = (currentRatingAverage * (float) currentTotalRatings) + newUserStars;
                finalRatingAverage = totalStars / (float) finalTotalRatings;

                Toast.makeText(this, "Cảm ơn bạn đã đánh giá " + (int)newUserStars + " sao!", Toast.LENGTH_SHORT).show();
            } else {
                // TH 2: Người dùng sửa lại số sao cũ -> Giữ nguyên số lượng đánh giá tổng
                finalTotalRatings = currentTotalRatings;
                float oldUserStars = myOldRating.getUserStars();

                float totalStars = (currentRatingAverage * (float) currentTotalRatings) - oldUserStars + newUserStars;
                finalRatingAverage = totalStars / (float) finalTotalRatings;

                Toast.makeText(this, "Đã cập nhật đánh giá thành " + (int)newUserStars + " sao!", Toast.LENGTH_SHORT).show();
            }

            // Làm tròn toán học lấy 1 chữ số sau dấu phẩy (Ví dụ: 4.56 -> 4.6)
            if (finalTotalRatings > 0) {
                finalRatingAverage = Math.round(finalRatingAverage * 10.0f) / 10.0f;
            } else {
                finalRatingAverage = 0.0f;
            }

            // Ghi nhận giá trị mới trực tiếp vào biến lưu trữ Đánh giá của truyện
            currentComic.setRating(finalRatingAverage);
            currentComic.setRatingCount(finalTotalRatings);

            AppDatabase.databaseWriteExecutor.execute(() -> {
                appDatabase.comicDao().updateComic(currentComic);
                appDatabase.ratingDao().insertOrUpdateRating(new Rating(comicId, newUserStars));
            });
        });

        // NÚT XÓA HẲN ĐÁNH GIÁ CŨ
        if (myOldRating != null) {
            builder.setNeutralButton("Xóa đánh giá", (dialog, which) -> {
                float currentRatingAverage = currentComic.getRating();
                int currentTotalRatings = currentComic.getRatingCount();

                int finalTotalRatings = currentTotalRatings - 1;
                float finalRatingAverage = 0.0f;

                if (finalTotalRatings > 0) {
                    float oldUserStars = myOldRating.getUserStars();
                    float totalStars = (currentRatingAverage * (float) currentTotalRatings) - oldUserStars;
                    finalRatingAverage = totalStars / (float) finalTotalRatings;
                    finalRatingAverage = Math.round(finalRatingAverage * 10.0f) / 10.0f;
                }

                currentComic.setRating(finalRatingAverage);
                currentComic.setRatingCount(finalTotalRatings); // Lùi số lượng đánh giá đi 1

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