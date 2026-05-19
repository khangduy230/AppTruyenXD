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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerFeatured, recyclerRanking, recyclerAllComics;
    private ComicAdapter featuredAdapter, rankingAdapter, allComicsAdapter;

    public HomeFragment() {
        // Required empty public constructor
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

        setupFeaturedComics();
        setupRankingComics();
        setupAllComics();
    }

    private void setupFeaturedComics() {
        List<Comic> featuredList = new ArrayList<>();

        featuredList.add(new Comic(R.drawable.thientai, "Truyện Nổi Bật 1"));
        featuredList.add(new Comic(R.drawable.thientai, "Truyện Nổi Bật 2"));
        featuredList.add(new Comic(R.drawable.thientai, "Truyện Nổi Bật 3"));
        featuredList.add(new Comic(R.drawable.thientai, "Truyện Nổi Bật 4"));

        featuredAdapter = new ComicAdapter(featuredList, comic -> openDetailComic(comic));

        setupHorizontalRecyclerView(recyclerFeatured, featuredAdapter);
    }

    private void setupRankingComics() {
        List<Comic> rankingList = new ArrayList<>();

        rankingList.add(new Comic(R.drawable.thientai, "Truyện Xếp Hạng 1"));
        rankingList.add(new Comic(R.drawable.thientai, "Truyện Xếp Hạng 2"));
        rankingList.add(new Comic(R.drawable.thientai, "Truyện Xếp Hạng 3"));

        rankingAdapter = new ComicAdapter(rankingList, comic -> openDetailComic(comic));

        setupHorizontalRecyclerView(recyclerRanking, rankingAdapter);
    }

    private void setupAllComics() {
        List<Comic> allComicsList = new ArrayList<>();

        allComicsList.add(new Comic(R.drawable.thientai, "Truyện 1"));
        allComicsList.add(new Comic(R.drawable.thientai, "Truyện 2"));
        allComicsList.add(new Comic(R.drawable.thientai, "Truyện 3"));
        allComicsList.add(new Comic(R.drawable.thientai, "Truyện 4"));
        allComicsList.add(new Comic(R.drawable.thientai, "Truyện 5"));

        allComicsAdapter = new ComicAdapter(allComicsList, comic -> openDetailComic(comic));

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

    private void openDetailComic(Comic comic) {
        Intent intent = new Intent(requireContext(), DetailComicActivity.class);

        intent.putExtra("COMIC_NAME", comic.getName());
        intent.putExtra("COMIC_IMAGE", comic.getImage());

        startActivity(intent);
    }
}