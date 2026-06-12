package com.nhom5.ftcomic.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(
        tableName = "reading_history",
        primaryKeys = {"userId", "comicId"}
)
public class ReadingHistory {

    @NonNull
    private String userId;

    private int comicId;
    private int chapterId;
    private int pageNumber;
    private long lastReadAt;

    public ReadingHistory() {
        this.userId = "";
    }

    @Ignore
    public ReadingHistory(@NonNull String userId, int comicId, int chapterId, int pageNumber, long lastReadAt) {
        this.userId = userId;
        this.comicId = comicId;
        this.chapterId = chapterId;
        this.pageNumber = pageNumber;
        this.lastReadAt = lastReadAt;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public int getComicId() {
        return comicId;
    }

    public void setComicId(int comicId) {
        this.comicId = comicId;
    }

    public int getChapterId() {
        return chapterId;
    }

    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public long getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(long lastReadAt) {
        this.lastReadAt = lastReadAt;
    }
}