package com.example.fonos;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.Player;
import androidx.media3.common.ForwardingPlayer;
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

        // Wrap ExoPlayer in a ForwardingPlayer to expose and override next/previous controls
        ForwardingPlayer forwardingPlayer = new ForwardingPlayer(player) {
            @Override
            public Player.Commands getAvailableCommands() {
                return super.getAvailableCommands().buildUpon()
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .build();
            }

            @Override
            public boolean isCommandAvailable(int command) {
                if (command == Player.COMMAND_SEEK_TO_NEXT ||
                    command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) {
                    return true;
                }
                return super.isCommandAvailable(command);
            }

            @Override
            public void seekToNext() {
                customSeekToNext();
            }

            @Override
            public void seekToNextMediaItem() {
                customSeekToNext();
            }

            @Override
            public void seekToPrevious() {
                customSeekToPrevious();
            }

            @Override
            public void seekToPreviousMediaItem() {
                customSeekToPrevious();
            }

            private void customSeekToNext() {
                long totalDur = player.getDuration();
                if (totalDur > 0) {
                    android.content.SharedPreferences sharedPref = getSharedPreferences("FonosPref", MODE_PRIVATE);
                    int chapters = sharedPref.getInt("active_book_chapters", 10);
                    int totalChapters = chapters > 0 ? chapters : 10;
                    long chapterDuration = totalDur / totalChapters;
                    long currentPos = player.getCurrentPosition();
                    
                    int currentChapter = (int) (currentPos / chapterDuration) + 1;
                    if (currentChapter < totalChapters) {
                        int nextChapter = currentChapter + 1;
                        long targetPos = (nextChapter - 1) * chapterDuration;
                        player.seekTo(targetPos);
                    } else {
                        player.seekTo(totalDur);
                    }
                }
            }

            private void customSeekToPrevious() {
                player.seekTo(0);
            }
        };

        // 2. Configure PendingIntent so tapping the lock screen notification re-launches AudioPlayerActivity
        Intent intent = new Intent(this, AudioPlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 3. Initialize the MediaSession, binding it to the forwardingPlayer instance
        mediaSession = new MediaSession.Builder(this, forwardingPlayer)
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
