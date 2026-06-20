package com.nhom5.ftcomic.activities;

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

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.nhom5.ftcomic.R;
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

/**
 * Hoạt động hiển thị tất cả truyện với các chức năng lọc và sắp xếp.
 */
public class AllStoryActivity extends AppCompatActivity {
    // Khai báo các thành phần giao diện
    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewAllStory;
    private ChipGroup chipGroup;
    private Chip chipFilter;

    // Adapter và Repository để quản lý dữ liệu truyện
    private ComicAdapter comicAdapter;
    private FilterCategoryAdapter filterCategoryAdapter; // Tham chiếu để cập nhật danh sách thể loại trong BottomSheet

    // Danh sách dữ liệu
    private List<Comic> fullComicList = new ArrayList<>(); // Danh sách gốc từ server
    private List<Comic> displayedList = new ArrayList<>(); // Danh sách đang được hiển thị (đã lọc/sắp xếp)

    // Dữ liệu thể loại và các ID thể loại đang được chọn
    private final List<CategoryResponse> categoriesFromApi = new ArrayList<>();
    private final Set<Integer> selectedCategoryIds = new HashSet<>();

    // Loại sắp xếp hiện tại: 0: A-Z, 1: Mới nhất (mặc định), 2: Đánh giá
    private int currentSortType = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_story);

        // Xử lý Edge-to-Edge để giao diện không bị đè bởi thanh hệ thống
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);
                return insets;
            });
        }

        initViews();               // Khởi tạo các View
        setupToolbar();           // Thiết lập thanh công cụ
        setupRecyclerView();      // Thiết lập danh sách truyện
        setupData();              // Lấy dữ liệu truyện từ Repository
        setupListeners();         // Thiết lập các sự kiện click
        loadCategoriesFromSupabase(); // Tải danh sách thể loại cho bộ lọc
    }

    /**
     * Tìm và ánh xạ các view từ XML.
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbarAllStory);
        recyclerViewAllStory = findViewById(R.id.recyclerView_all_story);
        chipGroup = findViewById(R.id.chipGroup);
        chipFilter = findViewById(R.id.chip_filter);
    }

    /**
     * Thiết lập Toolbar với nút quay lại.
     */
    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    /**
     * Khởi tạo RecyclerView hiển thị truyện theo dạng lưới (Grid).
     */
    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        recyclerViewAllStory.setLayoutManager(gridLayoutManager);
        comicAdapter = new ComicAdapter(new ArrayList<>(), comic -> {
            // Việc điều hướng đến chi tiết truyện đã được ComicAdapter xử lý tự động
        });
        recyclerViewAllStory.setAdapter(comicAdapter);
    }

    /**
     * Lấy dữ liệu truyện từ local database (Room) thông qua Repository và đồng bộ từ server.
     */
    private void setupData() {
        ComicRepository comicRepository = new ComicRepository(this);
        comicRepository.getAllComicsLive().observe(this, comics -> {
            if (comics != null) {
                fullComicList = new ArrayList<>(comics);
                applyFilters(); // Áp dụng bộ lọc ngay khi có dữ liệu mới
            }
        });
        comicRepository.syncAllComicsFromSupabase();
    }

    /**
     * Cập nhật danh sách hiển thị và thực hiện sắp xếp.
     */
    private void updateDisplayedList(List<Comic> newList) {
        displayedList = new ArrayList<>(newList);
        applyCurrentSort();
    }

    /**
     * Thực hiện sắp xếp danh sách dựa trên loại đã chọn.
     */
    private void applyCurrentSort() {
        if (currentSortType == 0) sortAlphabeticalLocal();
        else if (currentSortType == 1) sortNewestLocal();
        else if (currentSortType == 2) sortRatingLocal();
        else comicAdapter.setComicList(displayedList);
    }

    /**
     * Thiết lập các bộ lắng nghe sự kiện cho Chip và Nút lọc.
     */
    private void setupListeners() {
        if (chipFilter != null) {
            chipFilter.setOnClickListener(v -> showFilterBottomSheet());
        }

        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.contains(R.id.chip_new_update)) {
                    currentSortType = 1; // Sắp xếp mới nhất
                    applyFilters();
                } else {
                    applyFilters();      // Quay về mặc định nếu bỏ chọn
                }
            });
        }
    }

    /**
     * Tải danh sách các thể loại truyện từ API Supabase.
     */
    private void loadCategoriesFromSupabase() {
        SupabaseClient.getApi().getAllCategories("name.asc").enqueue(new Callback<List<CategoryResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<CategoryResponse>> call, @NonNull Response<List<CategoryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoriesFromApi.clear();
                    categoriesFromApi.addAll(response.body());
                    // Cập nhật lại giao diện bộ lọc nếu nó đang mở
                    if (filterCategoryAdapter != null) {
                        filterCategoryAdapter.notifyDataSetChanged();
                    }
                    Log.d("ALL_STORY", "Tải thể loại thành công: " + categoriesFromApi.size());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CategoryResponse>> call, @NonNull Throwable t) {
                Log.e("ALL_STORY", "Lỗi tải thể loại", t);
            }
        });
    }

    /**
     * Hiển thị bảng điều khiển bộ lọc (Bottom Sheet).
     */
    private void showFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.item_filter, null, false);
        bottomSheetDialog.setContentView(view);

        // Làm cho nền của BottomSheet trong suốt để thấy được bo góc của drawable custom
        int bottomSheetId = getResources().getIdentifier("design_bottom_sheet", "id", "com.google.android.material");
        View bottomSheetInternal = bottomSheetDialog.findViewById(bottomSheetId);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
        }

        // Ánh xạ các view trong BottomSheet
        RelativeLayout headerSort = view.findViewById(R.id.headerSort);
        LinearLayout layoutSortOptions = view.findViewById(R.id.layoutSortOptions);
        ImageView ivToggleSort = view.findViewById(R.id.ivToggleSort);

        RelativeLayout headerGenre = view.findViewById(R.id.headerGenre);
        LinearLayout layoutGenreOptions = view.findViewById(R.id.layoutGenreOptions);
        ImageView ivToggleGenre = view.findViewById(R.id.ivToggleGenre);

        RecyclerView rvFilterCategories = view.findViewById(R.id.rvFilterCategories);

        // Thiết lập trạng thái tích chọn cho các mục sắp xếp
        CheckBox cbAlpha = view.findViewById(R.id.cbSortAlpha);
        CheckBox cbNewest = view.findViewById(R.id.cbSortNewest);
        CheckBox cbRating = view.findViewById(R.id.cbSortRating);

        if (cbAlpha != null) cbAlpha.setChecked(currentSortType == 0);
        if (cbNewest != null) cbNewest.setChecked(currentSortType == 1);
        if (cbRating != null) cbRating.setChecked(currentSortType == 2);

        // Thiết lập trạng thái xoay của mũi tên dựa trên việc ẩn/hiện section
        ivToggleSort.setRotation(layoutSortOptions.getVisibility() == View.VISIBLE ? 0 : 180);
        ivToggleGenre.setRotation(layoutGenreOptions.getVisibility() == View.VISIBLE ? 0 : 180);

        // Sự kiện thu gọn/mở rộng các mục
        headerSort.setOnClickListener(v -> toggleSection(layoutSortOptions, ivToggleSort));
        headerGenre.setOnClickListener(v -> toggleSection(layoutGenreOptions, ivToggleGenre));

        // Sự kiện click cho các mục sắp xếp
        view.findViewById(R.id.itemSortAlpha).setOnClickListener(v -> {
            currentSortType = 0; applyCurrentSort(); bottomSheetDialog.dismiss();
        });
        view.findViewById(R.id.itemSortNewest).setOnClickListener(v -> {
            currentSortType = 1; applyCurrentSort(); bottomSheetDialog.dismiss();
        });
        view.findViewById(R.id.itemSortRating).setOnClickListener(v -> {
            currentSortType = 2; applyCurrentSort(); bottomSheetDialog.dismiss();
        });

        // Thiết lập RecyclerView cho danh sách thể loại trong bộ lọc
        if (rvFilterCategories != null) {
            rvFilterCategories.setLayoutManager(new LinearLayoutManager(this));
            filterCategoryAdapter = new FilterCategoryAdapter(categoriesFromApi, selectedCategoryIds, categoryId -> {
                if (selectedCategoryIds.contains(categoryId)) {
                    selectedCategoryIds.remove(categoryId);
                } else {
                    selectedCategoryIds.add(categoryId);
                }
                applyFilters(); // Thực hiện lọc ngay khi thay đổi lựa chọn
            });
            rvFilterCategories.setAdapter(filterCategoryAdapter);

            // Tải lại thể loại nếu danh sách vẫn trống
            if (categoriesFromApi.isEmpty()) {
                Toast.makeText(this, "Đang tải thể loại...", Toast.LENGTH_SHORT).show();
                loadCategoriesFromSupabase();
            }
        }

        bottomSheetDialog.show();
    }

    /**
     * Hoạt ảnh ẩn/hiện và xoay mũi tên cho các phần trong bộ lọc.
     */
    private void toggleSection(View layout, ImageView arrow) {
        if (layout.getVisibility() == View.VISIBLE) {
            layout.setVisibility(View.GONE);
            arrow.animate().rotation(180).setDuration(200).start();
        } else {
            layout.setVisibility(View.VISIBLE);
            arrow.animate().rotation(0).setDuration(200).start();
        }
    }

    /**
     * Gửi yêu cầu lọc truyện dựa trên các ID thể loại đã chọn qua Supabase API.
     */
    private void applyFilters() {
        if (selectedCategoryIds.isEmpty()) {
            updateDisplayedList(fullComicList); // Nếu không chọn gì thì hiện tất cả
            return;
        }

        // Xây dựng chuỗi filter cho Supabase, ví dụ: in.(1,2,5)
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

                    // Lọc lại danh sách truyện local dựa trên các ID hợp lệ từ server
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
                Toast.makeText(AllStoryActivity.this, "Lỗi kết nối bộ lọc", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sắp xếp danh sách theo bảng chữ cái tiếng Việt.
     */
    private void sortAlphabeticalLocal() {
        Collator collator = Collator.getInstance(new Locale("vi", "VN"));
        displayedList.sort((c1, c2) -> collator.compare(c1.getName(), c2.getName()));
        comicAdapter.setComicList(displayedList);
    }

    /**
     * Sắp xếp danh sách theo truyện mới nhất (dựa trên ID giảm dần).
     */
    private void sortNewestLocal() {
        displayedList.sort((c1, c2) -> Integer.compare(c2.getId(), c1.getId()));
        comicAdapter.setComicList(displayedList);
    }

    /**
     * Sắp xếp danh sách theo điểm đánh giá từ cao đến thấp.
     */
    private void sortRatingLocal() {
        displayedList.sort((c1, c2) -> Float.compare(c2.getRating(), c1.getRating()));
        comicAdapter.setComicList(displayedList);
    }

    /**
     * Adapter nội bộ để hiển thị danh sách thể loại có CheckBox trong bộ lọc.
     */
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

            // Cho phép chọn thể loại khi nhấn vào item
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