package com.nhom5.ftcomic.network.request;

import com.google.gson.annotations.SerializedName;

public class CommentRequest {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("comic_id")
    private int comicId;

    @SerializedName("parent_id")
    private Integer parentId;

    @SerializedName("content")
    private String content;

    @SerializedName("chapter_id")
    private int chapterId;

    @SerializedName("chapter_name")
    private String chapterName;

    @SerializedName("username")
    private String username;

    @SerializedName("avatar_url")
    private String avatarUrl;

    public CommentRequest(String userId, int comicId, int parentId, String username, String avatarUrl,
                          String content, int chapterId, String chapterName) {
        this.userId = userId;
        this.comicId = comicId;
        this.parentId = parentId;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.content = content;
        this.chapterId = chapterId;
        this.chapterName = chapterName;
    }
}