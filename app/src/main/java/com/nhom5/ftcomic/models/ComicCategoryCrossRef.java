package com.nhom5.ftcomic.models;

import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(
        tableName = "comic_category_cross_ref",
        primaryKeys = {"comicId", "categoryId"}
)
public class ComicCategoryCrossRef {

    private int comicId;
    private int categoryId;

    public ComicCategoryCrossRef() {
    }

    @Ignore
    public ComicCategoryCrossRef(int comicId, int categoryId) {
        this.comicId = comicId;
        this.categoryId = categoryId;
    }

    public int getComicId() {
        return comicId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setComicId(int comicId) {
        this.comicId = comicId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
}