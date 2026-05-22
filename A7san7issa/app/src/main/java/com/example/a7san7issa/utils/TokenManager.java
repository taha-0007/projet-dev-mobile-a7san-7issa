package com.example.a7san7issa.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "A7san7issaPrefs";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String IS_STAFF = "is_staff";
    private static final String USERNAME = "username";
    private static final String EMAIL = "email";
    private static final String PROFILE_IMAGE_PATH = "profile_image_path";

    private SharedPreferences prefs;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveTokens(String access, String refresh) {
        prefs.edit().putString(ACCESS_TOKEN, access).putString(REFRESH_TOKEN, refresh).apply();
    }

    public String getAccessToken() {
        return prefs.getString(ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(REFRESH_TOKEN, null);
    }

    public void clearTokens() {
        prefs.edit().remove(ACCESS_TOKEN).remove(REFRESH_TOKEN).remove(IS_STAFF).remove(USERNAME).remove(EMAIL).remove(PROFILE_IMAGE_PATH).apply();
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void saveUserInfo(boolean isStaff, String username, String email) {
        prefs.edit().putBoolean(IS_STAFF, isStaff).putString(USERNAME, username).putString(EMAIL, email).apply();
    }

    public boolean isStaff() {
        return prefs.getBoolean(IS_STAFF, false);
    }

    public String getUsername() {
        return prefs.getString(USERNAME, "");
    }

    public String getEmail() {
        return prefs.getString(EMAIL, "");
    }

    public void saveProfileImagePath(String path) {
        prefs.edit().putString(PROFILE_IMAGE_PATH, path).apply();
    }

    public String getProfileImagePath() {
        return prefs.getString(PROFILE_IMAGE_PATH, null);
    }
}