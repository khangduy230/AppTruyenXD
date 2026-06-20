package com.nhom5.ftcomic.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.nhom5.ftcomic.models.Comment;

import java.util.List;

@Dao
public interface CommentDao {

    @Query("SELECT * FROM comments WHERE comicId = :comicId ORDER BY createdAt DESC")
    LiveData<List<Comment>> getCommentsByComicId(int comicId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComment(Comment comment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComments(List<Comment> comments);

    @Query("DELETE FROM comments WHERE comicId = :comicId")
    void deleteCommentsByComicId(int comicId);

    @Query("DELETE FROM comments WHERE id = :commentId")
    void deleteCommentById(int commentId);
}