package com.example.pocket;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.example.pocket.utils.SharedPrefManager;

public class PocketApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Apply theme mode on launch
        int themeMode = SharedPrefManager.getInstance(this).getThemeMode();
        AppCompatDelegate.setDefaultNightMode(themeMode);

        // Apply language locale on launch (as fallback for older APIs)
        String localeTag = SharedPrefManager.getInstance(this).getLanguageLocale();
        if (localeTag != null) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag));
        }
    }
}
