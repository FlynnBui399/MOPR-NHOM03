package com.example.pocket.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class FcmHelper {
    private static final String TAG = "FcmHelper";
    private static final String FCM_URL_TEMPLATE = "https://fcm.googleapis.com/v1/projects/%s/messages:send";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final int MAX_PREVIEW_LENGTH = 100;

    private static final ExecutorService NETWORK_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    private FcmHelper() {
    }

    public static void sendMessageNotification(@NonNull String receiverUid,
                                               @NonNull String senderName,
                                               @NonNull String messageText) {
        if (receiverUid.trim().isEmpty()) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(receiverUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String receiverToken = snapshot == null ? null : snapshot.getString("fcmToken");
                    if (receiverToken == null || receiverToken.trim().isEmpty()) {
                        return;
                    }

                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser == null) {
                        return;
                    }

                    currentUser.getIdToken(false)
                            .addOnSuccessListener(tokenResult -> {
                                String authToken = tokenResult.getToken();
                                if (authToken == null || authToken.trim().isEmpty()) {
                                    return;
                                }
                                NETWORK_EXECUTOR.execute(() -> postNotification(
                                        receiverToken,
                                        authToken,
                                        currentUser.getUid(),
                                        senderName,
                                        messageText
                                ));
                            })
                            .addOnFailureListener(error -> Log.w(TAG, "Unable to get Firebase auth token", error));
                })
                .addOnFailureListener(error -> Log.w(TAG, "Unable to fetch receiver FCM token", error));
    }

    private static void postNotification(@NonNull String receiverToken,
                                         @NonNull String authToken,
                                         @NonNull String senderUid,
                                         @NonNull String senderName,
                                         @NonNull String messageText) {
        String projectId = FirebaseApp.getInstance().getOptions().getProjectId();
        if (projectId == null || projectId.trim().isEmpty()) {
            Log.w(TAG, "Firebase project id is missing");
            return;
        }

        try {
            String payload = buildPayload(receiverToken, senderUid, senderName, messageText);
            Request request = new Request.Builder()
                    .url(String.format(FCM_URL_TEMPLATE, projectId))
                    .addHeader("Authorization", "Bearer " + authToken)
                    .addHeader("Content-Type", "application/json; UTF-8")
                    .post(RequestBody.create(payload, JSON))
                    .build();

            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.w(TAG, "FCM send failed: " + response.code());
                }
            }
        } catch (IOException | JSONException error) {
            Log.w(TAG, "Unable to send FCM notification", error);
        }
    }

    @NonNull
    private static String buildPayload(@NonNull String receiverToken,
                                       @NonNull String senderUid,
                                       @NonNull String senderName,
                                       @NonNull String messageText) throws JSONException {
        JSONObject notification = new JSONObject()
                .put("title", senderName)
                .put("body", truncate(messageText));

        JSONObject data = new JSONObject()
                .put("type", "message")
                .put("senderUid", senderUid)
                .put("senderName", senderName);

        JSONObject message = new JSONObject()
                .put("token", receiverToken)
                .put("notification", notification)
                .put("data", data);

        return new JSONObject()
                .put("message", message)
                .toString();
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
