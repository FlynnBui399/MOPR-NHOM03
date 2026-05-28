package com.example.fonos;

import android.media.MediaMetadataRetriever;

import java.io.IOException;
import java.io.File;
import java.util.Locale;

public final class AudioDurationUtils {
    private AudioDurationUtils() {
    }

    public static String getLocalAudioDuration(File audioFile) {
        long durationMs = getLocalAudioDurationMs(audioFile);
        return durationMs > 0 ? formatDuration(durationMs) : "";
    }

    public static long getLocalAudioDurationMs(File audioFile) {
        if (audioFile == null || !audioFile.exists() || audioFile.length() <= 0) {
            return 0L;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(audioFile.getAbsolutePath());
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration == null || duration.trim().isEmpty()) {
                return 0L;
            }

            return Long.parseLong(duration);
        } catch (RuntimeException e) {
            return 0L;
        } finally {
            try {
                retriever.release();
            } catch (IOException | RuntimeException ignored) {
            }
        }
    }

    public static String formatDuration(long durationMs) {
        if (durationMs <= 0) {
            return "";
        }

        long totalSeconds = durationMs / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }

        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }
}
