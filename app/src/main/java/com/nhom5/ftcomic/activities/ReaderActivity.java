package com.nhom5.ftcomic.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.adapters.ReaderPageAdapter;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.ReadingHistory;

import java.util.ArrayList;

public class ReaderActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPages;
    private ReaderPageAdapter readerPageAdapter;

    private AppDatabase appDatabase;

    private int comicId = -1;
    private int chapterId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        comicId = getIntent().getIntExtra("COMIC_ID", -1);
        chapterId = getIntent().getIntExtra("CHAPTER_ID", -1);

        appDatabase = AppDatabase.getInstance(this);

        recyclerViewPages = findViewById(R.id.recyclerViewPages);

        readerPageAdapter = new ReaderPageAdapter(new ArrayList<>());
        recyclerViewPages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPages.setAdapter(readerPageAdapter);

        observeChapterPages();
        saveReadingHistory();
    }

    private void observeChapterPages() {
        appDatabase.chapterPageDao().getPagesByChapterId(chapterId)
                .observe(this, pages -> readerPageAdapter.setPageList(pages));
    }

    private void saveReadingHistory() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            ReadingHistory history = new ReadingHistory(
                    comicId,
                    chapterId,
                    1,
                    System.currentTimeMillis()
            );

            appDatabase.readingHistoryDao().insertOrUpdateHistory(history);
        });
    }
}