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

    @Query("SELECT * FROM reading_history WHERE comicId = :comicId LIMIT 1")
    ReadingHistory getHistoryByComicId(int comicId);

    @Query("SELECT comics.* FROM comics " +
            "INNER JOIN reading_history ON comics.id = reading_history.comicId " +
            "ORDER BY reading_history.lastReadAt DESC")
    LiveData<List<Comic>> getHistoryComics();

    @Query("DELETE FROM reading_history WHERE comicId = :comicId")
    void deleteHistoryByComicId(int comicId);

    @Query("SELECT * FROM reading_history WHERE comicId = :comicId LIMIT 1")
    com.nhom5.ftcomic.models.ReadingHistory getHistoryByComicIdSync(int comicId);
}