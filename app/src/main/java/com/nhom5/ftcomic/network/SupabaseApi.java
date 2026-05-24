package com.nhom5.ftcomic.network;

import com.nhom5.ftcomic.network.response.CategoryResponse;
import com.nhom5.ftcomic.network.response.ChapterPageResponse;
import com.nhom5.ftcomic.network.response.ChapterResponse;
import com.nhom5.ftcomic.network.response.ComicCategoryResponse;
import com.nhom5.ftcomic.network.response.ComicResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SupabaseApi {

    @GET("comics")
    Call<List<ComicResponse>> getComicsBySection(
            @Query("section") String sectionFilter,
            @Query("order") String order
    );

    @GET("comics")
    Call<List<ComicResponse>> getComicById(
            @Query("id") String idFilter
    );

    @GET("chapters")
    Call<List<ChapterResponse>> getChaptersByComicId(
            @Query("comic_id") String comicIdFilter,
            @Query("order") String order
    );

    @GET("chapter_pages")
    Call<List<ChapterPageResponse>> getPagesByChapterId(
            @Query("chapter_id") String chapterIdFilter,
            @Query("order") String order
    );

    // Supabase hiện tại của bạn dùng cột name, không dùng title
    @GET("comics")
    Call<List<ComicResponse>> searchComics(
            @Query("name") String nameFilter,
            @Query("order") String order
    );

    @GET("categories")
    Call<List<CategoryResponse>> getAllCategories(
            @Query("order") String order
    );

    @GET("comic_categories")
    Call<List<ComicCategoryResponse>> getComicCategoryRefs(
            @Query("category_id") String categoryFilter
    );

    @GET("comics")
    Call<List<ComicResponse>> getComicsByIds(
            @Query("id") String idFilter,
            @Query("order") String order
    );
}