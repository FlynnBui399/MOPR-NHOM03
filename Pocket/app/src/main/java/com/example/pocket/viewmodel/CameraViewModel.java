package com.example.pocket.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pocket.data.model.Photo;
import com.example.pocket.data.remote.GeminiService;
import com.example.pocket.data.repository.PhotoRepository;
import com.example.pocket.utils.ImageUtils;
import com.example.pocket.utils.CaptionCacheManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraViewModel extends AndroidViewModel {
    private static final String TAG = "CameraViewModel";
    private static final String TAG_AI = "PocketAI";

    private final PhotoRepository photoRepository;
    private final GeminiService geminiService;
    private final CaptionCacheManager captionCache;
    private final ExecutorService captionExecutor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<UploadStatus> uploadStatus = new MutableLiveData<>(UploadStatus.idle());
    private final MutableLiveData<CaptionSuggestion> captionSuggestion = new MutableLiveData<>();

    public CameraViewModel(@NonNull Application application) {
        this(application, new PhotoRepository(), new GeminiService());
    }

    CameraViewModel(@NonNull Application application,
                    @NonNull PhotoRepository photoRepository,
                    @NonNull GeminiService geminiService) {
        super(application);
        this.photoRepository = photoRepository;
        this.geminiService = geminiService;
        this.captionCache = new CaptionCacheManager(application);
    }

    @NonNull
    public LiveData<UploadStatus> getUploadStatus() {
        return uploadStatus;
    }

    @NonNull
    public LiveData<CaptionSuggestion> getCaptionSuggestion() {
        return captionSuggestion;
    }

    public void sendPhoto(@NonNull byte[] jpegBytes,
                          @NonNull String senderId,
                          @NonNull String senderName,
                          @NonNull List<String> receiverIds,
                          @NonNull String caption) {
        uploadStatus.setValue(UploadStatus.loading());
        photoRepository.sendPhoto(jpegBytes, senderId, senderName, receiverIds, caption)
                .addOnSuccessListener(photo -> uploadStatus.setValue(UploadStatus.success(photo)))
                .addOnFailureListener(error -> uploadStatus.setValue(UploadStatus.error(error.getMessage())));
    }

    public void generateCaption(@NonNull byte[] jpegBytes) {
        Log.d(TAG_AI, "CameraViewModel.generateCaption triggered: sourceBytes="
                + jpegBytes.length + ", mimeType=image/jpeg");
        byte[] sourceBytes = jpegBytes.clone();
        captionExecutor.execute(() -> {
            final byte[] optimizedBytes;
            final String imageHash;
            try {
                optimizedBytes = ImageUtils.optimizeForCaption(sourceBytes);
                imageHash = ImageUtils.sha256(optimizedBytes);
                Log.d(TAG_AI, "Caption image prepared: optimizedBytes="
                        + optimizedBytes.length + ", hashSource=optimizedJPEG_SHA256"
                        + ", hashPrefix=" + imageHash.substring(0, Math.min(12, imageHash.length())));
            } catch (RuntimeException error) {
                Log.e(TAG_AI, "Caption image preparation failed: type="
                        + error.getClass().getName() + ", message=" + error.getMessage()
                        + ", rootCause=" + rootCauseMessage(error)
                        + "; fallbackPath=image_preparation", error);
                Log.w(TAG, "Unable to prepare image for Gemini caption", error);
                publishFallback(error.getMessage());
                return;
            }

            List<String> cached = captionCache.get(imageHash);
            if (!cached.isEmpty()) {
                Log.d(TAG_AI, "Using caption CACHE result: captionCount=" + cached.size()
                        + "; Gemini service will not be called");
                captionSuggestion.postValue(new CaptionSuggestion(
                        ensureThreeCaptions(cached), CaptionSource.CACHE));
                return;
            }

            Log.d(TAG_AI, "Caption cache miss; calling GeminiService with optimizedBytes="
                    + optimizedBytes.length);
            geminiService.generateCaptions(optimizedBytes)
                    .addOnSuccessListener(captions -> {
                        Log.d(TAG_AI, "GeminiService success: rawCaptionCount="
                                + (captions == null ? 0 : captions.size()));
                        List<String> completed = ensureThreeCaptions(captions);

                        // Cache only Gemini's cleaned captions. Fallback padding remains transient.
                        if (captions != null && !captions.isEmpty()) {
                            captionCache.put(imageHash, captions);
                            Log.d(TAG_AI, "Caption cache SAVE requested: aiCaptionCount="
                                    + captions.size());
                        }

                        captionSuggestion.postValue(new CaptionSuggestion(
                                completed, CaptionSource.AI));
                        Log.d(TAG_AI, "Publishing AI caption result: finalCaptionCount="
                                + completed.size());
                    })
                    .addOnFailureListener(error -> {
                        Log.e(TAG_AI, "GeminiService failure: type="
                                + error.getClass().getName() + ", message=" + error.getMessage()
                                + ", rootCause=" + rootCauseMessage(error)
                                + "; fallbackPath=service_failure", error);
                        Log.w(TAG, "Gemini caption unavailable; error: " + error.getMessage(), error);
                        publishFallback(error.getMessage());
                    });
        });
    }

    private void publishFallback(String errorMessage) {
        List<String> fallback = GeminiService.fallbackCaptionSet();
        Log.w(TAG_AI, "Publishing FALLBACK captions: reason="
                + (errorMessage == null ? "unknown" : errorMessage)
                + ", captionCount=" + fallback.size());
        captionSuggestion.postValue(new CaptionSuggestion(
                fallback, CaptionSource.FALLBACK, errorMessage));
    }

    @NonNull
    private static String rootCauseMessage(@NonNull Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getName() + ": " + root.getMessage();
    }

    @NonNull
    private List<String> ensureThreeCaptions(List<String> captions) {
        List<String> result = new ArrayList<>();
        if (captions != null) {
            for (String caption : captions) {
                if (caption != null && !caption.trim().isEmpty()
                        && !result.contains(caption.trim())) {
                    result.add(caption.trim());
                }
                if (result.size() == 3) {
                    return result;
                }
            }
        }
        for (String fallback : fallbackCaptions()) {
            if (!result.contains(fallback)) {
                result.add(fallback);
            }
            if (result.size() == 3) {
                break;
            }
        }
        return result;
    }

    @NonNull
    private List<String> fallbackCaptions() {
        List<String> fallback = new ArrayList<>();
        fallback.add("vừa chụp nè 📸");
        fallback.add("một chút hôm nay");
        fallback.add("gửi bạn khoảnh khắc này");
        return fallback;
    }

    @Override
    protected void onCleared() {
        captionExecutor.shutdownNow();
        super.onCleared();
    }

    public enum CaptionSource {
        AI,
        CACHE,
        FALLBACK
    }

    public static class CaptionSuggestion {
        private final List<String> captions;
        private final CaptionSource source;
        private final String errorMessage;

        CaptionSuggestion(@NonNull List<String> captions, @NonNull CaptionSource source) {
            this(captions, source, null);
        }

        CaptionSuggestion(@NonNull List<String> captions, @NonNull CaptionSource source, String errorMessage) {
            this.captions = new ArrayList<>(captions);
            this.source = source;
            this.errorMessage = errorMessage;
        }

        @NonNull
        public List<String> getCaptions() {
            return new ArrayList<>(captions);
        }

        @NonNull
        public CaptionSource getSource() {
            return source;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
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
            return new UploadStatus(State.ERROR, message, null);
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
