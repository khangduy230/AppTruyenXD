package com.nhom5.ftcomic.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.nhom5.ftcomic.models.Rating;

@Dao
public interface RatingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateRating(Rating rating);

    @Query("SELECT * FROM ratings WHERE comicId = :comicId LIMIT 1")
    LiveData<Rating> getUserRatingLive(int comicId);

    @Query("SELECT * FROM ratings WHERE comicId = :comicId LIMIT 1")
    Rating getUserRating(int comicId);

    @Query("DELETE FROM ratings WHERE comicId = :comicId")
    void deleteRatingByComicId(int comicId);
}