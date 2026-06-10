package com.example.pocket.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.pocket.data.model.User;
import com.example.pocket.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class FriendViewModel extends AndroidViewModel {
    public final MutableLiveData<List<User>> friends = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<List<User>> pendingRequests = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final UserRepository repository;
    private ListenerRegistration friendsRegistration;
    private ListenerRegistration requestsRegistration;

    public FriendViewModel(@NonNull Application application) {
        super(application);
        repository = UserRepository.getInstance();
        load();
    }

    public void load() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            friends.setValue(new ArrayList<>());
            pendingRequests.setValue(new ArrayList<>());
            return;
        }
        if (friendsRegistration != null) {
            friendsRegistration.remove();
        }
        if (requestsRegistration != null) {
            requestsRegistration.remove();
        }
        friendsRegistration = repository.getFriendList(uid, friends);
        requestsRegistration = repository.getPendingRequests(pendingRequests);
    }

    public void acceptRequest(@NonNull String fromUid) {
        repository.acceptFriendRequest(fromUid, callback());
    }

    public void declineRequest(@NonNull String fromUid) {
        repository.declineFriendRequest(fromUid, callback());
    }

    public void removeFriend(@NonNull String friendUid) {
        repository.removeFriend(friendUid, callback());
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (friendsRegistration != null) {
            friendsRegistration.remove();
        }
        if (requestsRegistration != null) {
            requestsRegistration.remove();
        }
    }

    private UserRepository.Callback<Void> callback() {
        return new UserRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onError(@NonNull Exception error) {
                errorMessage.setValue(error.getMessage());
            }
        };
    }
}
