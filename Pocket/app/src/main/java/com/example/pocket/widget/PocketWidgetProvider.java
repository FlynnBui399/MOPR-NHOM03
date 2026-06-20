package com.example.pocket.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.example.pocket.MainActivity;
import com.example.pocket.R;
import com.example.pocket.data.model.Photo;
import com.example.pocket.utils.SharedPrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PocketWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_WIDGET_UPDATE = "com.example.pocket.action.WIDGET_UPDATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        updateWidgets(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_WIDGET_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, PocketWidgetProvider.class));
            updateWidgets(context, appWidgetManager, appWidgetIds);
        }
    }

    public static void updateLatestPhoto(@NonNull Context context, @Nullable Photo photo) {
        if (photo == null) {
            return;
        }
        String imageUrl = firstNonEmpty(photo.getImageUrl(), photo.getThumbnailUrl());
        if (imageUrl.isEmpty()) {
            return;
        }
        SharedPrefManager sharedPrefManager = SharedPrefManager.getInstance(context);
        sharedPrefManager.setLatestPhotoUrl(imageUrl);
        sharedPrefManager.setLatestSenderName(photo.getSenderName());
        long timestampMillis = photo.getCreatedAt() == null
                ? System.currentTimeMillis()
                : photo.getCreatedAt().toDate().getTime();
        sharedPrefManager.setLatestPhotoTimestamp(timestampMillis);
        requestUpdate(context);
    }

    public static void updateLatestPhoto(@NonNull Context context,
                                         @Nullable String imageUrl,
                                         @Nullable String senderName,
                                         long timestampMillis) {
        String safeImageUrl = firstNonEmpty(imageUrl);
        if (safeImageUrl.isEmpty()) {
            return;
        }
        SharedPrefManager sharedPrefManager = SharedPrefManager.getInstance(context);
        sharedPrefManager.setLatestPhotoUrl(safeImageUrl);
        sharedPrefManager.setLatestSenderName(senderName);
        sharedPrefManager.setLatestPhotoTimestamp(timestampMillis > 0L
                ? timestampMillis
                : System.currentTimeMillis());
        requestUpdate(context);
    }

    public static void requestUpdate(@NonNull Context context) {
        Intent updateWidgetIntent = new Intent(context, PocketWidgetProvider.class);
        updateWidgetIntent.setAction(ACTION_WIDGET_UPDATE);
        updateWidgetIntent.setPackage(context.getPackageName());
        context.sendBroadcast(updateWidgetIntent);
    }

    private static void updateWidgets(Context context, AppWidgetManager appWidgetManager,
                                      int[] appWidgetIds) {
        SharedPrefManager sharedPrefManager = SharedPrefManager.getInstance(context);
        String imageUrl = sharedPrefManager.getLatestPhotoUrl();
        String senderName = sharedPrefManager.getLatestSenderName();
        long timestampMillis = sharedPrefManager.getLatestPhotoTimestamp();

        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_pocket);
            setupOpenAppAction(context, remoteViews, appWidgetId);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                remoteViews.setViewVisibility(R.id.widget_empty_message, View.GONE);
                remoteViews.setTextViewText(R.id.widget_sender_name,
                        senderName != null && !senderName.trim().isEmpty()
                                ? senderName
                                : context.getString(R.string.app_name));
                remoteViews.setTextViewText(R.id.widget_timestamp,
                        formatWidgetTime(timestampMillis));

                AppWidgetTarget appWidgetTarget = new AppWidgetTarget(context,
                        R.id.widget_image, remoteViews, appWidgetId) {
                    @Override
                    public void onResourceReady(@NonNull android.graphics.Bitmap resource,
                                                @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                        super.onResourceReady(resource, transition);
                        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
                    }
                };

                Glide.with(context.getApplicationContext())
                        .asBitmap()
                        .load(imageUrl)
                        .into(appWidgetTarget);
            } else {
                remoteViews.setImageViewResource(R.id.widget_image,
                        R.drawable.placeholder_pocket_widget_preview);
                remoteViews.setViewVisibility(R.id.widget_empty_message, View.VISIBLE);
                remoteViews.setTextViewText(R.id.widget_sender_name,
                        context.getString(R.string.app_name));
                remoteViews.setTextViewText(R.id.widget_timestamp,
                        context.getString(R.string.widget_empty_hint));
            }

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    private static void setupOpenAppAction(Context context, RemoteViews remoteViews,
                                           int appWidgetId) {
        Intent intent = new Intent(context, MainActivity.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, flags);
        remoteViews.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
    }

    @NonNull
    private static String formatWidgetTime(long timestampMillis) {
        long safeTimestamp = timestampMillis > 0L ? timestampMillis : System.currentTimeMillis();
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(safeTimestamp));
    }

    @NonNull
    private static String firstNonEmpty(@Nullable String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }
}
