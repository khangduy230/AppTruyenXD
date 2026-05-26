package com.nhom5.ftcomic.activities;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.CommentsAdapter;

public class CommentsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);
        findViewById(R.id.topAppBar).setOnClickListener(v -> finish());
        RecyclerView recyclerView = findViewById(R.id.recyclerViewComments);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.setAdapter(new CommentsAdapter());
    }
}