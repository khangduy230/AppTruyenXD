package com.nhom5.ftcomic.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.nhom5.ftcomic.database.AppDatabase;
import com.nhom5.ftcomic.models.ChapterPage;
import com.nhom5.ftcomic.models.DownloadedChapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OfflineDownloadManager {

    public interface DownloadCallback {
        void onStart(int totalPages);
        void onProgress(int currentPage, int totalPages);
        void onSuccess(int totalPages);
        void onError(String message);
    }

    private final Context context;
    private final AppDatabase appDatabase;
    private final OkHttpClient okHttpClient;
    private final Handler mainHandler;

    public OfflineDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.appDatabase = AppDatabase.getInstance(context);
        this.okHttpClient = new OkHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void downloadChapter(int comicId, int chapterId, DownloadCallback callback) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<ChapterPage> pages = appDatabase.chapterPageDao()
                        .getPagesByChapterIdSync(chapterId);

                if (pages == null || pages.isEmpty()) {
                    postError(callback, "Chưa có dữ liệu trang truyện để tải");
                    return;
                }

                int totalPages = pages.size();
                mainHandler.post(() -> callback.onStart(totalPages));

                File chapterDir = getChapterDownloadDir(comicId, chapterId);

                if (!chapterDir.exists()) {
                    boolean created = chapterDir.mkdirs();

                    if (!created && !chapterDir.exists()) {
                        postError(callback, "Không tạo được thư mục tải xuống");
                        return;
                    }
                }

                int current = 0;

                for (ChapterPage page : pages) {
                    current++;

                    String imageUrl = page.getImageUrl();

                    if (imageUrl == null || imageUrl.trim().isEmpty()) {
                        postError(callback, "Trang " + page.getPageNumber() + " không có image_url");
                        return;
                    }

                    File imageFile = new File(
                            chapterDir,
                            "page_" + page.getPageNumber() + ".jpg"
                    );

                    if (!imageFile.exists() || imageFile.length() == 0) {
                        downloadImageToFile(imageUrl, imageFile);
                    }

                    appDatabase.chapterPageDao().updateLocalFilePath(
                            page.getId(),
                            imageFile.getAbsolutePath()
                    );

                    int finalCurrent = current;
                    mainHandler.post(() -> callback.onProgress(finalCurrent, totalPages));
                }

                DownloadedChapter downloadedChapter = new DownloadedChapter(
                        chapterId,
                        comicId,
                        System.currentTimeMillis(),
                        totalPages
                );

                appDatabase.downloadedChapterDao().insertDownloadedChapter(downloadedChapter);

                mainHandler.post(() -> callback.onSuccess(totalPages));

            } catch (Exception e) {
                postError(callback, "Tải thất bại: " + e.getMessage());
            }
        });
    }

    public void deleteDownloadedChapter(int comicId, int chapterId, Runnable onDone) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            File chapterDir = getChapterDownloadDir(comicId, chapterId);
            deleteFileOrDirectory(chapterDir);

            appDatabase.chapterPageDao().clearLocalFilePathsByChapterId(chapterId);
            appDatabase.downloadedChapterDao().deleteDownloadedChapter(chapterId);

            if (onDone != null) {
                mainHandler.post(onDone);
            }
        });
    }

    private void downloadImageToFile(String imageUrl, File outputFile) throws Exception {
        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        Response response = okHttpClient.newCall(request).execute();

        if (!response.isSuccessful() || response.body() == null) {
            throw new Exception("Không tải được ảnh: HTTP " + response.code());
        }

        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            inputStream = response.body().byteStream();
            outputStream = new FileOutputStream(outputFile);

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();

        } finally {
            response.close();

            if (inputStream != null) {
                inputStream.close();
            }

            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    private File getChapterDownloadDir(int comicId, int chapterId) {
        return new File(
                context.getFilesDir(),
                "downloads/comic_" + comicId + "/chapter_" + chapterId
        );
    }

    private void deleteFileOrDirectory(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();

            if (children != null) {
                for (File child : children) {
                    deleteFileOrDirectory(child);
                }
            }
        }

        file.delete();
    }

    private void postError(DownloadCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}