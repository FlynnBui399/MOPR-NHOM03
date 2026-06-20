package com.example.pocket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.pocket.data.model.Photo;
import com.example.pocket.data.remote.CloudinaryService;
import com.example.pocket.ui.PocketButton;
import com.example.pocket.utils.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VideoPreviewActivity extends AppCompatActivity {
    private static final String TAG = "VideoPreviewActivity";

    public static final String EXTRA_VIDEO_URI = "video_uri";
    public static final String EXTRA_RECEIVER_IDS = "receiver_ids";
    public static final String EXTRA_CAPTION = "caption";

    private PlayerView playerView;
    private ExoPlayer player;
    private PocketButton btnRetake;
    private PocketButton btnSend;
    private TextInputEditText captionInput;

    private Uri rawVideoUri;
    private List<String> receiverIds;
    private String captionText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_preview);

        String uriString = getIntent().getStringExtra(EXTRA_VIDEO_URI);
        if (uriString == null) {
            Toast.makeText(this, "Error: No video to preview", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        rawVideoUri = Uri.parse(uriString);
        receiverIds = getIntent().getStringArrayListExtra(EXTRA_RECEIVER_IDS);
        if (receiverIds == null) {
            receiverIds = new ArrayList<>();
        }
        captionText = getIntent().getStringExtra(EXTRA_CAPTION);
        if (captionText == null) {
            captionText = "";
        }

        bindViews();
        setupPlayer();
        setupListeners();
        
        if (captionInput != null && !captionText.isEmpty()) {
            captionInput.setText(captionText);
        }
    }

    private void bindViews() {
        playerView = findViewById(R.id.player_view);
        btnRetake = findViewById(R.id.btn_retake);
        btnSend = findViewById(R.id.btn_send);
        captionInput = findViewById(R.id.caption_input);
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(rawVideoUri);
        player.setMediaItem(mediaItem);
        player.setRepeatMode(Player.REPEAT_MODE_ALL); // Loop playback
        player.prepare();
        player.play();
    }

    private void setupListeners() {
        btnRetake.setOnClickListener(v -> handleRetake());
        btnSend.setOnClickListener(v -> handleSend());
    }

    private void handleRetake() {
        if (player != null) {
            player.stop();
        }
        deleteTempFile();
        setResult(RESULT_CANCELED);
        finish();
    }

    private void handleSend() {
        String caption = captionInput != null && captionInput.getText() != null
                ? captionInput.getText().toString().trim() : "";
        btnSend.setLoading(true);
        btnRetake.setEnabled(false);

        PostActivitySheet.showFriendPicker(this, rawVideoUri, "video", caption, new PostActivitySheet.PostCallback() {
            @Override
            public void onSuccess() {
                deleteTempFile();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(VideoPreviewActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                btnSend.setLoading(false);
                btnRetake.setEnabled(true);
            }
        });
    }

    private void deleteTempFile() {
        if (rawVideoUri != null && "file".equals(rawVideoUri.getScheme())) {
            try {
                File file = new File(rawVideoUri.getPath());
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete temp file", e);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onBackPressed() {
        handleRetake();
    }
}
