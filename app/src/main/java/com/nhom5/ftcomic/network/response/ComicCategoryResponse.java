package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;

public class ComicCategoryResponse {

    @SerializedName("comic_id")
    private int comicId;

    @SerializedName("category_id")
    private int categoryId;

    public int getComicId() {
        return comicId;
    }

    public int getCategoryId() {
        return categoryId;
    }
}