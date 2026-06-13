package com.nhom5.ftcomic.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.models.ReadingHistory;

import java.util.List;

@Dao
public interface ReadingHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateHistory(ReadingHistory history);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertHistories(List<ReadingHistory> histories);

    @Query("SELECT * FROM reading_history WHERE userId = :userId AND comicId = :comicId LIMIT 1")
    ReadingHistory getHistoryByComicId(String userId, int comicId);

    @Query("SELECT * FROM reading_history WHERE userId = :userId AND comicId = :comicId LIMIT 1")
    ReadingHistory getHistoryByComicIdSync(String userId, int comicId);

    @Query("SELECT comics.* FROM comics " +
            "INNER JOIN reading_history ON comics.id = reading_history.comicId " +
            "WHERE reading_history.userId = :userId " +
            "ORDER BY reading_history.lastReadAt DESC")
    LiveData<List<Comic>> getHistoryComics(String userId);

    @Query("DELETE FROM reading_history WHERE userId = :userId AND comicId = :comicId")
    void deleteHistoryByComicId(String userId, int comicId);

    @Query("DELETE FROM reading_history WHERE userId = :userId")
    void deleteHistoriesByUser(String userId);

    @Query("DELETE FROM reading_history")
    void deleteAllHistories();
}