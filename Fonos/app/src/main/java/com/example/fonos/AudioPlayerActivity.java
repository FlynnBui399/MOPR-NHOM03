package com.example.fonos;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AudioPlayerActivity extends AppCompatActivity {

    private TextView tvPlayerTitle, tvPlayerSubtitle, tvCurrentTime, tvTotalTime, tvCoverTitle;
    private TextView btnPlayPause, tvSpeedValue;
    private View viewPlayerCover;
    private SeekBar seekBarAudio;

    private boolean isPlaying = false;

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
        int coverRes = getIntent().getIntExtra("book_cover", R.drawable.bg_book_cover_1);

        if (title == null) title = getString(R.string.app_name);
        if (author == null) author = "";
        if (duration == null) duration = "8h 30m";

        tvPlayerTitle.setText(title);
        tvPlayerSubtitle.setText(getString(R.string.player_subtitle_demo, author));
        tvCoverTitle.setText(title);

        tvCurrentTime.setText("00:00");
        tvTotalTime.setText(duration);

        tvSpeedValue.setText("1.0x");

        viewPlayerCover.setBackgroundResource(coverRes);

        seekBarAudio.setMax(100);
        seekBarAudio.setProgress(0);
    }

    private void setupActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnPrev).setOnClickListener(v -> {
            // demo UI
        });

        findViewById(R.id.btnNext).setOnClickListener(v -> {
            // demo UI
        });

        findViewById(R.id.btnRewind15).setOnClickListener(v -> {
            int current = seekBarAudio.getProgress();
            seekBarAudio.setProgress(Math.max(current - 15, 0));
        });

        findViewById(R.id.btnForward30).setOnClickListener(v -> {
            int current = seekBarAudio.getProgress();
            seekBarAudio.setProgress(Math.min(current + 30, 100));
        });

        btnPlayPause.setOnClickListener(v -> {
            isPlaying = !isPlaying;
            btnPlayPause.setText(isPlaying ? "❚❚" : "▶");
        });
    }
}