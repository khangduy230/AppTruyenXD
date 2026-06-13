package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;

public class ComicResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("author")
    private String author;

    @SerializedName("description")
    private String description;

    @SerializedName("status")
    private String status;

    @SerializedName("section")
    private String section;

    @SerializedName("cover_url")
    private String coverUrl;

    @SerializedName("like_count")
    private int likeCount;

    @SerializedName("rating_avg")
    private float rating;

    @SerializedName("rating_count")
    private int ratingCount;

    @SerializedName("comment_count")
    private int commentCount;

    @SerializedName("view_count")
    private int viewCount;

    public int getId() {
        return id;
    }

    public String getName() {
        return safe(name, "Chưa có tên");
    }

    public String getAuthor() {
        return safe(author, "Đang cập nhật");
    }

    public String getDescription() {
        return safe(description, "Chưa có mô tả.");
    }

    public String getStatus() {
        return safe(status, "Đang ra");
    }

    public String getSection() {
        return safe(section, "all");
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public float getRating() {
        return rating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public int getViewCount() {
        return viewCount;
    }

    private String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }
}