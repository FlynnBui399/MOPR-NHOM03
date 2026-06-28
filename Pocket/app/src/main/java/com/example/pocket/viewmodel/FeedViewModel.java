package com.example.pocket.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pocket.data.model.Photo;
import com.example.pocket.data.repository.PhotoRepository;

import java.util.ArrayList;
import java.util.List;

public class FeedViewModel extends ViewModel {
    private final PhotoRepository photoRepository;
    private final MediatorLiveData<List<Photo>> timelinePhotos =
            new MediatorLiveData<>();
    private LiveData<List<Photo>> timelineSource;

    public FeedViewModel() {
        this(new PhotoRepository());
    }

    FeedViewModel(@NonNull PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
    }

    @NonNull
    public LiveData<List<Photo>> getTimelinePhotos() {
        return timelinePhotos;
    }

    public void loadTimeline(@NonNull String userId) {
        if (timelineSource != null) {
            timelinePhotos.removeSource(timelineSource);
        }
        timelineSource = photoRepository.getTimelinePhotos(userId);
        timelinePhotos.addSource(timelineSource, photos ->
                timelinePhotos.setValue(photos == null ? new ArrayList<>() : photos));
    }

    public void reactToPhoto(@NonNull String photoId,
                             @NonNull String userId,
                             @NonNull String reaction) {
        photoRepository.reactToPhoto(photoId, userId, reaction);
    }

    public void markPhotoSeen(@NonNull String photoId, @NonNull String userId) {
        photoRepository.markPhotoSeen(photoId, userId);
    }

    @Override
    protected void onCleared() {
        photoRepository.clearTimelineListeners();
        super.onCleared();
    }
}
