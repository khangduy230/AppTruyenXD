package com.nhom5.ftcomic.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "comics")
public class Comic {

    @PrimaryKey
    private int id;

    private int image;
    private String coverUrl;

    private String name;
    private String author;
    private String description;
    private String status;
    private String section;

    private int likeCount;
    private float rating;
    private int ratingCount;
    private int commentCount;
    private int viewCount;

    public Comic() {
    }
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    @Ignore
    public Comic(int image, String name) {
        this.image = image;
        this.name = name;
        this.coverUrl = "";
        this.author = "Đang cập nhật";
        this.description = "Chưa có mô tả.";
        this.status = "Đang ra";
        this.section = "all";
    }

    @Ignore
    public Comic(int id, int image, String coverUrl, String name, String author,
                 String description, String status, String section, int likeCount,
                 float rating, int ratingCount, int commentCount, int viewCount) {
        this.id = id;
        this.image = image;
        this.coverUrl = coverUrl;
        this.name = name;
        this.author = author;
        this.description = description;
        this.status = status;
        this.section = section;
        this.likeCount = likeCount;
        this.rating = rating;
        this.ratingCount = ratingCount;
        this.commentCount = commentCount;
        this.viewCount = viewCount;
    }

    public int getId() {
        return id;
    }

    public int getImage() {
        return image;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getSection() {
        return section;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public float getRating() {
        return rating;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
}