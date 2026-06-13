package com.nhom5.ftcomic.network;

public class SupabaseConfig {

    public static final String PROJECT_URL = "https://enkliqnftrprfyszkyov.supabase.co/";

    public static final String BASE_URL = PROJECT_URL + "rest/v1/";
    public static final String AUTH_BASE_URL = PROJECT_URL + "auth/v1/";

    // Giữ publishable key hiện tại của bạn ở đây
    public static final String API_KEY = "sb_publishable_AmtjAUXr15c4VmXZfl-UEg_Njdqm6Zq";

    public static final String STORAGE_PUBLIC_URL =
            PROJECT_URL + "storage/v1/object/public/";

    public static final String COMICS_BUCKET = "comics-storage";
    public static final String AVATARS_BUCKET = "avatars";

    public static String getComicCoverUrl(String fileName) {
        return STORAGE_PUBLIC_URL + COMICS_BUCKET + "/covers/" + fileName;
    }

    public static String getChapterPageUrl(String folderName, int chapterNumber, int pageNumber) {
        return STORAGE_PUBLIC_URL
                + COMICS_BUCKET
                + "/chapters/"
                + folderName
                + "/chapter-" + chapterNumber
                + "/page-" + pageNumber
                + ".jpg";
    }
}