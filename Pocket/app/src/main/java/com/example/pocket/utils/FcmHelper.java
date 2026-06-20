package com.example.pocket.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

public final class FcmHelper {
    private static final String TAG = "FcmHelper";
    private static final int MAX_PREVIEW_LENGTH = 100;

    private FcmHelper() {
    }

    public static void sendMessageNotification(@NonNull String receiverUid,
                                               @NonNull String senderName,
                                               @NonNull String messageText) {
        if (receiverUid.trim().isEmpty()) {
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        Map<String, Object> trigger = new HashMap<>();
        trigger.put("type", "message");
        trigger.put("recipientId", receiverUid);
        trigger.put("senderId", currentUser.getUid());
        trigger.put("senderName", senderName);
        trigger.put("body", truncate(messageText));
        trigger.put("createdAt", Timestamp.now());
        trigger.put("processed", false);

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_FCM_TRIGGERS)
                .document()
                .set(trigger)
                .addOnFailureListener(error -> Log.w(TAG, "Unable to create FCM trigger", error));
    }

    @NonNull
    private static String truncate(@NonNull String value) {
        String trimmed = value.trim();
        if (trimmed.length() <= MAX_PREVIEW_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_PREVIEW_LENGTH);
    }
}
