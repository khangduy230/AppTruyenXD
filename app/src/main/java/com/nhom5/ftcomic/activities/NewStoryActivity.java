package com.nhom5.ftcomic.activities;

import android.os.Bundle;
import android.view.View;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.nhom5.ftcomic.R;
import com.google.android.material.appbar.MaterialToolbar;

public class NewStoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_story);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Xử lý nút quay lại
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Xử lý nút Thêm thể loại
        View btnAddGenre = findViewById(R.id.btnAddGenre);
        if (btnAddGenre != null) {
            btnAddGenre.setOnClickListener(v -> showAddCategoryBottomSheet());
        }
    }

    private void showAddCategoryBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.item_add_category, null);
        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }
}