package com.nhom5.ftcomic.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.activities.DetailComicActivity;
import com.nhom5.ftcomic.adapters.CategoryAdapter;
import com.nhom5.ftcomic.adapters.ComicAdapter;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.response.CategoryResponse;
import com.nhom5.ftcomic.network.response.ComicCategoryResponse;
import com.nhom5.ftcomic.network.response.ComicResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private TextInputEditText edtSearch;
    private TextView tvSearchStatus, tvSearchResultTitle, tvCategoryTitle;
    private RecyclerView recyclerViewCategories, recyclerViewSearchResults;

    private CategoryAdapter categoryAdapter;
    private ComicAdapter searchComicAdapter;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private boolean isSelectingCategory = false;

    public SearchFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupRecyclerViews();
        setupSearchInput();
        loadCategoriesFromSupabase();
    }

    private void bindViews(View view) {
        edtSearch = view.findViewById(R.id.edtSearch);
        tvSearchStatus = view.findViewById(R.id.tvSearchStatus);
        tvSearchResultTitle = view.findViewById(R.id.tvSearchResultTitle);
        tvCategoryTitle = view.findViewById(R.id.tvCategoryTitle);
        recyclerViewCategories = view.findViewById(R.id.recyclerView_categories);
        recyclerViewSearchResults = view.findViewById(R.id.recyclerView_search_results);
    }

    private void setupRecyclerViews() {
        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        float density = getResources().getDisplayMetrics().density;
        int itemWidthPx = (int) (115 * density); // Giảm xuống 115dp để dễ nhảy lên 3 cột
        int spanCount = Math.max(2, screenWidthPx / itemWidthPx);

        searchComicAdapter = new ComicAdapter(new ArrayList<>(), this::openDetailComic);
        recyclerViewSearchResults.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        recyclerViewSearchResults.setNestedScrollingEnabled(false);
        recyclerViewSearchResults.setAdapter(searchComicAdapter);

        categoryAdapter = new CategoryAdapter(new ArrayList<>(), category -> {
            isSelectingCategory = true;
            if (edtSearch.getText() != null) edtSearch.getText().clear();
            searchComicsByCategory(category);
        });
        recyclerViewCategories.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerViewCategories.setAdapter(categoryAdapter);
    }


    private void setupSearchInput() {
        edtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                isSelectingCategory = false;
                searchComicsByKeyword(getSearchKeyword());
                return true;
            }
            return false;
        });

        edtSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSelectingCategory) {
                    return;
                }

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    String keyword = getSearchKeyword();

                    if (keyword.isEmpty()) {
                        showCategoryMode();
                    } else {
                        searchComicsByKeyword(keyword);
                    }
                };

                searchHandler.postDelayed(searchRunnable, 500);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
    }

    private String getSearchKeyword() {
        if (edtSearch.getText() == null) {
            return "";
        }
        return edtSearch.getText().toString().trim();
    }

    private void loadCategoriesFromSupabase() {
        tvSearchStatus.setText("Đang tải thể loại...");

        SupabaseClient.getApi()
                .getAllCategories("name.asc")
                .enqueue(new Callback<List<CategoryResponse>>() {
                    @Override
                    public void onResponse(Call<List<CategoryResponse>> call,
                                           Response<List<CategoryResponse>> response) {
                        Log.d("SEARCH_API", "Categories URL: " + call.request().url());
                        Log.d("SEARCH_API", "Categories code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            categoryAdapter.setCategoryList(response.body());
                            tvSearchStatus.setText("Nhập tên truyện hoặc chọn thể loại");
                        } else {
                            tvSearchStatus.setText("Không tải được thể loại");
                            logErrorBody(response, "Categories error");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CategoryResponse>> call, Throwable t) {
                        tvSearchStatus.setText("Lỗi kết nối Supabase");
                        Log.e("SEARCH_API", "Load categories failed", t);
                    }
                });
    }

    private void searchComicsByKeyword(String keyword) {
        isSelectingCategory = false;

        if (keyword == null || keyword.trim().isEmpty()) {
            showCategoryMode();
            return;
        }

        tvSearchStatus.setText("Đang tìm kiếm...");
        showSearchMode();

        SupabaseClient.getApi()
                .searchComics("ilike.*" + keyword + "*", "id.asc")
                .enqueue(new Callback<List<ComicResponse>>() {
                    @Override
                    public void onResponse(Call<List<ComicResponse>> call,
                                           Response<List<ComicResponse>> response) {
                        Log.d("SEARCH_API", "Search URL: " + call.request().url());
                        Log.d("SEARCH_API", "Search code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            List<Comic> comics = mapComicResponsesToComics(response.body());
                            searchComicAdapter.setComicList(comics);

                            if (comics.isEmpty()) {
                                tvSearchStatus.setText("Không tìm thấy truyện phù hợp");
                            } else {
                                tvSearchStatus.setText("Tìm thấy " + comics.size() + " truyện");
                            }
                        } else {
                            tvSearchStatus.setText("Tìm kiếm thất bại");
                            logErrorBody(response, "Search error");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicResponse>> call, Throwable t) {
                        tvSearchStatus.setText("Lỗi kết nối khi tìm kiếm");
                        Log.e("SEARCH_API", "Search failed", t);
                    }
                });
    }

    private void searchComicsByCategory(CategoryResponse category) {
        if (category == null) {
            return;
        }

        tvSearchStatus.setText("Đang lọc thể loại: " + category.getName());
        showSearchMode();

        SupabaseClient.getApi()
                .getComicCategoryRefs("eq." + category.getId())
                .enqueue(new Callback<List<ComicCategoryResponse>>() {
                    @Override
                    public void onResponse(Call<List<ComicCategoryResponse>> call,
                                           Response<List<ComicCategoryResponse>> response) {
                        Log.d("SEARCH_API", "Category refs URL: " + call.request().url());
                        Log.d("SEARCH_API", "Category refs code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            List<ComicCategoryResponse> refs = response.body();

                            if (refs.isEmpty()) {
                                searchComicAdapter.setComicList(new ArrayList<>());
                                tvSearchStatus.setText("Thể loại " + category.getName() + " chưa có truyện");
                                return;
                            }

                            String idFilter = buildComicIdFilter(refs);
                            loadComicsByIds(idFilter, category.getName());
                        } else {
                            tvSearchStatus.setText("Lọc thể loại thất bại");
                            logErrorBody(response, "Category refs error");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicCategoryResponse>> call, Throwable t) {
                        tvSearchStatus.setText("Lỗi kết nối khi lọc thể loại");
                        Log.e("SEARCH_API", "Category refs failed", t);
                    }
                });
    }

    private String buildComicIdFilter(List<ComicCategoryResponse> refs) {
        StringBuilder builder = new StringBuilder();
        builder.append("in.(");

        for (int i = 0; i < refs.size(); i++) {
            builder.append(refs.get(i).getComicId());

            if (i < refs.size() - 1) {
                builder.append(",");
            }
        }

        builder.append(")");
        return builder.toString();
    }

    private void loadComicsByIds(String idFilter, String categoryName) {
        SupabaseClient.getApi()
                .getComicsByIds(idFilter, "id.asc")
                .enqueue(new Callback<List<ComicResponse>>() {
                    @Override
                    public void onResponse(Call<List<ComicResponse>> call,
                                           Response<List<ComicResponse>> response) {
                        Log.d("SEARCH_API", "Comics by ids URL: " + call.request().url());
                        Log.d("SEARCH_API", "Comics by ids code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            List<Comic> comics = mapComicResponsesToComics(response.body());
                            searchComicAdapter.setComicList(comics);

                            if (comics.isEmpty()) {
                                tvSearchStatus.setText("Thể loại " + categoryName + " chưa có truyện");
                            } else {
                                tvSearchStatus.setText("Thể loại " + categoryName + ": " + comics.size() + " truyện");
                            }
                        } else {
                            tvSearchStatus.setText("Không lấy được truyện theo thể loại");
                            logErrorBody(response, "Comics by ids error");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicResponse>> call, Throwable t) {
                        tvSearchStatus.setText("Lỗi kết nối khi lấy truyện");
                        Log.e("SEARCH_API", "Load comics by ids failed", t);
                    }
                });
    }


    private List<Comic> mapComicResponsesToComics(List<ComicResponse> responses) {
        List<Comic> comics = new ArrayList<>();

        for (ComicResponse item : responses) {
            Comic comic = new Comic(
                    item.getId(),
                    R.drawable.thientai,
                    item.getCoverUrl(),
                    item.getName(),
                    item.getAuthor(),
                    item.getDescription(),
                    item.getStatus(),
                    item.getSection(),
                    item.getLikeCount(),
                    item.getRating(),
                    item.getRatingCount(),
                    item.getCommentCount(),
                    item.getViewCount()
            );

            comics.add(comic);
        }

        return comics;
    }

    private void showSearchMode() {
        tvSearchResultTitle.setVisibility(View.VISIBLE);
        recyclerViewSearchResults.setVisibility(View.VISIBLE);
        tvCategoryTitle.setVisibility(View.VISIBLE);
        recyclerViewCategories.setVisibility(View.VISIBLE);
    }

    private void showCategoryMode() {
        isSelectingCategory = false;
        searchComicAdapter.setComicList(new ArrayList<>());
        tvSearchResultTitle.setVisibility(View.GONE);
        recyclerViewSearchResults.setVisibility(View.GONE);
        tvSearchStatus.setText("Nhập tên truyện hoặc chọn thể loại");
        tvCategoryTitle.setVisibility(View.VISIBLE);
        recyclerViewCategories.setVisibility(View.VISIBLE);
    }

    private void openDetailComic(Comic comic) {
        Intent intent = new Intent(requireContext(), DetailComicActivity.class);
        intent.putExtra("COMIC_ID", comic.getId());
        startActivity(intent);
    }

    private void logErrorBody(Response<?> response, String tag) {
        try {
            if (response.errorBody() != null) {
                Log.e("SEARCH_API", tag + ": " + response.errorBody().string());
            }
        } catch (Exception e) {
            Log.e("SEARCH_API", "Không đọc được error body", e);
        }
    }
}