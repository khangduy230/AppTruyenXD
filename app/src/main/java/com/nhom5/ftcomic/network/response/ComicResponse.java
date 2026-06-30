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

    @SerializedName("uploader_id")
    private String uploaderId;

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

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // lấy từ View latest_comics
    @SerializedName("last_update")
    private String lastUpdate;

    @SerializedName("profiles")
    private UploaderProfile profiles;

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

    public String getUploaderId() {
        return uploaderId;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public String getUploaderName() {
        if (profiles != null &&
                profiles.username != null &&
                !profiles.username.trim().isEmpty()) {

            return profiles.username;
        }

        return "FTComic";
    }

    private String safe(String value, String fallback) {

        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }

        return value;
    }

    public static class UploaderProfile {

        @SerializedName("username")
        public String username;

    }

}