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

    public CommentRequest(String userId, int comicId, Integer parentId, String content) {
        this.userId = userId;
        this.comicId = comicId;
        this.parentId = parentId;
        this.content = content;
    }
}