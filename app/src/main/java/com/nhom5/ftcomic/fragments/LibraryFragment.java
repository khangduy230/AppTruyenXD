package com.nhom5.ftcomic.fragments;

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
import com.nhom5.ftcomic.database.LocalDataSeeder;
import com.nhom5.ftcomic.models.Comic;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {

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

        chipGroupFilter = view.findViewById(R.id.chipGroup_filter);
        recyclerViewLibrary = view.findViewById(R.id.recyclerView_library);
        tvLibraryEmpty = view.findViewById(R.id.tvLibraryEmpty);

        appDatabase = AppDatabase.getInstance(requireContext());

        // Đảm bảo dữ liệu mẫu đã được tạo nếu lần đầu mở thẳng vào Library
        LocalDataSeeder.seedIfNeeded(requireContext());

        setupRecyclerView();
        setupChipListener();

        // Mặc định mở tab Đã đọc
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
        observeComicList(appDatabase.readingHistoryDao().getHistoryComics(), "Bạn chưa đọc truyện nào");
    }

    private void observeFavorites() {
        observeComicList(appDatabase.favoriteDao().getFavoriteComics(), "Bạn chưa lưu truyện nào");
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
}