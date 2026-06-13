package com.nhom5.ftcomic.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ComicAdapter;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.repository.ComicRepository;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AllStoryActivity extends AppCompatActivity {
    // Khai báo các thành phần giao diện
    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewAllStory;
    private ChipGroup chipGroup;
    private Chip chipFilter;
    // Khai báo Adapter và Repository để xử lý dữ liệu
    private ComicAdapter comicAdapter;
    private ComicRepository comicRepository;
    // Danh sách để quản lý truyện
    private List<Comic> fullComicList = new ArrayList<>(); // Danh sách gốc lưu toàn bộ truyện tải về từ database
    private List<Comic> displayedList = new ArrayList<>(); // Danh sách phụ dùng để hiển thị (sau khi đã lọc hoặc sắp xếp)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Kích hoạt tính năng EdgeToEdge (hiển thị tràn viền)
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_story);
        // Xử lý Padding hệ thống (Status bar, Navigation bar) để layout không bị đè
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);
                return insets;
            });
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupData();
        setupListeners();
    }
    // Ánh xạ các View từ file XML vào biến Java
    private void initViews() {
        toolbar = findViewById(R.id.toolbarAllStory);
        recyclerViewAllStory = findViewById(R.id.recyclerView_all_story);
        chipGroup = findViewById(R.id.chipGroup);
        chipFilter = findViewById(R.id.chip_filter);
    }
    // Thiết lập các sự kiện lắng nghe người dùng
    private void setupListeners() {
        // Sự kiện khi bấm vào nút "Bộ lọc"
        if (chipFilter != null) {
            chipFilter.setOnClickListener(v -> showFilterBottomSheet());
        }
        // Sự kiện khi thay đổi lựa chọn trong ChipGroup
        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.contains(R.id.chip_new_update)) {
                    sortNewest();// Gọi hàm sắp xếp mới nhất
                }
            });
        }
    }
    // Hiển thị hộp thoại bộ lọc dưới dạng BottomSheet (trượt từ dưới lên)
    private void showFilterBottomSheet() {
        // Tạo BottomSheetDialog và nạp nội dung từ item_filter.xml vào layout
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.item_filter, null);
        bottomSheetDialog.setContentView(view);
        // Làm trong suốt nền mặc định của BottomSheet để hiển thị bo góc custom
        View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheetInternal != null) {
            bottomSheetInternal.setBackgroundResource(android.R.color.transparent);
        }

        // Ánh xạ các Header và Icon để xử lý đóng/mở (Toggle)
        RelativeLayout headerSort = view.findViewById(R.id.headerSort);
        LinearLayout layoutSortOptions = view.findViewById(R.id.layoutSortOptions);
        ImageView ivToggleSort = view.findViewById(R.id.ivToggleSort);

        RelativeLayout headerGenre = view.findViewById(R.id.headerGenre);
        LinearLayout layoutGenreOptions = view.findViewById(R.id.layoutGenreOptions);
        ImageView ivToggleGenre = view.findViewById(R.id.ivToggleGenre);

        // Mặc định ban đầu mũi tên hướng lên (0 độ)
        ivToggleSort.setRotation(0f);
        ivToggleGenre.setRotation(0f);
        // Gán sự kiện Click cho Header để thu gọn/mở rộng các mục
        headerSort.setOnClickListener(v -> toggleSection(layoutSortOptions, ivToggleSort));
        headerGenre.setOnClickListener(v -> toggleSection(layoutGenreOptions, ivToggleGenre));

        // Ánh xạ các tùy chọn sắp xếp bên trong BottomSheet
        TextView tvSortAlpha = view.findViewById(R.id.tvSortAlpha);
        TextView tvSortNewest = view.findViewById(R.id.tvSortNewest);
        TextView tvSortRating = view.findViewById(R.id.tvSortRating);

        // Xử lý Click cho từng kiểu sắp xếp và đóng BottomSheet sau khi chọn
        tvSortAlpha.setOnClickListener(v -> { sortAlphabetical(); bottomSheetDialog.dismiss(); });
        tvSortNewest.setOnClickListener(v -> { sortNewest(); bottomSheetDialog.dismiss(); });
        tvSortRating.setOnClickListener(v -> { sortRating(); bottomSheetDialog.dismiss(); });

        // Ánh xạ và xử lý các Checkbox lọc thể loại
        CheckBox cbMMB1 = view.findViewById(R.id.cbMMB1);
        CheckBox cbGenreNew = view.findViewById(R.id.cbGenreNew);
        View.OnClickListener filterClick = v -> applyFilters(cbMMB1.isChecked(), cbGenreNew.isChecked());
        cbMMB1.setOnClickListener(filterClick);
        cbGenreNew.setOnClickListener(filterClick);

        bottomSheetDialog.show();
    }
    // Hàm xử lý hiệu ứng xoay mũi tên và ẩn/hiện nội dung
    private void toggleSection(View layout, ImageView arrow) {
        //Nếu đang hiện thì ẩn, ngược lại hiện
        if (layout.getVisibility() == View.VISIBLE) {
            layout.setVisibility(View.GONE);
            // hướng xuống
            arrow.animate().rotation(180).setDuration(200).start(); // Quay xuống
        } else {
            layout.setVisibility(View.VISIBLE);
            //hướng lên mặc định
            arrow.animate().rotation(0).setDuration(200).start(); // Quay lên
        }
    }

    // Logic lọc danh sách truyện dựa trên các lựa chọn thể loại
    private void applyFilters(boolean mmb1, boolean newest) {
        displayedList.clear();
        displayedList.addAll(fullComicList); // Hiện tại đang reset về danh sách gốc
        comicAdapter.setComicList(displayedList); // Cập nhật lại giao diện
    }
    //Sắp xếp theo bảng chữ cái tiếng việt
    private void sortAlphabetical() {
        Collator collator = Collator.getInstance(new Locale("vi", "VN"));
        Collections.sort(displayedList, (c1, c2) -> collator.compare(c1.getName(), c2.getName()));
        comicAdapter.setComicList(displayedList);
    }
    //Sắp xếp theo mới nhất
    private void sortNewest() {
        Collections.sort(displayedList, (c1, c2) -> Integer.compare(c2.getId(), c1.getId()));
        comicAdapter.setComicList(displayedList);
    }
    //Sắp xếp theo đánh giá
    private void sortRating() {
        Collections.sort(displayedList, (c1, c2) -> Float.compare(c2.getRating(), c1.getRating()));
        comicAdapter.setComicList(displayedList);
    }

    // Cấu hình Toolbar và nút quay lại (Back)
    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }
    // Thiết lập RecyclerView để hiển thị danh sách truyện dạng lưới
    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        recyclerViewAllStory.setLayoutManager(gridLayoutManager);
        // Khởi tạo Adapter và gắn vào RecyclerView
        comicAdapter = new ComicAdapter(new ArrayList<>(), comic -> {
            android.content.Intent intent = new android.content.Intent(
                    AllStoryActivity.this,
                    DetailComicActivity.class
            );
            intent.putExtra("COMIC_ID", comic.getId());
            startActivity(intent);
        });
        recyclerViewAllStory.setAdapter(comicAdapter);
    }
    // Khởi tạo Repository và lấy dữ liệu thực tế từ database/server
    private void setupData() {
        comicRepository = new ComicRepository(this);

        comicRepository.getAllComicsLive().observe(this, comics -> {
            if (comics != null) {
                fullComicList = new ArrayList<>(comics);
                displayedList = new ArrayList<>(comics);
                comicAdapter.setComicList(displayedList);
            }
        });

        comicRepository.syncAllComicsFromSupabase();
    }
}