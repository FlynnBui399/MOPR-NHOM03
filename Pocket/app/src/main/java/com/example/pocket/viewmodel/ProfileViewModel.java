package com.example.pocket.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.pocket.data.model.User;
import com.example.pocket.utils.Constants;
import com.example.pocket.utils.SharedPrefManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileViewModel extends AndroidViewModel {
    private static final String TAG = "ProfileViewModel";

    public final MutableLiveData<User> currentUser = new MutableLiveData<>();
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final FirebaseFirestore firestore;
    private final SharedPrefManager sharedPrefManager;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        firestore = FirebaseFirestore.getInstance();
        sharedPrefManager = SharedPrefManager.getInstance(application);
        loadCurrentUser();
    }

    public void loadCurrentUser() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            errorMessage.setValue("Bạn chưa đăng nhập");
            return;
        }

        firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        user.setId(snapshot.getId());
                        currentUser.setValue(user);
                        sharedPrefManager.saveUser(user);
                    }
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to load current user", error);
                    errorMessage.setValue(error.getMessage());
                });
    }

    public void updateAvatar(@NonNull String newAvatarUrl) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            errorMessage.setValue("Bạn chưa đăng nhập");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("avatarUrl", newAvatarUrl);
        firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.getUid())
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    sharedPrefManager.updateAvatarUrl(newAvatarUrl);
                    User user = currentUser.getValue();
                    if (user != null) {
                        user.setAvatarUrl(newAvatarUrl);
                        currentUser.setValue(user);
                    } else {
                        loadCurrentUser();
                    }
                })
                .addOnFailureListener(error -> {
                    Log.e(TAG, "Failed to update avatar", error);
                    errorMessage.setValue(error.getMessage());
                });
    }
}
