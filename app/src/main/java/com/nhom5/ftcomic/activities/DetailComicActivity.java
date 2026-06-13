package com.nhom5.ftcomic.activities;

import androidx.lifecycle.LiveData;


import com.nhom5.ftcomic.network.request.RatingRequest;
import com.nhom5.ftcomic.network.response.RatingResponse;

import com.nhom5.ftcomic.network.request.LikeRequest;
import com.nhom5.ftcomic.network.response.LikeResponse;

import android.content.ActivityNotFoundException;
import com.nhom5.ftcomic.fragments.LoginFragment;
import com.nhom5.ftcomic.network.SupabaseApi;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.request.FavoriteRequest;
import com.nhom5.ftcomic.network.response.FavoriteResponse;
import com.nhom5.ftcomic.utils.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class DetailComicActivity extends AppCompatActivity {


    private SessionManager sessionManager;
    private LiveData<Integer> favoriteLiveData;
    private LinearLayout layoutRating;
    private Comic currentComic;

    private TextView tvLikeCount, tvRating, tvRatingCount, tvCommentCount;
    private TextView tvTitle, tvAuthor, tvDescription;

    private ImageView imgCover;
    private ImageView imgBlurredBackground;
    private Button btnSave, btnReadFirstChapter;
    private RecyclerView recyclerViewChapters;
    private ChipGroup chipGroupCategories;

    private AppDatabase appDatabase;
    private ComicRepository comicRepository;
    private ChapterAdapter chapterAdapter;

    private Rating myOldRating = null;

    private int comicId = -1;
    private boolean isFavorite = false;
    private boolean isLiked = false;
    private boolean isLikeRequestRunning = false;
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
        sessionManager = new SessionManager(this);

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
        syncMyLikeFromSupabase();

        comicRepository.syncChaptersByComicId(comicId);
        comicRepository.syncCategoriesByComicId(comicId);
    }

    private void bindViews() {
        imgCover = findViewById(R.id.imgCover);
        imgBlurredBackground = findViewById(R.id.imgBlurredBackground);

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

        chipGroupCategories = findViewById(R.id.chipGroupCategories);

        // Hiệu ứng các thứ giữa Home và Detail
        com.google.android.material.card.MaterialCardView cardCover = findViewById(R.id.cardCover);
        String transitionName = getIntent().getStringExtra("TRANSITION_NAME");

        if (transitionName != null) {
            androidx.core.view.ViewCompat.setTransitionName(cardCover, transitionName);        }
        // Chờ ảnh load xong mới transition để tránh bị bựa
        supportPostponeEnterTransition();
        String comicCoverUrl = getIntent().getStringExtra("COMIC_COVER_URL");
        if (comicCoverUrl != null && !comicCoverUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(comicCoverUrl)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)

                    .dontAnimate()
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {

                            supportStartPostponedEnterTransition();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            supportStartPostponedEnterTransition();
                            return false;
                        }
                    })
                    .into(imgCover);
        } else {
            supportStartPostponedEnterTransition();
        }

        // Android 12 trở lên thì blur cover, dưới thì dẹp :v
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ImageView imgBackground = findViewById(R.id.imgBlurredBackground);
            imgBackground.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(35f, 35f, android.graphics.Shader.TileMode.CLAMP));
        }
        // Thêm padding để tránh bị đè
        View main = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(main, (v, windowInsets) -> {
            var insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            var backParams = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) findViewById(R.id.btnBack).getLayoutParams();
            backParams.topMargin = insets.top + (int)(10 * getResources().getDisplayMetrics().density);
            findViewById(R.id.btnBack).setLayoutParams(backParams);

            var readParams = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) btnReadFirstChapter.getLayoutParams();
            readParams.bottomMargin = insets.bottom + (int)(16 * getResources().getDisplayMetrics().density);
            btnReadFirstChapter.setLayoutParams(readParams);

            var scrollView = findViewById(R.id.NestedView1);
            scrollView.setPadding(0, 0, 0, insets.bottom);

            return WindowInsetsCompat.CONSUMED;
        });
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
    /*
      Nút này chỉ dùng để LƯU TRUYỆN / ĐỌC SAU.
      Không liên quan đến like.
    */
        btnSave.setOnClickListener(v -> toggleFavorite());

    /*
      Ô lượt thích / số tim dùng để LIKE truyện.
      Không gọi toggleFavorite nữa.
    */
        if (tvLikeCount != null) {
            tvLikeCount.setClickable(true);
            tvLikeCount.setOnClickListener(v -> toggleLike());
        }

    /*
      Trường hợp layout XML có ô/card chứa tim,
      bạn thêm android:id="@+id/layoutLikeAction" vào XML rồi dùng đoạn này.
      Nếu chưa có id này thì code vẫn chạy bình thường.
    */
        View layoutLikeAction = findViewById(R.id.layoutLikeAction);

        if (layoutLikeAction != null) {
            layoutLikeAction.setClickable(true);
            layoutLikeAction.setOnClickListener(v -> toggleLike());
        }

        View btnShare = findViewById(R.id.btnShare);

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> shareComic());
        }

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
                        .into(imgBlurredBackground);
            } else {
                imgBlurredBackground.setImageResource(comic.getImage());

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
        if (favoriteLiveData != null) {
            favoriteLiveData.removeObservers(this);
        }

        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            isFavorite = false;
            btnSave.setText("Lưu vào thư viện");
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            isFavorite = false;
            btnSave.setText("Lưu vào thư viện");
            return;
        }

        favoriteLiveData = appDatabase.favoriteDao().isFavoriteLive(userId, comicId);

        favoriteLiveData.observe(this, count -> {
            isFavorite = count != null && count > 0;

            if (isFavorite) {
                btnSave.setText("Đã lưu");
            } else {
                btnSave.setText("Lưu vào thư viện");
            }
        });
    }

    private void observeUserRating() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            myOldRating = null;
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            myOldRating = null;
            return;
        }

        appDatabase.ratingDao()
                .getUserRatingLive(userId, comicId)
                .observe(this, rating -> myOldRating = rating);

        syncMyRatingFromSupabase();
    }

    private void toggleFavorite() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            openLoginThen(() -> {
                observeFavoriteStatus();
                addFavoriteToSupabase();
            });
            return;
        }

        if (isFavorite) {
            deleteFavoriteFromSupabase();
        } else {
            addFavoriteToSupabase();
        }
    }

    private void openReaderActivity(int targetChapterId) {
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
        intent.putExtra("PAGE_NUMBER", 1);
        startActivity(intent);
    }

    private void showRatingDialog() {
        if (currentComic == null) {
            Toast.makeText(this, "Chưa có dữ liệu truyện", Toast.LENGTH_SHORT).show();
            return;
        }

        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            openLoginThen(() -> {
                observeUserRating();
                showRatingDialog();
            });
            return;
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);

        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);

        ratingBar.setStepSize(1.0f);

        if (myOldRating != null) {
            ratingBar.setRating(myOldRating.getUserStars());
        } else {
            ratingBar.setRating(0);
        }

        builder.setView(dialogView);

        builder.setPositiveButton("Gửi", (dialog, which) -> {
            int newStars = Math.round(ratingBar.getRating());

            if (newStars <= 0) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 sao!", Toast.LENGTH_SHORT).show();
                return;
            }

            submitRatingToSupabase(newStars);
        });

        if (myOldRating != null) {
            builder.setNeutralButton("Xóa đánh giá", (dialog, which) -> {
                deleteRatingFromSupabase();
            });
        }

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        observeFavoriteStatus();

        if (sessionManager != null && sessionManager.isLoggedIn()) {
            syncFavoritesFromSupabase();
            syncMyLikeFromSupabase();
        } else {
            isFavorite = false;
            isLiked = false;

            if (btnSave != null) {
                btnSave.setText("Lưu vào thư viện");
                btnSave.setEnabled(true);
            }
        }
    }

    private void addFavoriteToSupabase() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Bạn cần đăng nhập để lưu truyện", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comicId <= 0) {
            Toast.makeText(this, "Không tìm thấy COMIC_ID", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        SupabaseApi api = SupabaseClient.getApi(this);
        FavoriteRequest request = new FavoriteRequest(userId, comicId);

        api.addFavorite("return=minimal", request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                android.util.Log.d("FAVORITE_SUPABASE", "ADD URL = " + call.request().url());
                android.util.Log.d("FAVORITE_SUPABASE", "ADD CODE = " + response.code());

                btnSave.setEnabled(true);

                if (response.isSuccessful() || response.code() == 409) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        appDatabase.favoriteDao().insertFavorite(
                                new Favorite(userId, comicId, System.currentTimeMillis())
                        );
                    });

                    isFavorite = true;

                    runOnUiThread(() -> {
                        btnSave.setText("Đã lưu");
                    });

                    Toast.makeText(
                            DetailComicActivity.this,
                            "Đã lưu vào thư viện",
                            Toast.LENGTH_SHORT
                    ).show();

                    syncFavoritesFromSupabase();

                } else {
                    try {
                        String error = response.errorBody() != null
                                ? response.errorBody().string()
                                : "Không có errorBody";

                        android.util.Log.e("FAVORITE_SUPABASE", "ADD ERROR CODE = " + response.code());
                        android.util.Log.e("FAVORITE_SUPABASE", "ADD ERROR BODY = " + error);
                    } catch (Exception e) {
                        android.util.Log.e("FAVORITE_SUPABASE", "Không đọc được errorBody", e);
                    }

                    Toast.makeText(
                            DetailComicActivity.this,
                            "Lưu truyện thất bại: " + response.code(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSave.setEnabled(true);

                android.util.Log.e("FAVORITE_SUPABASE", "ADD FAIL = " + t.getMessage(), t);

                Toast.makeText(
                        DetailComicActivity.this,
                        "Lỗi mạng khi lưu truyện: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void deleteFavoriteFromSupabase() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Bạn cần đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comicId <= 0) {
            Toast.makeText(this, "Không tìm thấy COMIC_ID", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        SupabaseApi api = SupabaseClient.getApi(this);

        api.deleteFavorite(
                "eq." + userId,
                "eq." + comicId
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                android.util.Log.d("FAVORITE_SUPABASE", "DELETE URL = " + call.request().url());
                android.util.Log.d("FAVORITE_SUPABASE", "DELETE CODE = " + response.code());

                btnSave.setEnabled(true);

                if (response.isSuccessful()) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        appDatabase.favoriteDao().deleteFavoriteByComicId(userId, comicId);
                    });

                    isFavorite = false;

                    runOnUiThread(() -> {
                        btnSave.setText("Lưu vào thư viện");
                    });

                    Toast.makeText(
                            DetailComicActivity.this,
                            "Đã bỏ lưu khỏi thư viện",
                            Toast.LENGTH_SHORT
                    ).show();

                    syncFavoritesFromSupabase();

                } else {
                    try {
                        String error = response.errorBody() != null
                                ? response.errorBody().string()
                                : "Không có errorBody";

                        android.util.Log.e("FAVORITE_SUPABASE", "DELETE ERROR CODE = " + response.code());
                        android.util.Log.e("FAVORITE_SUPABASE", "DELETE ERROR BODY = " + error);
                    } catch (Exception e) {
                        android.util.Log.e("FAVORITE_SUPABASE", "Không đọc được errorBody", e);
                    }

                    Toast.makeText(
                            DetailComicActivity.this,
                            "Bỏ lưu thất bại: " + response.code(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSave.setEnabled(true);

                android.util.Log.e("FAVORITE_SUPABASE", "DELETE FAIL = " + t.getMessage(), t);

                Toast.makeText(
                        DetailComicActivity.this,
                        "Lỗi mạng khi bỏ lưu: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void syncFavoritesFromSupabase() {
        if (!sessionManager.isLoggedIn()) {
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        SupabaseApi api = SupabaseClient.getApi(this);

        api.getMyFavorites(
                "eq." + userId,
                "created_at.desc"
        ).enqueue(new Callback<List<FavoriteResponse>>() {
            @Override
            public void onResponse(Call<List<FavoriteResponse>> call, Response<List<FavoriteResponse>> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                List<FavoriteResponse> remoteFavorites = response.body();

                if (remoteFavorites == null) {
                    remoteFavorites = new java.util.ArrayList<>();
                }

                java.util.List<Favorite> localFavorites = new java.util.ArrayList<>();

                for (FavoriteResponse item : remoteFavorites) {
                    localFavorites.add(
                            new Favorite(
                                    userId,
                                    item.getComicId(),
                                    item.getCreatedAtMillis()
                            )
                    );
                }

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    appDatabase.favoriteDao().deleteFavoritesByUser(userId);
                    appDatabase.favoriteDao().insertFavorites(localFavorites);
                });
            }

            @Override
            public void onFailure(Call<List<FavoriteResponse>> call, Throwable t) {
                // Không spam Toast ở đây
            }
        });
    }

    private void openLoginThen(Runnable afterLogin) {
        LoginFragment loginFragment = new LoginFragment();
        loginFragment.show(getSupportFragmentManager(), "LoginFragment");

        getSupportFragmentManager().setFragmentResultListener(
                "key_dang_nhap",
                this,
                (requestKey, result) -> afterLogin.run()
        );
    }

    private void shareComic() {
        if (currentComic == null) {
            Toast.makeText(this, "Chưa có dữ liệu truyện để chia sẻ", Toast.LENGTH_SHORT).show();
            return;
        }

        String shareText =
                "Mình đang đọc truyện: " + currentComic.getName() + "\n"
                        + "Tác giả: " + currentComic.getAuthor() + "\n"
                        + "Trạng thái: " + currentComic.getStatus() + "\n\n"
                        + "Mở app FTComic để đọc truyện này nhé!";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentComic.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        try {
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ truyện"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Không tìm thấy ứng dụng để chia sẻ", Toast.LENGTH_SHORT).show();
        }
    }

    private void syncMyRatingFromSupabase() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        SupabaseApi api = SupabaseClient.getApi(this);

        api.getMyRating(
                "eq." + userId,
                "eq." + comicId,
                1
        ).enqueue(new Callback<List<RatingResponse>>() {
            @Override
            public void onResponse(Call<List<RatingResponse>> call,
                                   Response<List<RatingResponse>> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                List<RatingResponse> remoteRatings = response.body();

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    if (remoteRatings == null || remoteRatings.isEmpty()) {
                        appDatabase.ratingDao().deleteRatingByComicId(userId, comicId);
                    } else {
                        RatingResponse item = remoteRatings.get(0);

                        appDatabase.ratingDao().insertOrUpdateRating(
                                new Rating(
                                        userId,
                                        item.getComicId(),
                                        item.getRating()
                                )
                        );
                    }
                });
            }

            @Override
            public void onFailure(Call<List<RatingResponse>> call, Throwable t) {
                // Không spam Toast
            }
        });
    }

    private void submitRatingToSupabase(int stars) {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Bạn cần đăng nhập để đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        SupabaseApi api = SupabaseClient.getApi(this);

        RatingRequest request = new RatingRequest(userId, comicId, stars);

        api.upsertRating(
                "resolution=merge-duplicates,return=representation",
                "user_id,comic_id",
                request
        ).enqueue(new Callback<List<RatingResponse>>() {
            @Override
            public void onResponse(Call<List<RatingResponse>> call, Response<List<RatingResponse>> response) {
                android.util.Log.d("RATING_SUPABASE", "URL = " + call.request().url());
                android.util.Log.d("RATING_SUPABASE", "CODE = " + response.code());

                if (response.isSuccessful()) {
                    List<RatingResponse> result = response.body();

                    if (result == null || result.isEmpty()) {
                        android.util.Log.e("RATING_SUPABASE", "Supabase success nhưng không trả row rating");
                        Toast.makeText(
                                DetailComicActivity.this,
                                "Supabase chưa trả dữ liệu đánh giá",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    RatingResponse savedRating = result.get(0);

                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        appDatabase.ratingDao().insertOrUpdateRating(
                                new Rating(
                                        userId,
                                        savedRating.getComicId(),
                                        savedRating.getRating()
                                )
                        );
                    });

                    Toast.makeText(
                            DetailComicActivity.this,
                            "Đã đánh giá " + savedRating.getRating() + " sao",
                            Toast.LENGTH_SHORT
                    ).show();

                    syncMyRatingFromSupabase();

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (comicRepository != null) {
                            comicRepository.syncComicById(comicId);
                            comicRepository.syncAllHomeComics();
                        }
                    }, 800);

                } else {
                    try {
                        String error = response.errorBody() != null
                                ? response.errorBody().string()
                                : "Không có errorBody";

                        android.util.Log.e("RATING_SUPABASE", "ERROR = " + error);

                        Toast.makeText(
                                DetailComicActivity.this,
                                "Gửi đánh giá thất bại: " + response.code(),
                                Toast.LENGTH_SHORT
                        ).show();

                    } catch (Exception e) {
                        android.util.Log.e("RATING_SUPABASE", "Không đọc được errorBody", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<RatingResponse>> call, Throwable t) {
                android.util.Log.e("RATING_SUPABASE", "FAIL = " + t.getMessage(), t);

                Toast.makeText(
                        DetailComicActivity.this,
                        "Lỗi mạng khi gửi đánh giá: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void deleteRatingFromSupabase() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        SupabaseApi api = SupabaseClient.getApi(this);

        api.deleteRating(
                "eq." + userId,
                "eq." + comicId
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        appDatabase.ratingDao().deleteRatingByComicId(userId, comicId);
                    });

                    myOldRating = null;

                    Toast.makeText(
                            DetailComicActivity.this,
                            "Đã xóa đánh giá",
                            Toast.LENGTH_SHORT
                    ).show();

                    syncMyRatingFromSupabase();

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (comicRepository != null) {
                            comicRepository.syncComicById(comicId);
                            comicRepository.syncAllHomeComics();
                        }
                    }, 800);
                } else {
                    Toast.makeText(
                            DetailComicActivity.this,
                            "Xóa đánh giá thất bại: " + response.code(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(
                        DetailComicActivity.this,
                        "Lỗi mạng khi xóa đánh giá: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void toggleLike() {
        if (isLikeRequestRunning) {
            return;
        }

        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            openLoginThen(() -> {
                syncMyLikeFromSupabase();
                addLikeToSupabase();
            });
            return;
        }

        if (isLiked) {
            deleteLikeFromSupabase();
        } else {
            addLikeToSupabase();
        }
    }

    private void syncMyLikeFromSupabase() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            isLiked = false;
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            isLiked = false;
            return;
        }

        SupabaseApi api = SupabaseClient.getApi(this);

        api.getMyLike(
                "eq." + userId,
                "eq." + comicId,
                1
        ).enqueue(new Callback<List<LikeResponse>>() {
            @Override
            public void onResponse(Call<List<LikeResponse>> call, Response<List<LikeResponse>> response) {
                android.util.Log.d("LIKE_SUPABASE", "GET URL = " + call.request().url());
                android.util.Log.d("LIKE_SUPABASE", "GET CODE = " + response.code());

                if (response.isSuccessful()) {
                    List<LikeResponse> likes = response.body();
                    isLiked = likes != null && !likes.isEmpty();
                } else {
                    isLiked = false;

                    try {
                        String error = response.errorBody() != null
                                ? response.errorBody().string()
                                : "Không có errorBody";

                        android.util.Log.e("LIKE_SUPABASE", "GET ERROR = " + error);
                    } catch (Exception e) {
                        android.util.Log.e("LIKE_SUPABASE", "Không đọc được errorBody", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<LikeResponse>> call, Throwable t) {
                isLiked = false;
                android.util.Log.e("LIKE_SUPABASE", "GET FAIL = " + t.getMessage(), t);
            }
        });
    }

    private void addLikeToSupabase() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Bạn cần đăng nhập để thích truyện", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comicId <= 0) {
            Toast.makeText(this, "Không tìm thấy COMIC_ID", Toast.LENGTH_SHORT).show();
            return;
        }

        isLikeRequestRunning = true;

        SupabaseApi api = SupabaseClient.getApi(this);
        LikeRequest request = new LikeRequest(userId, comicId);

        api.addLike("return=minimal", request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLikeRequestRunning = false;

                android.util.Log.d("LIKE_SUPABASE", "ADD URL = " + call.request().url());
                android.util.Log.d("LIKE_SUPABASE", "ADD CODE = " + response.code());

                if (response.isSuccessful()) {
                    isLiked = true;

                    Toast.makeText(
                            DetailComicActivity.this,
                            "Đã thích truyện",
                            Toast.LENGTH_SHORT
                    ).show();

                    syncMyLikeFromSupabase();
                    refreshComicAfterLikeChanged();

                } else if (response.code() == 409) {
                    isLiked = true;

                    Toast.makeText(
                            DetailComicActivity.this,
                            "Bạn đã thích truyện này rồi",
                            Toast.LENGTH_SHORT
                    ).show();

                    syncMyLikeFromSupabase();
                    refreshComicAfterLikeChanged();

                } else {
                    try {
                        String error = response.errorBody() != null
                                ? response.errorBody().string()
                                : "Không có errorBody";

                        android.util.Log.e("LIKE_SUPABASE", "ADD ERROR CODE = " + response.code());
                        android.util.Log.e("LIKE_SUPABASE", "ADD ERROR BODY = " + error);
                    } catch (Exception e) {
                        android.util.Log.e("LIKE_SUPABASE", "Không đọc được errorBody", e);
                    }

                    Toast.makeText(
                            DetailComicActivity.this,
                            "Thích truyện thất bại: " + response.code(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLikeRequestRunning = false;

                android.util.Log.e("LIKE_SUPABASE", "ADD FAIL = " + t.getMessage(), t);

                Toast.makeText(
                        DetailComicActivity.this,
                        "Lỗi mạng khi thích truyện: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void deleteLikeFromSupabase() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Bạn cần đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comicId <= 0) {
            Toast.makeText(this, "Không tìm thấy COMIC_ID", Toast.LENGTH_SHORT).show();
            return;
        }

        isLikeRequestRunning = true;

        SupabaseApi api = SupabaseClient.getApi(this);

        api.deleteLike(
                "eq." + userId,
                "eq." + comicId
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLikeRequestRunning = false;

                android.util.Log.d("LIKE_SUPABASE", "DELETE URL = " + call.request().url());
                android.util.Log.d("LIKE_SUPABASE", "DELETE CODE = " + response.code());

                if (response.isSuccessful()) {
                    isLiked = false;

                    Toast.makeText(
                            DetailComicActivity.this,
                            "Đã bỏ thích truyện",
                            Toast.LENGTH_SHORT
                    ).show();

                    syncMyLikeFromSupabase();
                    refreshComicAfterLikeChanged();

                } else {
                    try {
                        String error = response.errorBody() != null
                                ? response.errorBody().string()
                                : "Không có errorBody";

                        android.util.Log.e("LIKE_SUPABASE", "DELETE ERROR CODE = " + response.code());
                        android.util.Log.e("LIKE_SUPABASE", "DELETE ERROR BODY = " + error);
                    } catch (Exception e) {
                        android.util.Log.e("LIKE_SUPABASE", "Không đọc được errorBody", e);
                    }

                    Toast.makeText(
                            DetailComicActivity.this,
                            "Bỏ thích thất bại: " + response.code(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLikeRequestRunning = false;

                android.util.Log.e("LIKE_SUPABASE", "DELETE FAIL = " + t.getMessage(), t);

                Toast.makeText(
                        DetailComicActivity.this,
                        "Lỗi mạng khi bỏ thích: " + t.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void refreshComicAfterLikeChanged() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (comicRepository != null) {
                comicRepository.syncComicById(comicId);
                comicRepository.syncAllHomeComics();
            }
        }, 00);
    }
}