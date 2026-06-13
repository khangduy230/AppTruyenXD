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
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.response.CategoryResponse;
import com.nhom5.ftcomic.network.response.ChapterPageResponse;
import com.nhom5.ftcomic.network.response.ChapterResponse;
import com.nhom5.ftcomic.network.response.ComicCategoryResponse;
import com.nhom5.ftcomic.network.response.ComicResponse;
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

    // =========================
    // Kiểm tra mạng trước khi sync
    // =========================

    private boolean canSyncFromSupabase(String taskName) {
        if (!NetworkUtils.isOnline(context)) {
            Log.w(TAG, taskName + ": Không có mạng, dùng cache Room");
            return false;
        }

        return true;
    }

    // =========================
    // Lấy dữ liệu từ Room
    // =========================

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

    // =========================
    // Sync truyện trang chủ
    // =========================

    public void syncAllHomeComics() {
        if (!canSyncFromSupabase("syncAllHomeComics")) {
            return;
        }

        syncComicsBySection("featured");
        syncAllComicsFromSupabase();
    }

    public void syncComicsBySection(String section) {
        if (!canSyncFromSupabase("syncComicsBySection")) {
            return;
        }

        SupabaseClient.getApi()
                .getComicsBySection("eq." + section, "id.asc")
                .enqueue(new Callback<List<ComicResponse>>() {
                    @Override
                    public void onResponse(Call<List<ComicResponse>> call,
                                           Response<List<ComicResponse>> response) {
                        Log.d(TAG, "Comics URL: " + call.request().url());
                        Log.d(TAG, "Comics code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            List<Comic> comics = mapComicResponsesToEntities(response.body());

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.comicDao().insertComics(comics);
                                Log.d(TAG, "Đã cache section " + section
                                        + ", size=" + comics.size());
                            });
                        } else {
                            logErrorBody(response, "syncComicsBySection");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicResponse>> call, Throwable t) {
                        Log.e(TAG, "syncComicsBySection lỗi API, giữ cache Room cũ", t);
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
                    public void onResponse(Call<List<ComicResponse>> call,
                                           Response<List<ComicResponse>> response) {
                        Log.d(TAG, "All comics URL: " + call.request().url());
                        Log.d(TAG, "All comics code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            List<Comic> comics = mapComicResponsesToEntities(response.body());

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.comicDao().insertComics(comics);
                                Log.d(TAG, "Đã cache tất cả truyện, size=" + comics.size());
                            });
                        } else {
                            logErrorBody(response, "syncAllComicsFromSupabase");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicResponse>> call, Throwable t) {
                        Log.e(TAG, "syncAllComicsFromSupabase lỗi API, giữ cache Room cũ", t);
                    }
                });
    }

    public void syncComicById(int comicId) {
        if (comicId <= 0) {
            Log.e(TAG, "comicId không hợp lệ khi syncComicById: " + comicId);
            return;
        }

        if (!canSyncFromSupabase("syncComicById")) {
            return;
        }

        SupabaseClient.getApi()
                .getComicById("eq." + comicId)
                .enqueue(new Callback<List<ComicResponse>>() {
                    @Override
                    public void onResponse(Call<List<ComicResponse>> call,
                                           Response<List<ComicResponse>> response) {
                        Log.d(TAG, "Comic detail URL: " + call.request().url());
                        Log.d(TAG, "Comic detail code: " + response.code());

                        if (response.isSuccessful()
                                && response.body() != null
                                && !response.body().isEmpty()) {

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
                                    item.getViewCount()
                            );

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.comicDao().insertComic(comic);
                                Log.d(TAG, "Đã sync lại comic id=" + comicId);
                            });
                        } else {
                            logErrorBody(response, "syncComicById");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicResponse>> call, Throwable t) {
                        Log.e(TAG, "syncComicById lỗi API, giữ cache Room cũ", t);
                    }
                });
    }

    // =========================
    // Sync chương
    // =========================

    public void syncChaptersByComicId(int comicId) {
        if (comicId <= 0) {
            Log.e(TAG, "comicId không hợp lệ: " + comicId);
            return;
        }

        if (!canSyncFromSupabase("syncChaptersByComicId")) {
            return;
        }

        SupabaseClient.getApi()
                .getChaptersByComicId("eq." + comicId, "chapter_number.asc")
                .enqueue(new Callback<List<ChapterResponse>>() {
                    @Override
                    public void onResponse(Call<List<ChapterResponse>> call,
                                           Response<List<ChapterResponse>> response) {
                        Log.d(TAG, "Chapters URL: " + call.request().url());
                        Log.d(TAG, "Chapters code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            List<Chapter> chapters = mapChapterResponsesToEntities(response.body());

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.chapterDao().insertChapters(chapters);
                                Log.d(TAG, "Đã cache chapters comicId="
                                        + comicId + ", size=" + chapters.size());
                            });
                        } else {
                            logErrorBody(response, "syncChaptersByComicId");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ChapterResponse>> call, Throwable t) {
                        Log.e(TAG, "syncChaptersByComicId lỗi API, giữ cache Room cũ", t);
                    }
                });
    }

    // =========================
    // Sync trang truyện
    // =========================

    public void syncPagesByChapterId(int chapterId) {
        if (chapterId <= 0) {
            Log.e(TAG, "chapterId không hợp lệ: " + chapterId);
            return;
        }

        if (!canSyncFromSupabase("syncPagesByChapterId")) {
            return;
        }

        SupabaseClient.getApi()
                .getPagesByChapterId("eq." + chapterId, "page_number.asc")
                .enqueue(new Callback<List<ChapterPageResponse>>() {
                    @Override
                    public void onResponse(Call<List<ChapterPageResponse>> call,
                                           Response<List<ChapterPageResponse>> response) {
                        Log.d(TAG, "Pages URL: " + call.request().url());
                        Log.d(TAG, "Pages code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            List<ChapterPage> remotePages =
                                    mapPageResponsesToEntities(response.body());

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                List<ChapterPage> oldPages =
                                        appDatabase.chapterPageDao()
                                                .getPagesByChapterIdSync(chapterId);

                                Map<Integer, String> localPathMap = new HashMap<>();

                                if (oldPages != null) {
                                    for (ChapterPage oldPage : oldPages) {
                                        if (oldPage.getLocalFilePath() != null
                                                && !oldPage.getLocalFilePath().trim().isEmpty()) {
                                            localPathMap.put(
                                                    oldPage.getPageNumber(),
                                                    oldPage.getLocalFilePath()
                                            );
                                        }
                                    }
                                }

                                for (ChapterPage newPage : remotePages) {
                                    String oldLocalPath =
                                            localPathMap.get(newPage.getPageNumber());

                                    if (oldLocalPath != null
                                            && !oldLocalPath.trim().isEmpty()) {
                                        newPage.setLocalFilePath(oldLocalPath);
                                    }
                                }

                                /*
                                  Chỉ xóa page cũ khi API đã thành công.
                                  Nếu mất mạng / API lỗi thì cache Room cũ vẫn giữ nguyên.
                                */
                                appDatabase.chapterPageDao().deletePagesByChapterId(chapterId);
                                appDatabase.chapterPageDao().insertPages(remotePages);

                                Log.d(TAG, "Đã cache pages chapterId="
                                        + chapterId + ", size=" + remotePages.size());
                            });
                        } else {
                            logErrorBody(response, "syncPagesByChapterId");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ChapterPageResponse>> call, Throwable t) {
                        Log.e(TAG, "syncPagesByChapterId lỗi API, giữ cache Room cũ", t);
                    }
                });
    }

    // =========================
    // Sync thể loại
    // =========================

    public void syncCategoriesByComicId(int comicId) {
        if (comicId <= 0) {
            Log.e(TAG, "comicId không hợp lệ khi sync category: " + comicId);
            return;
        }

        if (!canSyncFromSupabase("syncCategoriesByComicId")) {
            return;
        }

        syncAllCategories();
        syncComicCategoryRefsByComicId(comicId);
    }

    private void syncAllCategories() {
        if (!canSyncFromSupabase("syncAllCategories")) {
            return;
        }

        SupabaseClient.getApi()
                .getAllCategories("name.asc")
                .enqueue(new Callback<List<CategoryResponse>>() {
                    @Override
                    public void onResponse(Call<List<CategoryResponse>> call,
                                           Response<List<CategoryResponse>> response) {
                        Log.d(TAG, "Categories URL: " + call.request().url());
                        Log.d(TAG, "Categories code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            List<Category> categories =
                                    mapCategoryResponsesToEntities(response.body());

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.categoryDao().insertCategories(categories);
                                Log.d(TAG, "Đã cache categories size=" + categories.size());
                            });
                        } else {
                            logErrorBody(response, "syncAllCategories");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<CategoryResponse>> call, Throwable t) {
                        Log.e(TAG, "syncAllCategories lỗi API, giữ cache Room cũ", t);
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
                    public void onResponse(Call<List<ComicCategoryResponse>> call,
                                           Response<List<ComicCategoryResponse>> response) {
                        Log.d(TAG, "Comic category refs URL: " + call.request().url());
                        Log.d(TAG, "Comic category refs code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            List<ComicCategoryCrossRef> refs =
                                    mapComicCategoryResponsesToEntities(response.body());

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.categoryDao().insertComicCategoryRefs(refs);
                                Log.d(TAG, "Đã cache category refs comicId="
                                        + comicId + ", size=" + refs.size());
                            });
                        } else {
                            logErrorBody(response, "syncComicCategoryRefsByComicId");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicCategoryResponse>> call, Throwable t) {
                        Log.e(TAG, "syncComicCategoryRefsByComicId lỗi API, giữ cache Room cũ", t);
                    }
                });
    }

    // =========================
    // Mapper
    // =========================

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
                    item.getViewCount()
            );

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
                    item.getUpdatedAt()
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

    private List<ComicCategoryCrossRef> mapComicCategoryResponsesToEntities(
            List<ComicCategoryResponse> responses
    ) {
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
            Log.e(TAG, "Không đọc được error body", e);
        }
    }
}