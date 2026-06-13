package com.nhom5.ftcomic.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.activities.AllStoryActivity;
import com.nhom5.ftcomic.activities.DetailComicActivity;
import com.nhom5.ftcomic.adapters.ComicAdapter;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.repository.ComicRepository;
import com.nhom5.ftcomic.utils.NetworkUtils;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerFeatured, recyclerRanking, recyclerAllComics;
    private ComicAdapter featuredAdapter, rankingAdapter, allComicsAdapter;
    private TextView tvAllComicsTitle;

    private ComicRepository comicRepository;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerFeatured = view.findViewById(R.id.recyclerView_featured);
        recyclerRanking = view.findViewById(R.id.recyclerView_ranking);
        recyclerAllComics = view.findViewById(R.id.recyclerView_all_comics);
        tvAllComicsTitle = view.findViewById(R.id.tvAllComicsTitle);

        comicRepository = new ComicRepository(requireContext());

        setupRecyclerViews();

        // Luôn observe Room trước để mất mạng vẫn có dữ liệu cache
        observeComicsFromRoom();

        if (tvAllComicsTitle != null) {
            tvAllComicsTitle.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AllStoryActivity.class);
                startActivity(intent);
            });
        }

        // Có mạng thì sync Supabase -> Room
        // Mất mạng thì vẫn hiển thị dữ liệu Room
        if (NetworkUtils.isOnline(requireContext())) {
            comicRepository.syncAllHomeComics();
        } else {
            Toast.makeText(
                    requireContext(),
                    "Đang hiển thị dữ liệu đã lưu offline",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void setupRecyclerViews() {
        featuredAdapter = new ComicAdapter(new ArrayList<>(), this::openDetailComic);
        rankingAdapter = new ComicAdapter(new ArrayList<>(), this::openDetailComic);
        allComicsAdapter = new ComicAdapter(new ArrayList<>(), this::openDetailComic);

        setupHorizontalRecyclerView(recyclerFeatured, featuredAdapter);
        setupHorizontalRecyclerView(recyclerRanking, rankingAdapter);
        setupHorizontalRecyclerView(recyclerAllComics, allComicsAdapter);
    }

    private void setupHorizontalRecyclerView(RecyclerView recyclerView, ComicAdapter adapter) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        );

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(adapter);
    }

    private void observeComicsFromRoom() {
        comicRepository.getComicsBySection("featured")
                .observe(getViewLifecycleOwner(), comics -> {
                    if (comics != null) {
                        featuredAdapter.setComicList(comics);
                    }
                });

        comicRepository.getRankingComics()
                .observe(getViewLifecycleOwner(), comics -> {
                    if (comics != null) {
                        rankingAdapter.setComicList(comics);
                    }
                });

        comicRepository.getAllComicsLive()
                .observe(getViewLifecycleOwner(), comics -> {
                    if (comics != null) {
                        allComicsAdapter.setComicList(comics);
                    }
                });
    }

    private void openDetailComic(Comic comic) {
        if (comic == null) {
            return;
        }

        Intent intent = new Intent(requireContext(), DetailComicActivity.class);
        intent.putExtra("COMIC_ID", comic.getId());
        intent.putExtra("COMIC_COVER_URL", comic.getCoverUrl());
        startActivity(intent);
    }
}