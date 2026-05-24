package com.nhom5.ftcomic.models;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class Favorite {

    @PrimaryKey
    private int comicId;

    private long createdAt;

    public Favorite() {
    }

    @Ignore
    public Favorite(int comicId, long createdAt) {
        this.comicId = comicId;
        this.createdAt = createdAt;
    }

    public int getComicId() {
        return comicId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setComicId(int comicId) {
        this.comicId = comicId;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}