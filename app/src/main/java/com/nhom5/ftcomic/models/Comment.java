package com.nhom5.ftcomic.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "comments")
public class Comment {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int comicId;
    private int parentId;
    private String userName;
    private String avatarUri;
    private String content;
    private long createdAt;
    private int chapterId;
    private String chapterName;

    public Comment(int comicId, int parentId, String userName, String avatarUri, String content, long createdAt, int chapterId, String chapterName) {
        this.comicId = comicId;
        this.parentId = parentId;
        this.userName = userName;
        this.avatarUri = avatarUri;
        this.content = content;
        this.createdAt = createdAt;
        this.chapterId = chapterId;
        this.chapterName = chapterName;
    }

    // Getter và Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getComicId() { return comicId; }
    public int getParentId() { return parentId; }
    public String getUserName() { return userName; }
    public String getAvatarUri() { return avatarUri; }
    public String getContent() { return content; }
    public long getCreatedAt() { return createdAt; }

    public int getChapterId() { return chapterId; }
    public void setChapterId(int chapterId) { this.chapterId = chapterId; }
    public String getChapterName() { return chapterName; }
    public void setChapterName(String chapterName) { this.chapterName = chapterName; }
}