package com.example.pocket.widget;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocket.data.model.Photo;
import com.example.pocket.data.model.User;
import com.example.pocket.utils.Constants;
import com.example.pocket.utils.SharedPrefManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public final class WidgetPhotoUpdater {
    private WidgetPhotoUpdater() {
    }

    public static boolean isFriendPhoto(@Nullable Photo photo,
                                        @Nullable String currentUserId,
                                        @Nullable List<User> friends) {
        if (photo == null) {
            return false;
        }
        return isFriendSender(photo.getSenderId(), currentUserId, friendIdsFromUsers(friends));
    }

    public static void updateLatestFriendPhoto(@NonNull Context context,
                                               @NonNull Photo photo,
                                               @Nullable String currentUserId,
                                               @Nullable List<User> friends,
                                               @Nullable String senderName) {
        if (!isFriendPhoto(photo, currentUserId, friends)) {
            return;
        }
        String imageUrl = firstNonEmpty(photo.getImageUrl(), photo.getThumbnailUrl());
        if (imageUrl.isEmpty()) {
            return;
        }
        long timestampMillis = photo.getCreatedAt() == null
                ? System.currentTimeMillis()
                : photo.getCreatedAt().toDate().getTime();
        PocketWidgetProvider.updateLatestPhoto(context, photo.getId(), imageUrl,
                senderName, timestampMillis);
    }

    public static void updateLatestCurrentFriendPhoto(@NonNull Context context,
                                                      @NonNull Photo photo) {
        String currentUserId = SharedPrefManager.getInstance(context).getUserId();
        updateLatestCurrentFriendPhoto(context, photo, currentUserId);
    }

    public static void updateLatestCurrentFriendPhoto(@NonNull Context context,
                                                      @NonNull Photo photo,
                                                      @Nullable String currentUserId) {
        String senderId = photo.getSenderId();
        if (senderId == null || senderId.trim().isEmpty()
                || currentUserId == null || currentUserId.trim().isEmpty()
                || senderId.equals(currentUserId)) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    User currentUser = snapshot.toObject(User.class);
                    List<String> friendIds = currentUser == null
                            ? null
                            : currentUser.getFriendIds();
                    if (isFriendSender(senderId, currentUserId, friendIds)) {
                        PocketWidgetProvider.updateLatestPhoto(
                                context.getApplicationContext(), photo);
                    }
                });
    }

    public static void updateLatestCurrentFriendPhoto(@NonNull Context context,
                                                      @Nullable String senderId,
                                                      @Nullable String photoId,
                                                      @Nullable String imageUrl,
                                                      @Nullable String senderName,
                                                      long timestampMillis) {
        String currentUserId = SharedPrefManager.getInstance(context).getUserId();
        if (senderId == null || senderId.trim().isEmpty()
                || currentUserId == null || currentUserId.trim().isEmpty()
                || senderId.equals(currentUserId)) {
            return;
        }
        String safeImageUrl = firstNonEmpty(imageUrl);
        if (safeImageUrl.isEmpty()) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    User currentUser = snapshot.toObject(User.class);
                    List<String> friendIds = currentUser == null
                            ? null
                            : currentUser.getFriendIds();
                    if (isFriendSender(senderId, currentUserId, friendIds)) {
                        PocketWidgetProvider.updateLatestPhoto(
                                context.getApplicationContext(),
                                photoId,
                                safeImageUrl,
                                senderName,
                                timestampMillis);
                    }
                });
    }

    private static boolean isFriendSender(@Nullable String senderId,
                                          @Nullable String currentUserId,
                                          @Nullable List<String> friendIds) {
        if (senderId == null || senderId.trim().isEmpty()
                || currentUserId == null || currentUserId.trim().isEmpty()
                || senderId.equals(currentUserId)
                || friendIds == null) {
            return false;
        }
        return friendIds.contains(senderId);
    }

    @NonNull
    private static List<String> friendIdsFromUsers(@Nullable List<User> friends) {
        java.util.ArrayList<String> friendIds = new java.util.ArrayList<>();
        if (friends == null) {
            return friendIds;
        }
        for (User friend : friends) {
            if (friend != null && friend.getId() != null && !friend.getId().trim().isEmpty()) {
                friendIds.add(friend.getId());
            }
        }
        return friendIds;
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
