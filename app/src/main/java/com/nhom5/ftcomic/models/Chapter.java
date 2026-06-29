package com.nhom5.ftcomic.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "chapters")
public class Chapter {

    @PrimaryKey
    private int id;

    private int comicId;
    private int chapterNumber;
    private String chapterName;
    private String updatedAt;
    private boolean isHidden;

    public Chapter() {
        // Room cần constructor rỗng
    }

    @Ignore
    public Chapter(int id, int comicId, int chapterNumber, String chapterName, String updatedAt) {
        this(id, comicId, chapterNumber, chapterName, updatedAt, false);
    }

    @Ignore
    public Chapter(int id, int comicId, int chapterNumber, String chapterName, String updatedAt, boolean isHidden) {
        this.id = id;
        this.comicId = comicId;
        this.chapterNumber = chapterNumber;
        this.chapterName = chapterName;
        this.updatedAt = updatedAt;
        this.isHidden = isHidden;
    }

    public int getId() {
        return id;
    }

    public int getComicId() {
        return comicId;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public String getChapterName() {
        return chapterName;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setComicId(int comicId) {
        this.comicId = comicId;
    }

    public void setChapterNumber(int chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public void setChapterName(String chapterName) {
        this.chapterName = chapterName;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }
}
