package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;

public class ReadingHistoryResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("comic_id")
    private int comicId;

    @SerializedName("chapter_id")
    private int chapterId;

    @SerializedName("page_number")
    private int pageNumber;

    @SerializedName("last_read_at_millis")
    private long lastReadAtMillis;

    public int getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public int getComicId() {
        return comicId;
    }

    public int getChapterId() {
        return chapterId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public long getLastReadAtMillis() {
        return lastReadAtMillis;
    }
}