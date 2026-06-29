package com.nhom5.ftcomic.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.nhom5.ftcomic.models.Chapter;

import java.util.List;

@Dao
public interface ChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChapter(Chapter chapter);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChapters(List<Chapter> chapters);

    @Query("SELECT * FROM chapters WHERE comicId = :comicId AND isHidden = 0 ORDER BY chapterNumber ASC")
    LiveData<List<Chapter>> getChaptersByComicId(int comicId);

    @Query("SELECT * FROM chapters WHERE comicId = :comicId ORDER BY chapterNumber ASC")
    LiveData<List<Chapter>> getAllChaptersByComicId(int comicId);

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    Chapter getChapterById(int chapterId);

    @Query("SELECT * FROM chapters WHERE comicId = :comicId AND isHidden = 0 ORDER BY chapterNumber ASC LIMIT 1")
    Chapter getFirstChapterByComicId(int comicId);

    @Query("DELETE FROM chapters WHERE comicId = :comicId")
    void deleteChaptersByComicId(int comicId);
}
