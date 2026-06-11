package com.example.pocket.data.repository;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pocket.data.model.Photo;
import com.example.pocket.data.remote.CloudinaryService;
import com.example.pocket.utils.Constants;
import com.example.pocket.utils.ImageUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhotoRepository {
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_RECIPIENT_IDS = "recipientIds";

    private final FirebaseFirestore firestore;
    private final CloudinaryService cloudinaryService;
    private ListenerRegistration photoFeedRegistration;

    public PhotoRepository() {
        this(FirebaseFirestore.getInstance(), new CloudinaryService());
    }

    public PhotoRepository(@NonNull FirebaseFirestore firestore,
                           @NonNull CloudinaryService cloudinaryService) {
        this.firestore = firestore;
        this.cloudinaryService = cloudinaryService;
    }

    @NonNull
    public Task<Photo> sendPhoto(@NonNull Bitmap bitmap,
                                 @NonNull String senderId,
                                 @NonNull String senderName,
                                 @NonNull List<String> recipientIds,
                                 @NonNull String caption) {
        return sendPhoto(ImageUtils.compress(bitmap), senderId, senderName, recipientIds, caption);
    }

    @NonNull
    public Task<Photo> sendPhoto(@NonNull byte[] jpegBytes,
                                 @NonNull String senderId,
                                 @NonNull String senderName,
                                 @NonNull List<String> recipientIds,
                                 @NonNull String caption) {
        List<String> recipients = new ArrayList<>(recipientIds);
        if (recipients.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException());
        }

        return cloudinaryService.uploadUnsigned(jpegBytes).continueWithTask(uploadTask -> {
            CloudinaryService.UploadResult upload = uploadTask.getResult();
            DocumentReference photoRef = firestore.collection(Constants.COLLECTION_PHOTOS).document();
            Photo photo = new Photo(
                    photoRef.getId(),
                    senderId,
                    senderName,
                    upload.getSecureUrl(),
                    upload.getThumbnailUrl(),
                    upload.getPublicId(),
                    caption,
                    recipients,
                    Timestamp.now()
            );

            return photoRef.set(photo)
                    .continueWithTask(setTask -> createFcmTriggers(photo))
                    .continueWith(triggerTask -> photo);
        });
    }

    @NonNull
    public LiveData<List<Photo>> getPhotoFeed(@NonNull String userId) {
        MutableLiveData<List<Photo>> liveData = new MutableLiveData<>(new ArrayList<>());
        if (photoFeedRegistration != null) {
            photoFeedRegistration.remove();
        }

        photoFeedRegistration = firestore.collection(Constants.COLLECTION_PHOTOS)
                .whereArrayContains(FIELD_RECIPIENT_IDS, userId)
                .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        liveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<Photo> photos = new ArrayList<>();
                    if (snapshot != null) {
                        photos.addAll(snapshot.toObjects(Photo.class));
                    }
                    liveData.setValue(photos);
                });
        return liveData;
    }

    public void clearPhotoFeedListener() {
        if (photoFeedRegistration != null) {
            photoFeedRegistration.remove();
            photoFeedRegistration = null;
        }
    }

    @NonNull
    private Task<Void> createFcmTriggers(@NonNull Photo photo) {
        List<Task<Void>> tasks = new ArrayList<>();
        for (String recipientId : photo.getRecipientIds()) {
            Map<String, Object> trigger = new HashMap<>();
            trigger.put("type", "photo_received");
            trigger.put("photoId", photo.getId());
            trigger.put("senderId", photo.getSenderId());
            trigger.put("senderName", photo.getSenderName());
            trigger.put("recipientId", recipientId);
            trigger.put("imageUrl", photo.getThumbnailUrl());
            trigger.put("caption", photo.getCaption());
            trigger.put("createdAt", Timestamp.now());
            trigger.put("processed", false);

            tasks.add(firestore.collection(Constants.COLLECTION_FCM_TRIGGERS)
                    .document()
                    .set(trigger));
        }
        return Tasks.whenAll(tasks);
    }
}
