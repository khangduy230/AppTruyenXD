package com.nhom5.ftcomic.network.request;

import com.google.gson.annotations.SerializedName;

public class ReadingHistoryRequest {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("comic_id")
    private int comicId;

    @SerializedName("chapter_id")
    private int chapterId;

    @SerializedName("page_number")
    private int pageNumber;

    public ReadingHistoryRequest(String userId, int comicId, int chapterId, int pageNumber) {
        this.userId = userId;
        this.comicId = comicId;
        this.chapterId = chapterId;
        this.pageNumber = pageNumber;
    }
}