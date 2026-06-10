package com.example.pocket.data.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.List;

public class User {
    @DocumentId
    private String id;
    private String displayName;
    private String username;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private String fcmToken;
    private List<String> friendIds;
    private List<String> friends;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public User() {
        friendIds = new ArrayList<>();
        friends = new ArrayList<>();
    }

    public User(String id, String displayName, String username, String email, String avatarUrl,
                String fcmToken, List<String> friendIds, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.displayName = displayName;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.fcmToken = fcmToken;
        this.friendIds = friendIds == null ? new ArrayList<>() : new ArrayList<>(friendIds);
        this.friends = friendIds == null ? new ArrayList<>() : new ArrayList<>(friendIds);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public List<String> getFriendIds() {
        if ((friendIds == null || friendIds.isEmpty()) && friends != null) {
            return friends;
        }
        return friendIds;
    }

    public void setFriendIds(List<String> friendIds) {
        this.friendIds = friendIds == null ? new ArrayList<>() : new ArrayList<>(friendIds);
    }

    public List<String> getFriends() {
        if ((friends == null || friends.isEmpty()) && friendIds != null) {
            return friendIds;
        }
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends == null ? new ArrayList<>() : new ArrayList<>(friends);
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
