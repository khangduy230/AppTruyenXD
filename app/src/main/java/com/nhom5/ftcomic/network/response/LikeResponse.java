package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;

public class LikeResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("comic_id")
    private int comicId;

    public int getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public int getComicId() {
        return comicId;
    }
}