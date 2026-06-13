package com.nhom5.ftcomic.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(
        tableName = "ratings",
        primaryKeys = {"userId", "comicId"}
)
public class Rating {

    @NonNull
    private String userId;

    private int comicId;
    private float userStars;

    public Rating() {
        this.userId = "";
    }

    @Ignore
    public Rating(@NonNull String userId, int comicId, float userStars) {
        this.userId = userId;
        this.comicId = comicId;
        this.userStars = userStars;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public int getComicId() {
        return comicId;
    }

    public void setComicId(int comicId) {
        this.comicId = comicId;
    }

    public float getUserStars() {
        return userStars;
    }

    public void setUserStars(float userStars) {
        this.userStars = userStars;
    }
}