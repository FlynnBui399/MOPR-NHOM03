package com.example.pocket.data.model;

import com.google.firebase.Timestamp;

public class Conversation {
    private final User friend;
    private final Message lastMessage;

    public Conversation(User friend, Message lastMessage) {
        this.friend = friend;
        this.lastMessage = lastMessage;
    }

    public User getFriend() {
        return friend;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public Timestamp getLatestTimestamp() {
        return lastMessage != null ? lastMessage.getCreatedAt() : null;
    }
}
