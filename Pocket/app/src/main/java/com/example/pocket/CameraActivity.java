package com.example.pocket;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pocket.data.model.User;
import com.example.pocket.ui.AvatarView;
import com.example.pocket.ui.PocketButton;
import com.example.pocket.utils.Constants;
import com.example.pocket.utils.ImageUtils;
import com.example.pocket.viewmodel.CameraViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private PreviewView previewView;
    private ImageView capturedImageView;
    private TextInputLayout captionInputLayout;
    private TextInputEditText captionInput;
    private PocketButton captureButton;
    private PocketButton retakeButton;
    private PocketButton suggestCaptionButton;
    private PocketButton sendPhotoButton;
    private PocketButton flipButton;
    private PocketButton flashButton;

    private CameraViewModel viewModel;
    private FirebaseFirestore firestore;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private Camera camera;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean flashEnabled;
    private byte[] capturedJpegBytes;
    private final List<User> friends = new ArrayList<>();
    private boolean waitingForCaptionOptions;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        firestore = FirebaseFirestore.getInstance();
        cameraExecutor = Executors.newSingleThreadExecutor();
        viewModel = new ViewModelProvider(this).get(CameraViewModel.class);

        bindViews();
        bindActions();
        observeViewModel();
        loadFriends();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void bindViews() {
        previewView = findViewById(R.id.camera_preview);
        capturedImageView = findViewById(R.id.captured_image);
        captionInputLayout = findViewById(R.id.caption_input_layout);
        captionInput = findViewById(R.id.caption_input);
        captureButton = findViewById(R.id.capture_button);
        retakeButton = findViewById(R.id.retake_button);
        suggestCaptionButton = findViewById(R.id.suggest_caption_button);
        sendPhotoButton = findViewById(R.id.send_photo_button);
        flipButton = findViewById(R.id.flip_button);
        flashButton = findViewById(R.id.flash_button);
    }

    private void bindActions() {
        captureButton.setOnClickListener(view -> capturePhoto());
        retakeButton.setOnClickListener(view -> resetCapture());
        sendPhotoButton.setOnClickListener(view -> showFriendSelectorSheet());
        suggestCaptionButton.setOnClickListener(view -> {
            if (capturedJpegBytes == null) {
                return;
            }
            waitingForCaptionOptions = true;
            suggestCaptionButton.setLoading(true);
            viewModel.generateCaption(capturedJpegBytes);
        });
        flipButton.setOnClickListener(view -> {
            lensFacing = lensFacing == CameraSelector.LENS_FACING_BACK
                    ? CameraSelector.LENS_FACING_FRONT
                    : CameraSelector.LENS_FACING_BACK;
            startCamera();
        });
        flashButton.setOnClickListener(view -> {
            flashEnabled = !flashEnabled;
            applyFlashMode();
        });
    }

    private void observeViewModel() {
        viewModel.getUploadStatus().observe(this, status -> {
            boolean loading = status.getState() == CameraViewModel.UploadStatus.State.LOADING;
            sendPhotoButton.setLoading(loading);
            captureButton.setEnabled(!loading);
            retakeButton.setEnabled(!loading);
            suggestCaptionButton.setEnabled(!loading);

            if (status.getState() == CameraViewModel.UploadStatus.State.SUCCESS) {
                Toast.makeText(this, R.string.camera_upload_success, Toast.LENGTH_SHORT).show();
                resetCapture();
            } else if (status.getState() == CameraViewModel.UploadStatus.State.ERROR) {
                String message = status.getMessage() == null
                        ? getString(R.string.camera_upload_error)
                        : status.getMessage();
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getSuggestedCaptions().observe(this, captions -> {
            if (!waitingForCaptionOptions || captions == null || captions.isEmpty()) {
                return;
            }
            waitingForCaptionOptions = false;
            suggestCaptionButton.setLoading(false);
            showCaptionOptionsSheet(captions);
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(flashEnabled ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                        .build();
                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, selector, preview, imageCapture);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                applyFlashMode();
            } catch (Exception exception) {
                Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        if (imageCapture == null) {
            return;
        }

        captureButton.setLoading(true);
        File outputFile = new File(getCacheDir(), "pocket-capture-" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                try {
                    Bitmap bitmap = ImageUtils.uriToBitmap(CameraActivity.this, Uri.fromFile(outputFile));
                    byte[] compressed = ImageUtils.compress(bitmap);
                    runOnUiThread(() -> onPhotoCaptured(bitmap, compressed));
                } catch (IOException exception) {
                    runOnUiThread(() -> {
                        captureButton.setLoading(false);
                        Toast.makeText(CameraActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                    });
                } finally {
                    if (outputFile.exists()) {
                        outputFile.delete();
                    }
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> {
                    captureButton.setLoading(false);
                    Toast.makeText(CameraActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void onPhotoCaptured(@NonNull Bitmap bitmap, @NonNull byte[] compressed) {
        capturedJpegBytes = compressed;
        capturedImageView.setImageBitmap(bitmap);
        capturedImageView.setVisibility(View.VISIBLE);
        captionInputLayout.setVisibility(View.VISIBLE);
        captureButton.setLoading(false);
        captureButton.setVisibility(View.GONE);
        retakeButton.setVisibility(View.VISIBLE);
        suggestCaptionButton.setVisibility(View.VISIBLE);
        sendPhotoButton.setVisibility(View.VISIBLE);
        showFriendSelectorSheet();
    }

    private void resetCapture() {
        capturedJpegBytes = null;
        capturedImageView.setImageDrawable(null);
        capturedImageView.setVisibility(View.GONE);
        captionInput.setText(null);
        captionInputLayout.setVisibility(View.GONE);
        captureButton.setVisibility(View.VISIBLE);
        retakeButton.setVisibility(View.GONE);
        suggestCaptionButton.setVisibility(View.GONE);
        sendPhotoButton.setVisibility(View.GONE);
    }

    private void applyFlashMode() {
        if (imageCapture != null) {
            imageCapture.setFlashMode(flashEnabled
                    ? ImageCapture.FLASH_MODE_ON
                    : ImageCapture.FLASH_MODE_OFF);
        }
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(flashEnabled);
        }
        flashButton.setText(flashEnabled ? R.string.camera_flash_on : R.string.camera_flash_off);
    }

    private void loadFriends() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            firestore.collection(Constants.COLLECTION_USERS)
                    .limit(20)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        friends.clear();
                        for (QueryDocumentSnapshot document : snapshot) {
                            friends.add(document.toObject(User.class));
                        }
                    });
            return;
        }

        firestore.collection(Constants.COLLECTION_USERS)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    User user = document.toObject(User.class);
                    List<String> friendIds = user == null ? new ArrayList<>() : user.getFriendIds();
                    loadUsersByIds(friendIds);
                });
    }

    private void loadUsersByIds(@Nullable List<String> userIds) {
        friends.clear();
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        for (int start = 0; start < userIds.size(); start += 10) {
            int end = Math.min(start + 10, userIds.size());
            List<String> chunk = userIds.subList(start, end);
            firestore.collection(Constants.COLLECTION_USERS)
                    .whereIn("__name__", chunk)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (QueryDocumentSnapshot document : snapshot) {
                            friends.add(document.toObject(User.class));
                        }
                    });
        }
    }

    private void showFriendSelectorSheet() {
        if (capturedJpegBytes == null) {
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_friend_selector, null, false);
        RecyclerView friendList = content.findViewById(R.id.friend_selector_list);
        TextView emptyView = content.findViewById(R.id.friend_selector_empty);
        PocketButton sendButton = content.findViewById(R.id.friend_selector_send_button);
        Set<String> selectedIds = new HashSet<>();
        FriendAdapter adapter = new FriendAdapter(friends, selectedIds, () ->
                sendButton.setEnabled(!selectedIds.isEmpty()));

        friendList.setLayoutManager(new LinearLayoutManager(this));
        friendList.setAdapter(adapter);
        emptyView.setVisibility(friends.isEmpty() ? View.VISIBLE : View.GONE);
        friendList.setVisibility(friends.isEmpty() ? View.GONE : View.VISIBLE);
        sendButton.setEnabled(false);
        sendButton.setOnClickListener(view -> {
            String senderId = currentUserId();
            String senderName = currentUserName();
            String caption = captionInput.getText() == null ? "" : captionInput.getText().toString().trim();
            viewModel.sendPhoto(capturedJpegBytes, senderId, senderName,
                    new ArrayList<>(selectedIds), caption);
            dialog.dismiss();
        });

        dialog.setContentView(content);
        dialog.show();
    }

    private void showCaptionOptionsSheet(@NonNull List<String> captions) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_caption_options, null, false);
        RecyclerView captionList = content.findViewById(R.id.caption_options_list);
        CaptionAdapter adapter = new CaptionAdapter(captions, caption -> {
            captionInput.setText(caption);
            captionInput.setSelection(caption.length());
            dialog.dismiss();
        });
        captionList.setLayoutManager(new LinearLayoutManager(this));
        captionList.setAdapter(adapter);
        dialog.setContentView(content);
        dialog.show();
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

    private static class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {
        private final List<User> friends;
        private final Set<String> selectedIds;
        private final Runnable selectionChanged;

        FriendAdapter(List<User> friends, Set<String> selectedIds, Runnable selectionChanged) {
            this.friends = friends;
            this.selectedIds = selectedIds;
            this.selectionChanged = selectionChanged;
        }

        @NonNull
        @Override
        public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend_selector, parent, false);
            return new FriendViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
            User friend = friends.get(position);
            String name = friend.getDisplayName() == null || friend.getDisplayName().trim().isEmpty()
                    ? friend.getUsername()
                    : friend.getDisplayName();
            if (name == null || name.trim().isEmpty()) {
                name = holder.itemView.getContext().getString(R.string.camera_default_user);
            }
            String id = friend.getId();

            holder.nameView.setText(name);
            holder.avatarView.setAvatarText(name);
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(id != null && selectedIds.contains(id));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (id != null) {
                    if (isChecked) {
                        selectedIds.add(id);
                    } else {
                        selectedIds.remove(id);
                    }
                }
                selectionChanged.run();
            });
            holder.itemView.setOnClickListener(view -> holder.checkBox.setChecked(!holder.checkBox.isChecked()));
        }

        @Override
        public int getItemCount() {
            return friends.size();
        }

        private static class FriendViewHolder extends RecyclerView.ViewHolder {
            final AvatarView avatarView;
            final TextView nameView;
            final CheckBox checkBox;

            FriendViewHolder(@NonNull View itemView) {
                super(itemView);
                avatarView = itemView.findViewById(R.id.friend_avatar);
                nameView = itemView.findViewById(R.id.friend_name);
                checkBox = itemView.findViewById(R.id.friend_checkbox);
            }
        }
    }

    private static class CaptionAdapter extends RecyclerView.Adapter<CaptionAdapter.CaptionViewHolder> {
        private final List<String> captions;
        private final CaptionClickListener listener;

        CaptionAdapter(List<String> captions, CaptionClickListener listener) {
            this.captions = captions;
            this.listener = listener;
        }

        @NonNull
        @Override
        public CaptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_caption_option, parent, false);
            return new CaptionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CaptionViewHolder holder, int position) {
            String caption = captions.get(position);
            holder.captionView.setText(caption);
            holder.itemView.setOnClickListener(view -> listener.onCaptionClicked(caption));
        }

        @Override
        public int getItemCount() {
            return captions.size();
        }

        private interface CaptionClickListener {
            void onCaptionClicked(String caption);
        }

        private static class CaptionViewHolder extends RecyclerView.ViewHolder {
            final TextView captionView;

            CaptionViewHolder(@NonNull View itemView) {
                super(itemView);
                captionView = itemView.findViewById(R.id.caption_option_text);
            }
        }
    }
}
