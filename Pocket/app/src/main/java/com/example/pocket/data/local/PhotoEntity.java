package com.example.pocket.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "photos")
public class PhotoEntity {
    @PrimaryKey
    @NonNull
    private String photoId;
    private String senderId;
    private String senderName;
    private String imageUrl;
    private String thumbnailUrl;
    private String caption;
    private long createdAt;

    public PhotoEntity(@NonNull String photoId, String senderId, String senderName,
                       String imageUrl, String thumbnailUrl, String caption, long createdAt) {
        this.photoId = photoId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.caption = caption;
        this.createdAt = createdAt;
    }

    @NonNull
    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(@NonNull String photoId) {
        this.photoId = photoId;
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

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
