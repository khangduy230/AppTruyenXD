package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;

public class FavoriteResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("comic_id")
    private int comicId;

    @SerializedName("created_at_millis")
    private long createdAtMillis;

    public int getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public int getComicId() {
        return comicId;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }
}