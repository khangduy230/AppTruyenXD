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
        // THÊM: Callback khi tiến trình tải bị huỷ bỏ
        default void onCancel() {}
    }

    private final Context context;
    private final AppDatabase appDatabase;
    private final OkHttpClient okHttpClient;
    private final Handler mainHandler;
    
    // THÊM: Biến volatile để đánh dấu trạng thái huỷ tải, đảm bảo các luồng (thread) đều thấy giá trị mới nhất
    private volatile boolean isCancelled = false;

    public OfflineDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.appDatabase = AppDatabase.getInstance(context);
        this.okHttpClient = new OkHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    // THÊM: Phương thức công khai để bên ngoài (Activity/Fragment) gọi khi muốn dừng tải
    public void cancel() {
        isCancelled = true;
    }

    public void downloadChapter(int comicId, int chapterId, DownloadCallback callback) {
        isCancelled = false; // Reset cờ huỷ mỗi khi bắt đầu một lượt tải mới
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
                    // KIỂM TRA: Nếu người dùng đã bấm huỷ, thoát khỏi vòng lặp và thông báo callback
                    if (isCancelled) {
                        mainHandler.post(callback::onCancel);
                        return;
                    }
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

                    // KIỂM TRA: Kiểm tra huỷ sau khi tải xong một ảnh (trước khi lưu vào Database)
                    if (isCancelled) {
                        mainHandler.post(callback::onCancel);
                        return;
                    }

                    appDatabase.chapterPageDao().updateLocalFilePath(
                            page.getId(),
                            imageFile.getAbsolutePath()
                    );

                    int finalCurrent = current;
                    mainHandler.post(() -> callback.onProgress(finalCurrent, totalPages));
                }

                if (isCancelled) {
                    mainHandler.post(callback::onCancel);
                    return;
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
                // Nếu lỗi xảy ra do ta chủ động ngắt (huỷ) thì gọi onCancel thay vì onError
                if (isCancelled) {
                    mainHandler.post(callback::onCancel);
                } else {
                    postError(callback, "Tải thất bại: " + e.getMessage());
                }
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
                // KIỂM TRA: Nếu huỷ trong lúc đang ghi file, thoát vòng lặp ngay
                if (isCancelled) break;
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
            
            // Xoá file bị tải dở nếu người dùng huỷ
            if (isCancelled && outputFile.exists()) {
                outputFile.delete();
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