package com.nhom5.ftcomic.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.models.Favorite;

import java.util.List;

@Dao
public interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavorite(Favorite favorite);

    @Query("DELETE FROM favorites WHERE comicId = :comicId")
    void deleteFavoriteByComicId(int comicId);

    @Query("SELECT COUNT(*) FROM favorites WHERE comicId = :comicId")
    LiveData<Integer> isFavoriteLive(int comicId);

    @Query("SELECT COUNT(*) FROM favorites WHERE comicId = :comicId")
    int isFavorite(int comicId);

    @Query("SELECT comics.* FROM comics " +
            "INNER JOIN favorites ON comics.id = favorites.comicId " +
            "ORDER BY favorites.createdAt DESC")
    LiveData<List<Comic>> getFavoriteComics();
}