package com.example.pocket.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocket.data.model.User;

public class SharedPrefManager {
    private static final String PREF_NAME = "pocket_user_prefs";
    private static final String KEY_UID = "uid";
    private static final String KEY_DISPLAY_NAME = "displayName";
    private static final String KEY_PHONE_NUMBER = "phoneNumber";
    private static final String KEY_AVATAR_URL = "avatarUrl";

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

    public void clear() {
        preferences.edit().clear().apply();
    }
}
