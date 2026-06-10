package com.example.pocket.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.List;

public class Photo {
    @DocumentId
    private String id;
    private String senderId;
    private String senderName;
    private String imageUrl;
    private String thumbnailUrl;
    private String cloudinaryPublicId;
    private String caption;
    private List<String> recipientIds;
    private Timestamp createdAt;

    public Photo() {
        recipientIds = new ArrayList<>();
    }

    public Photo(String id, String senderId, String senderName, String imageUrl, String thumbnailUrl,
                 String cloudinaryPublicId, String caption, List<String> recipientIds,
                 Timestamp createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.cloudinaryPublicId = cloudinaryPublicId;
        this.caption = caption;
        this.recipientIds = recipientIds == null ? new ArrayList<>() : new ArrayList<>(recipientIds);
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getCloudinaryPublicId() {
        return cloudinaryPublicId;
    }

    public void setCloudinaryPublicId(String cloudinaryPublicId) {
        this.cloudinaryPublicId = cloudinaryPublicId;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public List<String> getRecipientIds() {
        return recipientIds;
    }

    public void setRecipientIds(List<String> recipientIds) {
        this.recipientIds = recipientIds == null ? new ArrayList<>() : new ArrayList<>(recipientIds);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
