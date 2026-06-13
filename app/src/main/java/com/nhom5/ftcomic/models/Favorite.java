package com.nhom5.ftcomic.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(
        tableName = "favorites",
        primaryKeys = {"userId", "comicId"}
)
public class Favorite {

    @NonNull
    private String userId;

    private int comicId;
    private long createdAt;

    public Favorite() {
        this.userId = "";
    }

    @Ignore
    public Favorite(@NonNull String userId, int comicId, long createdAt) {
        this.userId = userId;
        this.comicId = comicId;
        this.createdAt = createdAt;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}