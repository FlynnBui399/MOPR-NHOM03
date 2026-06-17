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
    }

    private void bindViews() {
        playerView = findViewById(R.id.player_view);
        btnRetake = findViewById(R.id.btn_retake);
        btnSend = findViewById(R.id.btn_send);
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
        btnSend.setLoading(true);
        btnRetake.setEnabled(false);

        new Thread(() -> {
            try {
                Uri localUri = getLocalFileUri(rawVideoUri);
                if (localUri == null) {
                    throw new Exception("Local URI resolved to null");
                }

                File videoFile = new File(localUri.getPath());
                Log.d(TAG, "Uploading video to Cloudinary. Local path: " + videoFile.getAbsolutePath());

                if (!videoFile.exists() || videoFile.length() == 0) {
                    throw new Exception("Video file does not exist or is empty");
                }

                CloudinaryService cloudinaryService = new CloudinaryService();
                com.google.android.gms.tasks.Task<CloudinaryService.UploadResult> uploadTask =
                        cloudinaryService.uploadUnsignedVideo(videoFile);

                CloudinaryService.UploadResult result = com.google.android.gms.tasks.Tasks.await(uploadTask);
                String videoUrl = result.getSecureUrl();

                saveVideoToFirestore(videoUrl, videoFile);

            } catch (Exception e) {
                Log.e(TAG, "Upload failed", e);
                runOnUiThread(() -> {
                    Toast.makeText(VideoPreviewActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnSend.setLoading(false);
                    btnRetake.setEnabled(true);
                });
            }
        }).start();
    }

    private void saveVideoToFirestore(String videoUrl, File videoFile) {
        String senderId = currentUserId();
        String senderName = currentUserName();

        DocumentReference photoRef = FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_PHOTOS).document();

        Photo photo = new Photo(
                photoRef.getId(),
                senderId,
                senderName,
                "",
                "",
                "",
                captionText,
                receiverIds,
                new HashMap<>(),
                new ArrayList<>(),
                Timestamp.now()
        );
        photo.setType("video");
        photo.setVideoUrl(videoUrl);

        photoRef.set(photo)
                .addOnSuccessListener(unused -> {
                    for (String receiverId : receiverIds) {
                        com.example.pocket.utils.StreakHelper.updateStreak(senderId, receiverId);
                    }
                    if (videoFile.exists()) {
                        videoFile.delete();
                    }
                    deleteTempFile(); // Also delete the original raw video file if it's different
                    runOnUiThread(() -> {
                        Toast.makeText(VideoPreviewActivity.this, R.string.camera_upload_success, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save video to Firestore", e);
                    runOnUiThread(() -> {
                        Toast.makeText(VideoPreviewActivity.this, "Save to Firestore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnSend.setLoading(false);
                        btnRetake.setEnabled(true);
                    });
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

    private Uri getLocalFileUri(Uri uri) {
        if (uri == null) return null;
        if ("file".equals(uri.getScheme())) {
            return uri;
        }
        if ("content".equals(uri.getScheme())) {
            try {
                File cacheFile = new File(getCacheDir(), "temp-video-" + System.currentTimeMillis() + ".mp4");
                try (java.io.InputStream in = getContentResolver().openInputStream(uri);
                     java.io.OutputStream out = new java.io.FileOutputStream(cacheFile)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
                return Uri.fromFile(cacheFile);
            } catch (Exception e) {
                Log.e(TAG, "Failed to copy content URI to cache file", e);
            }
        }
        return uri;
    }

    @NonNull
    private String currentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? "local_user" : user.getUid();
    }

    @NonNull
    private String currentUserName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return getString(R.string.camera_default_user);
        }
        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName();
        }
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            return user.getEmail();
        }
        return getString(R.string.camera_default_user);
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
