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

    @SerializedName("chapter_id")
    private int chapterId;

    @SerializedName("chapter_name")
    private String chapterName;

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

    public long getCreatedAt() {
        return createdAtMillis;
    }

    public String getUserName() {
        if (username == null || username.trim().isEmpty()) {
            return "Người dùng ẩn danh";
        }
        return username;
    }

    public String getAvatarUri() {
        return avatarUrl;
    }

    public int getChapterId() {
        return chapterId;
    }

    public String getChapterName() {
        return chapterName;
    }
}