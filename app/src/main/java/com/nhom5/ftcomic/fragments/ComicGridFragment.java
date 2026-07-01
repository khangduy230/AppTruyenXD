package com.nhom5.ftcomic.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.activities.DetailComicActivity;
import com.nhom5.ftcomic.adapters.ComicAdapter;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.response.CategoryResponse;
import com.nhom5.ftcomic.network.response.ComicCategoryResponse;
import com.nhom5.ftcomic.repository.ComicRepository;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicGridFragment extends Fragment {

    private static final String ARG_SECTION = "section_type";

    private String sectionType;
    private RecyclerView recyclerView;
    private ComicAdapter adapter;
    private ComicRepository comicRepository;

    // Dữ liệu phục vụ bộ lọc (chỉ dùng cho tab "Tất cả")
    private List<Comic> fullComicList = new ArrayList<>();
    private List<Comic> displayedList = new ArrayList<>();
    private final List<CategoryResponse> categoriesFromApi = new ArrayList<>();
    private final Set<Integer> selectedCategoryIds = new HashSet<>();
    private int currentSortType = 1; // 0: A-Z, 1: Mới nhất, 2: Đánh giá
    private FilterCategoryAdapter filterCategoryAdapter;

    public static ComicGridFragment newInstance(String sectionType) {
        ComicGridFragment fragment = new ComicGridFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SECTION, sectionType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sectionType = getArguments().getString(ARG_SECTION);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comic_grid, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerViewGrid);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        adapter = new ComicAdapter(new ArrayList<>(), this::openDetailComic);
        recyclerView.setAdapter(adapter);

        comicRepository = new ComicRepository(requireContext());

        if ("all".equals(sectionType)) {
            setupFilters(view);
            androidx.fragment.app.Fragment parent = getParentFragment();
            if (parent instanceof HomeFragment) {
                ((HomeFragment) parent).setAllComicsFragment(this);
            }
        }

        observeComics();
    }

    @Override
    public void onDestroyView() {
        if ("all".equals(sectionType)) {
            androidx.fragment.app.Fragment parent = getParentFragment();
            if (parent instanceof HomeFragment) {
                ((HomeFragment) parent).setAllComicsFragment(null);
            }
        }
        super.onDestroyView();
    }

    private void setupFilters(View view) {
        loadCategoriesFromSupabase();
    }

    private void observeComics() {

        if ("latest".equals(sectionType)) {

            comicRepository.getLatestComics()
                    .observe(getViewLifecycleOwner(), comics -> {
                        if (comics != null && !comics.isEmpty()) {
                            adapter.setComicList(comics);
                        } else {
                            Log.d("ComicGridFragment", "Danh sách truyện mới nhất trống");
                        }
                    });

        } else if ("ranking".equals(sectionType)) {
            comicRepository.getRankingComics()
                    .observe(getViewLifecycleOwner(), comics -> {
                        if (comics != null) {
                            adapter.setComicList(comics);
                        }
                    });

        } else {
            comicRepository.getAllComicsLive()
                    .observe(getViewLifecycleOwner(), comics -> {
                        if (comics != null) {
                            fullComicList = new ArrayList<>(comics);
                            applyFilters();
                        }
                    });
        }
    }

    private void loadCategoriesFromSupabase() {
        SupabaseClient.getApi().getAllCategories("name.asc").enqueue(new Callback<List<CategoryResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<CategoryResponse>> call, @NonNull Response<List<CategoryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoriesFromApi.clear();
                    categoriesFromApi.addAll(response.body());
                    if (filterCategoryAdapter != null) {
                        filterCategoryAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CategoryResponse>> call, @NonNull Throwable t) {
                // Thất bại trong âm thầm hoặc log ra
            }
        });
    }

    public void showFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.item_filter, null, false);
        bottomSheetDialog.setContentView(view);

        int bottomSheetId = getResources().getIdentifier("design_bottom_sheet", "id", "com.google.android.material");
        View bottomSheetInternal = bottomSheetDialog.findViewById(bottomSheetId);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
        }

        RelativeLayout headerSort = view.findViewById(R.id.headerSort);
        LinearLayout layoutSortOptions = view.findViewById(R.id.layoutSortOptions);
        ImageView ivToggleSort = view.findViewById(R.id.ivToggleSort);

        RelativeLayout headerGenre = view.findViewById(R.id.headerGenre);
        LinearLayout layoutGenreOptions = view.findViewById(R.id.layoutGenreOptions);
        ImageView ivToggleGenre = view.findViewById(R.id.ivToggleGenre);

        RecyclerView rvFilterCategories = view.findViewById(R.id.rvFilterCategories);

        CheckBox cbAlpha = view.findViewById(R.id.cbSortAlpha);
        CheckBox cbNewest = view.findViewById(R.id.cbSortNewest);
        CheckBox cbRating = view.findViewById(R.id.cbSortRating);

        if (cbAlpha != null) cbAlpha.setChecked(currentSortType == 0);
        if (cbNewest != null) cbNewest.setChecked(currentSortType == 1);
        if (cbRating != null) cbRating.setChecked(currentSortType == 2);

        ivToggleSort.setRotation(layoutSortOptions.getVisibility() == View.VISIBLE ? 0 : 180);
        ivToggleGenre.setRotation(layoutGenreOptions.getVisibility() == View.VISIBLE ? 0 : 180);

        headerSort.setOnClickListener(v -> toggleSection(layoutSortOptions, ivToggleSort));
        headerGenre.setOnClickListener(v -> toggleSection(layoutGenreOptions, ivToggleGenre));

        view.findViewById(R.id.itemSortAlpha).setOnClickListener(v -> {
            currentSortType = 0; applyCurrentSort(); bottomSheetDialog.dismiss();
        });
        view.findViewById(R.id.itemSortNewest).setOnClickListener(v -> {
            currentSortType = 1; applyCurrentSort(); bottomSheetDialog.dismiss();
        });
        view.findViewById(R.id.itemSortRating).setOnClickListener(v -> {
            currentSortType = 2; applyCurrentSort(); bottomSheetDialog.dismiss();
        });

        if (rvFilterCategories != null) {
            rvFilterCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
            filterCategoryAdapter = new FilterCategoryAdapter(categoriesFromApi, selectedCategoryIds, categoryId -> {
                if (selectedCategoryIds.contains(categoryId)) {
                    selectedCategoryIds.remove(categoryId);
                } else {
                    selectedCategoryIds.add(categoryId);
                }
                applyFilters();
            });
            rvFilterCategories.setAdapter(filterCategoryAdapter);

            if (categoriesFromApi.isEmpty()) {
                Toast.makeText(requireContext(), "Đang tải thể loại...", Toast.LENGTH_SHORT).show();
                loadCategoriesFromSupabase();
            }
        }

        bottomSheetDialog.show();
    }

    private void toggleSection(View layout, ImageView arrow) {
        if (layout.getVisibility() == View.VISIBLE) {
            layout.setVisibility(View.GONE);
            arrow.animate().rotation(180).setDuration(200).start();
        } else {
            layout.setVisibility(View.VISIBLE);
            arrow.animate().rotation(0).setDuration(200).start();
        }
    }

    private void applyFilters() {
        if (selectedCategoryIds.isEmpty()) {
            updateDisplayedList(fullComicList);
            return;
        }

        StringBuilder builder = new StringBuilder("in.(");
        int i = 0;
        for (Integer id : selectedCategoryIds) {
            builder.append(id);
            if (++i < selectedCategoryIds.size()) builder.append(",");
        }
        builder.append(")");

        SupabaseClient.getApi().getComicCategoryRefs(builder.toString()).enqueue(new Callback<List<ComicCategoryResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ComicCategoryResponse>> call, @NonNull Response<List<ComicCategoryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Set<Integer> validComicIds = new HashSet<>();
                    for (ComicCategoryResponse ref : response.body()) {
                        validComicIds.add(ref.getComicId());
                    }

                    List<Comic> filtered = new ArrayList<>();
                    for (Comic c : fullComicList) {
                        if (validComicIds.contains(c.getId())) {
                            filtered.add(c);
                        }
                    }
                    updateDisplayedList(filtered);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ComicCategoryResponse>> call, @NonNull Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi kết nối bộ lọc", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateDisplayedList(List<Comic> newList) {
        displayedList = new ArrayList<>(newList);
        applyCurrentSort();
    }

    private void applyCurrentSort() {
        if (currentSortType == 0) sortAlphabeticalLocal();
        else if (currentSortType == 1) sortNewestLocal();
        else if (currentSortType == 2) sortRatingLocal();
        else adapter.setComicList(displayedList);
    }

    private void sortAlphabeticalLocal() {
        Collator collator = Collator.getInstance(new Locale("vi", "VN"));
        displayedList.sort((c1, c2) -> collator.compare(c1.getName(), c2.getName()));
        adapter.setComicList(displayedList);
    }

    private void sortNewestLocal() {
        // Xóa toàn bộ ruột cũ đi và dán đoạn này vào
        displayedList.sort((c1, c2) -> {
            // So sánh theo ID giảm dần (truyện thêm sau cùng có ID lớn nhất sẽ lên đầu)
            return Integer.compare(c2.getId(), c1.getId());
        });
        adapter.setComicList(displayedList);
    }

    private void sortRatingLocal() {
        displayedList.sort((c1, c2) -> Float.compare(c2.getRating(), c1.getRating()));
        adapter.setComicList(displayedList);
    }

    private void openDetailComic(Comic comic) {
        if (comic == null) return;
        Intent intent = new Intent(requireContext(), DetailComicActivity.class);
        intent.putExtra("COMIC_ID", comic.getId());
        intent.putExtra("COMIC_COVER_URL", comic.getCoverUrl());
        startActivity(intent);
    }

    private static class FilterCategoryAdapter extends RecyclerView.Adapter<FilterCategoryAdapter.ViewHolder> {
        private final List<CategoryResponse> categories;
        private final Set<Integer> selectedIds;
        private final OnCategoryToggleListener listener;

        interface OnCategoryToggleListener {
            void onToggle(int categoryId);
        }

        public FilterCategoryAdapter(List<CategoryResponse> categories, Set<Integer> selectedIds, OnCategoryToggleListener listener) {
            this.categories = categories;
            this.selectedIds = selectedIds;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_filter_genre, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryResponse category = categories.get(position);
            holder.tvName.setText(category.getName());
            holder.checkBox.setChecked(selectedIds.contains(category.getId()));

            holder.itemView.setOnClickListener(v -> {
                listener.onToggle(category.getId());
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            CheckBox checkBox;
            ViewHolder(View view) {
                super(view);
                tvName = view.findViewById(R.id.tvGenreName);
                checkBox = view.findViewById(R.id.cbGenre);
            }
        }
    }
}
