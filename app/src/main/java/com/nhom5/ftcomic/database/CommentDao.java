package com.nhom5.ftcomic.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.nhom5.ftcomic.models.Comment;
import java.util.List;

@Dao
public interface CommentDao {
    // Lấy toàn bộ bình luận của một bộ truyện (sắp xếp thời gian mới nhất lên đầu)
    @Query("SELECT * FROM comments WHERE comicId = :comicId ORDER BY createdAt DESC")
    LiveData<List<Comment>> getCommentsByComicId(int comicId);

    @Insert
    void insertComment(Comment comment);
}