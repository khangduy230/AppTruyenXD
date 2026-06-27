package com.nhom5.ftcomic.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "my_application";

    private static final String KEY_LOGGED_IN = "DaDangNhap";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";

    private final SharedPreferences sharedPreferences;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String accessToken, String refreshToken, String userId, String email, String role) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ROLE, role);

        editor.apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, "");
    }

    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, "");
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, "");
    }

    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, "");
    }

    public String getRole() {
        return sharedPreferences.getString(KEY_ROLE, "user");
    }

    public void saveRole(String role) {
        sharedPreferences.edit().putString(KEY_ROLE, role).apply();
    }

    public void saveUsername(String username) {
        String email = getEmail();
        sharedPreferences.edit()
                .putString("username_" + email, username)
                .apply();
    }

    public String getUsername() {
        String email = getEmail();
        return sharedPreferences.getString("username_" + email, null);
    }

    public void logout() {
        String savedUsername = getUsername();
        String email = getEmail();
        sharedPreferences.edit().clear().apply();
        sharedPreferences.edit()
                .putString("username_" + email, savedUsername)
                .apply();
    }

    public void saveAvatarUri(String uri) {
        String email = getEmail();
        sharedPreferences.edit()
                .putString("avatar_" + email, uri)
                .apply();
    }

    public String getAvatarUri() {
        String email = getEmail();
        return sharedPreferences.getString("avatar_" + email, null);
    }
}