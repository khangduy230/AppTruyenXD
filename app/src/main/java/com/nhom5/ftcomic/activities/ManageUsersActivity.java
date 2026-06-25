package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ManageUsersAdapter;
import com.nhom5.ftcomic.models.User;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private ManageUsersAdapter adapter;
    private List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        initViews();
        setupToolbar();
        setupUsersList();
    }

    private void initViews() {
        rvUsers = findViewById(R.id.rvUsers);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupUsersList() {
        // Dữ liệu demo
        userList = new ArrayList<>();
        userList.add(new User("1", "Hoàng Trần", "ht1231@gmail.com"));
        userList.add(new User("2", "Hoàng Trần", "ht1231@gmail.com"));
        userList.add(new User("3", "Hoàng Trần", "ht1231@gmail.com"));
        userList.add(new User("4", "Hoàng Trần", "ht1231@gmail.com"));

        adapter = new ManageUsersAdapter(userList, new ManageUsersAdapter.OnUserActionListener() {
            @Override
            public void onEdit(User user) {
                // Nhảy sang User Profile có sẵn
                Intent intent = new Intent(ManageUsersActivity.this, UserProfileActivity.class);
                startActivity(intent);
            }

            @Override
            public void onDelete(User user, int position) {
                showDeleteConfirmationDialog(user, position);
            }
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);
    }

    private void showDeleteConfirmationDialog(User user, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xoá người dùng")
                .setMessage("Bạn có chắc chắn muốn xoá vĩnh viễn người dùng này?")
                .setPositiveButton("Xoá", (dialog, which) -> {
                    // Xoá demo
                    if (position >= 0 && position < userList.size()) {
                        userList.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, userList.size());
                        Toast.makeText(ManageUsersActivity.this, "Đã xoá người dùng", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Huỷ", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
