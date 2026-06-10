package com.example.pocket.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocket.data.model.Photo;
import com.example.pocket.data.remote.GeminiService;
import com.example.pocket.data.repository.PhotoRepository;
import com.example.pocket.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

public class CameraViewModel extends ViewModel {
    private final PhotoRepository photoRepository;
    private final GeminiService geminiService;
    private final MutableLiveData<UploadStatus> uploadStatus = new MutableLiveData<>(UploadStatus.idle());
    private final MutableLiveData<List<String>> suggestedCaptions = new MutableLiveData<>(new ArrayList<>());

    public CameraViewModel() {
        this(new PhotoRepository(), new GeminiService());
    }

    CameraViewModel(@NonNull PhotoRepository photoRepository, @NonNull GeminiService geminiService) {
        this.photoRepository = photoRepository;
        this.geminiService = geminiService;
    }

    @NonNull
    public LiveData<UploadStatus> getUploadStatus() {
        return uploadStatus;
    }

    @NonNull
    public LiveData<List<String>> getSuggestedCaptions() {
        return suggestedCaptions;
    }

    public void sendPhoto(@NonNull byte[] jpegBytes,
                          @NonNull String senderId,
                          @NonNull String senderName,
                          @NonNull List<String> recipientIds,
                          @NonNull String caption) {
        uploadStatus.setValue(UploadStatus.loading());
        photoRepository.sendPhoto(jpegBytes, senderId, senderName, recipientIds, caption)
                .addOnSuccessListener(photo -> uploadStatus.setValue(UploadStatus.success(photo)))
                .addOnFailureListener(error -> uploadStatus.setValue(UploadStatus.error(error.getMessage())));
    }

    public void generateCaption(@NonNull byte[] jpegBytes) {
        suggestedCaptions.setValue(new ArrayList<>());
        geminiService.generateCaptions(ImageUtils.toBase64(jpegBytes))
                .addOnSuccessListener(suggestedCaptions::setValue)
                .addOnFailureListener(error -> {
                    List<String> fallback = new ArrayList<>();
                    fallback.add("Mot khoanh khac that dep");
                    fallback.add("Gui ban chut niem vui hom nay");
                    fallback.add("Luu lai ngay nay nhe");
                    suggestedCaptions.setValue(fallback);
                });
    }

    public static class UploadStatus {
        public enum State {
            IDLE,
            LOADING,
            SUCCESS,
            ERROR
        }

        private final State state;
        private final String message;
        private final Photo photo;

        private UploadStatus(State state, String message, Photo photo) {
            this.state = state;
            this.message = message;
            this.photo = photo;
        }

        public static UploadStatus idle() {
            return new UploadStatus(State.IDLE, null, null);
        }

        public static UploadStatus loading() {
            return new UploadStatus(State.LOADING, null, null);
        }

        public static UploadStatus success(Photo photo) {
            return new UploadStatus(State.SUCCESS, null, photo);
        }

        public static UploadStatus error(String message) {
            return new UploadStatus(State.ERROR, message == null ? "Unable to send photo" : message, null);
        }

        public State getState() {
            return state;
        }

        public String getMessage() {
            return message;
        }

        public Photo getPhoto() {
            return photo;
        }
    }
}
