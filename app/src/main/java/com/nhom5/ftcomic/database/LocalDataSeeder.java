package com.nhom5.ftcomic.database;

import android.content.Context;

import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.models.Category;
import com.nhom5.ftcomic.models.Chapter;
import com.nhom5.ftcomic.models.ChapterPage;
import com.nhom5.ftcomic.models.Comic;
import com.nhom5.ftcomic.models.ComicCategoryCrossRef;

import java.util.ArrayList;
import java.util.List;

public class LocalDataSeeder {

    public static void seedIfNeeded(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            int count = db.comicDao().countComics();

            if (count > 0) {
                return;
            }

            insertComics(db);
            insertCategories(db);
            insertComicCategoryRefs(db);
            insertChapters(db);
            insertChapterPages(db);
        });
    }

    private static void insertComics(AppDatabase db) {
        List<Comic> comics = new ArrayList<>();

        comics.add(new Comic(
                1,
                R.drawable.thientai,
                "Thiên Tài Bóng Đêm",
                "Futho High",
                "Một bộ truyện giả tưởng kể về nhân vật chính trên hành trình khám phá sức mạnh bí ẩn.",
                "Đang ra",
                "featured",
                3690,
                4.7f,
                150,
                12000
        ));

        comics.add(new Comic(
                2,
                R.drawable.thientai,
                "Học Viện Siêu Năng",
                "Nhóm 5",
                "Câu chuyện về những học sinh sở hữu năng lực đặc biệt trong một học viện bí ẩn.",
                "Đang ra",
                "featured",
                2800,
                4.5f,
                90,
                9800
        ));

        comics.add(new Comic(
                3,
                R.drawable.thientai,
                "Vua Truyện Tranh",
                "Tác giả A",
                "Một hành trình hài hước và kịch tính của nhân vật chính trong thế giới truyện tranh.",
                "Hoàn thành",
                "ranking",
                5100,
                4.9f,
                230,
                25000
        ));

        comics.add(new Comic(
                4,
                R.drawable.thientai,
                "Thanh Xuân Rực Rỡ",
                "Tác giả B",
                "Một câu chuyện học đường nhẹ nhàng, tình cảm và gần gũi.",
                "Đang ra",
                "ranking",
                4100,
                4.6f,
                180,
                17000
        ));

        comics.add(new Comic(
                5,
                R.drawable.thientai,
                "Truyện Phiêu Lưu 1",
                "Tác giả C",
                "Nhân vật chính bắt đầu chuyến phiêu lưu qua nhiều vùng đất kỳ lạ.",
                "Đang ra",
                "all",
                1200,
                4.2f,
                40,
                5000
        ));

        comics.add(new Comic(
                6,
                R.drawable.thientai,
                "Truyện Hành Động 2",
                "Tác giả D",
                "Một bộ truyện hành động với nhiều trận chiến gay cấn.",
                "Đang ra",
                "all",
                1800,
                4.4f,
                60,
                6500
        ));

        comics.add(new Comic(
                7,
                R.drawable.thientai,
                "Truyện Hài Hước 3",
                "Tác giả E",
                "Bộ truyện giải trí nhẹ nhàng, phù hợp đọc thư giãn.",
                "Hoàn thành",
                "all",
                900,
                4.1f,
                25,
                3000
        ));

        db.comicDao().insertComics(comics);
    }

    private static void insertCategories(AppDatabase db) {
        List<Category> categories = new ArrayList<>();

        categories.add(new Category(1, "Hành động"));
        categories.add(new Category(2, "Phiêu lưu"));
        categories.add(new Category(3, "Học đường"));
        categories.add(new Category(4, "Hài hước"));
        categories.add(new Category(5, "Giả tưởng"));

        db.categoryDao().insertCategories(categories);
    }

    private static void insertComicCategoryRefs(AppDatabase db) {
        List<ComicCategoryCrossRef> refs = new ArrayList<>();

        refs.add(new ComicCategoryCrossRef(1, 1));
        refs.add(new ComicCategoryCrossRef(1, 5));

        refs.add(new ComicCategoryCrossRef(2, 3));
        refs.add(new ComicCategoryCrossRef(2, 5));

        refs.add(new ComicCategoryCrossRef(3, 1));
        refs.add(new ComicCategoryCrossRef(3, 4));

        refs.add(new ComicCategoryCrossRef(4, 3));

        refs.add(new ComicCategoryCrossRef(5, 2));
        refs.add(new ComicCategoryCrossRef(6, 1));
        refs.add(new ComicCategoryCrossRef(7, 4));

        db.categoryDao().insertComicCategoryRefs(refs);
    }

    private static void insertChapters(AppDatabase db) {
        List<Chapter> chapters = new ArrayList<>();

        chapters.add(new Chapter(101, 1, 1, "Chương 1: Khởi đầu", "2 ngày trước"));
        chapters.add(new Chapter(102, 1, 2, "Chương 2: Sức mạnh thức tỉnh", "1 ngày trước"));

        chapters.add(new Chapter(201, 2, 1, "Chương 1: Học viện bí ẩn", "3 ngày trước"));
        chapters.add(new Chapter(202, 2, 2, "Chương 2: Bài kiểm tra đầu tiên", "1 ngày trước"));

        chapters.add(new Chapter(301, 3, 1, "Chương 1: Hành trình bắt đầu", "5 ngày trước"));
        chapters.add(new Chapter(302, 3, 2, "Chương 2: Đối thủ xuất hiện", "4 ngày trước"));

        chapters.add(new Chapter(401, 4, 1, "Chương 1: Ngày đầu đến lớp", "6 ngày trước"));
        chapters.add(new Chapter(501, 5, 1, "Chương 1: Vùng đất mới", "7 ngày trước"));
        chapters.add(new Chapter(601, 6, 1, "Chương 1: Trận chiến đầu tiên", "8 ngày trước"));
        chapters.add(new Chapter(701, 7, 1, "Chương 1: Một ngày kỳ lạ", "9 ngày trước"));

        db.chapterDao().insertChapters(chapters);
    }

    private static void insertChapterPages(AppDatabase db) {
        List<ChapterPage> pages = new ArrayList<>();

        int id = 1;

        int[] chapterIds = {
                101, 102,
                201, 202,
                301, 302,
                401, 501, 601, 701
        };

        for (int chapterId : chapterIds) {
            pages.add(new ChapterPage(id++, chapterId, 1, R.drawable.thientai, "", ""));
            pages.add(new ChapterPage(id++, chapterId, 2, R.drawable.thientai, "", ""));
            pages.add(new ChapterPage(id++, chapterId, 3, R.drawable.thientai, "", ""));
        }

        db.chapterPageDao().insertPages(pages);
    }
}