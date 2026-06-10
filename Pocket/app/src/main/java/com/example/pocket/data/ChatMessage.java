package com.example.pocket.data;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class ChatMessage {
    @DocumentId
    private String id;
    private String senderId;
    private String text;
    private Timestamp createdAt;

    public ChatMessage() {
    }

    public ChatMessage(String senderId, String text, Timestamp createdAt) {
        this.senderId = senderId;
        this.text = text;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getText() {
        return text;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }
}
