package com.nhom5.ftcomic.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloaded_chapters")
public class DownloadedChapter {

    @PrimaryKey
    private int chapterId;

    private int comicId;
    private long downloadedAt;
    private int totalPages;

    public DownloadedChapter() {
    }

    @Ignore
    public DownloadedChapter(int chapterId, int comicId, long downloadedAt, int totalPages) {
        this.chapterId = chapterId;
        this.comicId = comicId;
        this.downloadedAt = downloadedAt;
        this.totalPages = totalPages;
    }

    public int getChapterId() {
        return chapterId;
    }

    public int getComicId() {
        return comicId;
    }

    public long getDownloadedAt() {
        return downloadedAt;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }

    public void setComicId(int comicId) {
        this.comicId = comicId;
    }

    public void setDownloadedAt(long downloadedAt) {
        this.downloadedAt = downloadedAt;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}