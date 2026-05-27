package com.nhom5.ftcomic.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "comments")
public class Comment {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int comicId;
    private int parentId; // Nếu bằng 0 thì là bình luận gốc, nếu > 0 thì là ID của bình luận cha
    private String userName;
    private String content;
    private long createdAt;

    public Comment(int comicId, int parentId, String userName, String content, long createdAt) {
        this.comicId = comicId;
        this.parentId = parentId;
        this.userName = userName;
        this.content = content;
        this.createdAt = createdAt;
    }

    // Getter và Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getComicId() { return comicId; }
    public int getParentId() { return parentId; }
    public String getUserName() { return userName; }
    public String getContent() { return content; }
    public long getCreatedAt() { return createdAt; }
}