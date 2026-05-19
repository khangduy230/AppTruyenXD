package com.nhom5.ftcomic.network;

import com.nhom5.ftcomic.network.response.ChapterPageResponse;
import com.nhom5.ftcomic.network.response.ChapterResponse;
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

    @GET("comics")
    Call<List<ComicResponse>> searchComics(
            @Query("name") String nameFilter,
            @Query("order") String order
    );
}