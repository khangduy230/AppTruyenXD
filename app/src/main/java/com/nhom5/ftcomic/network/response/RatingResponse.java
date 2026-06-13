package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;

public class RatingResponse {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("comic_id")
    private int comicId;

    @SerializedName("rating")
    private int rating;

    public String getUserId() {
        return userId;
    }

    public int getComicId() {
        return comicId;
    }

    public int getRating() {
        return rating;
    }
}