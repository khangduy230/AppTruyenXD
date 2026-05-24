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

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.activities.DetailComicActivity;
import com.nhom5.ftcomic.adapters.ComicAdapter;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.repository.ComicRepository;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerFeatured, recyclerRanking, recyclerAllComics;
    private ComicAdapter featuredAdapter, rankingAdapter, allComicsAdapter;

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

        comicRepository = new ComicRepository(requireContext());

        setupRecyclerViews();
        observeComicsFromRoom();

        // Gọi API Supabase rồi lưu vào Room
        comicRepository.syncAllHomeComics();
    }

    private void setupRecyclerViews() {
        featuredAdapter = new ComicAdapter(new ArrayList<>(), comic -> openDetailComic(comic));
        rankingAdapter = new ComicAdapter(new ArrayList<>(), comic -> openDetailComic(comic));
        allComicsAdapter = new ComicAdapter(new ArrayList<>(), comic -> openDetailComic(comic));

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
                .observe(getViewLifecycleOwner(), comics -> featuredAdapter.setComicList(comics));

        comicRepository.getComicsBySection("ranking")
                .observe(getViewLifecycleOwner(), comics -> rankingAdapter.setComicList(comics));

        comicRepository.getComicsBySection("all")
                .observe(getViewLifecycleOwner(), comics -> allComicsAdapter.setComicList(comics));
    }

    private void openDetailComic(Comic comic) {
        Intent intent = new Intent(requireContext(), DetailComicActivity.class);
        intent.putExtra("COMIC_ID", comic.getId());
        startActivity(intent);
    }
}