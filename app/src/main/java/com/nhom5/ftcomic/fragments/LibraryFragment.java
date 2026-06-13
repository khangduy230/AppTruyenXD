package com.nhom5.ftcomic.fragments;

import com.nhom5.ftcomic.models.Favorite;
import com.nhom5.ftcomic.network.SupabaseApi;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.response.FavoriteResponse;
import com.nhom5.ftcomic.utils.SessionManager;

import com.nhom5.ftcomic.models.ReadingHistory;
import com.nhom5.ftcomic.network.response.ReadingHistoryResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.activities.DetailComicActivity;
import com.nhom5.ftcomic.adapters.ComicAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.Comic;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {

    private SessionManager sessionManager;
    private ChipGroup chipGroupFilter;
    private RecyclerView recyclerViewLibrary;
    private TextView tvLibraryEmpty;

    private ComicAdapter comicAdapter;
    private AppDatabase appDatabase;

    private LiveData<List<Comic>> currentLiveData;

    public LibraryFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        chipGroupFilter = view.findViewById(R.id.chipGroup_filter);
        recyclerViewLibrary = view.findViewById(R.id.recyclerView_library);
        tvLibraryEmpty = view.findViewById(R.id.tvLibraryEmpty);

        appDatabase = AppDatabase.getInstance(requireContext());

        setupRecyclerView();
        setupChipListener();

        observeReadHistory();
    }

    private void setupRecyclerView() {
        comicAdapter = new ComicAdapter(new ArrayList<>(), comic -> openDetailComic(comic));

        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        float density = getResources().getDisplayMetrics().density;
        int itemWidthPx = (int) (115 * density);
        int spanCount = Math.max(2, screenWidthPx / itemWidthPx);

        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), spanCount);
        recyclerViewLibrary.setLayoutManager(layoutManager);
        recyclerViewLibrary.setNestedScrollingEnabled(false);
        recyclerViewLibrary.setAdapter(comicAdapter);
    }

    private void setupChipListener() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                return;
            }

            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chip_read) {
                observeReadHistory();
            } else if (checkedId == R.id.chip_later) {
                observeFavorites();
            } else if (checkedId == R.id.chip_downloaded) {
                observeDownloaded();
            }
        });
    }

    private void observeReadHistory() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            if (currentLiveData != null) {
                currentLiveData.removeObservers(getViewLifecycleOwner());
            }

            comicAdapter.setComicList(new ArrayList<>());
            tvLibraryEmpty.setText("Bạn cần đăng nhập để xem lịch sử đọc");
            tvLibraryEmpty.setVisibility(View.VISIBLE);
            recyclerViewLibrary.setVisibility(View.GONE);
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            comicAdapter.setComicList(new ArrayList<>());
            tvLibraryEmpty.setText("Bạn cần đăng nhập để xem lịch sử đọc");
            tvLibraryEmpty.setVisibility(View.VISIBLE);
            recyclerViewLibrary.setVisibility(View.GONE);
            return;
        }

        syncReadingHistoryFromSupabase(userId);
        observeComicList(appDatabase.readingHistoryDao().getHistoryComics(userId), "Bạn chưa đọc truyện nào");
    }

    private void observeFavorites() {
        if (sessionManager == null || !sessionManager.isLoggedIn()) {
            if (currentLiveData != null) {
                currentLiveData.removeObservers(getViewLifecycleOwner());
            }

            comicAdapter.setComicList(new ArrayList<>());
            tvLibraryEmpty.setText("Bạn cần đăng nhập để xem truyện đã lưu");
            tvLibraryEmpty.setVisibility(View.VISIBLE);
            recyclerViewLibrary.setVisibility(View.GONE);
            return;
        }

        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            comicAdapter.setComicList(new ArrayList<>());
            tvLibraryEmpty.setText("Bạn cần đăng nhập để xem truyện đã lưu");
            tvLibraryEmpty.setVisibility(View.VISIBLE);
            recyclerViewLibrary.setVisibility(View.GONE);
            return;
        }

        syncFavoritesFromSupabase(userId);
        observeComicList(appDatabase.favoriteDao().getFavoriteComics(userId), "Bạn chưa lưu truyện nào");
    }

    private void observeDownloaded() {
        observeComicList(appDatabase.downloadedChapterDao().getDownloadedComics(), "Bạn chưa tải truyện nào");
    }

    private void observeComicList(LiveData<List<Comic>> liveData, String emptyMessage) {
        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }

        currentLiveData = liveData;

        currentLiveData.observe(getViewLifecycleOwner(), comics -> {
            if (comics == null || comics.isEmpty()) {
                comicAdapter.setComicList(new ArrayList<>());
                tvLibraryEmpty.setText(emptyMessage);
                tvLibraryEmpty.setVisibility(View.VISIBLE);
                recyclerViewLibrary.setVisibility(View.GONE);
            } else {
                comicAdapter.setComicList(comics);
                tvLibraryEmpty.setVisibility(View.GONE);
                recyclerViewLibrary.setVisibility(View.VISIBLE);
            }
        });
    }

    private void openDetailComic(Comic comic) {
        Intent intent = new Intent(requireContext(), DetailComicActivity.class);
        intent.putExtra("COMIC_ID", comic.getId());
        startActivity(intent);
    }

    private void syncFavoritesFromSupabase(String userId) {
        SupabaseApi api = SupabaseClient.getApi(requireContext());

        api.getMyFavorites(
                "eq." + userId,
                "created_at.desc"
        ).enqueue(new Callback<List<FavoriteResponse>>() {
            @Override
            public void onResponse(Call<List<FavoriteResponse>> call, Response<List<FavoriteResponse>> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Không tải được danh sách lưu: " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                List<FavoriteResponse> remoteFavorites = response.body();

                if (remoteFavorites == null) {
                    remoteFavorites = new ArrayList<>();
                }

                List<Favorite> localFavorites = new ArrayList<>();

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
                Toast.makeText(requireContext(), "Lỗi mạng khi tải truyện đã lưu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void syncReadingHistoryFromSupabase(String userId) {
        SupabaseApi api = SupabaseClient.getApi(requireContext());

        api.getMyReadingHistory(
                "eq." + userId,
                "last_read_at.desc"
        ).enqueue(new Callback<List<ReadingHistoryResponse>>() {
            @Override
            public void onResponse(
                    Call<List<ReadingHistoryResponse>> call,
                    Response<List<ReadingHistoryResponse>> response
            ) {
                if (!response.isSuccessful()) {
                    Toast.makeText(
                            requireContext(),
                            "Không tải được lịch sử đọc: " + response.code(),
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                List<ReadingHistoryResponse> remoteHistories = response.body();

                if (remoteHistories == null) {
                    remoteHistories = new ArrayList<>();
                }

                List<ReadingHistory> localHistories = new ArrayList<>();

                for (ReadingHistoryResponse item : remoteHistories) {
                    localHistories.add(
                            new ReadingHistory(
                                    userId,
                                    item.getComicId(),
                                    item.getChapterId(),
                                    item.getPageNumber(),
                                    item.getLastReadAtMillis()
                            )
                    );
                }

                AppDatabase.databaseWriteExecutor.execute(() -> {
                    appDatabase.readingHistoryDao().deleteHistoriesByUser(userId);
                    appDatabase.readingHistoryDao().insertHistories(localHistories);
                });
            }

            @Override
            public void onFailure(Call<List<ReadingHistoryResponse>> call, Throwable t) {
                Toast.makeText(
                        requireContext(),
                        "Lỗi mạng khi tải lịch sử đọc",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
}