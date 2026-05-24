package com.nhom5.ftcomic.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "chapter_pages")
public class ChapterPage {

    @PrimaryKey
    private int id;

    private int chapterId;
    private int pageNumber;
    private int image;
    private String imageUrl;
    private String localFilePath;

    public ChapterPage() {
    }

    @Ignore
    public ChapterPage(int id, int chapterId, int pageNumber, int image,
                       String imageUrl, String localFilePath) {
        this.id = id;
        this.chapterId = chapterId;
        this.pageNumber = pageNumber;
        this.image = image;
        this.imageUrl = imageUrl;
        this.localFilePath = localFilePath;
    }

    public int getId() {
        return id;
    }

    public int getChapterId() {
        return chapterId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getImage() {
        return image;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }
}