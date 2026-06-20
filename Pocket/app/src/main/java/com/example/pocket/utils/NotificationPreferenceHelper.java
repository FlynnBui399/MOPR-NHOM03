package com.example.pocket.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public final class NotificationPreferenceHelper {
    public static final String FIELD_NOTIFICATIONS_ENABLED = "notificationsEnabled";

    private NotificationPreferenceHelper() {
    }

    public static boolean hasPostNotificationPermission(@NonNull Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean areNotificationsAllowed(@NonNull Context context) {
        return SharedPrefManager.getInstance(context).areNotificationsEnabled()
                && hasPostNotificationPermission(context);
    }

    public static void setNotificationsEnabled(@NonNull Context context, boolean enabled) {
        SharedPrefManager.getInstance(context).setNotificationsEnabled(enabled);
        syncCurrentUserPreference(enabled);
    }

    public static void syncCurrentUserPreference(boolean enabled) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .update(FIELD_NOTIFICATIONS_ENABLED, enabled);
    }
}
