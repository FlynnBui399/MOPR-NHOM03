package com.example.pocket.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.example.pocket.R;
import com.example.pocket.utils.SharedPrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PocketWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        updateWidgets(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if ("WIDGET_UPDATE".equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                    new android.content.ComponentName(context, PocketWidgetProvider.class));
            updateWidgets(context, appWidgetManager, appWidgetIds);
        }
    }

    private static void updateWidgets(Context context, AppWidgetManager appWidgetManager,
                                      int[] appWidgetIds) {
        String imageUrl = SharedPrefManager.getInstance(context).getLatestPhotoUrl();
        String senderName = SharedPrefManager.getInstance(context).getLatestSenderName();

        for (int appWidgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_pocket);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                AppWidgetTarget appWidgetTarget = new AppWidgetTarget(context,
                        R.id.widget_image, remoteViews, appWidgetId) {
                    @Override
                    public void onResourceReady(@NonNull android.graphics.Bitmap resource,
                                                @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                        super.onResourceReady(resource, transition);
                        remoteViews.setTextViewText(R.id.widget_sender_name,
                                senderName != null ? senderName : "Pocket");
                        remoteViews.setTextViewText(R.id.widget_timestamp,
                                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
                        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
                    }
                };

                Glide.with(context.getApplicationContext())
                        .asBitmap()
                        .load(imageUrl)
                        .into(appWidgetTarget);
            } else {
                remoteViews.setTextViewText(R.id.widget_sender_name, "Pocket");
            }

            // Thêm sự kiện bấm vào Widget để mở app
            Intent intent = new Intent(context, com.example.pocket.MainActivity.class);
            int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
            }
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(context, 0, intent, flags);
            remoteViews.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }
}
