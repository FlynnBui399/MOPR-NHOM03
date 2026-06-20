package com.example.pocket.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.pocket.data.model.User;

public class SharedPrefManager {
    private static final String PREF_NAME = "pocket_user_prefs";
    private static final String KEY_UID = "uid";
    private static final String KEY_DISPLAY_NAME = "displayName";
    private static final String KEY_PHONE_NUMBER = "phoneNumber";
    private static final String KEY_AVATAR_URL = "avatarUrl";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_LANGUAGE_LOCALE = "language_locale";
    private static final String KEY_LATEST_PHOTO_URL = "latest_photo_url";
    private static final String KEY_LATEST_SENDER_NAME = "latest_sender_name";
    private static final String KEY_LATEST_PHOTO_TIMESTAMP = "latest_photo_timestamp";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static SharedPrefManager instance;
    private final SharedPreferences preferences;

    private SharedPrefManager(@NonNull Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public static synchronized SharedPrefManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new SharedPrefManager(context);
        }
        return instance;
    }

    public void saveUser(@NonNull User user) {
        preferences.edit()
                .putString(KEY_UID, user.getId())
                .putString(KEY_DISPLAY_NAME, user.getDisplayName())
                .putString(KEY_PHONE_NUMBER, user.getPhoneNumber())
                .putString(KEY_AVATAR_URL, user.getAvatarUrl())
                .apply();
    }

    public void updateAvatarUrl(@Nullable String avatarUrl) {
        preferences.edit().putString(KEY_AVATAR_URL, avatarUrl).apply();
    }

    @Nullable
    public String getAvatarUrl() {
        return preferences.getString(KEY_AVATAR_URL, null);
    }

    public void setThemeMode(int mode) {
        preferences.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    public int getThemeMode() {
        return preferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_YES);
    }

    public void setLanguageLocale(@NonNull String localeTag) {
        preferences.edit().putString(KEY_LANGUAGE_LOCALE, localeTag).apply();
    }
    @Nullable
    public String getUserId() {
        return preferences.getString(KEY_UID, null);
    }
    @Nullable
    public String getLanguageLocale() {
        return preferences.getString(KEY_LANGUAGE_LOCALE, null);
    }

    public void clear() {
        preferences.edit().clear().apply();
    }

    public void setLatestPhotoUrl(@Nullable String imageUrl) {
        preferences.edit().putString(KEY_LATEST_PHOTO_URL, imageUrl).apply();
    }

    @Nullable
    public String getLatestPhotoUrl() {
        return preferences.getString(KEY_LATEST_PHOTO_URL, null);
    }

    public void setLatestSenderName(@Nullable String senderName) {
        preferences.edit().putString(KEY_LATEST_SENDER_NAME, senderName).apply();
    }

    @Nullable
    public String getLatestSenderName() {
        return preferences.getString(KEY_LATEST_SENDER_NAME, null);
    }

    public void setLatestPhotoTimestamp(long timestampMillis) {
        preferences.edit().putLong(KEY_LATEST_PHOTO_TIMESTAMP, timestampMillis).apply();
    }

    public long getLatestPhotoTimestamp() {
        return preferences.getLong(KEY_LATEST_PHOTO_TIMESTAMP, 0L);
    }

    public void setNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public boolean areNotificationsEnabled() {
        return preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }
}
