package com.example.fonos;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.bumptech.glide.Glide;

import java.util.Locale;

public class AudioPlayerActivity extends AppCompatActivity {

    private TextView tvPlayerTitle, tvPlayerSubtitle, tvCurrentTime, tvTotalTime, tvCoverTitle;
    private TextView btnPlayPause, tvSpeedValue;
    private ImageView viewPlayerCover;
    private SeekBar seekBarAudio;

    private ExoPlayer player;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean isUserSeeking = false;
    private float currentSpeed = 1.0f;

    private String audioUrl;
    private String coverUrl;
    private int coverRes;

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 500);
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

        initViews();
        loadBookData();
        setupPlayer();
        setupActions();
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
    }

    private void loadBookData() {
        String title = getIntent().getStringExtra("book_title");
        String author = getIntent().getStringExtra("book_author");
        String duration = getIntent().getStringExtra("book_duration");

        coverRes = getIntent().getIntExtra("book_cover", R.drawable.bg_book_cover_1);
        coverUrl = getIntent().getStringExtra("book_cover_url");

        audioUrl = getIntent().getStringExtra("audio_url");
        if (!isValidUrl(audioUrl)) {
            audioUrl = getIntent().getStringExtra("book_audio_url");
        }

        if (title == null) title = getString(R.string.app_name);
        if (author == null) author = "";
        if (duration == null) duration = "00:00";

        tvPlayerTitle.setText(title);
        tvPlayerSubtitle.setText(getString(R.string.player_subtitle_demo, author));
        tvCoverTitle.setText(title);

        tvCurrentTime.setText("00:00");
        tvTotalTime.setText(duration);

        currentSpeed = 1.0f;
        tvSpeedValue.setText("1.0x");

        seekBarAudio.setMax(100);
        seekBarAudio.setProgress(0);

        renderCover();
    }

    private void renderCover() {
        if (isValidUrl(coverUrl)) {
            Glide.with(this)
                    .load(coverUrl)
                    .placeholder(coverRes)
                    .error(coverRes)
                    .into(viewPlayerCover);

            tvCoverTitle.setVisibility(View.GONE);
        } else {
            viewPlayerCover.setImageResource(coverRes);
            tvCoverTitle.setVisibility(View.VISIBLE);
        }
    }

    private void setupPlayer() {
        if (!isValidUrl(audioUrl)) {
            btnPlayPause.setEnabled(false);
            Toast.makeText(this, "Sách này chưa có audioUrl hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        player = new ExoPlayer.Builder(this).build();
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(audioUrl));
        player.setMediaItem(mediaItem);

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnPlayPause.setText(isPlaying ? "❚❚" : "▶");
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long durationMs = player.getDuration();

                    if (durationMs != C.TIME_UNSET && durationMs > 0) {
                        seekBarAudio.setMax((int) durationMs);
                        tvTotalTime.setText(formatTime(durationMs));
                    }

                    updateProgress();
                } else if (playbackState == Player.STATE_ENDED) {
                    btnPlayPause.setText("▶");
                    seekBarAudio.setProgress(0);
                    tvCurrentTime.setText("00:00");

                    if (player != null) {
                        player.seekTo(0);
                        player.pause();
                    }
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                btnPlayPause.setText("▶");
                Toast.makeText(
                        AudioPlayerActivity.this,
                        "Không phát được audio",
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        player.prepare();
    }

    private void setupActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnPrev).setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng chuyển chương trước sẽ làm sau", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnNext).setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng chuyển chương sau sẽ làm sau", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnRewind15).setOnClickListener(v -> {
            if (player == null) return;

            long newPosition = Math.max(player.getCurrentPosition() - 15_000, 0);
            player.seekTo(newPosition);
            updateProgress();
        });

        findViewById(R.id.btnForward30).setOnClickListener(v -> {
            if (player == null) return;

            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();
            long newPosition = currentPosition + 30_000;

            if (duration != C.TIME_UNSET && duration > 0) {
                newPosition = Math.min(newPosition, duration);
            }

            player.seekTo(newPosition);
            updateProgress();
        });

        btnPlayPause.setOnClickListener(v -> {
            if (player == null) {
                Toast.makeText(this, "Chưa sẵn sàng phát audio", Toast.LENGTH_SHORT).show();
                return;
            }

            if (player.isPlaying()) {
                player.pause();
            } else {
                player.play();
            }
        });

        seekBarAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (player != null) {
                    player.seekTo(seekBar.getProgress());
                }

                isUserSeeking = false;
            }
        });

        tvSpeedValue.setOnClickListener(v -> {
            if (player == null) return;

            if (currentSpeed == 1.0f) {
                currentSpeed = 1.25f;
            } else if (currentSpeed == 1.25f) {
                currentSpeed = 1.5f;
            } else if (currentSpeed == 1.5f) {
                currentSpeed = 2.0f;
            } else {
                currentSpeed = 1.0f;
            }

            player.setPlaybackParameters(new PlaybackParameters(currentSpeed));
            tvSpeedValue.setText(formatSpeed(currentSpeed));
        });
    }

    private void updateProgress() {
        if (player == null || isUserSeeking) return;

        long currentPosition = player.getCurrentPosition();
        long duration = player.getDuration();

        if (duration != C.TIME_UNSET && duration > 0) {
            seekBarAudio.setMax((int) duration);
        }

        seekBarAudio.setProgress((int) currentPosition);
        tvCurrentTime.setText(formatTime(currentPosition));
    }

    private String formatTime(long milliseconds) {
        if (milliseconds < 0) return "00:00";

        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        }

        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private String formatSpeed(float speed) {
        if (speed == 1.0f) return "1.0x";
        if (speed == 1.25f) return "1.25x";
        if (speed == 1.5f) return "1.5x";
        if (speed == 2.0f) return "2.0x";
        return speed + "x";
    }

    private boolean isValidUrl(String value) {
        return value != null &&
                (value.trim().startsWith("http://") || value.trim().startsWith("https://"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(progressRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(progressRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);

        if (player != null) {
            player.release();
            player = null;
        }
    }
}