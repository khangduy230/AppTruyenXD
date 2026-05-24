package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;

public class ComicResponse {

    @SerializedName("id")
    private int id;

    // Bảng comics của bạn đang dùng name
    @SerializedName("name")
    private String name;

    @SerializedName("author")
    private String author;

    @SerializedName("description")
    private String description;

    @SerializedName("cover_url")
    private String coverUrl;

    @SerializedName("status")
    private String status;

    @SerializedName("section")
    private String section;

    @SerializedName("like_count")
    private int likeCount;

    @SerializedName("rating")
    private float rating;

    @SerializedName("comment_count")
    private int commentCount;

    @SerializedName("view_count")
    private int viewCount;

    public int getId() {
        return id;
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

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getSection() {
        if (section == null || section.isEmpty()) {
            return "all";
        }
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
}