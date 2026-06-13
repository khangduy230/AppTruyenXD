package com.nhom5.ftcomic.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.nhom5.ftcomic.models.Rating;

import java.util.List;

@Dao
public interface RatingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateRating(Rating rating);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRatings(List<Rating> ratings);

    @Query("SELECT * FROM ratings WHERE userId = :userId AND comicId = :comicId LIMIT 1")
    LiveData<Rating> getUserRatingLive(String userId, int comicId);

    @Query("SELECT * FROM ratings WHERE userId = :userId AND comicId = :comicId LIMIT 1")
    Rating getUserRating(String userId, int comicId);

    @Query("DELETE FROM ratings WHERE userId = :userId AND comicId = :comicId")
    void deleteRatingByComicId(String userId, int comicId);

    @Query("DELETE FROM ratings WHERE userId = :userId")
    void deleteRatingsByUser(String userId);

    @Query("DELETE FROM ratings")
    void deleteAllRatings();
}