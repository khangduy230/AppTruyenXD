package com.nhom5.ftcomic.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.repository.ComicRepository;
import com.nhom5.ftcomic.utils.NetworkUtils;

public class HomeFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ComicRepository comicRepository;
    private ComicGridFragment allComicsFragment;

    public void setAllComicsFragment(ComicGridFragment fragment) {
        this.allComicsFragment = fragment;
    }

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

        tabLayout = view.findViewById(R.id.tabLayoutHome);
        viewPager = view.findViewById(R.id.viewPagerHome);

        MaterialToolbar toolbar = view.findViewById(R.id.topAppBar);
        toolbar.inflateMenu(R.menu.menu_home);

        MenuItem filterItem = toolbar.getMenu().findItem(R.id.action_filter);
        if (filterItem != null) {
            filterItem.setVisible(false); // Ẩn mặc định cho đến khi chuyển qua tab "Tất cả"
        }

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_filter) {
                if (allComicsFragment != null) {
                    allComicsFragment.showFilterBottomSheet();
                }
                return true;
            }
            return false;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                MenuItem item = toolbar.getMenu().findItem(R.id.action_filter);
                if (item != null) {
                    item.setVisible(position == 2);
                }
            }
        });

        comicRepository = new ComicRepository(requireContext());

        // Thiết lập Adapter cho ViewPager2
        HomePagerAdapter adapter = new HomePagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Liên kết TabLayout và ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Mới nhất");
                    break;
                case 1:
                    tab.setText("Bảng xếp hạng");
                    break;
                case 2:
                    tab.setText("Tất cả");
                    break;
            }
        }).attach();

        // Đồng bộ dữ liệu truyện từ Supabase về Room khi có mạng
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

    private static class HomePagerAdapter extends FragmentStateAdapter {

        public HomePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return ComicGridFragment.newInstance("featured");
                case 1:
                    return ComicGridFragment.newInstance("ranking");
                case 2:
                default:
                    return ComicGridFragment.newInstance("all");
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}