package com.example.fonos;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.bumptech.glide.Glide;
import com.example.fonos.fragment.HomeFragment;
import com.example.fonos.fragment.LibraryFragment;
import com.example.fonos.fragment.ProfileFragment;
import com.example.fonos.fragment.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private View miniPlayerContainer;
    private ImageView imgMiniPlayerCover;
    private TextView tvMiniPlayerTitle;
    private TextView tvMiniPlayerSubtitle;
    private ImageButton btnMiniPlayerPlayPause;
    private ProgressBar miniPlayerProgress;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaController != null && mediaController.isPlaying()) {
                long currentPos = mediaController.getCurrentPosition();
                long totalDur = mediaController.getDuration();
                if (totalDur > 0) {
                    int progress = (int) ((currentPos * 100) / totalDur);
                    if (miniPlayerProgress != null) {
                        miniPlayerProgress.setProgress(progress);
                    }
                }
            }
            progressHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        setupMiniPlayer();

        // Do not seed placeholder data from the app.
        // Firestore data should be managed from Firebase Console or official seed scripts.
        // FirestoreSeeder.seedBooks(this);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_search) {
                fragment = new SearchFragment();
            } else if (itemId == R.id.nav_library) {
                fragment = new LibraryFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }

            return false;
        });
    }
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    public void selectSearchTab() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_search);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMiniPlayerMetadata();
        updateMiniPlayerVisibility();
    }

    @Override
    protected void onDestroy() {
        progressHandler.removeCallbacks(progressRunnable);
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
        super.onDestroy();
    }

    private void setupMiniPlayer() {
        miniPlayerContainer = findViewById(R.id.miniPlayer);
        if (miniPlayerContainer != null) {
            imgMiniPlayerCover = miniPlayerContainer.findViewById(R.id.imgMiniPlayerCover);
            tvMiniPlayerTitle = miniPlayerContainer.findViewById(R.id.tvMiniPlayerTitle);
            tvMiniPlayerSubtitle = miniPlayerContainer.findViewById(R.id.tvMiniPlayerSubtitle);
            btnMiniPlayerPlayPause = miniPlayerContainer.findViewById(R.id.btnMiniPlayerPlayPause);
            miniPlayerProgress = miniPlayerContainer.findViewById(R.id.miniPlayerProgress);

            if (btnMiniPlayerPlayPause != null) {
                btnMiniPlayerPlayPause.setOnClickListener(v -> {
                    if (mediaController != null) {
                        if (mediaController.isPlaying()) {
                            mediaController.pause();
                        } else {
                            mediaController.play();
                        }
                    }
                });
            }
        }

        // Bind/Connect to the AudioPlayerService
        connectToPlayerService();
    }

    private void connectToPlayerService() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, AudioPlayerService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                onPlayerConnected();
            } catch (ExecutionException | InterruptedException e) {
                // Ignore connection errors
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void onPlayerConnected() {
        if (mediaController == null) return;

        mediaController.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseState(isPlaying);
                updateMiniPlayerVisibility();
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                updateMiniPlayerVisibility();
                if (playbackState == Player.STATE_READY) {
                    startProgressUpdater();
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable androidx.media3.common.MediaItem mediaItem, int reason) {
                updateMiniPlayerMetadata();
                updateMiniPlayerVisibility();
            }
        });

        // Initial state update
        updateMiniPlayerMetadata();
        updatePlayPauseState(mediaController.isPlaying());
        updateMiniPlayerVisibility();

        if (mediaController.isPlaying()) {
            startProgressUpdater();
        }
    }

    private void updatePlayPauseState(boolean isPlaying) {
        if (btnMiniPlayerPlayPause != null) {
            btnMiniPlayerPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        }
    }

    private void startProgressUpdater() {
        progressHandler.removeCallbacks(progressRunnable);
        progressHandler.post(progressRunnable);
    }

    private void updateMiniPlayerVisibility() {
        if (mediaController != null &&
                (mediaController.isPlaying() || mediaController.getPlaybackState() == Player.STATE_READY) &&
                mediaController.getCurrentMediaItem() != null) {
            if (miniPlayerContainer != null) {
                miniPlayerContainer.setVisibility(View.VISIBLE);
            }
        } else {
            if (miniPlayerContainer != null) {
                miniPlayerContainer.setVisibility(View.GONE);
            }
        }
    }

    private void updateMiniPlayerMetadata() {
        SharedPreferences sharedPref = getSharedPreferences("FonosPref", MODE_PRIVATE);

        int activeBookId = sharedPref.getInt("active_book_id", 0);
        String activeTitle = sharedPref.getString("active_book_title", null);
        String activeAuthor = sharedPref.getString("active_book_author", null);
        String activeDuration = sharedPref.getString("active_book_duration", null);
        int activeCover = sharedPref.getInt("active_book_cover", R.drawable.bg_book_cover_1);
        String activeCoverUrl = sharedPref.getString("active_book_cover_url", null);
        String activeAudioUrl = sharedPref.getString("active_book_audio_url", null);
        String activeCategory = sharedPref.getString("active_book_category", null);

        // Fallback to activeMediaItem if shared preferences is empty but service has active item
        if ((activeTitle == null || activeTitle.trim().isEmpty()) && mediaController != null && mediaController.getCurrentMediaItem() != null) {
            activeTitle = mediaController.getCurrentMediaItem().mediaMetadata.title != null ?
                    mediaController.getCurrentMediaItem().mediaMetadata.title.toString() : "";
            activeAuthor = mediaController.getCurrentMediaItem().mediaMetadata.artist != null ?
                    mediaController.getCurrentMediaItem().mediaMetadata.artist.toString() : "";
            if (mediaController.getCurrentMediaItem().mediaMetadata.artworkUri != null) {
                activeCoverUrl = mediaController.getCurrentMediaItem().mediaMetadata.artworkUri.toString();
            }
        }

        if (activeTitle != null && !activeTitle.trim().isEmpty()) {
            if (tvMiniPlayerTitle != null) {
                tvMiniPlayerTitle.setText(activeTitle);
            }
            if (tvMiniPlayerSubtitle != null) {
                tvMiniPlayerSubtitle.setText(activeAuthor != null ? activeAuthor : "");
            }

            if (imgMiniPlayerCover != null) {
                if (isValidUrl(activeCoverUrl)) {
                    Glide.with(this)
                            .load(activeCoverUrl)
                            .placeholder(activeCover)
                            .error(activeCover)
                            .into(imgMiniPlayerCover);
                } else {
                    imgMiniPlayerCover.setImageResource(activeCover);
                }
            }

            if (miniPlayerContainer != null) {
                final int bookIdVal = activeBookId;
                final String titleVal = activeTitle;
                final String authorVal = activeAuthor;
                final String durationVal = activeDuration;
                final int coverVal = activeCover;
                final String coverUrlVal = activeCoverUrl;
                final String audioUrlVal = activeAudioUrl;
                final String categoryVal = activeCategory;

                miniPlayerContainer.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, AudioPlayerActivity.class);
                    intent.putExtra("book_id", bookIdVal);
                    intent.putExtra("book_title", titleVal);
                    intent.putExtra("book_author", authorVal);
                    intent.putExtra("book_duration", durationVal);
                    intent.putExtra("book_cover", coverVal);
                    intent.putExtra("book_cover_url", coverUrlVal);
                    intent.putExtra("audio_url", audioUrlVal);
                    intent.putExtra("book_audio_url", audioUrlVal);
                    intent.putExtra("book_category", categoryVal);
                    startActivity(intent);
                });
            }
        } else {
            if (miniPlayerContainer != null) {
                miniPlayerContainer.setOnClickListener(v -> {
                    Toast.makeText(this, "Hãy chọn một sách để bắt đầu nghe", Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    private boolean isValidUrl(String value) {
        return value != null &&
                (value.trim().startsWith("http://") || value.trim().startsWith("https://"));
    }
}
