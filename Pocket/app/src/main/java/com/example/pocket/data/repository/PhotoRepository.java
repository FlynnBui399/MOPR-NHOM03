package com.example.pocket.data.repository;

import android.graphics.Bitmap;
import android.util.Log;

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
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhotoRepository {
    private static final String TAG = "PhotoRepository";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_RECEIVER_IDS = "receiverIds";
    private static final String FIELD_SENDER_ID = "senderId";

    private final FirebaseFirestore firestore;
    private final CloudinaryService cloudinaryService;
    private ListenerRegistration photoFeedRegistration;
    private ListenerRegistration receivedTimelineRegistration;
    private ListenerRegistration sentTimelineRegistration;

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
                                 @NonNull List<String> receiverIds,
                                 @NonNull String caption) {
        return sendPhoto(ImageUtils.compress(bitmap), senderId, senderName, receiverIds, caption);
    }

    @NonNull
    public Task<Photo> sendPhoto(@NonNull byte[] jpegBytes,
                                 @NonNull String senderId,
                                 @NonNull String senderName,
                                 @NonNull List<String> receiverIds,
                                 @NonNull String caption) {
        List<String> receivers = new ArrayList<>(receiverIds);
        if (receivers.isEmpty()) {
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
                    receivers,
                    new HashMap<>(),
                    new ArrayList<>(),
                    Timestamp.now()
            );

            return photoRef.set(photo)
                    .continueWithTask(setTask -> {
                        setTask.getResult();
                        return createFcmTriggers(photo);
                    })
                    .continueWith(triggerTask -> {
                        triggerTask.getResult();
                        return photo;
                    });
        });
    }

    @NonNull
    public LiveData<List<Photo>> getPhotoFeed(@NonNull String userId) {
        MutableLiveData<List<Photo>> liveData = new MutableLiveData<>(new ArrayList<>());
        if (photoFeedRegistration != null) {
            photoFeedRegistration.remove();
        }

        photoFeedRegistration = firestore.collection(Constants.COLLECTION_PHOTOS)
                .whereArrayContains(FIELD_RECEIVER_IDS, userId)
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

    @NonNull
    public LiveData<List<Photo>> getTimelinePhotos(@NonNull String userId) {
        MutableLiveData<List<Photo>> liveData = new MutableLiveData<>(new ArrayList<>());
        clearTimelineListeners();

        Map<String, Photo> receivedPhotos = new LinkedHashMap<>();
        Map<String, Photo> sentPhotos = new LinkedHashMap<>();

        receivedTimelineRegistration = firestore.collection(Constants.COLLECTION_PHOTOS)
                .whereArrayContains(FIELD_RECEIVER_IDS, userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Unable to load received timeline photos", error);
                    } else {
                        replacePhotos(receivedPhotos, snapshot);
                    }
                    liveData.setValue(mergeTimelinePhotos(receivedPhotos, sentPhotos));
                });

        sentTimelineRegistration = firestore.collection(Constants.COLLECTION_PHOTOS)
                .whereEqualTo(FIELD_SENDER_ID, userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Unable to load sent timeline photos", error);
                    } else {
                        replacePhotos(sentPhotos, snapshot);
                    }
                    liveData.setValue(mergeTimelinePhotos(receivedPhotos, sentPhotos));
                });

        return liveData;
    }

    public void clearPhotoFeedListener() {
        if (photoFeedRegistration != null) {
            photoFeedRegistration.remove();
            photoFeedRegistration = null;
        }
    }

    public void clearTimelineListeners() {
        if (receivedTimelineRegistration != null) {
            receivedTimelineRegistration.remove();
            receivedTimelineRegistration = null;
        }
        if (sentTimelineRegistration != null) {
            sentTimelineRegistration.remove();
            sentTimelineRegistration = null;
        }
    }

    private void replacePhotos(@NonNull Map<String, Photo> target,
                               QuerySnapshot snapshot) {
        target.clear();
        if (snapshot == null) {
            return;
        }
        for (Photo photo : snapshot.toObjects(Photo.class)) {
            target.put(photoKey(photo), photo);
        }
    }

    @NonNull
    private List<Photo> mergeTimelinePhotos(@NonNull Map<String, Photo> receivedPhotos,
                                            @NonNull Map<String, Photo> sentPhotos) {
        Map<String, Photo> merged = new LinkedHashMap<>(receivedPhotos);
        merged.putAll(sentPhotos);
        List<Photo> timeline = new ArrayList<>(merged.values());
        sortNewestFirst(timeline);
        return timeline;
    }

    @NonNull
    private String photoKey(@NonNull Photo photo) {
        if (photo.getId() != null && !photo.getId().trim().isEmpty()) {
            return photo.getId();
        }
        return String.valueOf(photo.getImageUrl()) + ':' + String.valueOf(photo.getCreatedAt());
    }

    private void sortNewestFirst(@NonNull List<Photo> photos) {
        Collections.sort(photos, (first, second) -> {
            if (first.getCreatedAt() == null && second.getCreatedAt() == null) return 0;
            if (first.getCreatedAt() == null) return 1;
            if (second.getCreatedAt() == null) return -1;
            return second.getCreatedAt().compareTo(first.getCreatedAt());
        });
    }

    @NonNull
    private Task<Void> createFcmTriggers(@NonNull Photo photo) {
        List<Task<Void>> tasks = new ArrayList<>();
        for (String recipientId : photo.getReceiverIds()) {
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

    @NonNull
    public Task<Void> reactToPhoto(@NonNull String photoId,
                                   @NonNull String userId,
                                   @NonNull String reaction) {
        DocumentReference photoRef = firestore.collection(Constants.COLLECTION_PHOTOS).document(photoId);
        return firestore.runTransaction(transaction -> {
            Photo photo = transaction.get(photoRef).toObject(Photo.class);
            if (photo != null) {
                Map<String, String> reactions = photo.getReactions();
                reactions.put(userId, reaction);
                transaction.update(photoRef, "reactions", reactions);
            }
            return null;
        });
    }

    @NonNull
    public Task<Void> removeReaction(@NonNull String photoId,
                                     @NonNull String userId) {
        DocumentReference photoRef = firestore.collection(Constants.COLLECTION_PHOTOS).document(photoId);
        return firestore.runTransaction(transaction -> {
            Photo photo = transaction.get(photoRef).toObject(Photo.class);
            if (photo != null) {
                Map<String, String> reactions = photo.getReactions();
                reactions.remove(userId);
                transaction.update(photoRef, "reactions", reactions);
            }
            return null;
        });
    }
}
