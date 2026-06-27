package com.example.pocket;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Application;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.example.pocket.utils.SharedPrefManager;

public class PocketApplication extends Application {
    private static final String MESSAGES_CHANNEL_NAME = "Messages";
    private static final String MESSAGES_CHANNEL_DESCRIPTION = "New Pocket chat messages";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();

        // Apply theme mode on launch
        int themeMode = SharedPrefManager.getInstance(this).getThemeMode();
        AppCompatDelegate.setDefaultNightMode(themeMode);

        // Apply language locale on launch (as fallback for older APIs)
        String localeTag = SharedPrefManager.getInstance(this).getLanguageLocale();
        if (localeTag != null) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag));
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel messagesChannel = new NotificationChannel(
                PocketMessagingService.CHANNEL_MESSAGES,
                MESSAGES_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        messagesChannel.setDescription(MESSAGES_CHANNEL_DESCRIPTION);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(messagesChannel);
        }
    }
}
