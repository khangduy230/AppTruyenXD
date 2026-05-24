package com.nhom5.ftcomic.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ratings")
public class Rating {
    @PrimaryKey
    private int comicId;
    private float userStars;

    public Rating(int comicId, float userStars) {
        this.comicId = comicId;
        this.userStars = userStars;
    }

    public int getComicId() { return comicId; }
    public void setComicId(int comicId) { this.comicId = comicId; }

    public float getUserStars() { return userStars; }
    public void setUserStars(float userStars) { this.userStars = userStars; }
}