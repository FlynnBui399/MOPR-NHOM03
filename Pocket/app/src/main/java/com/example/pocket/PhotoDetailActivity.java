package com.example.pocket;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.pocket.data.model.Photo;
import com.example.pocket.viewmodel.FeedViewModel;
import com.example.pocket.widget.PocketWidgetProvider;
import com.google.android.material.appbar.MaterialToolbar;
import com.ortiz.touchview.TouchImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PhotoDetailActivity extends AppCompatActivity {
    private TouchImageView photoImageView;
    private TextView senderNameView;
    private TextView timestampView;
    private TextView captionView;
    private CircleImageView senderAvatarView;
    private LinearLayout infoBar;
    private LinearLayout reactionBar;
    private ImageButton backButton;
    private ImageButton saveButton;
    private TextView reactionHotdog;
    private TextView reactionHeartEyes;
    private TextView reactionHearts;
    private TextView reactionSmile;
    private TextView reactionFire;
    private FeedViewModel feedViewModel;
    private Photo currentPhoto;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_detail);

        initializeViews();
        setupViewModel();
        loadPhoto();
        setupClickListeners();
    }

    private void initializeViews() {
        photoImageView = findViewById(R.id.photo_detail_image);
        senderNameView = findViewById(R.id.photo_detail_sender_name);
        timestampView = findViewById(R.id.photo_detail_timestamp);
        captionView = findViewById(R.id.photo_detail_caption);
        senderAvatarView = findViewById(R.id.photo_detail_sender_avatar);
        infoBar = findViewById(R.id.photo_detail_info_bar);
        reactionBar = findViewById(R.id.photo_detail_reaction_bar);
        backButton = findViewById(R.id.photo_detail_back);
        saveButton = findViewById(R.id.photo_detail_save);
        reactionHotdog = findViewById(R.id.reaction_hotdog);
        reactionHeartEyes = findViewById(R.id.reaction_heart_eyes);
        reactionHearts = findViewById(R.id.reaction_hearts);
        reactionSmile = findViewById(R.id.reaction_smile);
        reactionFire = findViewById(R.id.reaction_fire);
    }

    private void setupViewModel() {
        feedViewModel = new ViewModelProvider(this).get(FeedViewModel.class);
        currentUserId = com.example.pocket.utils.SharedPrefManager.getInstance(this).getUserId();
    }

    private void loadPhoto() {
        Intent intent = getIntent();
        String photoId = intent.getStringExtra("photo_id");

        if (photoId != null) {
            loadPhotoById(photoId);
        }
    }

    private void loadPhotoById(String photoId) {
        // Load photo from Firestore
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("photos")
                .document(photoId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading photo", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        currentPhoto = snapshot.toObject(Photo.class);
                        displayPhoto(currentPhoto);
                    }
                });
    }

    private void displayPhoto(Photo photo) {
        if (photo == null) return;
        PocketWidgetProvider.updateLatestPhoto(this, photo);
        // Load image
        Glide.with(this)
                .load(photo.getImageUrl())
                .into(photoImageView);

        // Display metadata
        senderNameView.setText(photo.getSenderName());
        timestampView.setText(getTimeAgoText(photo.getCreatedAt()));

        if (photo.getCaption() != null && !photo.getCaption().isEmpty()) {
            captionView.setText(photo.getCaption());
            captionView.setVisibility(android.view.View.VISIBLE);
        }

        // Load sender avatar
        if (photo.getSenderId() != null) {
            loadSenderAvatar(photo.getSenderId());
        }
    }

    private void loadSenderAvatar(String senderId) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(senderId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error == null && snapshot != null && snapshot.exists()) {
                        String avatarUrl = snapshot.getString("avatarUrl");
                        if (avatarUrl != null) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .into(senderAvatarView);
                        }
                    }
                });
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> {
            if (currentPhoto != null) {
                savePhotoToGallery(currentPhoto.getImageUrl());
            }
        });

        reactionHotdog.setOnClickListener(v -> reactToPhoto("🌭"));
        reactionHeartEyes.setOnClickListener(v -> reactToPhoto("😍"));
        reactionHearts.setOnClickListener(v -> reactToPhoto("❤️"));
        reactionSmile.setOnClickListener(v -> reactToPhoto("😊"));
        reactionFire.setOnClickListener(v -> reactToPhoto("🔥"));

        findViewById(R.id.photo_detail_reply).setOnClickListener(v -> {
            if (currentPhoto != null) {
                openChat(currentPhoto.getSenderId());
            }
        });
    }

    private void reactToPhoto(String reaction) {
        if (currentPhoto != null && currentUserId != null) {
            feedViewModel.reactToPhoto(currentPhoto.getId(), currentUserId, reaction);
            Toast.makeText(this, reaction + " sent!", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePhotoToGallery(String imageUrl) {
        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource,
                                                @Nullable Transition<? super Bitmap> transition) {
                        saveBitmapToGallery(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                    }
                });
    }

    private void saveBitmapToGallery(Bitmap bitmap) {
        try {
            File picturesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES);
            if (!picturesDir.exists()) {
                picturesDir.mkdirs();
            }

            String fileName = "Pocket_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) +
                    ".jpg";
            File file = new File(picturesDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            }

            // Notify gallery
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(file));
            sendBroadcast(scanIntent);

            Toast.makeText(this, "Saved to gallery", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void openChat(String recipientId) {
        Intent chatIntent = new Intent(this, ChatActivity.class);
        chatIntent.putExtra("recipient_id", recipientId);
        startActivity(chatIntent);
    }

    private String getTimeAgoText(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "";

        long timeMillis = timestamp.getSeconds() * 1000;
        long currentTime = System.currentTimeMillis();
        long diffMillis = currentTime - timeMillis;

        long seconds = diffMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "just now";
        } else if (minutes < 60) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (days < 7) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else {
            return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new Date(timeMillis));
        }
    }
}
