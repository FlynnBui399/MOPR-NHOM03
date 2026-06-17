package com.example.pocket.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Photo {
    @DocumentId
    private String id;
    private String senderId;
    private String senderName;
    private String imageUrl;
    private String thumbnailUrl;
    private String cloudinaryPublicId;
    private String caption;
    private List<String> receiverIds;
    private List<String> recipients;
    private Map<String, String> reactions;
    private List<String> seenBy;
    private Timestamp createdAt;
    private String type;
    private String videoUrl;

    public Photo() {
        receiverIds = new ArrayList<>();
        recipients = new ArrayList<>();
        reactions = new HashMap<>();
        seenBy = new ArrayList<>();
    }

    public Photo(String id, String senderId, String senderName, String imageUrl, String thumbnailUrl,
                 String cloudinaryPublicId, String caption, List<String> receiverIds,
                 Map<String, String> reactions, List<String> seenBy, Timestamp createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.cloudinaryPublicId = cloudinaryPublicId;
        this.caption = caption;
        this.receiverIds = receiverIds == null ? new ArrayList<>() : new ArrayList<>(receiverIds);
        this.recipients = receiverIds == null ? new ArrayList<>() : new ArrayList<>(receiverIds);
        this.reactions = reactions == null ? new HashMap<>() : new HashMap<>(reactions);
        this.seenBy = seenBy == null ? new ArrayList<>() : new ArrayList<>(seenBy);
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

    public List<String> getReceiverIds() {
        return receiverIds;
    }

    public void setReceiverIds(List<String> receiverIds) {
        this.receiverIds = receiverIds == null ? new ArrayList<>() : new ArrayList<>(receiverIds);
    }

    public Map<String, String> getReactions() {
        return reactions;
    }

    public void setReactions(Map<String, String> reactions) {
        this.reactions = reactions == null ? new HashMap<>() : new HashMap<>(reactions);
    }

    public List<String> getSeenBy() {
        return seenBy;
    }

    public void setSeenBy(List<String> seenBy) {
        this.seenBy = seenBy == null ? new ArrayList<>() : new ArrayList<>(seenBy);
    }

    @Exclude
    public List<String> getRecipientIds() {
        return getReceiverIds();
    }

    @Exclude
    public void setRecipientIds(List<String> recipientIds) {
        setReceiverIds(recipientIds);
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients == null ? new ArrayList<>() : new ArrayList<>(recipients);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
