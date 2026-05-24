package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;

public class ChapterPageResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("chapter_id")
    private int chapterId;

    @SerializedName("page_number")
    private int pageNumber;

    @SerializedName("image_url")
    private String imageUrl;

    public int getId() {
        return id;
    }

    public int getChapterId() {
        return chapterId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}