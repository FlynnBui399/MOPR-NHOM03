package com.example.pocket;

import android.Manifest;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.pocket.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class PocketMessagingService extends FirebaseMessagingService {
    public static final String CHANNEL_MESSAGES = "pocket_messages";
    private static final int MESSAGE_NOTIFICATION_BASE_ID = 4000;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Map<String, String> data = message.getData();
        boolean isMessagePayload = "message".equals(data.get("type"));
        if (!isMessagePayload && isAppInForeground()) {
            return;
        }

        RemoteMessage.Notification notification = message.getNotification();
        String senderName = valueOrDefault(data.get("senderName"),
                notification == null ? getString(R.string.app_name) : notification.getTitle());
        String body = valueOrDefault(data.get("body"),
                notification == null ? "" : notification.getBody());
        String friendUid = firstNonEmpty(
                data.get("friendUid"),
                data.get("senderUid"),
                data.get("fromUid")
        );
        String avatarUrl = valueOrDefault(data.get("senderAvatar"), data.get("friendAvatar"));

        showMessageNotification(senderName, body, friendUid, avatarUrl);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        saveTokenToFirestore(token);
    }

    public static void refreshTokenForCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(PocketMessagingService::saveTokenToFirestore);
    }

    private static void saveTokenToFirestore(@NonNull String token) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || token.trim().isEmpty()) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(user.getUid())
                .update("fcmToken", token);
    }

    private void showMessageNotification(@NonNull String senderName,
                                         @NonNull String body,
                                         @NonNull String friendUid,
                                         @NonNull String avatarUrl) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_UID, friendUid);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_NAME, senderName);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_AVATAR, avatarUrl);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int notificationId = MESSAGE_NOTIFICATION_BASE_ID + Math.abs(friendUid.hashCode() % 1000);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, notificationId, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle(senderName)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(this).notify(notificationId, builder.build());
    }

    private static boolean isAppInForeground() {
        ActivityManager.RunningAppProcessInfo processInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(processInfo);
        return processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                || processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
    }

    @NonNull
    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    @NonNull
    private static String valueOrDefault(String value, String fallback) {
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }
        return fallback == null ? "" : fallback;
    }
}
