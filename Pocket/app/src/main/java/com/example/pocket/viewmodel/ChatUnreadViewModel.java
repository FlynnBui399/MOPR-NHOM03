package com.example.pocket.viewmodel;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocket.data.model.User;
import com.example.pocket.data.repository.ChatRepository;
import com.example.pocket.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatUnreadViewModel extends ViewModel {
    private static final String TAG = "ChatUnreadViewModel";

    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final ChatRepository chatRepository = new ChatRepository();
    private final Map<String, ListenerRegistration> messageListeners = new HashMap<>();
    private final Map<String, Integer> unreadByChat = new HashMap<>();
    private ListenerRegistration userListener;

    public ChatUnreadViewModel() {
        listenForFriends();
    }

    @NonNull
    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }

    private void listenForFriends() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) {
            return;
        }
        userListener = firestore.collection(Constants.COLLECTION_USERS)
                .document(currentUserId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Failed to listen for friends", error);
                        return;
                    }
                    User user = snapshot == null ? null : snapshot.toObject(User.class);
                    updateChatListeners(currentUserId,
                            user == null ? new ArrayList<>() : user.getFriendIds());
                });
    }

    private void updateChatListeners(@NonNull String currentUserId,
                                     @NonNull List<String> friendIds) {
        List<String> activeChatIds = new ArrayList<>();
        for (String friendId : friendIds) {
            activeChatIds.add(chatRepository.getChatId(currentUserId, friendId));
        }

        List<String> stale = new ArrayList<>();
        for (String chatId : messageListeners.keySet()) {
            if (!activeChatIds.contains(chatId)) {
                stale.add(chatId);
            }
        }
        for (String chatId : stale) {
            ListenerRegistration registration = messageListeners.remove(chatId);
            if (registration != null) {
                registration.remove();
            }
            unreadByChat.remove(chatId);
        }

        for (String chatId : activeChatIds) {
            if (messageListeners.containsKey(chatId)) {
                continue;
            }
            ListenerRegistration registration = firestore.collection(Constants.COLLECTION_CHATS)
                    .document(chatId)
                    .collection(Constants.COLLECTION_MESSAGES)
                    .whereEqualTo("read", false)
                    .addSnapshotListener((snapshot, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Failed to count unread messages", error);
                            return;
                        }
                        int count = 0;
                        if (snapshot != null) {
                            count = (int) snapshot.getDocuments().stream()
                                    .filter(document -> !currentUserId.equals(
                                            document.getString("senderId")))
                                    .count();
                        }
                        unreadByChat.put(chatId, count);
                        postTotal();
                    });
            messageListeners.put(chatId, registration);
        }
        postTotal();
    }

    private void postTotal() {
        int total = 0;
        for (Integer count : unreadByChat.values()) {
            total += count == null ? 0 : count;
        }
        unreadCount.setValue(total);
    }

    @Override
    protected void onCleared() {
        if (userListener != null) {
            userListener.remove();
        }
        for (ListenerRegistration registration : messageListeners.values()) {
            registration.remove();
        }
        messageListeners.clear();
        super.onCleared();
    }
}
