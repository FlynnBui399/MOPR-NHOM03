package com.example.pocket.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.example.pocket.data.model.User;
import com.example.pocket.utils.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static UserRepository instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    private UserRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public void getUserByPhone(@NonNull String phone, @NonNull Callback<User> callback) {
        firestore.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("phoneNumber", normalizePhoneNumber(phone))
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) {
                        callback.onSuccess(null);
                        return;
                    }
                    User user = snapshot.getDocuments().get(0).toObject(User.class);
                    if (user != null) {
                        user.setId(snapshot.getDocuments().get(0).getId());
                    }
                    callback.onSuccess(user);
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to get user by phone", error);
                    callback.onError(error);
                });
    }

    private String normalizePhoneNumber(String raw) {
        String phone = raw.trim().replaceAll("[\\s\\-()]", "");
        if (phone.startsWith("0") && phone.length() == 10) {
            return "+84" + phone.substring(1);
        }
        if (phone.startsWith("84") && phone.length() == 11) {
            return "+" + phone;
        }
        return phone;
    }

    public void getUserById(@NonNull String uid, @NonNull Callback<User> callback) {
        firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        user.setId(snapshot.getId());
                    }
                    callback.onSuccess(user);
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to get user by id", error);
                    callback.onError(error);
                });
    }

    public void sendFriendRequest(@NonNull String toUid, @NonNull Callback<Void> callback) {
        String fromUid = currentUid();
        if (fromUid == null) {
            callback.onError(new IllegalStateException("User is not signed in"));
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("fromUid", fromUid);
        request.put("toUid", toUid);
        request.put("status", "pending");
        request.put("createdAt", Timestamp.now());

        firestore.collection("friendRequests")
                .document(toUid)
                .collection("incoming")
                .document(fromUid)
                .set(request)
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to send friend request", error);
                    callback.onError(error);
                });
    }

    public void acceptFriendRequest(@NonNull String fromUid, @NonNull Callback<Void> callback) {
        String currentUid = currentUid();
        if (currentUid == null) {
            callback.onError(new IllegalStateException("User is not signed in"));
            return;
        }

        firestore.runBatch(batch -> {
                    batch.update(firestore.collection(Constants.COLLECTION_USERS).document(currentUid),
                            "friends", FieldValue.arrayUnion(fromUid),
                            "friendIds", FieldValue.arrayUnion(fromUid));
                    batch.update(firestore.collection(Constants.COLLECTION_USERS).document(fromUid),
                            "friends", FieldValue.arrayUnion(currentUid),
                            "friendIds", FieldValue.arrayUnion(currentUid));
                    batch.delete(firestore.collection("friendRequests")
                            .document(currentUid)
                            .collection("incoming")
                            .document(fromUid));
                })
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to accept friend request", error);
                    callback.onError(error);
                });
    }

    public ListenerRegistration getFriendList(@NonNull String uid,
                                              @NonNull MutableLiveData<List<User>> liveData) {
        return firestore.collection(Constants.COLLECTION_USERS)
                .document(uid)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Failed to listen to friend list", error);
                        liveData.setValue(new ArrayList<>());
                        return;
                    }
                    User user = snapshot == null ? null : snapshot.toObject(User.class);
                    List<String> friendIds = user == null ? new ArrayList<>() : user.getFriends();
                    fetchUsers(friendIds, liveData);
                });
    }

    public ListenerRegistration getPendingRequests(@NonNull MutableLiveData<List<User>> liveData) {
        String uid = currentUid();
        if (uid == null) {
            liveData.setValue(new ArrayList<>());
            return null;
        }

        return firestore.collection("friendRequests")
                .document(uid)
                .collection("incoming")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Failed to listen to pending requests", error);
                        liveData.setValue(new ArrayList<>());
                        return;
                    }
                    List<String> requestIds = new ArrayList<>();
                    if (snapshot != null) {
                        snapshot.getDocuments().forEach(document -> requestIds.add(document.getId()));
                    }
                    fetchUsers(requestIds, liveData);
                });
    }

    public void declineFriendRequest(@NonNull String fromUid, @NonNull Callback<Void> callback) {
        String currentUid = currentUid();
        if (currentUid == null) {
            callback.onError(new IllegalStateException("User is not signed in"));
            return;
        }

        firestore.collection("friendRequests")
                .document(currentUid)
                .collection("incoming")
                .document(fromUid)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to decline friend request", error);
                    callback.onError(error);
                });
    }

    public void removeFriend(@NonNull String friendUid, @NonNull Callback<Void> callback) {
        String currentUid = currentUid();
        if (currentUid == null) {
            callback.onError(new IllegalStateException("User is not signed in"));
            return;
        }

        firestore.runBatch(batch -> {
                    batch.update(firestore.collection(Constants.COLLECTION_USERS).document(currentUid),
                            "friends", FieldValue.arrayRemove(friendUid),
                            "friendIds", FieldValue.arrayRemove(friendUid));
                    batch.update(firestore.collection(Constants.COLLECTION_USERS).document(friendUid),
                            "friends", FieldValue.arrayRemove(currentUid),
                            "friendIds", FieldValue.arrayRemove(currentUid));
                })
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to remove friend", error);
                    callback.onError(error);
                });
    }

    private void fetchUsers(@NonNull List<String> userIds, @NonNull MutableLiveData<List<User>> liveData) {
        List<User> users = new ArrayList<>();
        if (userIds.isEmpty()) {
            liveData.setValue(users);
            return;
        }

        final int[] remaining = {userIds.size()};
        for (String userId : userIds) {
            getUserById(userId, new Callback<User>() {
                @Override
                public void onSuccess(User result) {
                    if (result != null) {
                        users.add(result);
                    }
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        liveData.setValue(users);
                    }
                }

                @Override
                public void onError(@NonNull Exception error) {
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        liveData.setValue(users);
                    }
                }
            });
        }
    }

    private String currentUid() {
        return auth.getCurrentUser() == null ? null : auth.getCurrentUser().getUid();
    }

    public interface Callback<T> {
        void onSuccess(T result);

        void onError(@NonNull Exception error);
    }
}
