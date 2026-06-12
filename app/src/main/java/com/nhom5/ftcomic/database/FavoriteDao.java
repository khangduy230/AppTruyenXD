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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavorites(List<Favorite> favorites);

    @Query("DELETE FROM favorites WHERE userId = :userId AND comicId = :comicId")
    void deleteFavoriteByComicId(String userId, int comicId);

    @Query("DELETE FROM favorites WHERE userId = :userId")
    void deleteFavoritesByUser(String userId);

    @Query("DELETE FROM favorites")
    void deleteAllFavorites();

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId AND comicId = :comicId")
    LiveData<Integer> isFavoriteLive(String userId, int comicId);

    @Query("SELECT COUNT(*) FROM favorites WHERE userId = :userId AND comicId = :comicId")
    int isFavorite(String userId, int comicId);

    @Query("SELECT comics.* FROM comics " +
            "INNER JOIN favorites ON comics.id = favorites.comicId " +
            "WHERE favorites.userId = :userId " +
            "ORDER BY favorites.createdAt DESC")
    LiveData<List<Comic>> getFavoriteComics(String userId);
}