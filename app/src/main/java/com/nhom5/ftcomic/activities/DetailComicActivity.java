package com.nhom5.ftcomic.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nhom5.ftcomic.R;

public class DetailComicActivity extends AppCompatActivity {

    private ImageView imgCover;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_comic);

        imgCover = findViewById(R.id.imgCover);
        tvTitle = findViewById(R.id.tvTitle);

        String comicName = getIntent().getStringExtra("COMIC_NAME");
        int comicImage = getIntent().getIntExtra("COMIC_IMAGE", R.drawable.thientai);

        tvTitle.setText(comicName);
        imgCover.setImageResource(comicImage);
    }
}