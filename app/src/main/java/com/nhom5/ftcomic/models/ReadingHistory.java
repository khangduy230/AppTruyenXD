package com.nhom5.ftcomic.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "reading_history")
public class ReadingHistory {

    @PrimaryKey
    private int comicId;

    private int chapterId;
    private int pageNumber;
    private long lastReadAt;

    public ReadingHistory() {
    }

    @Ignore
    public ReadingHistory(int comicId, int chapterId, int pageNumber, long lastReadAt) {
        this.comicId = comicId;
        this.chapterId = chapterId;
        this.pageNumber = pageNumber;
        this.lastReadAt = lastReadAt;
    }

    public int getComicId() {
        return comicId;
    }

    public int getChapterId() {
        return chapterId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public long getLastReadAt() {
        return lastReadAt;
    }

    public void setComicId(int comicId) {
        this.comicId = comicId;
    }

    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public void setLastReadAt(long lastReadAt) {
        this.lastReadAt = lastReadAt;
    }
}