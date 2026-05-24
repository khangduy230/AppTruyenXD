package com.nhom5.ftcomic.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.nhom5.ftcomic.models.Category;
import com.nhom5.ftcomic.models.Chapter;
import com.nhom5.ftcomic.models.ChapterPage;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.models.ComicCategoryCrossRef;
import com.nhom5.ftcomic.models.DownloadedChapter;
import com.nhom5.ftcomic.models.Favorite;
import com.nhom5.ftcomic.models.ReadingHistory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                Comic.class,
                Category.class,
                ComicCategoryCrossRef.class,
                Chapter.class,
                ChapterPage.class,
                Favorite.class,
                ReadingHistory.class,
                DownloadedChapter.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract ComicDao comicDao();

    public abstract CategoryDao categoryDao();

    public abstract ChapterDao chapterDao();

    public abstract ChapterPageDao chapterPageDao();

    public abstract FavoriteDao favoriteDao();

    public abstract ReadingHistoryDao readingHistoryDao();

    public abstract DownloadedChapterDao downloadedChapterDao();

    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "ftcomic_database"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}