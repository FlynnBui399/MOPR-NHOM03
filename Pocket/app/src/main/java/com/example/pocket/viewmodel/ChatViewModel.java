package com.example.pocket.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pocket.R;
import com.example.pocket.data.model.Message;
import com.example.pocket.data.repository.ChatRepository;
import com.example.pocket.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ChatViewModel extends AndroidViewModel {
    public LiveData<List<Message>> messages = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final ChatRepository repository;
    private String chatId;

    public ChatViewModel(@NonNull Application application) {
        super(application);
        repository = new ChatRepository();
    }

    public void initialize(@NonNull String friendUid) {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() == null
                ? null
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUid == null) {
            errorMessage.setValue(getApplication().getString(R.string.profile_not_signed_in));
            return;
        }
        chatId = repository.getChatId(currentUid, friendUid);
        messages = repository.getMessages(chatId);
    }

    public void sendMessage(@NonNull String content) {
        send(content, "text", null);
    }

    public void sendMessage(@NonNull String content, @Nullable SendCallback callback) {
        send(content, "text", callback);
    }

    public void sendEmoji(@NonNull String emoji) {
        send(emoji, "emoji", null);
    }

    public void sendEmoji(@NonNull String emoji, @Nullable SendCallback callback) {
        send(emoji, "emoji", callback);
    }

    private void send(@NonNull String content, @NonNull String type, @Nullable SendCallback callback) {
        if (chatId == null || chatId.trim().isEmpty() || content.trim().isEmpty()) {
            return;
        }
        repository.sendMessage(chatId, content.trim(), type, new UserRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(@NonNull Exception error) {
                errorMessage.setValue(error.getMessage());
            }
        });
    }

    public interface SendCallback {
        void onSuccess();
    }
}
