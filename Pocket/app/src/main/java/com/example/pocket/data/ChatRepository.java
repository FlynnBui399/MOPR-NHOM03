package com.example.pocket.data;

import androidx.annotation.NonNull;

import com.example.pocket.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_SENDER_ID = "senderId";
    private static final String FIELD_TEXT = "text";

    private final FirebaseFirestore firestore;

    public ChatRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public ChatRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    public ListenerRegistration listenToMessages(
            @NonNull String chatId,
            @NonNull MessagesListener listener
    ) {
        return firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.COLLECTION_MESSAGES)
                .orderBy(FIELD_CREATED_AT, Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    List<ChatMessage> messages = new ArrayList<>();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot document : snapshot) {
                            ChatMessage message = document.toObject(ChatMessage.class);
                            messages.add(message);
                        }
                    }
                    listener.onMessagesChanged(messages);
                });
    }

    public Task<Void> sendMessage(
            @NonNull String chatId,
            @NonNull String senderId,
            @NonNull String text
    ) {
        Map<String, Object> message = new HashMap<>();
        message.put(FIELD_SENDER_ID, senderId);
        message.put(FIELD_TEXT, text);
        message.put(FIELD_CREATED_AT, Timestamp.now());

        return firestore.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .collection(Constants.COLLECTION_MESSAGES)
                .document()
                .set(message);
    }

    public interface MessagesListener {
        void onMessagesChanged(@NonNull List<ChatMessage> messages);

        void onError(@NonNull Exception error);
    }
}
