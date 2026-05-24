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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ComicRepository {

    private static final String TAG = "ComicRepository";

    private final AppDatabase appDatabase;

    public ComicRepository(Context context) {
        appDatabase = AppDatabase.getInstance(context);
    }

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

    public void syncAllHomeComics() {
        syncComicsBySection("featured");
        syncComicsBySection("ranking");
        syncComicsBySection("all");
    }

    public void syncComicsBySection(String section) {
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
                                Log.d(TAG, "Đã lưu section " + section + ", size=" + comics.size());
                            });
                        } else {
                            logErrorBody(response, "syncComicsBySection");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ComicResponse>> call, Throwable t) {
                        Log.e(TAG, "syncComicsBySection lỗi API", t);
                    }
                });
    }

    public void syncChaptersByComicId(int comicId) {
        if (comicId <= 0) {
            Log.e(TAG, "comicId không hợp lệ: " + comicId);
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
                                Log.d(TAG, "Đã lưu chapters comicId=" + comicId + ", size=" + chapters.size());
                            });
                        } else {
                            logErrorBody(response, "syncChapters");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ChapterResponse>> call, Throwable t) {
                        Log.e(TAG, "syncChapters lỗi API", t);
                    }
                });
    }

    public void syncPagesByChapterId(int chapterId) {
        if (chapterId <= 0) {
            Log.e(TAG, "chapterId không hợp lệ: " + chapterId);
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
                            List<ChapterPage> remotePages = mapPageResponsesToEntities(response.body());

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                List<ChapterPage> oldPages = appDatabase.chapterPageDao()
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
                                    String oldLocalPath = localPathMap.get(newPage.getPageNumber());

                                    if (oldLocalPath != null && !oldLocalPath.trim().isEmpty()) {
                                        newPage.setLocalFilePath(oldLocalPath);
                                    }
                                }

                                appDatabase.chapterPageDao().deletePagesByChapterId(chapterId);
                                appDatabase.chapterPageDao().insertPages(remotePages);

                                Log.d(TAG, "Đã lưu pages chapterId=" + chapterId
                                        + ", size=" + remotePages.size());
                            });
                        } else {
                            logErrorBody(response, "syncPages");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<ChapterPageResponse>> call, Throwable t) {
                        Log.e(TAG, "syncPages lỗi API", t);
                    }
                });
    }

    private List<Comic> mapComicResponsesToEntities(List<ComicResponse> responses) {
        List<Comic> comics = new ArrayList<>();

        for (ComicResponse item : responses) {
            // Kiểm tra xem trường lấy số lượng đánh giá từ server có tồn tại không.
            // Nếu server Supabase chưa tạo cột ratingCount, ta sẽ tạm thời lấy item.getCommentCount()
            // hoặc gán cứng một con số an toàn ban đầu (ví dụ: 40) để tránh lỗi chia cho 0.
            int remoteRatingCount = item.getCommentCount();
            if (remoteRatingCount <= 0) {
                remoteRatingCount = 40; // Con số an toàn phòng thủ để không bị chia cho 0 khi tính toán
            }

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
                    remoteRatingCount, // <--- THAM SỐ THỨ 11: Đã truyền chính xác vào trường ratingCount
                    item.getCommentCount(), // <--- THAM SỐ THỨ 12: trường commentCount độc lập hoàn toàn
                    item.getViewCount()    // <--- THAM SỐ THỨ 13: trường viewCount
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