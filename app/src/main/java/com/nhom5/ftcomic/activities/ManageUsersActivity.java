package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ManageUsersAdapter;
import com.nhom5.ftcomic.models.User;
import com.nhom5.ftcomic.network.SupabaseClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private TabLayout tabLayoutRoles;
    private TextInputEditText etSearch;
    private ManageUsersAdapter adapter;

    private List<User> fullUserList = new ArrayList<>();
    private List<User> filteredUserList = new ArrayList<>();

    private String currentSelectedRole = "user";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        initViews();
        setupToolbar();
        setupListeners();
        fetchUsersFromSupabase();
    }

    private void initViews() {
        rvUsers = findViewById(R.id.rvUsers);
        tabLayoutRoles = findViewById(R.id.tabLayoutRoles);
        etSearch = findViewById(R.id.etSearch);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupListeners() {
        tabLayoutRoles.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        currentSelectedRole = "user";
                        break;
                    case 1:
                        currentSelectedRole = "translator";
                        break;
                    case 2:
                        currentSelectedRole = "admin";
                        break;
                }
                applyFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchUsersFromSupabase() {
        // ĐÃ SỬA: Chỉ lấy các tài khoản có cột is_deleted bằng false
        SupabaseClient.getApi(this).getAllProfiles("username.asc", "eq.false").enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullUserList = response.body();
                    setupAdapter();
                } else {
                    Toast.makeText(ManageUsersActivity.this, "Không thể tải danh sách tài khoản", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(ManageUsersActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAdapter() {
        adapter = new ManageUsersAdapter(filteredUserList, new ManageUsersAdapter.OnUserActionListener() {
            @Override
            public void onEdit(User user, int position) {
                Intent intent = new Intent(ManageUsersActivity.this, UserProfileActivity.class);
                intent.putExtra("USER_ID", user.getId());
                intent.putExtra("USER_NAME", user.getUsername());
                intent.putExtra("USER_ROLE", user.getRole());
                startActivity(intent);
            }

            @Override
            public void onDelete(User user, int position) {
                showDeleteConfirmationDialog(user, position);
            }
        });
        rvUsers.setAdapter(adapter);
        applyFilter();
    }

    private void applyFilter() {
        filteredUserList.clear();
        for (User user : fullUserList) {
            boolean matchesRole = user.getRole() != null && user.getRole().equals(currentSelectedRole);
            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    (user.getUsername() != null && user.getUsername().toLowerCase().contains(currentSearchQuery)) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(currentSearchQuery));

            if (matchesRole && matchesSearch) {
                filteredUserList.add(user);
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showDeleteConfirmationDialog(User user, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Vô hiệu hóa tài khoản")
                .setMessage("Bạn có chắc chắn muốn xóa mềm (vô hiệu hóa) người dùng này?")
                .setPositiveButton("Xóa", (dialog, which) -> {


                    Map<String, Object> body = new HashMap<>();
                    body.put("is_deleted", true);


                    SupabaseClient.getApi(this).softDeleteProfile("eq." + user.getId(), body).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                fullUserList.remove(user);
                                applyFilter();
                                Toast.makeText(ManageUsersActivity.this, "Đã vô hiệu hóa tài khoản", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ManageUsersActivity.this, "Thao tác thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(ManageUsersActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Huỷ", (dialog, which) -> dialog.dismiss())
                .show();
    }
}