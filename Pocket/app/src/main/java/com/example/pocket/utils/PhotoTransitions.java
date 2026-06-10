package com.example.pocket.utils;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;

import com.example.pocket.R;

public final class PhotoTransitions {
    private PhotoTransitions() {
    }

    public static Bundle createPhotoDetailOptions(
            @NonNull Activity activity,
            @NonNull View sharedImageView
    ) {
        String transitionName = activity.getString(R.string.transition_photo_detail_image);
        ViewCompat.setTransitionName(sharedImageView, transitionName);
        return ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity, sharedImageView, transitionName)
                .toBundle();
    }
}
