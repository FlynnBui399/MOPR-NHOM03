package com.example.pocket.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class Message {
    @DocumentId
    private String id;
    private String chatId;
    private String senderId;
    private String receiverId;
    private String text;
    private String content;
    private String photoId;
    private String photoUrl;
    private String type;
    private Timestamp createdAt;
    private boolean read;
    private String quotedPhotoUrl;
    private String quotedPhotoId;

    public Message() {
    }

    public Message(String id, String chatId, String senderId, String receiverId, String text,
                   String photoId, String photoUrl, String type, Timestamp createdAt, boolean read) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.photoId = photoId;
        this.photoUrl = photoUrl;
        this.type = type;
        this.createdAt = createdAt;
        this.read = read;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getText() {
        if ((text == null || text.isEmpty()) && content != null) {
            return content;
        }
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getContent() {
        if ((content == null || content.isEmpty()) && text != null) {
            return text;
        }
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getQuotedPhotoUrl() {
        return quotedPhotoUrl;
    }

    public void setQuotedPhotoUrl(String quotedPhotoUrl) {
        this.quotedPhotoUrl = quotedPhotoUrl;
    }

    public String getQuotedPhotoId() {
        return quotedPhotoId;
    }

    public void setQuotedPhotoId(String quotedPhotoId) {
        this.quotedPhotoId = quotedPhotoId;
    }
}
