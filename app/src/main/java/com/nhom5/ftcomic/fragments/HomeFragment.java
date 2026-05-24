package com.nhom5.ftcomic.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nhom5.ftcomic.activities.DetailComicActivity;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ComicAdapter;


import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // Khai báo các biến RecyclerView và Adapter
    private RecyclerView recyclerFeatured, recyclerRanking, recyclerAllComics;
    private ComicAdapter featuredAdapter, rankingAdapter, allComicsAdapter;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //gán giao diện vào biến view
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        //tìm các view trong giao diện
        recyclerFeatured = view.findViewById(R.id.recyclerView_featured);
        recyclerRanking = view.findViewById(R.id.recyclerView_ranking);
        recyclerAllComics = view.findViewById(R.id.recyclerView_all_comics);

        //gọi hàm để setup dữ liệu và adapter cho các RecyclerView
        setupFeaturedComics();
        setupRankingComics();
        setupAllComics();

        //trả về view
        return view;
    }

    //hàm để setup dữ liệu và adapter cho RecyclerView All Comics
    private void setupAllComics() {
        //tạo danh sách tất cả truyện
        List<Comic> allComicsList = new ArrayList<>();
        allComicsList.add(new Comic(R.drawable.thientai, "Truyện 1"));
        allComicsList.add(new Comic(R.drawable.thientai, "Truyện 2"));
        allComicsList.add(new Comic(R.drawable.thientai, "Truyện 3"));

        //tạo adapter và gán vào RecyclerView
        allComicsAdapter = new ComicAdapter(allComicsList, new ComicAdapter.OnComicClickListener() {
            @Override
            public void onComicClick(Comic comic) {
                //chuyển từ HomeFragment -> DetailComicActivity
                Intent intent = new Intent(requireContext(), DetailComicActivity.class);

                //gửi thông tin truyện đến DetailComicActivity
                intent.putExtra("COMIC_NAME", comic.getName());
                intent.putExtra("COMIC_IMAGE", comic.getImage());

                //chuyển sang DetailComicActivity
                startActivity(intent);
            }
        });

        //gán adapter cho RecyclerView
        recyclerAllComics.setAdapter(allComicsAdapter);
    }

    //hàm để setup dữ liệu và adapter cho RecyclerView Featured
    private void setupFeaturedComics() {
        //tạo danh sách truyện nổi bật
        List<Comic> featuredList = new ArrayList<>();
        featuredList.add(new Comic(R.drawable.thientai, "Truyện Nổi Bật 1"));
        featuredList.add(new Comic(R.drawable.thientai, "Truyện Nổi Bật 2"));
        featuredList.add(new Comic(R.drawable.thientai, "Truyện Nổi Bật 3"));
        featuredList.add(new Comic(R.drawable.thientai, "Truyện Nổi Bật 4"));

        //tạo adapter và gán vào RecyclerView
        featuredAdapter = new ComicAdapter(featuredList, new ComicAdapter.OnComicClickListener() {
            @Override
            public void onComicClick(Comic comic) {
                //chuyển từ HomeFragment -> DetailComicActivity
                Intent intent = new Intent(requireContext(), DetailComicActivity.class);

                //gửi thông tin truyện đến DetailComicActivity
                intent.putExtra("COMIC_NAME", comic.getName());
                intent.putExtra("COMIC_IMAGE", comic.getImage());

                //chuyển sang DetailComicActivity
                startActivity(intent);
            }
        });

        //gán adapter cho RecyclerView
        recyclerFeatured.setAdapter(featuredAdapter);
    }

    //hàm để setup dữ liệu và adapter cho RecyclerView Ranking
    private void setupRankingComics() {
        //tạo danh sách truyện xếp hạng
        List<Comic> rankingList = new ArrayList<>();
        rankingList.add(new Comic(R.drawable.thientai, "Truyện Xếp Hạng 1"));
        rankingList.add(new Comic(R.drawable.thientai, "Truyện Xếp Hạng 2"));
        rankingList.add(new Comic(R.drawable.thientai, "Truyện Xếp Hạng 3"));

        //tạo adapter và gán vào RecyclerView
        rankingAdapter = new ComicAdapter(rankingList, new ComicAdapter.OnComicClickListener() {
            @Override
            public void onComicClick(Comic comic) {
                //chuyển từ HomeFragment -> DetailComicActivity
                Intent intent = new Intent(requireContext(), DetailComicActivity.class);

                //gửi thông tin truyện đến DetailComicActivity
                intent.putExtra("COMIC_NAME", comic.getName());
                intent.putExtra("COMIC_IMAGE", comic.getImage());

                //chuyển sang DetailComicActivity
                startActivity(intent);
            }
        });

        //gán adapter cho RecyclerView
        recyclerRanking.setAdapter(rankingAdapter);
    }
}