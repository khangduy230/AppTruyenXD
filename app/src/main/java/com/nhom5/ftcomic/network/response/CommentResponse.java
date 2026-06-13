package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;

public class CommentResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("comic_id")
    private int comicId;

    @SerializedName("parent_id")
    private Integer parentId;

    @SerializedName("content")
    private String content;

    @SerializedName("created_at_millis")
    private long createdAtMillis;

    @SerializedName("username")
    private String username;

    @SerializedName("avatar_url")
    private String avatarUrl;

    public int getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public int getComicId() {
        return comicId;
    }

    public int getParentId() {
        return parentId == null ? 0 : parentId;
    }

    public String getContent() {
        return content == null ? "" : content;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public String getUsername() {
        if (username == null || username.trim().isEmpty()) {
            return "Người dùng";
        }
        return username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}