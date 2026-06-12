package com.nhom5.ftcomic.network.request;

import com.google.gson.annotations.SerializedName;

public class FavoriteRequest {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("comic_id")
    private int comicId;

    public FavoriteRequest(String userId, int comicId) {
        this.userId = userId;
        this.comicId = comicId;
    }
}