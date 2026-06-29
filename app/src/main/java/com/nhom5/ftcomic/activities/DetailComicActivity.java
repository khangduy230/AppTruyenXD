package com.nhom5.ftcomic.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ChapterAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.fragments.LoginFragment;
import com.nhom5.ftcomic.models.Category;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.models.Favorite;
import com.nhom5.ftcomic.models.Rating;
import com.nhom5.ftcomic.network.SupabaseApi;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.request.FavoriteRequest;
import com.nhom5.ftcomic.network.request.LikeRequest;
import com.nhom5.ftcomic.network.request.RatingRequest;
import com.nhom5.ftcomic.network.response.FavoriteResponse;
import com.nhom5.ftcomic.network.response.LikeResponse;
import com.nhom5.ftcomic.network.response.RatingResponse;
import com.nhom5.ftcomic.repository.ComicRepository;
import com.nhom5.ftcomic.utils.NetworkUtils;
import com.nhom5.ftcomic.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailComicActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private LiveData<Integer> favoriteLiveData;
    private LinearLayout layoutRating;
    private MaterialCardView cardRating, cardLike;
    private Comic currentComic;
    private TextView tvLikeCount, tvRating, tvRatingCount, tvCommentCount;
    private TextView tvTitle, tvAuthor, tvDescription, tvStatus, tvUploader;
    private ImageView imgCover;
    private ImageView imgBlurredBackground;
    private MaterialButton btnSave;
    private ExtendedFloatingActionButton btnReadFirstChapter;
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

        if (comicId <= 0) {
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

        if (NetworkUtils.isOnline(this)) {
            comicRepository.syncComicById(comicId);
            comicRepository.syncChaptersByComicId(comicId);
            comicRepository.syncCategoriesByComicId(comicId);
            syncMyLikeFromSupabase();
        } else {
            Toast.makeText(this, "Đang dùng dữ liệu đã lưu offline", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindViews() {
        imgCover = findViewById(R.id.imgCover);
        imgBlurredBackground = findViewById(R.id.imgBlurredBackground);
        layoutRating = findViewById(R.id.layoutRating);
        cardRating = findViewById(R.id.cardRating);
        cardLike = findViewById(R.id.layoutLikeAction);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvStatus = findViewById(R.id.tvStatus);
        tvUploader = findViewById(R.id.tvUploader);
        tvDescription = findViewById(R.id.tvDescription);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvRating = findViewById(R.id.tvRating);
        tvRatingCount = findViewById(R.id.tvRatingCount);
        tvCommentCount = findViewById(R.id.tvCommentCount);
        btnSave = findViewById(R.id.btnSave);
        btnReadFirstChapter = findViewById(R.id.btnReadFirstChapter);
        recyclerViewChapters = findViewById(R.id.recyclerViewChapters);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);

        MaterialCardView cardCover = findViewById(R.id.cardCover);
        String transitionName = getIntent().getStringExtra("TRANSITION_NAME");

        if (transitionName != null) {
            ViewCompat.setTransitionName(cardCover, transitionName);
        }

        supportPostponeEnterTransition();

        String comicCoverUrl = getIntent().getStringExtra("COMIC_COVER_URL");

        if (comicCoverUrl != null && !comicCoverUrl.isEmpty()) {
            Glide.with(this)
                    .load(comicCoverUrl)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .dontAnimate()
                    .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(
                                @androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e,
                                Object model,
                                com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                boolean isFirstResource
                        ) {
                            supportStartPostponedEnterTransition();
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(
                                android.graphics.drawable.Drawable resource,
                                Object model,
                                com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                com.bumptech.glide.load.DataSource dataSource,
                                boolean isFirstResource
                        ) {
                            supportStartPostponedEnterTransition();
                            return false;
                        }
                    })
                    .into(imgCover);
        } else {
            supportStartPostponedEnterTransition();
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ImageView imgBackground = findViewById(R.id.imgBlurredBackground);
            imgBackground.setRenderEffect(
                    android.graphics.RenderEffect.createBlurEffect(
                            35f,
                            35f,
                            android.graphics.Shader.TileMode.CLAMP
                    )
            );
        }

        View main = findViewById(android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(main, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams backParams =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                            findViewById(R.id.btnBack).getLayoutParams();

            backParams.topMargin = insets.top + (int) (10 * getResources().getDisplayMetrics().density);
            findViewById(R.id.btnBack).setLayoutParams(backParams);

            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams readParams =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                            btnReadFirstChapter.getLayoutParams();

            readParams.bottomMargin = insets.bottom + (int) (16 * getResources().getDisplayMetrics().density);
            btnReadFirstChapter.setLayoutParams(readParams);

            View scrollView = findViewById(R.id.nestedView1);
            scrollView.setPadding(0, 0, 0, insets.bottom);

            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setupBackButton() {
        MaterialButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
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
        chapterAdapter = new ChapterAdapter(new ArrayList<>(), chapter -> openReaderActivity(chapter.getId()));
        recyclerViewChapters.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChapters.setNestedScrollingEnabled(false);
        recyclerViewChapters.setAdapter(chapterAdapter);
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> toggleFavorite());

        if (cardLike != null) {
            cardLike.setClickable(true);
            cardLike.setOnClickListener(v -> toggleLike());
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
            tvStatus.setText(comic.getStatus());
            tvUploader.setText("Người đăng: " + comic.getUploaderName());

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

    private void updateLikeUi(boolean liked) {
        if (cardLike == null || tvLikeCount == null) return;

        int idSurfaceContainer = getResources().getIdentifier("colorSurfaceContainer", "attr", getPackageName());
        int idOnSurface = getResources().getIdentifier("colorOnSurface", "attr", getPackageName());

        int clrSurfaceContainer = MaterialColors.getColor(this, idSurfaceContainer, Color.LTGRAY);
        int clrOnSurface = MaterialColors.getColor(this, idOnSurface, Color.BLACK);

        if (liked) {
            int clrLikedPink = Color.parseColor("#FF2D55");
            int clrLikedBg = Color.parseColor("#FFEBF0");
            cardLike.setCardBackgroundColor(ColorStateList.valueOf(clrLikedBg));
            tvLikeCount.setTextColor(clrLikedPink);
            tvLikeCount.setCompoundDrawableTintList(ColorStateList.valueOf(clrLikedPink));
        } else {
            cardLike.setCardBackgroundColor(ColorStateList.valueOf(clrSurfaceContainer));
            tvLikeCount.setTextColor(clrOnSurface);
            tvLikeCount.setCompoundDrawableTintList(ColorStateList.valueOf(clrOnSurface));
        }
    }

    private void observeFavoriteStatus() {
        if (favoriteLiveData != null) {
            favoriteLiveData.removeObservers(this);
        }

        int idPrimary = getResources().getIdentifier("colorPrimary", "attr", getPackageName());
        int idOnPrimary = getResources().getIdentifier("colorOnPrimary", "attr", getPackageName());
        int idSurfaceVariant = getResources().getIdentifier("colorSurfaceVariant", "attr", getPackageName());
        int idOnSurfaceVariant = getResources().getIdentifier("colorOnSurfaceVariant", "attr", getPackageName());

        int clrPrimary = MaterialColors.getColor(this, idPrimary, Color.BLUE);
        int clrOnPrimary = MaterialColors.getColor(this, idOnPrimary, Color.WHITE);
        int clrSurfaceVariant = MaterialColors.getColor(this, idSurfaceVariant, Color.LTGRAY);
        int clrOnSurfaceVariant = MaterialColors.getColor(this, idOnSurfaceVariant, Color.DKGRAY);

        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            isFavorite = false;
            btnSave.setText("Lưu vào thư viện");
            btnSave.setBackgroundTintList(ColorStateList.valueOf(clrSurfaceVariant));
            btnSave.setTextColor(clrOnSurfaceVariant);
            btnSave.setIconTint(ColorStateList.valueOf(clrOnSurfaceVariant));
            btnSave.setEnabled(true);
            return;
        }

        String userId = sessionManager.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            isFavorite = false;
            btnSave.setText("Lưu vào thư viện");
            btnSave.setBackgroundTintList(ColorStateList.valueOf(clrSurfaceVariant));
            btnSave.setTextColor(clrOnSurfaceVariant);
            btnSave.setIconTint(ColorStateList.valueOf(clrOnSurfaceVariant));
            btnSave.setEnabled(true);
            return;
        }

        favoriteLiveData = appDatabase.favoriteDao().isFavoriteLive(userId, comicId);
        favoriteLiveData.observe(this, count -> {
            isFavorite = count != null && count > 0;
            if (isFavorite) {
                btnSave.setText("Đã lưu");
                btnSave.setBackgroundTintList(ColorStateList.valueOf(clrPrimary));
                btnSave.setTextColor(clrOnPrimary);
                btnSave.setIconTint(ColorStateList.valueOf(clrOnPrimary));
            } else {
                btnSave.setText("Lưu vào thư viện");
                btnSave.setBackgroundTintList(ColorStateList.valueOf(clrSurfaceVariant));
                btnSave.setTextColor(clrOnSurfaceVariant);
                btnSave.setIconTint(ColorStateList.valueOf(clrOnSurfaceVariant));
            }
            btnSave.setEnabled(true);
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
                .observe(this, rating -> {
                    myOldRating = rating;

                    int idPrimaryContainer = getResources().getIdentifier("colorPrimaryContainer", "attr", getPackageName());
                    int idOnPrimaryContainer = getResources().getIdentifier("colorOnPrimaryContainer", "attr", getPackageName());
                    int idSurfaceContainer = getResources().getIdentifier("colorSurfaceContainer", "attr", getPackageName());
                    int idOnSurface = getResources().getIdentifier("colorOnSurface", "attr", getPackageName());
                    int idOnSurfaceVariant = getResources().getIdentifier("colorOnSurfaceVariant", "attr", getPackageName());

                    int clrPrimaryContainer = MaterialColors.getColor(this, idPrimaryContainer, Color.CYAN);
                    int clrOnPrimaryContainer = MaterialColors.getColor(this, idOnPrimaryContainer, Color.BLUE);
                    int clrSurfaceContainer = MaterialColors.getColor(this, idSurfaceContainer, Color.LTGRAY);
                    int clrOnSurface = MaterialColors.getColor(this, idOnSurface, Color.BLACK);
                    int clrOnSurfaceVariant = MaterialColors.getColor(this, idOnSurfaceVariant, Color.GRAY);

                    if (cardRating != null) {
                        if (rating != null) {
                            cardRating.setCardBackgroundColor(ColorStateList.valueOf(clrPrimaryContainer));
                            tvRating.setTextColor(clrOnPrimaryContainer);
                            tvRating.setCompoundDrawableTintList(ColorStateList.valueOf(clrOnPrimaryContainer));
                            tvRatingCount.setTextColor(clrOnPrimaryContainer);
                        } else {
                            cardRating.setCardBackgroundColor(ColorStateList.valueOf(clrSurfaceContainer));
                            tvRating.setTextColor(clrOnSurface);
                            tvRating.setCompoundDrawableTintList(ColorStateList.valueOf(clrOnSurface));
                            tvRatingCount.setTextColor(clrOnSurfaceVariant);
                        }
                    }
                });

        syncMyRatingFromSupabase();
    }

    @Override
    protected void onResume() {
        super.onResume();

        observeFavoriteStatus();

        if (sessionManager != null && sessionManager.isLoggedIn()) {
            syncFavoritesFromSupabase();
            syncMyLikeFromSupabase();
            syncMyRatingFromSupabase();
        } else {
            isFavorite = false;
            isLiked = false;
            myOldRating = null;

            int idSurfaceContainer = getResources().getIdentifier("colorSurfaceContainer", "attr", getPackageName());
            int idOnSurface = getResources().getIdentifier("colorOnSurface", "attr", getPackageName());
            int idOnSurfaceVariant = getResources().getIdentifier("colorOnSurfaceVariant", "attr", getPackageName());
            int idSurfaceVariant = getResources().getIdentifier("colorSurfaceVariant", "attr", getPackageName());

            int clrSurfaceContainer = MaterialColors.getColor(this, idSurfaceContainer, Color.LTGRAY);
            int clrOnSurface = MaterialColors.getColor(this, idOnSurface, Color.BLACK);
            int clrOnSurfaceVariant = MaterialColors.getColor(this, idOnSurfaceVariant, Color.GRAY);
            int clrSurfaceVariant = MaterialColors.getColor(this, idSurfaceVariant, Color.LTGRAY);

            if (btnSave != null) {
                btnSave.setText("Lưu vào thư viện");
                btnSave.setBackgroundTintList(ColorStateList.valueOf(clrSurfaceVariant));
                btnSave.setTextColor(clrOnSurfaceVariant);
                btnSave.setIconTint(ColorStateList.valueOf(clrSurfaceVariant));
                btnSave.setEnabled(true);
            }
            if (cardRating != null) {
                cardRating.setCardBackgroundColor(ColorStateList.valueOf(clrSurfaceContainer));
                tvRating.setTextColor(clrOnSurface);
                tvRating.setCompoundDrawableTintList(ColorStateList.valueOf(clrOnSurface));
                tvRatingCount.setTextColor(clrOnSurfaceVariant);
            }
            updateLikeUi(false);
        }

        if (comicRepository != null) {
            comicRepository.syncComicById(comicId);
        }
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

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Cần có mạng để lưu truyện", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        SupabaseApi api = SupabaseClient.getApi(this);
        FavoriteRequest request = new FavoriteRequest(userId, comicId);

        api.addFavorite("return=minimal", request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnSave.setEnabled(true);

                if (response.isSuccessful() || response.code() == 409) {
                    AppDatabase.databaseWriteExecutor.execute(() -> appDatabase.favoriteDao().insertFavorite(
                            new Favorite(userId, comicId, System.currentTimeMillis())
                    ));

                    isFavorite = true;
                    int idPrimary = getResources().getIdentifier("colorPrimary", "attr", getPackageName());
                    int idOnPrimary = getResources().getIdentifier("colorOnPrimary", "attr", getPackageName());
                    int clrPrimary = MaterialColors.getColor(DetailComicActivity.this, idPrimary, Color.BLUE);
                    int clrOnPrimary = MaterialColors.getColor(DetailComicActivity.this, idOnPrimary, Color.WHITE);

                    runOnUiThread(() -> {
                        btnSave.setText("Đã lưu");
                        btnSave.setBackgroundTintList(ColorStateList.valueOf(clrPrimary));
                        btnSave.setTextColor(clrOnPrimary);
                        btnSave.setIconTint(ColorStateList.valueOf(clrOnPrimary));
                    });
                    Toast.makeText(DetailComicActivity.this, "Đã lưu vào thư viện", Toast.LENGTH_SHORT).show();
                    syncFavoritesFromSupabase();
                } else {
                    logFavoriteError(response, "ADD");
                    Toast.makeText(DetailComicActivity.this, "Lưu truyện thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSave.setEnabled(true);
                Toast.makeText(DetailComicActivity.this, "Lỗi mạng khi lưu truyện: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Cần có mạng để bỏ lưu truyện", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        SupabaseApi api = SupabaseClient.getApi(this);
        api.deleteFavorite("eq." + userId, "eq." + comicId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnSave.setEnabled(true);

                if (response.isSuccessful()) {
                    AppDatabase.databaseWriteExecutor.execute(() -> appDatabase.favoriteDao().deleteFavoriteByComicId(userId, comicId));

                    isFavorite = false;
                    int idSurfaceVariant = getResources().getIdentifier("colorSurfaceVariant", "attr", getPackageName());
                    int idOnSurfaceVariant = getResources().getIdentifier("colorOnSurfaceVariant", "attr", getPackageName());
                    int clrSurfaceVariant = MaterialColors.getColor(DetailComicActivity.this, idSurfaceVariant, Color.LTGRAY);
                    int clrOnSurfaceVariant = MaterialColors.getColor(DetailComicActivity.this, idOnSurfaceVariant, Color.DKGRAY);

                    runOnUiThread(() -> {
                        btnSave.setText("Lưu vào thư viện");
                        btnSave.setBackgroundTintList(ColorStateList.valueOf(clrSurfaceVariant));
                        btnSave.setTextColor(clrOnSurfaceVariant);
                        btnSave.setIconTint(ColorStateList.valueOf(clrOnSurfaceVariant));
                    });
                    Toast.makeText(DetailComicActivity.this, "Đã bỏ lưu khỏi thư viện", Toast.LENGTH_SHORT).show();
                    syncFavoritesFromSupabase();
                } else {
                    logFavoriteError(response, "DELETE");
                    Toast.makeText(DetailComicActivity.this, "Bỏ lưu thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSave.setEnabled(true);
                Toast.makeText(DetailComicActivity.this, "Lỗi mạng khi bỏ lưu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncFavoritesFromSupabase() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            return;
        }

        if (!NetworkUtils.isOnline(this)) {
            return;
        }

        String userId = sessionManager.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        SupabaseApi api = SupabaseClient.getApi(this);
        api.getMyFavorites("eq." + userId, "created_at.desc").enqueue(new Callback<List<FavoriteResponse>>() {
            @Override
            public void onResponse(Call<List<FavoriteResponse>> call, Response<List<FavoriteResponse>> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                List<FavoriteResponse> remoteFavorites = response.body();
                if (remoteFavorites == null) {
                    remoteFavorites = new ArrayList<>();
                }

                List<Favorite> localFavorites = new ArrayList<>();
                for (FavoriteResponse item : remoteFavorites) {
                    localFavorites.add(new Favorite(userId, item.getComicId(), item.getCreatedAtMillis()));
                }

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    appDatabase.favoriteDao().deleteFavoritesByUser(userId);
                    appDatabase.favoriteDao().insertFavorites(localFavorites);
                });
            }

            @Override
            public void onFailure(Call<List<FavoriteResponse>> call, Throwable t) {
            }
        });
    }

    private void syncMyLikeFromSupabase() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            isLiked = false;
            updateLikeUi(false);
            return;
        }

        if (!NetworkUtils.isOnline(this)) {
            return;
        }

        String userId = sessionManager.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            isLiked = false;
            updateLikeUi(false);
            return;
        }

        SupabaseApi api = SupabaseClient.getApi(this);
        api.getMyLike("eq." + userId, "eq." + comicId, 1).enqueue(new Callback<List<LikeResponse>>() {
            @Override
            public void onResponse(Call<List<LikeResponse>> call, Response<List<LikeResponse>> response) {
                if (response.isSuccessful()) {
                    List<LikeResponse> likes = response.body();
                    isLiked = likes != null && !likes.isEmpty();
                    updateLikeUi(isLiked);
                } else {
                    isLiked = false;
                    updateLikeUi(false);
                    logLikeError(response, "GET");
                }
            }

            @Override
            public void onFailure(Call<List<LikeResponse>> call, Throwable t) {
                isLiked = false;
                updateLikeUi(false);
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

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Cần có mạng để thích truyện", Toast.LENGTH_SHORT).show();
            return;
        }

        isLikeRequestRunning = true;
        isLiked = true;
        updateLikeCountImmediately(1);

        SupabaseApi api = SupabaseClient.getApi(this);
        LikeRequest request = new LikeRequest(userId, comicId);

        api.addLike("return=minimal", request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLikeRequestRunning = false;

                if (response.isSuccessful()) {
                    Toast.makeText(DetailComicActivity.this, "Đã thích truyện", Toast.LENGTH_SHORT).show();
                    syncMyLikeFromSupabase();
                    refreshComicAfterLikeChanged();
                } else if (response.code() == 409) {
                    updateLikeCountImmediately(-1);
                    isLiked = true;
                    Toast.makeText(DetailComicActivity.this, "Bạn đã thích truyện này rồi", Toast.LENGTH_SHORT).show();
                    syncMyLikeFromSupabase();
                    refreshComicAfterLikeChanged();
                } else {
                    updateLikeCountImmediately(-1);
                    isLiked = false;
                    logLikeError(response, "ADD");
                    Toast.makeText(DetailComicActivity.this, "Thích truyện thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLikeRequestRunning = false;
                updateLikeCountImmediately(-1);
                isLiked = false;
                Toast.makeText(DetailComicActivity.this, "Lỗi mạng khi thích truyện: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Cần có mạng để bỏ thích truyện", Toast.LENGTH_SHORT).show();
            return;
        }

        isLikeRequestRunning = true;
        isLiked = false;
        updateLikeCountImmediately(-1);

        SupabaseApi api = SupabaseClient.getApi(this);
        api.deleteLike("eq." + userId, "eq." + comicId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isLikeRequestRunning = false;

                if (response.isSuccessful()) {
                    Toast.makeText(DetailComicActivity.this, "Đã bỏ thích truyện", Toast.LENGTH_SHORT).show();
                    syncMyLikeFromSupabase();
                    refreshComicAfterLikeChanged();
                } else {
                    updateLikeCountImmediately(1);
                    isLiked = true;
                    logLikeError(response, "DELETE");
                    Toast.makeText(DetailComicActivity.this, "Bỏ thích thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isLikeRequestRunning = false;
                updateLikeCountImmediately(1);
                isLiked = true;
                Toast.makeText(DetailComicActivity.this, "Lỗi mạng khi bỏ thích: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLikeCountImmediately(int delta) {
        int current = getCurrentLikeCountFromUi();
        int newCount = Math.max(0, current + delta);

        tvLikeCount.setText(String.valueOf(newCount));
        updateLikeUi(isLiked);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (delta > 0) {
                appDatabase.comicDao().increaseLikeCount(comicId);
            } else if (delta < 0) {
                appDatabase.comicDao().decreaseLikeCount(comicId);
            }
        });
    }

    private int getCurrentLikeCountFromUi() {
        try {
            return Integer.parseInt(tvLikeCount.getText().toString().trim());
        } catch (Exception e) {
            return currentComic != null ? currentComic.getLikeCount() : 0;
        }
    }

    private void refreshComicAfterLikeChanged() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (comicRepository != null && NetworkUtils.isOnline(this)) {
                comicRepository.syncComicById(comicId);
                comicRepository.syncAllHomeComics();
            }
        }, 700);
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

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rating, null);
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
            builder.setNeutralButton("Xóa đánh giá", (dialog, which) -> deleteRatingFromSupabase());
        }

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void syncMyRatingFromSupabase() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            return;
        }

        if (!NetworkUtils.isOnline(this)) {
            return;
        }

        String userId = sessionManager.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        SupabaseApi api = SupabaseClient.getApi(this);
        api.getMyRating("eq." + userId, "eq." + comicId, 1).enqueue(new Callback<List<RatingResponse>>() {
            @Override
            public void onResponse(Call<List<RatingResponse>> call, Response<List<RatingResponse>> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                List<RatingResponse> remoteRatings = response.body();
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    if (remoteRatings == null || remoteRatings.isEmpty()) {
                        appDatabase.ratingDao().deleteRatingByComicId(userId, comicId);
                    } else {
                        RatingResponse item = remoteRatings.get(0);
                        appDatabase.ratingDao().insertOrUpdateRating(new Rating(userId, item.getComicId(), item.getRating()));
                    }
                });
            }

            @Override
            public void onFailure(Call<List<RatingResponse>> call, Throwable t) {
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

        if (comicId <= 0) {
            Toast.makeText(this, "Không tìm thấy COMIC_ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Cần có mạng để gửi đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        SupabaseApi api = SupabaseClient.getApi(this);
        RatingRequest request = new RatingRequest(userId, comicId, stars);

        api.upsertRating("resolution=merge-duplicates,return=representation", "user_id,comic_id", request).enqueue(new Callback<List<RatingResponse>>() {
            @Override
            public void onResponse(Call<List<RatingResponse>> call, Response<List<RatingResponse>> response) {
                if (response.isSuccessful()) {
                    List<RatingResponse> result = response.body();

                    if (result == null || result.isEmpty()) {
                        refreshComicStatsFromServer();
                        return;
                    }

                    RatingResponse savedRating = result.get(0);
                    AppDatabase.databaseWriteExecutor.execute(() -> appDatabase.ratingDao().insertOrUpdateRating(new Rating(userId, savedRating.getComicId(), savedRating.getRating())));

                    myOldRating = new Rating(userId, savedRating.getComicId(), savedRating.getRating());
                    Toast.makeText(DetailComicActivity.this, "Đã đánh giá " + savedRating.getRating() + " sao", Toast.LENGTH_SHORT).show();

                    syncMyRatingFromSupabase();
                    refreshComicStatsFromServer();
                } else {
                    Toast.makeText(DetailComicActivity.this, "Gửi đánh giá thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<RatingResponse>> call, Throwable t) {
                Toast.makeText(DetailComicActivity.this, "Lỗi mạng khi gửi đánh giá: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteRatingFromSupabase() {
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

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Cần có mạng để xóa đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        SupabaseApi api = SupabaseClient.getApi(this);
        api.deleteRating("eq." + userId, "eq." + comicId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    AppDatabase.databaseWriteExecutor.execute(() -> appDatabase.ratingDao().deleteRatingByComicId(userId, comicId));

                    myOldRating = null;
                    Toast.makeText(DetailComicActivity.this, "Đã xóa đánh giá", Toast.LENGTH_SHORT).show();

                    syncMyRatingFromSupabase();
                    refreshComicAfterLikeChanged();
                } else {
                    Toast.makeText(DetailComicActivity.this, "Xóa đánh giá thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(DetailComicActivity.this, "Lỗi mạng khi xóa đánh giá: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openReaderActivity(int targetChapterId) {
        if (comicId <= 0 || targetChapterId <= 0) return;
        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra("COMIC_ID", comicId);
        intent.putExtra("CHAPTER_ID", targetChapterId);
        intent.putExtra("PAGE_NUMBER", 1);
        startActivity(intent);
    }

    private void openLoginThen(Runnable afterLogin) {
        LoginFragment loginFragment = new LoginFragment();
        loginFragment.show(getSupportFragmentManager(), "LoginFragment");
        getSupportFragmentManager().setFragmentResultListener("key_dang_nhap", this, (requestKey, result) -> afterLogin.run());
    }

    private void shareComic() {
        if (currentComic == null) return;
        String shareText = "Mình đang đọc truyện: " + currentComic.getName() + "\n"
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

    private void logFavoriteError(Response<?> response, String action) {
        try {
            if (response.errorBody() != null) {
                android.util.Log.e("FAVORITE_SUPABASE", action + " ERROR BODY = " + response.errorBody().string());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logLikeError(Response<?> response, String action) {
        try {
            if (response.errorBody() != null) {
                android.util.Log.e("LIKE_SUPABASE", action + " ERROR BODY = " + response.errorBody().string());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshComicStatsFromServer() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (comicRepository != null) {
                comicRepository.syncComicById(comicId);
                comicRepository.syncAllHomeComics();
            }
        }, 1200);
    }
}