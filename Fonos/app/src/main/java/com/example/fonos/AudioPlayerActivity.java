package com.example.fonos;

import android.Manifest;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.bumptech.glide.Glide;
import com.example.fonos.model.Book;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Spotify-style Audio Player Activity.
 * It binds to AudioPlayerService using Android Media3 MediaController.
 * This guarantees the player state persists across rotations or returning to the app.
 */
public class AudioPlayerActivity extends AppCompatActivity {

    private static final String TAG = "AudioPlayerActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    private TextView tvPlayerTitle, tvPlayerSubtitle, tvCurrentTime, tvTotalTime, tvCoverTitle;
    private TextView btnPlayPause, tvSpeedValue;
    private ImageView viewPlayerCover;
    private SeekBar seekBarAudio;

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;

    private String title, author, duration, coverUrl, audioUrl, category;
    private int coverRes, bookId;
    private int sleepTimerIndex = 0;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaController != null && mediaController.isPlaying()) {
                long currentPos = mediaController.getCurrentPosition();
                long totalDur = mediaController.getDuration();
                if (totalDur > 0) {
                    int progress = (int) ((currentPos * 100) / totalDur);
                    seekBarAudio.setProgress(progress);
                }
                tvCurrentTime.setText(formatTime(currentPos));
            }
            progressHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_audio_player);

        View root = findViewById(R.id.playerRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    systemBars.top,
                    v.getPaddingRight(),
                    systemBars.bottom
            );
            return insets;
        });

        // 1. Retrieve book metadata passed from BookDetailActivity or MainActivity
        loadIntentData();

        // 2. Initialize UI Views
        initViews();

        // 3. Configure player buttons, seekbar, and speed controls
        setupActions();

        // 4. Handle POST_NOTIFICATIONS permission for Android 13+
        checkNotificationPermission();

        // 5. Bind/Connect to the AudioPlayerService
        connectToPlayerService();
    }

    private void loadIntentData() {
        if (getIntent() != null) {
            bookId = getIntent().getIntExtra("book_id", 0);
            title = getIntent().getStringExtra("book_title");
            author = getIntent().getStringExtra("book_author");
            duration = getIntent().getStringExtra("book_duration");
            coverRes = getIntent().getIntExtra("book_cover", R.drawable.bg_book_cover_1);
            coverUrl = getIntent().getStringExtra("book_cover_url");
            audioUrl = getIntent().getStringExtra("book_audio_url");
            category = getIntent().getStringExtra("book_category");
        }

        // Apply defaults if empty
        if (title == null) title = getString(R.string.app_name);
        if (author == null) author = "";
        if (duration == null) duration = "8h 30m";
    }

    private void initViews() {
        tvPlayerTitle = findViewById(R.id.tvPlayerTitle);
        tvPlayerSubtitle = findViewById(R.id.tvPlayerSubtitle);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvCoverTitle = findViewById(R.id.tvCoverTitle);
        tvSpeedValue = findViewById(R.id.tvSpeedValue);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        viewPlayerCover = findViewById(R.id.viewPlayerCover);
        seekBarAudio = findViewById(R.id.seekBarAudio);

        // Populate initial UI metadata
        tvPlayerTitle.setText(title);
        tvPlayerSubtitle.setText(getString(R.string.player_subtitle_demo, author));
        tvCoverTitle.setText(title);
        tvTotalTime.setText(duration);

        // Load cover image using Glide
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this).load(coverUrl).into(viewPlayerCover);
        } else {
            viewPlayerCover.setImageResource(coverRes);
        }
    }

    private void setupActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Play/Pause Click Handler
        btnPlayPause.setOnClickListener(v -> {
            if (mediaController != null) {
                if (mediaController.isPlaying()) {
                    mediaController.pause();
                } else {
                    mediaController.play();
                }
            }
        });

        // 15 seconds skip backward
        findViewById(R.id.btnRewind15).setOnClickListener(v -> {
            if (mediaController != null) {
                long targetPos = Math.max(mediaController.getCurrentPosition() - 15000, 0);
                mediaController.seekTo(targetPos);
                tvCurrentTime.setText(formatTime(targetPos));
            }
        });

        // 30 seconds skip forward
        findViewById(R.id.btnForward30).setOnClickListener(v -> {
            if (mediaController != null) {
                long targetPos = Math.min(mediaController.getCurrentPosition() + 30000, mediaController.getDuration());
                mediaController.seekTo(targetPos);
                tvCurrentTime.setText(formatTime(targetPos));
            }
        });

        // Playback speed selection (cycles 1.0x -> 1.25x -> 1.5x -> 2.0x -> 0.75x -> 1.0x)
        tvSpeedValue.setOnClickListener(v -> {
            if (mediaController != null) {
                float currentSpeed = mediaController.getPlaybackParameters().speed;
                float newSpeed;
                if (currentSpeed == 1.0f) newSpeed = 1.25f;
                else if (currentSpeed == 1.25f) newSpeed = 1.5f;
                else if (currentSpeed == 1.5f) newSpeed = 2.0f;
                else if (currentSpeed == 2.0f) newSpeed = 0.75f;
                else newSpeed = 1.0f;

                mediaController.setPlaybackSpeed(newSpeed);
                tvSpeedValue.setText(String.format(Locale.getDefault(), "%.2fx", newSpeed));
            }
        });

        // Seekbar slide handler
        seekBarAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaController != null) {
                    long totalDur = mediaController.getDuration();
                    long targetPos = (totalDur * progress) / 100;
                    mediaController.seekTo(targetPos);
                    tvCurrentTime.setText(formatTime(targetPos));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Bookmark listener - saves book dynamically to library if user is logged in
        findViewById(R.id.btnBookmark).setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Da luu sach vao dau trang (Khach)!", Toast.LENGTH_SHORT).show();
                return;
            }

            Book book = new Book(bookId, title, author, "", 5.0f, duration, 10, coverRes, category != null ? category : "General");
            book.setCoverUrl(coverUrl);
            book.setAudioUrl(audioUrl);

            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.getUid())
                    .collection("library")
                    .document(String.valueOf(bookId))
                    .set(book)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(AudioPlayerActivity.this, "Da luu sach vao thu vien thanh cong!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AudioPlayerActivity.this, "Luu sach noi that bai.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Previous button - plays chapter from start
        findViewById(R.id.btnPrev).setOnClickListener(v -> {
            if (mediaController != null) {
                mediaController.seekTo(0);
                Toast.makeText(this, "Dang phat lai tu dau", Toast.LENGTH_SHORT).show();
            }
        });

        // Next button - next chapter placeholder
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            Toast.makeText(this, "Day la chuong cuoi cung cua sach", Toast.LENGTH_SHORT).show();
        });

        // Sleep Timer - cycle timer options
        findViewById(R.id.btnSleepTimer).setOnClickListener(v -> {
            sleepTimerIndex = (sleepTimerIndex + 1) % 4;
            String message;
            switch (sleepTimerIndex) {
                case 1: message = "Da hen gio tat sau 15 phut"; break;
                case 2: message = "Da hen gio tat sau 30 phut"; break;
                case 3: message = "Da hen gio tat sau 60 phut"; break;
                default: message = "Da tat che do hen gio"; break;
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        // Chapters button - chapters selection placeholder
        findViewById(R.id.btnChapters).setOnClickListener(v -> {
            Toast.makeText(this, "Danh sach chuong dang duoc tai...", Toast.LENGTH_SHORT).show();
        });
    }

    private void connectToPlayerService() {
        Log.d(TAG, "connectToPlayerService: Binding to AudioPlayerService");
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, AudioPlayerService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                Log.d(TAG, "MediaController connected successfully");
                onPlayerConnected();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Failed to resolve MediaController future: ", e);
                Toast.makeText(this, "Failed to connect to audio player service.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void onPlayerConnected() {
        if (mediaController == null) return;

        // Listen for player state changes
        mediaController.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseState(isPlaying);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long durationMs = mediaController.getDuration();
                    tvTotalTime.setText(formatTime(durationMs));
                    startProgressUpdater();
                }
            }
        });

        // Initialize state
        updatePlayPauseState(mediaController.isPlaying());

        // Check if we should load the new book or resume currently playing audio
        boolean isNewBook = true;
        MediaItem activeItem = mediaController.getCurrentMediaItem();
        if (activeItem != null && activeItem.mediaMetadata.title != null) {
            if (activeItem.mediaMetadata.title.toString().equalsIgnoreCase(title)) {
                isNewBook = false;
            }
        }

        if (isNewBook && audioUrl != null && !audioUrl.isEmpty()) {
            Log.d(TAG, "Loading new audiobook URL: " + audioUrl);
            
            // Build media metadata with titles and artwork URL for native lockscreen controls
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(author)
                    .setArtworkUri(coverUrl != null ? Uri.parse(coverUrl) : null)
                    .build();

            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(audioUrl)
                    .setMediaMetadata(metadata)
                    .build();

            mediaController.setMediaItem(mediaItem);
            mediaController.prepare();
            mediaController.play();
        } else {
            Log.d(TAG, "Resuming display of active audiobook: " + title);
            if (mediaController.getPlaybackState() == Player.STATE_READY) {
                long durationMs = mediaController.getDuration();
                tvTotalTime.setText(formatTime(durationMs));
                startProgressUpdater();
            }
        }
    }

    private void updatePlayPauseState(boolean isPlaying) {
        btnPlayPause.setText(isPlaying ? "❚❚" : "▶");
        if (isPlaying) {
            startProgressUpdater();
        } else {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    private void startProgressUpdater() {
        progressHandler.removeCallbacks(progressRunnable);
        progressHandler.post(progressRunnable);
    }

    private String formatTime(long ms) {
        if (ms == C.TIME_UNSET || ms < 0) return "00:00";
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
            } else {
                Toast.makeText(this, "Notification permission is required to control playback from the background.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Cleaning up activity references");
        progressHandler.removeCallbacks(progressRunnable);
        
        // Release MediaController future cleanly to prevent memory leaks
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
        super.onDestroy();
    }
}