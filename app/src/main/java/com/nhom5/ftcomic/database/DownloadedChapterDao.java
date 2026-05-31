package com.nhom5.ftcomic.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.models.DownloadedChapter;

import java.util.List;

@Dao
public interface DownloadedChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDownloadedChapter(DownloadedChapter downloadedChapter);

    @Query("SELECT * FROM downloaded_chapters WHERE chapterId = :chapterId LIMIT 1")
    LiveData<DownloadedChapter> getDownloadedChapter(int chapterId);

    @Query("SELECT COUNT(*) FROM downloaded_chapters WHERE chapterId = :chapterId")
    int isChapterDownloaded(int chapterId);

    @Query("SELECT * FROM downloaded_chapters ORDER BY downloadedAt DESC")
    LiveData<List<DownloadedChapter>> getAllDownloadedChapters();

    @Query("SELECT comics.* FROM comics " +
            "INNER JOIN downloaded_chapters " +
            "ON comics.id = downloaded_chapters.comicId " +
            "GROUP BY comics.id " +
            "ORDER BY MAX(downloaded_chapters.downloadedAt) DESC")
    LiveData<List<Comic>> getDownloadedComics();

    @Query("DELETE FROM downloaded_chapters WHERE chapterId = :chapterId")
    void deleteDownloadedChapter(int chapterId);

    @Query("DELETE FROM downloaded_chapters WHERE comicId = :comicId")
    void deleteChaptersByComicId(int comicId);

    @Query("DELETE FROM downloaded_chapters")
    void deleteAllDownloadedChapters();
}