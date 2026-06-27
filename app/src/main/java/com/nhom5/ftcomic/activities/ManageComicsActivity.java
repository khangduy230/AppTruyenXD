package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ManageComicAdapter;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.response.ComicResponse;
import com.nhom5.ftcomic.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageComicsActivity extends AppCompatActivity {

    private RecyclerView rvManageComics;
    private TextView tvEmptyState;
    private TextInputEditText etSearchComic;
    private ManageComicAdapter adapter;
    private SessionManager sessionManager;

    private List<ComicResponse> masterComicList = new ArrayList<>();
    private List<ComicResponse> displayComicList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_comics);

        sessionManager = new SessionManager(this);

        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);
                return insets;
            });
        }

        initViews();
        setupToolbar();
        setupSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUploadedComics();
    }

    private void initViews() {
        rvManageComics = findViewById(R.id.rvManageComics);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        etSearchComic = findViewById(R.id.etSearchComic);

        rvManageComics.setLayoutManager(new LinearLayoutManager(this));

        MaterialButton btnCreateNew = findViewById(R.id.btnCreateNew);
        if (btnCreateNew != null) {
            btnCreateNew.setOnClickListener(v -> {
                Intent intent = new Intent(ManageComicsActivity.this, NewStoryActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void fetchUploadedComics() {
        String role = sessionManager.getRole();
        String userId = sessionManager.getUserId();

        if (userId == null || userId.trim().isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("Vui lòng đăng nhập tài khoản Admin/Dịch giả để xem danh sách.");
            rvManageComics.setVisibility(View.GONE);
            return;
        }

        Call<List<ComicResponse>> call;
        if ("admin".equals(role)) {
            call = SupabaseClient.getApi(this).getAllComicsRemote("created_at.desc");
        } else {
            call = SupabaseClient.getApi(this).getComicsByUploader("eq." + userId, "created_at.desc");
        }

        call.enqueue(new Callback<List<ComicResponse>>() {
            @Override
            public void onResponse(Call<List<ComicResponse>> call, Response<List<ComicResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    masterComicList = response.body();
                    bindDataToRecyclerView();
                } else {
                    Toast.makeText(ManageComicsActivity.this, "Lỗi tải danh sách truyện: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ComicResponse>> call, Throwable t) {
                Toast.makeText(ManageComicsActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindDataToRecyclerView() {
        displayComicList.clear();
        displayComicList.addAll(masterComicList);

        if (displayComicList.isEmpty()) {
            tvEmptyState.setText("Tài khoản của bạn hiện chưa đăng tải bất kì bộ truyện nào.");
            tvEmptyState.setVisibility(View.VISIBLE);
            rvManageComics.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvManageComics.setVisibility(View.VISIBLE);
        }

        adapter = new ManageComicAdapter(displayComicList);
        rvManageComics.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearchComic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase();
                displayComicList.clear();
                for (ComicResponse item : masterComicList) {
                    if (item.getName() != null && item.getName().toLowerCase().contains(query)) {
                        displayComicList.add(item);
                    }
                }
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                tvEmptyState.setVisibility(displayComicList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}