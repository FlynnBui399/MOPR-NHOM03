package com.example.fonos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class AudioDownloadService extends Service {
    public static final String STATUS_QUEUED = "QUEUED";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_FAILED = "FAILED";

    private static final String CHANNEL_ID = "audio_downloads";
    private static final String COMPLETE_CHANNEL_ID = "audio_download_results";
    private static final int NOTIFICATION_ID = 2001;
    private static final int MAX_PARALLEL_DOWNLOADS = 3;

    private static final String EXTRA_DOWNLOAD_KEY = "download_key";
    private static final String EXTRA_BOOK_ID = "book_id";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_AUTHOR = "author";
    private static final String EXTRA_DESCRIPTION = "description";
    private static final String EXTRA_RATING = "rating";
    private static final String EXTRA_DURATION = "duration";
    private static final String EXTRA_CHAPTER_COUNT = "chapter_count";
    private static final String EXTRA_COVER_RES = "cover_res";
    private static final String EXTRA_CATEGORY = "category";
    private static final String EXTRA_COVER_URL = "cover_url";
    private static final String EXTRA_AUDIO_URL = "audio_url";

    private static final ExecutorService DOWNLOAD_EXECUTOR =
            Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS);
    private static final Map<String, DownloadState> DOWNLOAD_STATES = new ConcurrentHashMap<>();
    private static final Set<DownloadListener> LISTENERS = new CopyOnWriteArraySet<>();

    public interface DownloadListener {
        void onDownloadStateChanged(DownloadState state);
    }

    public static class DownloadState {
        public final String downloadKey;
        public final String status;
        public final int progress;

        public DownloadState(String downloadKey, String status, int progress) {
            this.downloadKey = downloadKey;
            this.status = status;
            this.progress = progress;
        }

        public boolean isActive() {
            return STATUS_QUEUED.equals(status) || STATUS_RUNNING.equals(status);
        }
    }

    public static void enqueueDownload(
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
            String audioUrl
    ) {
        String downloadKey = OfflineAudioManager.getDownloadKey(bookId, audioUrl);
        if (downloadKey == null) return;

        DownloadState existingState = DOWNLOAD_STATES.get(downloadKey);
        if (existingState != null && existingState.isActive()) return;

        Intent intent = new Intent(context, AudioDownloadService.class);
        intent.putExtra(EXTRA_DOWNLOAD_KEY, downloadKey);
        intent.putExtra(EXTRA_BOOK_ID, bookId);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_AUTHOR, author);
        intent.putExtra(EXTRA_DESCRIPTION, description);
        intent.putExtra(EXTRA_RATING, rating);
        intent.putExtra(EXTRA_DURATION, duration);
        intent.putExtra(EXTRA_CHAPTER_COUNT, chapterCount);
        intent.putExtra(EXTRA_COVER_RES, coverRes);
        intent.putExtra(EXTRA_CATEGORY, category);
        intent.putExtra(EXTRA_COVER_URL, coverUrl);
        intent.putExtra(EXTRA_AUDIO_URL, audioUrl);
        ContextCompat.startForegroundService(context.getApplicationContext(), intent);
    }

    public static void addListener(DownloadListener listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(DownloadListener listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    @Nullable
    public static DownloadState getDownloadState(String downloadKey) {
        return downloadKey != null ? DOWNLOAD_STATES.get(downloadKey) : null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification("Preparing downloads"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopIfIdle();
            return START_NOT_STICKY;
        }

        DownloadRequest request = DownloadRequest.fromIntent(intent);
        if (request == null) {
            stopIfIdle();
            return START_NOT_STICKY;
        }

        DownloadState existingState = DOWNLOAD_STATES.get(request.downloadKey);
        if (existingState != null && existingState.isActive()) {
            updateNotification();
            return START_STICKY;
        }

        updateState(request.downloadKey, STATUS_QUEUED, 0);
        DOWNLOAD_EXECUTOR.execute(() -> runDownload(request));
        updateNotification();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void runDownload(DownloadRequest request) {
        File targetFile = null;

        try {
            updateState(request.downloadKey, STATUS_RUNNING, 0);

            if (!OfflineAudioManager.ensureAudioDirectory(this)) {
                throw new IOException("Cannot create audio download directory");
            }

            targetFile = OfflineAudioManager.getDownloadTargetFile(this, request.bookId, request.audioUrl);
            if (targetFile == null) {
                throw new IOException("Cannot create audio target file");
            }

            if (OfflineAudioManager.isDirectStreamingUrl(request.audioUrl)) {
                downloadHttpAudio(request, targetFile);
            } else {
                downloadFirebaseStorageAudio(request, targetFile);
            }

            if (targetFile.length() <= 0) {
                throw new IOException("Downloaded file is empty");
            }

            OfflineAudioManager.saveDownloadedBook(
                    this,
                    request.bookId,
                    request.title,
                    request.author,
                    request.description,
                    request.rating,
                    request.duration,
                    request.chapterCount,
                    request.coverRes,
                    request.category,
                    request.coverUrl,
                    request.audioUrl,
                    targetFile
            );
            updateState(request.downloadKey, STATUS_SUCCEEDED, 100);
            showDownloadCompleteNotification(request);
        } catch (Exception e) {
            if (targetFile != null && targetFile.exists()) {
                targetFile.delete();
            }
            OfflineAudioManager.clearDownloadedAudio(this, request.bookId, request.audioUrl);
            updateState(request.downloadKey, STATUS_FAILED, 0);
        } finally {
            updateNotification();
            stopIfIdle();
        }
    }

    private void downloadHttpAudio(DownloadRequest request, File targetFile) throws IOException {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(request.audioUrl.trim());
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(30_000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Unexpected HTTP response " + responseCode);
            }

            long totalBytes = connection.getContentLengthLong();
            long downloadedBytes = 0;
            int lastProgress = -1;

            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream output = new FileOutputStream(targetFile, false)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;

                    if (totalBytes > 0) {
                        int progress = (int) ((downloadedBytes * 100) / totalBytes);
                        if (progress != lastProgress) {
                            lastProgress = progress;
                            updateState(request.downloadKey, STATUS_RUNNING, progress);
                        }
                    }
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void downloadFirebaseStorageAudio(DownloadRequest request, File targetFile) throws Exception {
        StorageReference audioRef = OfflineAudioManager.createStorageReference(request.audioUrl);
        if (audioRef == null) {
            throw new IOException("Unsupported Firebase Storage audio source");
        }

        final int[] lastProgress = {-1};
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        FileDownloadTask downloadTask = audioRef.getFile(targetFile);
        downloadTask.addOnProgressListener(snapshot -> {
            long totalBytes = snapshot.getTotalByteCount();
            if (totalBytes <= 0) return;

            int progress = (int) ((snapshot.getBytesTransferred() * 100) / totalBytes);
            if (progress != lastProgress[0]) {
                lastProgress[0] = progress;
                updateState(request.downloadKey, STATUS_RUNNING, progress);
            }
        });
        downloadTask.addOnSuccessListener(taskSnapshot -> latch.countDown());
        downloadTask.addOnFailureListener(e -> {
            error.set(e);
            latch.countDown();
        });

        latch.await();
        if (error.get() != null) {
            throw error.get();
        }
    }

    private void updateState(String downloadKey, String status, int progress) {
        DownloadState state = new DownloadState(downloadKey, status, progress);
        DOWNLOAD_STATES.put(downloadKey, state);

        for (DownloadListener listener : LISTENERS) {
            listener.onDownloadStateChanged(state);
        }
    }

    private void updateNotification() {
        int activeDownloads = getActiveDownloadCount();
        if (activeDownloads <= 0) {
            return;
        }

        String text = activeDownloads == 1
                ? "Downloading 1 audio file"
                : "Downloading " + activeDownloads + " audio files";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }

    private Notification buildNotification(String text) {
        Intent intent = new Intent(this, DownloadsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_headphones)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(0, 0, true)
                .build();
    }

    private void showDownloadCompleteNotification(DownloadRequest request) {
        Intent intent = new Intent(this, DownloadsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                request.downloadKey.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = request.title != null && !request.title.trim().isEmpty()
                ? request.title
                : "Audio";
        Notification notification = new NotificationCompat.Builder(this, COMPLETE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_headphones)
                .setContentTitle("Audio downloaded")
                .setContentText(title + " is ready for offline listening")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            int notificationId = 3000 + Math.abs(request.downloadKey.hashCode() % 100000);
            try {
                notificationManager.notify(notificationId, notification);
            } catch (SecurityException ignored) {
                // Android 13+ requires POST_NOTIFICATIONS; download still succeeds if denied.
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel downloadsChannel = new NotificationChannel(
                CHANNEL_ID,
                "Audio downloads",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationChannel resultsChannel = new NotificationChannel(
                COMPLETE_CHANNEL_ID,
                "Audio download results",
                NotificationManager.IMPORTANCE_HIGH
        );
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(downloadsChannel);
            notificationManager.createNotificationChannel(resultsChannel);
        }
    }

    private void stopIfIdle() {
        if (getActiveDownloadCount() > 0) {
            return;
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private static int getActiveDownloadCount() {
        int activeCount = 0;
        for (DownloadState state : DOWNLOAD_STATES.values()) {
            if (state.isActive()) {
                activeCount++;
            }
        }
        return activeCount;
    }

    private static class DownloadRequest {
        final String downloadKey;
        final int bookId;
        final String title;
        final String author;
        final String description;
        final float rating;
        final String duration;
        final int chapterCount;
        final int coverRes;
        final String category;
        final String coverUrl;
        final String audioUrl;

        DownloadRequest(
                String downloadKey,
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
                String audioUrl
        ) {
            this.downloadKey = downloadKey;
            this.bookId = bookId;
            this.title = title;
            this.author = author;
            this.description = description;
            this.rating = rating;
            this.duration = duration;
            this.chapterCount = chapterCount;
            this.coverRes = coverRes;
            this.category = category;
            this.coverUrl = coverUrl;
            this.audioUrl = audioUrl;
        }

        @Nullable
        static DownloadRequest fromIntent(Intent intent) {
            String downloadKey = intent.getStringExtra(EXTRA_DOWNLOAD_KEY);
            String audioUrl = intent.getStringExtra(EXTRA_AUDIO_URL);
            if (downloadKey == null || downloadKey.trim().isEmpty() ||
                    audioUrl == null || audioUrl.trim().isEmpty()) {
                return null;
            }

            return new DownloadRequest(
                    downloadKey,
                    intent.getIntExtra(EXTRA_BOOK_ID, 0),
                    intent.getStringExtra(EXTRA_TITLE),
                    intent.getStringExtra(EXTRA_AUTHOR),
                    intent.getStringExtra(EXTRA_DESCRIPTION),
                    intent.getFloatExtra(EXTRA_RATING, 0f),
                    intent.getStringExtra(EXTRA_DURATION),
                    intent.getIntExtra(EXTRA_CHAPTER_COUNT, 0),
                    intent.getIntExtra(EXTRA_COVER_RES, R.drawable.bg_book_cover_1),
                    intent.getStringExtra(EXTRA_CATEGORY),
                    intent.getStringExtra(EXTRA_COVER_URL),
                    audioUrl
            );
        }
    }
}
