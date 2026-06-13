package com.nhom5.ftcomic.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.nhom5.ftcomic.models.ChapterPage;

import java.util.List;

@Dao
public interface ChapterPageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPages(List<ChapterPage> pages);

    @Query("SELECT * FROM chapter_pages WHERE chapterId = :chapterId ORDER BY pageNumber ASC")
    LiveData<List<ChapterPage>> getPagesByChapterId(int chapterId);

    @Query("SELECT * FROM chapter_pages WHERE chapterId = :chapterId ORDER BY pageNumber ASC")
    List<ChapterPage> getPagesByChapterIdSync(int chapterId);

    @Query("SELECT COUNT(*) FROM chapter_pages WHERE chapterId = :chapterId")
    int countPagesByChapterId(int chapterId);

    @Query("UPDATE chapter_pages SET localFilePath = :localFilePath WHERE id = :pageId")
    void updateLocalFilePath(int pageId, String localFilePath);

    @Query("UPDATE chapter_pages SET localFilePath = '' WHERE chapterId = :chapterId")
    void clearLocalFilePathsByChapterId(int chapterId);

    @Query("DELETE FROM chapter_pages WHERE chapterId = :chapterId")
    void deletePagesByChapterId(int chapterId);

    @Query("UPDATE chapter_pages SET localFilePath = ''")
    void clearAllLocalFilePaths();
}