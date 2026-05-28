package com.example.fonos;

import android.media.MediaMetadataRetriever;

import java.io.IOException;
import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Parses human-readable duration strings like "8h 30m", "5h 45m", "45m", "1h"
     * into milliseconds.
     * @param durationStr the duration string to parse
     * @return duration in milliseconds, or 0 if parsing fails
     */
    public static long parseDurationToMs(String durationStr) {
        if (durationStr == null || durationStr.trim().isEmpty()) {
            return 0L;
        }
        long hours = 0;
        long minutes = 0;

        // Extract hours (number before 'h')
        Matcher hMatcher = Pattern.compile("(\\d+)\\s*h").matcher(durationStr);
        if (hMatcher.find()) {
            hours = Long.parseLong(hMatcher.group(1));
        }

        // Extract minutes (number before 'm')
        Matcher mMatcher = Pattern.compile("(\\d+)\\s*m").matcher(durationStr);
        if (mMatcher.find()) {
            minutes = Long.parseLong(mMatcher.group(1));
        }

        return (hours * 3600000L) + (minutes * 60000L);
    }
}
