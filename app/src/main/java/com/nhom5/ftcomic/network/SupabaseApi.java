package com.nhom5.ftcomic.network;

import com.nhom5.ftcomic.network.request.CommentRequest;
import com.nhom5.ftcomic.network.response.CommentResponse;

import com.nhom5.ftcomic.network.request.FavoriteRequest;
import com.nhom5.ftcomic.network.response.FavoriteResponse;

import com.nhom5.ftcomic.network.request.ReadingHistoryRequest;
import com.nhom5.ftcomic.network.response.ReadingHistoryResponse;

import com.nhom5.ftcomic.network.request.RatingRequest;
import com.nhom5.ftcomic.network.response.RatingResponse;

import com.nhom5.ftcomic.network.request.LikeRequest;
import com.nhom5.ftcomic.network.response.LikeResponse;

import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.POST;

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

    @GET("comic_categories")
    Call<List<ComicCategoryResponse>> getComicCategoryRefsByComicId(
            @Query("comic_id") String comicIdFilter
    );
    @GET("comics")
    Call<List<ComicResponse>> getComicsByIds(
            @Query("id") String idFilter,
            @Query("order") String order
    );

    @GET("comments_with_profiles")
    Call<List<CommentResponse>> getCommentsByComicId(
            @Query("comic_id") String comicIdFilter,
            @Query("order") String order
    );

    @POST("comments")
    Call<Void> addComment(
            @Header("Prefer") String prefer,
            @Body CommentRequest request
    );

    @GET("favorites_with_millis")
    Call<List<FavoriteResponse>> getMyFavorites(
            @Query("user_id") String userIdFilter,
            @Query("order") String order
    );

    @POST("favorites")
    Call<Void> addFavorite(
            @Header("Prefer") String prefer,
            @Body FavoriteRequest request
    );

    @DELETE("favorites")
    Call<Void> deleteFavorite(
            @Query("user_id") String userIdFilter,
            @Query("comic_id") String comicIdFilter
    );

    @GET("reading_history_with_millis")
    Call<List<ReadingHistoryResponse>> getMyReadingHistory(
            @Query("user_id") String userIdFilter,
            @Query("order") String order
    );

    @POST("reading_history")
    Call<Void> saveReadingHistory(
            @Header("Prefer") String prefer,
            @Query("on_conflict") String onConflict,
            @Body ReadingHistoryRequest request
    );

    @GET("comics")
    Call<List<ComicResponse>> getAllComicsRemote(
            @Query("order") String order
    );

    @GET("ratings")
    Call<List<RatingResponse>> getMyRating(
            @Query("user_id") String userIdFilter,
            @Query("comic_id") String comicIdFilter,
            @Query("limit") int limit
    );

    @POST("ratings")
    Call<List<RatingResponse>> upsertRating(
            @Header("Prefer") String prefer,
            @Query("on_conflict") String onConflict,
            @Body RatingRequest request
    );

    @DELETE("ratings")
    Call<Void> deleteRating(
            @Query("user_id") String userIdFilter,
            @Query("comic_id") String comicIdFilter
    );

    @DELETE("reading_history")
    Call<Void> deleteMyReadingHistory(
            @Query("user_id") String userIdFilter
    );

    @GET("likes")
    Call<List<LikeResponse>> getMyLike(
            @Query("user_id") String userIdFilter,
            @Query("comic_id") String comicIdFilter,
            @Query("limit") int limit
    );

    @POST("likes")
    Call<Void> addLike(
            @Header("Prefer") String prefer,
            @Body LikeRequest request
    );

    @DELETE("likes")
    Call<Void> deleteLike(
            @Query("user_id") String userIdFilter,
            @Query("comic_id") String comicIdFilter
    );

    @DELETE("comments")
    Call<Void> deleteComment(
            @Query("id") String idFilter
    );
}