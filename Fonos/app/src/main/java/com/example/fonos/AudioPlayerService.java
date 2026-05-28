package com.example.fonos;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

/**
 * Foreground Service for background audio playback using Media3 (ExoPlayer + MediaSession).
 * This service manages the player lifecycle, native Android media notifications, and lock screen controls.
 */
public class AudioPlayerService extends MediaSessionService {

    private static final String TAG = "AudioPlayerService";
    private ExoPlayer player;
    private MediaSession mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Initializing AudioPlayerService");

        // 1. Configure ExoPlayer with AudioAttributes to handle audio focus automatically (e.g. pauses on incoming calls)
        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
                .build();

        // 2. Configure PendingIntent so tapping the lock screen notification re-launches AudioPlayerActivity
        Intent intent = new Intent(this, AudioPlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 3. Initialize the MediaSession, binding it to the ExoPlayer instance
        mediaSession = new MediaSession.Builder(this, player)
                .setSessionActivity(pendingIntent)
                .build();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        // Return active media session so system/controllers can hook up and communicate
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: Releasing resources");
        
        // Release player and media session resources cleanly
        if (player != null) {
            player.release();
            player = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}
