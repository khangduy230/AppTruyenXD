package com.nhom5.ftcomic.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.nhom5.ftcomic.R;

public class ManageComicsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_comics);
        
        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, 0);
                return insets;
            });
        }

        // Toolbar back button
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // FAB: Tạo truyện mới
        ExtendedFloatingActionButton fabCreateComic = findViewById(R.id.fabCreateComic);
        if (fabCreateComic != null) {
            fabCreateComic.setOnClickListener(v -> {
                Intent intent = new Intent(ManageComicsActivity.this, NewStoryActivity.class);
                startActivity(intent);
            });
        }

        // TODO: Setup RecyclerView rvComics with an adapter
    }
}