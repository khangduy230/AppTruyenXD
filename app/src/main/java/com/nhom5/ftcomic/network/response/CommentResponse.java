package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;

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

    @SerializedName("created_at")
    private String createdAt;

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
        if (createdAt == null || createdAt.isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            String target = createdAt.replace(" ", "T");
            if (target.endsWith("+00")) {
                target = target.substring(0, target.length() - 3) + "Z";
            }
            if (!target.contains("Z") && !target.contains("+") && !target.contains("-")) {
                target += "Z";
            }
            return Instant.parse(target).toEpochMilli();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
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