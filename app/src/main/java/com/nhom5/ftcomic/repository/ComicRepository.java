package com.nhom5.ftcomic.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.Category;
import com.nhom5.ftcomic.models.Chapter;
import com.nhom5.ftcomic.models.ChapterPage;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.models.ComicCategoryCrossRef;
import com.nhom5.ftcomic.models.Comment;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.request.CommentRequest;
import com.nhom5.ftcomic.network.response.CategoryResponse;
import com.nhom5.ftcomic.network.response.ChapterPageResponse;
import com.nhom5.ftcomic.network.response.ChapterResponse;
import com.nhom5.ftcomic.network.response.ComicCategoryResponse;
import com.nhom5.ftcomic.network.response.ComicResponse;
import com.nhom5.ftcomic.network.response.CommentResponse;
import com.nhom5.ftcomic.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicRepository {

    private static final String TAG = "ComicRepository";

    private final Context context;
    private final AppDatabase appDatabase;

    public ComicRepository(Context context) {
        this.context = context.getApplicationContext();
        this.appDatabase = AppDatabase.getInstance(this.context);
    }

    private boolean canSyncFromSupabase(String taskName) {
        if (!NetworkUtils.isOnline(context)) {
            Log.w(TAG, taskName + ": Offline");
            return false;
        }
        return true;
    }

    public LiveData<List<Comic>> getComicsBySection(String section) {
        return appDatabase.comicDao().getComicsBySection(section);
    }

    public LiveData<Comic> getComicByIdLive(int comicId) {
        return appDatabase.comicDao().getComicByIdLive(comicId);
    }

    public LiveData<List<Comic>> getRankingComics() {
        return appDatabase.comicDao().getRankingComics();
    }

    public LiveData<List<Comic>> getAllComicsLive() {
        return appDatabase.comicDao().getAllComicsLive();
    }

    public LiveData<List<Chapter>> getChaptersByComicId(int comicId) {
        return appDatabase.chapterDao().getChaptersByComicId(comicId);
    }

    public LiveData<List<ChapterPage>> getPagesByChapterId(int chapterId) {
        return appDatabase.chapterPageDao().getPagesByChapterId(chapterId);
    }

    public LiveData<List<Category>> getCategoriesByComicId(int comicId) {
        return appDatabase.categoryDao().getCategoriesByComicId(comicId);
    }

    public LiveData<List<Category>> getAllCategoriesLive() {
        return appDatabase.categoryDao().getAllCategories();
    }

    public void syncAllHomeComics() {
        if (!canSyncFromSupabase("syncAllHomeComics")) {
            return;
        }
        syncLatestComics();
        syncRankingComics();
        syncAllComicsFromSupabase();
        syncAllCategories();
    }

    public void syncComicsBySection(String section) {
        if (!canSyncFromSupabase("syncComicsBySection")) {
            return;
        }
        SupabaseClient.getApi()
                .getComicsBySection("eq." + section, "id.asc")
                .enqueue(new Callback<List<ComicResponse>>() {
                    @Override
                    public void onResponse(Call<List<ComicResponse>> call, Response<List<ComicResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Comic> comics = mapComicResponsesToEntities(response.body());
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.comicDao().insertComics(comics);
                            });
                        } else {
                            logErrorBody(response, "syncComicsBySection");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicResponse>> call, Throwable t) {
                        Log.e(TAG, "syncComicsBySection Error", t);
                    }
                });
    }

    public void syncAllComicsFromSupabase() {
        if (!canSyncFromSupabase("syncAllComicsFromSupabase")) {
            return;
        }
        SupabaseClient.getApi()
                .getAllComicsRemote("id.asc")
                .enqueue(new Callback<List<ComicResponse>>() {
                    @Override
                    public void onResponse(Call<List<ComicResponse>> call, Response<List<ComicResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Comic> comics = mapComicResponsesToEntities(response.body());

                            List<Integer> remoteIds = new ArrayList<>();
                            for (Comic c : comics) {
                                remoteIds.add(c.getId());
                            }

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                if (!remoteIds.isEmpty()) {
                                    appDatabase.comicDao().deleteComicsNotIn(remoteIds);
                                } else {
                                    appDatabase.comicDao().deleteAllComics();
                                }
                                appDatabase.comicDao().insertComics(comics);
                            });
                        } else {
                            logErrorBody(response, "syncAllComicsFromSupabase");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicResponse>> call, Throwable t) {
                        Log.e(TAG, "syncAllComicsFromSupabase Error", t);
                    }
                });
    }

    public void syncComicById(int comicId) {
        if (comicId <= 0) {
            return;
        }
        if (!canSyncFromSupabase("syncComicById")) {
            return;
        }
        SupabaseClient.getApi()
                .getComicById("eq." + comicId)
                .enqueue(new Callback<List<ComicResponse>>() {
                    @Override
                    public void onResponse(Call<List<ComicResponse>> call, Response<List<ComicResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            ComicResponse item = response.body().get(0);
                            Comic comic = new Comic(
                                    item.getId(),
                                    R.drawable.thientai,
                                    item.getCoverUrl(),
                                    item.getName(),
                                    item.getAuthor(),
                                    item.getDescription(),
                                    item.getStatus(),
                                    item.getSection(),
                                    item.getLikeCount(),
                                    item.getRating(),
                                    item.getRatingCount(),
                                    item.getCommentCount(),
                                    item.getViewCount(),
                                    item.getCreatedAt(),
                                    item.getLastUpdate()
                            );
                            comic.setUploaderName(item.getUploaderName());
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.comicDao().insertComic(comic);
                            });
                        } else {
                            logErrorBody(response, "syncComicById");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicResponse>> call, Throwable t) {
                        Log.e(TAG, "syncComicById Error", t);
                    }
                });
    }

    public void syncChaptersByComicId(int comicId) {
        if (comicId <= 0) {
            return;
        }
        if (!canSyncFromSupabase("syncChaptersByComicId")) {
            return;
        }
        SupabaseClient.getApi()
                .getChaptersByComicId("eq." + comicId, "chapter_number.asc")
                .enqueue(new Callback<List<ChapterResponse>>() {
                    @Override
                    public void onResponse(Call<List<ChapterResponse>> call, Response<List<ChapterResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Chapter> chapters = mapChapterResponsesToEntities(response.body());
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.chapterDao().deleteChaptersByComicId(comicId);
                                appDatabase.chapterDao().insertChapters(chapters);
                            });
                        } else {
                            logErrorBody(response, "syncChaptersByComicId");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ChapterResponse>> call, Throwable t) {
                        Log.e(TAG, "syncChaptersByComicId Error", t);
                    }
                });
    }

    public void syncPagesByChapterId(int chapterId) {
        if (chapterId <= 0) {
            return;
        }
        if (!canSyncFromSupabase("syncPagesByChapterId")) {
            return;
        }
        SupabaseClient.getApi()
                .getPagesByChapterId("eq." + chapterId, "page_number.asc")
                .enqueue(new Callback<List<ChapterPageResponse>>() {
                    @Override
                    public void onResponse(Call<List<ChapterPageResponse>> call, Response<List<ChapterPageResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ChapterPage> remotePages = mapPageResponsesToEntities(response.body());
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                List<ChapterPage> oldPages = appDatabase.chapterPageDao().getPagesByChapterIdSync(chapterId);
                                Map<Integer, String> localPathMap = new HashMap<>();
                                if (oldPages != null) {
                                    for (ChapterPage oldPage : oldPages) {
                                        if (oldPage.getLocalFilePath() != null && !oldPage.getLocalFilePath().trim().isEmpty()) {
                                            localPathMap.put(oldPage.getPageNumber(), oldPage.getLocalFilePath());
                                        }
                                    }
                                }
                                for (ChapterPage newPage : remotePages) {
                                    String oldLocalPath = localPathMap.get(newPage.getPageNumber());
                                    if (oldLocalPath != null && !oldLocalPath.trim().isEmpty()) {
                                        newPage.setLocalFilePath(oldLocalPath);
                                    }
                                }
                                appDatabase.chapterPageDao().deletePagesByChapterId(chapterId);
                                appDatabase.chapterPageDao().insertPages(remotePages);
                            });
                        } else {
                            logErrorBody(response, "syncPagesByChapterId");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ChapterPageResponse>> call, Throwable t) {
                        Log.e(TAG, "syncPagesByChapterId Error", t);
                    }
                });
    }

    public void syncCategoriesByComicId(int comicId) {
        if (comicId <= 0) {
            return;
        }
        if (!canSyncFromSupabase("syncCategoriesByComicId")) {
            return;
        }
        syncAllCategories();
        syncComicCategoryRefsByComicId(comicId);
    }

    public void syncAllCategories() {
        if (!canSyncFromSupabase("syncAllCategories")) {
            return;
        }
        SupabaseClient.getApi()
                .getAllCategories("name.asc")
                .enqueue(new Callback<List<CategoryResponse>>() {
                    @Override
                    public void onResponse(Call<List<CategoryResponse>> call, Response<List<CategoryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Category> categories = mapCategoryResponsesToEntities(response.body());
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.categoryDao().insertCategories(categories);
                            });
                        } else {
                            logErrorBody(response, "syncAllCategories");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CategoryResponse>> call, Throwable t) {
                        Log.e(TAG, "syncAllCategories Error", t);
                    }
                });
    }

    private void syncComicCategoryRefsByComicId(int comicId) {
        if (!canSyncFromSupabase("syncComicCategoryRefsByComicId")) {
            return;
        }
        SupabaseClient.getApi()
                .getComicCategoryRefsByComicId("eq." + comicId)
                .enqueue(new Callback<List<ComicCategoryResponse>>() {
                    @Override
                    public void onResponse(Call<List<ComicCategoryResponse>> call, Response<List<ComicCategoryResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ComicCategoryCrossRef> refs = mapComicCategoryResponsesToEntities(response.body());
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.categoryDao().insertComicCategoryRefs(refs);
                            });
                        } else {
                            logErrorBody(response, "syncComicCategoryRefsByComicId");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicCategoryResponse>> call, Throwable t) {
                        Log.e(TAG, "syncComicCategoryRefsByComicId Error", t);
                    }
                });
    }

    private List<Comic> mapComicResponsesToEntities(List<ComicResponse> responses) {
        List<Comic> comics = new ArrayList<>();
        for (ComicResponse item : responses) {
            Comic comic = new Comic(
                    item.getId(),
                    R.drawable.thientai,
                    item.getCoverUrl(),
                    item.getName(),
                    item.getAuthor(),
                    item.getDescription(),
                    item.getStatus(),
                    item.getSection(),
                    item.getLikeCount(),
                    item.getRating(),
                    item.getRatingCount(),
                    item.getCommentCount(),
                    item.getViewCount(),
                    item.getCreatedAt(),
                    item.getLastUpdate()
            );
            comic.setUploaderName(item.getUploaderName());
            comics.add(comic);
        }
        return comics;
    }

    private List<Chapter> mapChapterResponsesToEntities(List<ChapterResponse> responses) {
        List<Chapter> chapters = new ArrayList<>();
        for (ChapterResponse item : responses) {
            Chapter chapter = new Chapter(
                    item.getId(),
                    item.getComicId(),
                    item.getChapterNumber(),
                    item.getChapterName(),
                    item.getUpdatedAt(),
                    item.isHidden()
            );
            chapters.add(chapter);
        }
        return chapters;
    }

    private List<ChapterPage> mapPageResponsesToEntities(List<ChapterPageResponse> responses) {
        List<ChapterPage> pages = new ArrayList<>();
        for (ChapterPageResponse item : responses) {
            ChapterPage page = new ChapterPage(
                    item.getId(),
                    item.getChapterId(),
                    item.getPageNumber(),
                    R.drawable.thientai,
                    item.getImageUrl(),
                    ""
            );
            pages.add(page);
        }
        return pages;
    }

    private List<Category> mapCategoryResponsesToEntities(List<CategoryResponse> responses) {
        List<Category> categories = new ArrayList<>();
        for (CategoryResponse item : responses) {
            Category category = new Category(
                    item.getId(),
                    item.getName()
            );
            categories.add(category);
        }
        return categories;
    }

    private List<ComicCategoryCrossRef> mapComicCategoryResponsesToEntities(List<ComicCategoryResponse> responses) {
        List<ComicCategoryCrossRef> refs = new ArrayList<>();
        for (ComicCategoryResponse item : responses) {
            ComicCategoryCrossRef ref = new ComicCategoryCrossRef(
                    item.getComicId(),
                    item.getCategoryId()
            );
            refs.add(ref);
        }
        return refs;
    }

    private void logErrorBody(Response<?> response, String tag) {
        try {
            if (response.errorBody() != null) {
                Log.e(TAG, tag + " error: " + response.errorBody().string());
            } else {
                Log.e(TAG, tag + " error code: " + response.code());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing body", e);
        }
    }

    public void syncCommentsByComicId(int comicId) {
        if (!canSyncFromSupabase("syncCommentsByComicId")) return;
        SupabaseClient.getApi()
                .getCommentsByComicId("eq." + comicId, "created_at.desc")
                .enqueue(new Callback<List<CommentResponse>>() {
                    @Override
                    public void onResponse(Call<List<CommentResponse>> call, Response<List<CommentResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Comment> comments = new ArrayList<>();
                            for (CommentResponse res : response.body()) {
                                Comment comment = new Comment(
                                        res.getComicId(),
                                        res.getParentId(),
                                        res.getUserName(),
                                        res.getAvatarUri(),
                                        res.getContent(),
                                        res.getCreatedAt(),
                                        res.getChapterId(),
                                        res.getChapterName()
                                );
                                comment.setId(res.getId());
                                comments.add(comment);
                            }
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.commentDao().deleteCommentsByComicId(comicId);
                                appDatabase.commentDao().insertComments(comments);
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CommentResponse>> call, Throwable t) {
                        Log.e(TAG, "syncCommentsByComicId Error", t);
                    }
                });
    }

    public void sendCommentToRemote(String userId, Comment comment, Runnable onSuccessCallback) {
        CommentRequest request = new CommentRequest(
                userId,
                comment.getComicId(),
                comment.getParentId(),
                comment.getUserName(),
                comment.getAvatarUri(),
                comment.getContent(),
                comment.getChapterId(),
                comment.getChapterName()
        );
        SupabaseClient.getApi()
                .addComment("return=representation", request)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            if (onSuccessCallback != null) onSuccessCallback.run();
                        } else {
                            try {
                                if (response.errorBody() != null) {
                                    Log.e(TAG, "Error: " + response.errorBody().string());
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "sendCommentToRemote Error", t);
                    }
                });
    }

    public void deleteCommentFromRemote(int commentId, Runnable onSuccessCallback) {
        SupabaseClient.getApi()
                .deleteComment("eq." + commentId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.commentDao().deleteCommentById(commentId);
                                if (onSuccessCallback != null) {
                                    onSuccessCallback.run();
                                }
                            });
                        } else {
                            logErrorBody(response, "deleteCommentFromRemote");
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "deleteCommentFromRemote Error", t);
                    }
                });
    }

    public LiveData<List<Comic>> getLatestComics() {
        return appDatabase.comicDao().getLatestComics();
    }

    public void syncLatestComics() {
        if (!canSyncFromSupabase("syncLatestComics")) {
            return;
        }

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.MONTH, -1);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String oneMonthAgoFilter = "gte." + sdf.format(calendar.getTime());

        SupabaseClient.getApi()
                .getLatestComics(
                        oneMonthAgoFilter,
                        "last_update.desc"
                )
                .enqueue(new Callback<List<ComicResponse>>() {
                    @Override
                    public void onResponse(Call<List<ComicResponse>> call, Response<List<ComicResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Comic> comics = mapComicResponsesToEntities(response.body());
                            AppDatabase.databaseWriteExecutor.execute(() ->
                                    appDatabase.comicDao().insertComics(comics));
                        } else {
                            Log.e(TAG, "API Error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicResponse>> call, Throwable t) {
                        Log.e(TAG, "syncLatestComics Error", t);
                    }
                });
    }

    public void syncRankingComics() {
        if (!canSyncFromSupabase("syncRankingComics")) {
            return;
        }

        SupabaseClient.getApi()
                .getTop10Comics(
                        "rating_avg.desc,view_count.desc,like_count.desc",
                        10
                )
                .enqueue(new Callback<List<ComicResponse>>() {

                    @Override
                    public void onResponse(Call<List<ComicResponse>> call,
                                           Response<List<ComicResponse>> response) {

                        if (response.isSuccessful() && response.body() != null) {
                            List<Comic> comics = mapComicResponsesToEntities(response.body());
                            AppDatabase.databaseWriteExecutor.execute(() ->
                                    appDatabase.comicDao().insertComics(comics));
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicResponse>> call,
                                          Throwable t) {
                        Log.e(TAG, "syncRankingComics Error", t);
                    }
                });
    }
}