package com.nhom5.ftcomic.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.nhom5.ftcomic.models.Comic;

import java.util.List;

@Dao
public interface ComicDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComic(Comic comic);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComics(List<Comic> comics);

    @Update
    void updateComic(Comic comic);

    @Query("SELECT * FROM comics ORDER BY id ASC")
    LiveData<List<Comic>> getAllComics();

    @Query("SELECT * FROM comics WHERE section = :section ORDER BY id ASC")
    LiveData<List<Comic>> getComicsBySection(String section);

    @Query("SELECT * FROM comics WHERE id = :comicId LIMIT 1")
    LiveData<Comic> getComicByIdLive(int comicId);

    @Query("SELECT * FROM comics WHERE id = :comicId LIMIT 1")
    Comic getComicById(int comicId);

    @Query("SELECT * FROM comics WHERE name LIKE '%' || :keyword || '%' ORDER BY id ASC")
    LiveData<List<Comic>> searchComics(String keyword);

    @Query("SELECT COUNT(*) FROM comics")
    int countComics();

    @Query("DELETE FROM comics")
    void deleteAllComics();

    @Query("UPDATE comics SET commentCount = :commentCount WHERE id = :comicId")
    void updateCommentCount(int comicId, int commentCount);

    @Query("UPDATE comics SET commentCount = commentCount + 1 WHERE id = :comicId")
    void increaseCommentCount(int comicId);

    @Query("UPDATE comics SET likeCount = :likeCount WHERE id = :comicId")
    void updateLikeCount(int comicId, int likeCount);

    @Query("UPDATE comics SET likeCount = likeCount + 1 WHERE id = :comicId")
    void increaseLikeCount(int comicId);

    @Query("UPDATE comics SET likeCount = CASE WHEN likeCount > 0 THEN likeCount - 1 ELSE 0 END WHERE id = :comicId")
    void decreaseLikeCount(int comicId);
}