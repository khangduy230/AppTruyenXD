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

    public String getStatus() {
        return status;
    }

    public String getSection() {
        return section;
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

    public int getCommentCount() {
        return commentCount;
    }

    public int getViewCount() {
        return viewCount;
    }
}