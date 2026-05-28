package com.example.fonos;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import com.example.fonos.data.DownloadedBookEntity;
import com.example.fonos.data.FonosDatabase;
import com.example.fonos.model.Book;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OfflineAudioManager {
    public static final String STATUS_DOWNLOADED = "DOWNLOADED";

    private static final String AUDIO_DIR = "fonos/audio";

    private OfflineAudioManager() {
    }

    public static boolean canUseOfflineDownloads() {
        return getCurrentUserId() != null;
    }

    public static boolean isRemoteAudioSource(String value) {
        if (value == null) return false;

        String source = value.trim();
        if (source.isEmpty()) return false;

        return source.startsWith("http://") ||
                source.startsWith("https://") ||
                source.startsWith("gs://") ||
                looksLikeStoragePath(source);
    }

    public static boolean isDirectStreamingUrl(String value) {
        if (value == null) return false;

        String source = value.trim();
        return source.startsWith("http://") || source.startsWith("https://");
    }

    public static StorageReference createStorageReference(String source) {
        if (source == null) return null;

        String trimmedSource = source.trim();
        if (trimmedSource.isEmpty()) return null;

        try {
            if (trimmedSource.startsWith("http://") ||
                    trimmedSource.startsWith("https://") ||
                    trimmedSource.startsWith("gs://")) {
                return FirebaseStorage.getInstance().getReferenceFromUrl(trimmedSource);
            }

            return FirebaseStorage.getInstance().getReference(trimmedSource);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean ensureAudioDirectory(Context context) {
        File dir = getAudioDirectory(context);
        return dir != null && (dir.exists() || dir.mkdirs());
    }

    public static File getDownloadTargetFile(Context context, int bookId, String audioSource) {
        File dir = getAudioDirectory(context);
        if (dir == null) return null;

        String userId = getCurrentUserId();
        if (userId == null) return null;

        String fileName = "book_" + buildDownloadKey(userId, bookId, audioSource) + getAudioExtension(audioSource);
        return new File(dir, fileName);
    }

    public static File getDownloadedAudioFile(Context context, int bookId, String audioSource) {
        DownloadedBookEntity entity = getDownloadedEntity(context, bookId, audioSource);
        if (entity == null || entity.localFilePath == null) {
            return null;
        }

        File localFile = new File(entity.localFilePath);
        if (localFile.exists() && localFile.length() > 0) {
            return localFile;
        }

        clearDownloadedAudio(context, bookId, audioSource, false);
        return null;
    }

    public static Uri getDownloadedAudioUri(Context context, int bookId, String audioSource) {
        File localFile = getDownloadedAudioFile(context, bookId, audioSource);
        return localFile != null ? Uri.fromFile(localFile) : null;
    }

    public static boolean hasDownloadedAudio(Context context, int bookId, String audioSource) {
        return getDownloadedAudioFile(context, bookId, audioSource) != null;
    }

    public static String getDownloadedAudioDuration(Context context, int bookId, String audioSource) {
        File localFile = getDownloadedAudioFile(context, bookId, audioSource);
        return AudioDurationUtils.getLocalAudioDuration(localFile);
    }

    public static void saveDownloadedBook(
            Context context,
            int bookId,
            String title,
            String author,
            String description,
            float rating,
            String duration,
            int chapterCount,
            int coverRes,
            String category,
            String coverUrl,
            String audioUrl,
            File audioFile
    ) {
        String userId = getCurrentUserId();
        if (userId == null || audioFile == null) return;

        String actualDuration = AudioDurationUtils.getLocalAudioDuration(audioFile);
        String key = buildDownloadKey(userId, bookId, audioUrl);
        DownloadedBookEntity entity = new DownloadedBookEntity(
                key,
                userId,
                bookId,
                safeString(title),
                safeString(author),
                safeString(description),
                rating,
                !actualDuration.isEmpty() ? actualDuration : safeString(duration),
                chapterCount,
                coverRes,
                safeString(category),
                safeString(coverUrl),
                safeString(audioUrl),
                audioFile.getAbsolutePath(),
                audioFile.length(),
                STATUS_DOWNLOADED,
                System.currentTimeMillis()
        );

        FonosDatabase.getInstance(context).downloadedBookDao().upsert(entity);
    }

    public static List<Book> getDownloadedBooks(Context context) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new ArrayList<>();
        }

        List<DownloadedBookEntity> entities = FonosDatabase.getInstance(context)
                .downloadedBookDao()
                .getAllForUser(userId);
        List<Book> books = new ArrayList<>();

        for (DownloadedBookEntity entity : entities) {
            File audioFile = entity.localFilePath != null ? new File(entity.localFilePath) : null;
            if (audioFile == null || !audioFile.exists() || audioFile.length() <= 0) {
                FonosDatabase.getInstance(context).downloadedBookDao().delete(entity);
                continue;
            }

            refreshDurationFromLocalFile(context, entity, audioFile);
            books.add(toBook(entity));
        }

        return books;
    }

    public static List<DownloadedBookEntity> getDownloadedBookEntities(Context context) {
        String userId = getCurrentUserId();
        if (userId == null) {
            return new ArrayList<>();
        }

        List<DownloadedBookEntity> entities = FonosDatabase.getInstance(context)
                .downloadedBookDao()
                .getAllForUser(userId);
        List<DownloadedBookEntity> validEntities = new ArrayList<>();

        for (DownloadedBookEntity entity : entities) {
            File audioFile = entity.localFilePath != null ? new File(entity.localFilePath) : null;
            if (audioFile == null || !audioFile.exists() || audioFile.length() <= 0) {
                FonosDatabase.getInstance(context).downloadedBookDao().delete(entity);
            } else {
                refreshDurationFromLocalFile(context, entity, audioFile);
                validEntities.add(entity);
            }
        }

        return validEntities;
    }

    public static void clearDownloadedAudio(Context context, int bookId, String audioSource) {
        clearDownloadedAudio(context, bookId, audioSource, true);
    }

    public static void clearDownloadedAudio(Context context, int bookId, String audioSource, boolean deleteFile) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        DownloadedBookEntity entity = getDownloadedEntity(context, bookId, audioSource);
        if (entity == null) {
            FonosDatabase.getInstance(context)
                    .downloadedBookDao()
                    .deleteByKey(buildDownloadKey(userId, bookId, audioSource));
            return;
        }

        if (deleteFile && entity.localFilePath != null) {
            File localFile = new File(entity.localFilePath);
            if (localFile.exists()) {
                localFile.delete();
            }
        }

        FonosDatabase.getInstance(context).downloadedBookDao().delete(entity);
    }

    public static void deleteDownloadedBook(Context context, DownloadedBookEntity entity) {
        if (entity == null) return;

        String userId = getCurrentUserId();
        if (userId == null || !userId.equals(entity.userId)) return;

        if (entity.localFilePath != null) {
            File localFile = new File(entity.localFilePath);
            if (localFile.exists()) {
                localFile.delete();
            }
        }

        FonosDatabase.getInstance(context).downloadedBookDao().delete(entity);
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";

        double kilobytes = bytes / 1024.0;
        if (kilobytes < 1024) {
            return String.format(Locale.US, "%.1f KB", kilobytes);
        }

        double megabytes = kilobytes / 1024.0;
        return String.format(Locale.US, "%.1f MB", megabytes);
    }

    public static Book toBook(DownloadedBookEntity entity) {
        Book book = new Book(
                entity.bookId,
                entity.title,
                entity.author,
                entity.description,
                entity.rating,
                entity.duration,
                entity.chapterCount,
                entity.coverDrawableRes,
                entity.category
        );
        book.setCoverUrl(entity.coverUrl);
        book.setAudioUrl(entity.audioUrl);
        return book;
    }

    private static DownloadedBookEntity getDownloadedEntity(Context context, int bookId, String audioSource) {
        String userId = getCurrentUserId();
        if (userId == null) return null;

        String key = buildDownloadKey(userId, bookId, audioSource);
        return FonosDatabase.getInstance(context).downloadedBookDao().getByKey(key);
    }

    private static void refreshDurationFromLocalFile(
            Context context,
            DownloadedBookEntity entity,
            File audioFile
    ) {
        String actualDuration = AudioDurationUtils.getLocalAudioDuration(audioFile);
        if (actualDuration.isEmpty() || actualDuration.equals(entity.duration)) {
            return;
        }

        entity.duration = actualDuration;
        FonosDatabase.getInstance(context).downloadedBookDao().upsert(entity);
    }

    private static File getAudioDirectory(Context context) {
        String userId = getCurrentUserId();
        if (userId == null) return null;

        File baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (baseDir == null) {
            baseDir = context.getFilesDir();
        }

        return new File(new File(baseDir, AUDIO_DIR), sanitizeFileName(userId));
    }

    private static String buildDownloadKey(String userId, int bookId, String audioSource) {
        String bookKey;
        if (bookId > 0) {
            bookKey = String.valueOf(bookId);
        } else if (audioSource == null || audioSource.trim().isEmpty()) {
            bookKey = "unknown";
        } else {
            long hash = Math.abs((long) audioSource.trim().hashCode());
            bookKey = "url_" + hash;
        }

        return sanitizeFileName(userId) + "_" + bookKey;
    }

    private static String safeString(String value) {
        return value != null ? value : "";
    }

    private static String getAudioExtension(String audioSource) {
        if (audioSource == null) return ".mp3";

        String source = audioSource.toLowerCase(Locale.US);
        int queryIndex = source.indexOf('?');
        if (queryIndex >= 0) {
            source = source.substring(0, queryIndex);
        }

        String[] extensions = {".mp3", ".m4a", ".aac", ".wav", ".ogg", ".opus"};
        for (String extension : extensions) {
            if (source.contains(extension)) {
                return extension;
            }
        }

        return ".mp3";
    }

    private static boolean looksLikeStoragePath(String source) {
        if (!source.contains("/")) return false;

        String lower = source.toLowerCase(Locale.US);
        return lower.contains(".mp3") ||
                lower.contains(".m4a") ||
                lower.contains(".aac") ||
                lower.contains(".wav") ||
                lower.contains(".ogg") ||
                lower.contains(".opus");
    }

    private static String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    private static String sanitizeFileName(String value) {
        if (value == null || value.trim().isEmpty()) return "unknown";
        return value.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
