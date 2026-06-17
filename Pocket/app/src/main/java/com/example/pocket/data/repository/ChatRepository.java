package com.example.pocket.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pocket.data.model.Message;
import com.example.pocket.utils.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {
    private static final String TAG = "ChatRepository";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public ChatRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    public String getChatId(@NonNull String uid1, @NonNull String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    public void sendMessage(@NonNull String chatId,
                            @NonNull String content,
                            @NonNull String type,
                            @NonNull UserRepository.Callback<Void> callback) {
        String senderId = auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
        if (senderId == null) {
            callback.onError(new IllegalStateException("User is not signed in"));
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("chatId", chatId);
        message.put("senderId", senderId);
        message.put("content", content);
        message.put("text", content);
        message.put("type", type);
        message.put("createdAt", Timestamp.now());
        message.put("read", false);

        firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.COLLECTION_MESSAGES)
                .document()
                .set(message)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to send message", error);
                    callback.onError(error);
                });
    }

    public void sendPhotoReply(@NonNull String chatId,
                               @NonNull String replyText,
                               @NonNull String quotedPhotoUrl,
                               @NonNull String quotedPhotoId,
                               @NonNull UserRepository.Callback<Void> callback) {
        String senderId = auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
        if (senderId == null) {
            callback.onError(new IllegalStateException("User is not signed in"));
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("chatId", chatId);
        message.put("senderId", senderId);
        message.put("text", replyText);
        message.put("content", replyText);
        message.put("type", "photo_reply");
        message.put("quotedPhotoUrl", quotedPhotoUrl);
        message.put("quotedPhotoId", quotedPhotoId);
        message.put("createdAt", Timestamp.now());
        message.put("read", false);

        firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.COLLECTION_MESSAGES)
                .document()
                .set(message)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to send photo reply", error);
                    callback.onError(error);
                });
    }

    @NonNull
    public LiveData<List<Message>> getMessages(@NonNull String chatId) {
        MutableLiveData<List<Message>> liveData = new MutableLiveData<>(new ArrayList<>());
        firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.COLLECTION_MESSAGES)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Failed to listen to messages", error);
                        liveData.setValue(new ArrayList<>());
                        return;
                    }
                    List<Message> messages = new ArrayList<>();
                    if (snapshot != null) {
                        snapshot.getDocuments().forEach(document -> {
                            Message message = document.toObject(Message.class);
                            if (message != null) {
                                message.setId(document.getId());
                                messages.add(message);
                            }
                        });
                    }
                    liveData.setValue(messages);
                });
        return liveData;
    }

    public void markMessagesRead(@NonNull String chatId) {
        String currentUserId = auth.getCurrentUser() == null
                ? null : auth.getCurrentUser().getUid();
        if (currentUserId == null) {
            return;
        }
        firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.COLLECTION_MESSAGES)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(snapshot -> firestore.runBatch(batch -> {
                    snapshot.getDocuments().forEach(document -> {
                        String senderId = document.getString("senderId");
                        if (senderId != null && !currentUserId.equals(senderId)) {
                            batch.update(document.getReference(), "read", true);
                        }
                    });
                }))
                .addOnFailureListener(error -> Log.e(TAG, "Failed to mark messages read", error));
    }
}
