package com.nhom5.ftcomic.network.response;

import com.google.gson.annotations.SerializedName;

public class ChapterResponse {

    @SerializedName("id")
    private int id;

    @SerializedName("comic_id")
    private int comicId;

    @SerializedName("chapter_number")
    private int chapterNumber;

    @SerializedName("chapter_name")
    private String chapterName;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("is_hidden")
    private boolean isHidden;

    public int getId() {
        return id;
    }

    public int getComicId() {
        return comicId;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public String getChapterName() {
        return chapterName;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public boolean isHidden() {
        return isHidden;
    }
}
