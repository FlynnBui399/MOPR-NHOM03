package com.example.pocket.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pocket.data.model.Conversation;
import com.example.pocket.data.model.Message;
import com.example.pocket.data.model.User;
import com.example.pocket.data.repository.ChatRepository;
import com.example.pocket.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatListViewModel extends AndroidViewModel {
    private static final String TAG = "ChatListViewModel";

    public final MutableLiveData<List<Conversation>> conversations = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final FirebaseFirestore firestore;

    private ListenerRegistration friendsRegistration;
    private final Map<String, ListenerRegistration> chatListeners = new HashMap<>();
    private final Map<String, Conversation> conversationMap = new HashMap<>();

    public ChatListViewModel(@NonNull Application application) {
        super(application);
        userRepository = UserRepository.getInstance();
        chatRepository = new ChatRepository();
        firestore = FirebaseFirestore.getInstance();
        loadConversations();
    }

    public void loadConversations() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUid == null) {
            errorMessage.setValue("Bạn chưa đăng nhập");
            return;
        }

        isLoading.setValue(true);
        if (friendsRegistration != null) {
            friendsRegistration.remove();
        }

        clearChatListeners();
        conversationMap.clear();
        conversations.setValue(new ArrayList<>());

        MutableLiveData<List<User>> friendsLiveData = new MutableLiveData<>();
        friendsRegistration = userRepository.getFriendList(currentUid, friendsLiveData);

        friendsLiveData.observeForever(friends -> {
            if (friends == null) {
                isLoading.setValue(false);
                return;
            }
            updateChatListeners(currentUid, friends);
        });
    }

    private void updateChatListeners(String currentUid, List<User> friends) {
        List<String> activeChatIds = new ArrayList<>();
        for (User friend : friends) {
            String chatId = chatRepository.getChatId(currentUid, friend.getId());
            activeChatIds.add(chatId);
        }

        List<String> toRemove = new ArrayList<>();
        for (String chatId : chatListeners.keySet()) {
            if (!activeChatIds.contains(chatId)) {
                toRemove.add(chatId);
            }
        }
        for (String chatId : toRemove) {
            ListenerRegistration reg = chatListeners.remove(chatId);
            if (reg != null) {
                reg.remove();
            }
            conversationMap.remove(chatId);
        }

        if (friends.isEmpty()) {
            isLoading.setValue(false);
            postConversations();
            return;
        }

        for (User friend : friends) {
            String chatId = chatRepository.getChatId(currentUid, friend.getId());
            if (!chatListeners.containsKey(chatId)) {
                ListenerRegistration reg = firestore.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(1)
                        .addSnapshotListener((snapshot, error) -> {
                            isLoading.setValue(false);
                            if (error != null) {
                                Log.e(TAG, "Error listening to chat: " + chatId, error);
                                return;
                            }
                            if (snapshot != null && !snapshot.isEmpty()) {
                                Message lastMessage = snapshot.getDocuments().get(0).toObject(Message.class);
                                if (lastMessage != null) {
                                    lastMessage.setId(snapshot.getDocuments().get(0).getId());
                                    conversationMap.put(chatId, new Conversation(friend, lastMessage));
                                }
                            } else {
                                conversationMap.remove(chatId);
                            }
                            postConversations();
                        });
                chatListeners.put(chatId, reg);
            }
        }
    }

    private void postConversations() {
        List<Conversation> list = new ArrayList<>(conversationMap.values());
        Collections.sort(list, (c1, c2) -> {
            if (c1.getLastMessage() == null || c1.getLastMessage().getCreatedAt() == null) {
                return 1;
            }
            if (c2.getLastMessage() == null || c2.getLastMessage().getCreatedAt() == null) {
                return -1;
            }
            return c2.getLastMessage().getCreatedAt().compareTo(c1.getLastMessage().getCreatedAt());
        });
        conversations.setValue(list);
    }

    private void clearChatListeners() {
        for (ListenerRegistration reg : chatListeners.values()) {
            if (reg != null) {
                reg.remove();
            }
        }
        chatListeners.clear();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (friendsRegistration != null) {
            friendsRegistration.remove();
        }
        clearChatListeners();
    }
}
