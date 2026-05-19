package com.nhom5.ftcomic.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.nhom5.ftcomic.models.Category;
import com.nhom5.ftcomic.models.ComicCategoryCrossRef;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCategories(List<Category> categories);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertComicCategoryRefs(List<ComicCategoryCrossRef> refs);

    @Query("SELECT * FROM categories ORDER BY name ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT categories.* FROM categories " +
            "INNER JOIN comic_category_cross_ref " +
            "ON categories.id = comic_category_cross_ref.categoryId " +
            "WHERE comic_category_cross_ref.comicId = :comicId")
    LiveData<List<Category>> getCategoriesByComicId(int comicId);
}