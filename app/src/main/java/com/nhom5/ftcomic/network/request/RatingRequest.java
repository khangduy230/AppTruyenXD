package com.nhom5.ftcomic.network.request;

import com.google.gson.annotations.SerializedName;

public class RatingRequest {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("comic_id")
    private int comicId;

    @SerializedName("rating")
    private int rating;

    public RatingRequest(String userId, int comicId, int rating) {
        this.userId = userId;
        this.comicId = comicId;
        this.rating = rating;
    }
}