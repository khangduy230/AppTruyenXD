package com.nhom5.ftcomic.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.Chapter;
import com.nhom5.ftcomic.models.ChapterPage;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.network.SupabaseClient;
import com.nhom5.ftcomic.network.response.ChapterPageResponse;
import com.nhom5.ftcomic.network.response.ChapterResponse;
import com.nhom5.ftcomic.network.response.ComicResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicRepository {

    private static final String TAG = "ComicRepository";

    private final AppDatabase appDatabase;

    public ComicRepository(Context context) {
        appDatabase = AppDatabase.getInstance(context);
    }

    // =========================
    // 1. Lấy dữ liệu từ Room
    // =========================

    public LiveData<List<Comic>> getComicsBySection(String section) {
        return appDatabase.comicDao().getComicsBySection(section);
    }

    public LiveData<Comic> getComicByIdLive(int comicId) {
        return appDatabase.comicDao().getComicByIdLive(comicId);
    }

    public LiveData<List<Chapter>> getChaptersByComicId(int comicId) {
        return appDatabase.chapterDao().getChaptersByComicId(comicId);
    }

    public LiveData<List<ChapterPage>> getPagesByChapterId(int chapterId) {
        return appDatabase.chapterPageDao().getPagesByChapterId(chapterId);
    }

    // =========================
    // 2. Sync Comics Supabase → Room
    // =========================

    public void syncComicsBySection(String section) {
        SupabaseClient.getApi()
                .getComicsBySection("eq." + section, "id.asc")
                .enqueue(new Callback<List<ComicResponse>>() {
                    @Override
                    public void onResponse(Call<List<ComicResponse>> call,
                                           Response<List<ComicResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Comic> comics = mapComicResponsesToEntities(response.body());

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.comicDao().insertComics(comics);
                                Log.d(TAG, "Đã sync " + section + ": " + comics.size());
                            });
                        } else {
                            Log.e(TAG, "syncComicsBySection lỗi response: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicResponse>> call, Throwable t) {
                        Log.e(TAG, "syncComicsBySection lỗi API", t);
                    }
                });
    }

    public void syncAllHomeComics() {
        syncComicsBySection("featured");
        syncComicsBySection("ranking");
        syncComicsBySection("all");
    }

    // =========================
    // 3. Sync Chapters Supabase → Room
    // =========================

    public void syncChaptersByComicId(int comicId) {
        SupabaseClient.getApi()
                .getChaptersByComicId("eq." + comicId, "chapter_number.asc")
                .enqueue(new Callback<List<ChapterResponse>>() {
                    @Override
                    public void onResponse(Call<List<ChapterResponse>> call,
                                           Response<List<ChapterResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Chapter> chapters = mapChapterResponsesToEntities(response.body());

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.chapterDao().insertChapters(chapters);
                                Log.d(TAG, "Đã sync chapters comicId=" + comicId);
                            });
                        } else {
                            Log.e(TAG, "syncChapters lỗi response: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ChapterResponse>> call, Throwable t) {
                        Log.e(TAG, "syncChapters lỗi API", t);
                    }
                });
    }

    // =========================
    // 4. Sync Pages Supabase → Room
    // =========================

    public void syncPagesByChapterId(int chapterId) {
        SupabaseClient.getApi()
                .getPagesByChapterId("eq." + chapterId, "page_number.asc")
                .enqueue(new Callback<List<ChapterPageResponse>>() {
                    @Override
                    public void onResponse(Call<List<ChapterPageResponse>> call,
                                           Response<List<ChapterPageResponse>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<ChapterPage> pages = mapPageResponsesToEntities(response.body());

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                appDatabase.chapterPageDao().insertPages(pages);
                                Log.d(TAG, "Đã sync pages chapterId=" + chapterId);
                            });
                        } else {
                            Log.e(TAG, "syncPages lỗi response: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ChapterPageResponse>> call, Throwable t) {
                        Log.e(TAG, "syncPages lỗi API", t);
                    }
                });
    }

    // =========================
    // 5. Mapper API Response → Room Entity
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
}