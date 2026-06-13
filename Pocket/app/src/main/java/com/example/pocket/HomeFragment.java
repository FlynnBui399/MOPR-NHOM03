package com.example.pocket;

import android.Manifest;
import android.content.ContentValues;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pocket.data.model.Photo;
import com.example.pocket.data.model.User;
import com.example.pocket.data.repository.ChatRepository;
import com.example.pocket.data.repository.UserRepository;
import com.example.pocket.ui.AvatarView;
import com.example.pocket.ui.PocketButton;
import com.example.pocket.utils.Constants;
import com.example.pocket.utils.ImageUtils;
import com.example.pocket.utils.SharedPrefManager;
import com.example.pocket.viewmodel.CameraViewModel;
import com.example.pocket.viewmodel.FeedViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {
    private static final String STATE_HOME_PAGE = "home_page";

    private PreviewView previewView;
    private View previewCard;
    private ImageView capturedImageView;
    private View captionPanel;
    private TextInputLayout captionInputLayout;
    private TextInputEditText captionInput;
    private PocketButton captureButton;
    private PocketButton retakeButton;
    private PocketButton suggestCaptionButton;
    private PocketButton sendPhotoButton;
    private PocketButton flipButton;
    private PocketButton flashButton;
    private PocketButton galleryButton;
    private PocketButton savePhotoButton;
    private PocketButton recipientPill;
    private CircleImageView profileAvatar;
    private RecyclerView homePager;
    private View historyHeader;
    private TextView historyCount;
    private PhotoHistoryAdapter historyAdapter;

    private CameraViewModel cameraViewModel;
    private FeedViewModel feedViewModel;
    private ChatRepository chatRepository;
    private FirebaseFirestore firestore;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private Camera camera;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean flashEnabled;
    private byte[] capturedJpegBytes;
    private final List<User> friends = new ArrayList<>();
    private final List<Photo> allTimelinePhotos = new ArrayList<>();
    private String selectedHistorySenderId;
    private String selectedHistorySenderName;
    private boolean waitingForCaptionOptions;
    private boolean cameraObserversBound;
    private boolean openHistoryAfterUpload;
    private int timelineCount;
    private int currentPageIndex;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.camera_permission_required,
                            Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    loadGalleryPhoto(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);
        feedViewModel = new ViewModelProvider(this).get(FeedViewModel.class);
        chatRepository = new ChatRepository();
        currentPageIndex = savedInstanceState == null
                ? 0
                : savedInstanceState.getInt(STATE_HOME_PAGE, 0);

        bindPager(view);
        observeTimeline();
    }

    private void bindPager(@NonNull View view) {
        homePager = view.findViewById(R.id.home_pager);
        homePager.setLayoutManager(new LinearLayoutManager(requireContext(),
                RecyclerView.VERTICAL, false));
        homePager.setItemViewCacheSize(2);
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(homePager);
        homePager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }
                View snappedView = snapHelper.findSnapView(recyclerView.getLayoutManager());
                if (snappedView != null) {
                    int position = recyclerView.getChildAdapterPosition(snappedView);
                    if (position != RecyclerView.NO_POSITION) {
                        currentPageIndex = position;
                    }
                }
            }
        });

        String userId = currentUserId();
        historyAdapter = new PhotoHistoryAdapter(userId, currentProfileAvatarUrl(),
                this::sendQuickReply,
                clicked -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).openProfile();
                    }
                }, clicked -> showHistoryFilterDialog(), getString(R.string.history_filter_everyone));
        homePager.setAdapter(new ConcatAdapter(new CameraPageAdapter(this::bindCameraPage),
                historyAdapter));
        restorePagerPosition();
    }

    private void bindCameraPage(@NonNull View view) {
        previewView = view.findViewById(R.id.camera_preview);
        previewCard = view.findViewById(R.id.camera_preview_card);
        capturedImageView = view.findViewById(R.id.captured_image);
        captionPanel = view.findViewById(R.id.caption_panel);
        captionInputLayout = view.findViewById(R.id.caption_input_layout);
        captionInput = view.findViewById(R.id.caption_input);
        captureButton = view.findViewById(R.id.capture_button);
        retakeButton = view.findViewById(R.id.retake_button);
        suggestCaptionButton = view.findViewById(R.id.suggest_caption_button);
        sendPhotoButton = view.findViewById(R.id.send_photo_button);
        flipButton = view.findViewById(R.id.flip_button);
        flashButton = view.findViewById(R.id.flash_button);
        galleryButton = view.findViewById(R.id.gallery_button);
        savePhotoButton = view.findViewById(R.id.save_photo_button);
        recipientPill = view.findViewById(R.id.recipient_pill);
        profileAvatar = view.findViewById(R.id.camera_profile_avatar);
        historyHeader = view.findViewById(R.id.history_header);
        historyCount = view.findViewById(R.id.history_count);
        historyCount.setText(String.valueOf(timelineCount));

        bindActions();
        applyHomeIconPolish();
        loadFriends();
        loadProfileAvatar();
        makePreviewSquare();
        restoreCameraPageState();
        observeCameraStateOnce();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void bindActions() {
        captureButton.setOnClickListener(view -> capturePhoto());
        retakeButton.setOnClickListener(view -> resetCapture());
        sendPhotoButton.setOnClickListener(view -> showFriendSelectorSheet());
        galleryButton.setOnClickListener(view -> galleryLauncher.launch("image/*"));
        savePhotoButton.setOnClickListener(view -> saveCapturedPhoto());
        historyHeader.setOnClickListener(view -> scrollToHistory());
        recipientPill.setOnClickListener(view -> {
            if (capturedJpegBytes == null) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openFriends();
                }
            } else {
                showFriendSelectorSheet();
            }
        });
        profileAvatar.setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openProfile();
            }
        });
        suggestCaptionButton.setOnClickListener(view -> {
            if (capturedJpegBytes == null) {
                return;
            }
            waitingForCaptionOptions = true;
            suggestCaptionButton.setLoading(true);
            cameraViewModel.generateCaption(capturedJpegBytes);
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

    private void observeCameraStateOnce() {
        if (cameraObserversBound) {
            return;
        }
        cameraObserversBound = true;
        cameraViewModel.getUploadStatus().observe(getViewLifecycleOwner(), status -> {
            boolean loading = status.getState() == CameraViewModel.UploadStatus.State.LOADING;
            sendPhotoButton.setLoading(loading);
            captureButton.setEnabled(!loading);
            retakeButton.setEnabled(!loading);
            suggestCaptionButton.setEnabled(!loading);

            if (status.getState() == CameraViewModel.UploadStatus.State.SUCCESS) {
                Toast.makeText(requireContext(), R.string.camera_upload_success,
                        Toast.LENGTH_SHORT).show();
                resetCapture();
                openHistoryAfterUpload = true;
                scrollToHistory();
            } else if (status.getState() == CameraViewModel.UploadStatus.State.ERROR) {
                String message = status.getMessage() == null
                        ? getString(R.string.camera_upload_error)
                        : status.getMessage();
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });

        cameraViewModel.getSuggestedCaptions().observe(getViewLifecycleOwner(), captions -> {
            if (!waitingForCaptionOptions || captions == null || captions.isEmpty()) {
                return;
            }
            waitingForCaptionOptions = false;
            suggestCaptionButton.setLoading(false);
            showCaptionOptionsSheet(captions);
        });
    }

    private void observeTimeline() {
        String userId = currentUserId();
        feedViewModel.getTimelinePhotos().observe(getViewLifecycleOwner(), photos -> {
            allTimelinePhotos.clear();
            if (photos != null) {
                allTimelinePhotos.addAll(photos);
            }
            applyHistoryFilter();
        });
        feedViewModel.loadTimeline(userId);
    }

    private void applyHistoryFilter() {
        List<Photo> visiblePhotos = new ArrayList<>();
        for (Photo photo : allTimelinePhotos) {
            if (selectedHistorySenderId == null
                    || selectedHistorySenderId.equals(photo.getSenderId())) {
                visiblePhotos.add(photo);
            }
        }
        timelineCount = visiblePhotos.size();
        historyAdapter.setFilterLabel(selectedHistorySenderName == null
                ? getString(R.string.history_filter_everyone)
                : selectedHistorySenderName);
        historyAdapter.submitList(visiblePhotos,
                    () -> {
                        if (historyCount != null) {
                            historyCount.setText(String.valueOf(timelineCount));
                        }
                        if (openHistoryAfterUpload && timelineCount > 0) {
                            openHistoryAfterUpload = false;
                            scrollToHistory();
                        } else {
                            restorePagerPosition();
                        }
                    });
    }

    private void showHistoryFilterDialog() {
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.history_filter_everyone));
        for (User friend : friends) {
            String name = friend.getDisplayName();
            labels.add(name == null || name.trim().isEmpty()
                    ? getString(R.string.camera_default_user) : name);
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.history_filter_title)
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        selectedHistorySenderId = null;
                        selectedHistorySenderName = null;
                    } else {
                        User friend = friends.get(which - 1);
                        selectedHistorySenderId = friend.getId();
                        selectedHistorySenderName = labels.get(which);
                    }
                    currentPageIndex = allTimelinePhotos.isEmpty() ? 0 : 1;
                    applyHistoryFilter();
                })
                .show();
    }

    private void makePreviewSquare() {
        previewCard.post(() -> {
            ViewGroup.LayoutParams params = previewCard.getLayoutParams();
            params.height = previewCard.getWidth();
            previewCard.setLayoutParams(params);
        });
    }

    private void scrollToHistory() {
        if (homePager != null && timelineCount > 0) {
            currentPageIndex = 1;
            homePager.smoothScrollToPosition(1);
        }
    }

    private void restorePagerPosition() {
        if (homePager == null || homePager.getAdapter() == null) {
            return;
        }
        int lastPosition = Math.max(0, homePager.getAdapter().getItemCount() - 1);
        int target = Math.min(currentPageIndex, lastPosition);
        homePager.post(() -> {
            if (homePager != null) {
                homePager.scrollToPosition(target);
            }
        });
    }

    private void sendQuickReply(@NonNull Photo photo,
                                @NonNull String content,
                                @NonNull String type,
                                @NonNull PhotoHistoryAdapter.ReplyResult result) {
        String currentUserId = currentUserId();
        String targetUserId = photo.getSenderId();
        if (targetUserId == null || targetUserId.trim().isEmpty()
                || currentUserId.equals(targetUserId)) {
            Toast.makeText(requireContext(), R.string.history_reply_own_photo,
                    Toast.LENGTH_SHORT).show();
            result.complete(false);
            return;
        }

        String chatId = chatRepository.getChatId(currentUserId, targetUserId);
        chatRepository.sendMessage(chatId, content, type, new UserRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void ignored) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.history_reply_sent,
                            Toast.LENGTH_SHORT).show();
                }
                result.complete(true);
            }

            @Override
            public void onError(@NonNull Exception error) {
                if (isAdded()) {
                    String message = error.getMessage() == null
                            ? getString(R.string.history_reply_failed)
                            : error.getMessage();
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
                result.complete(false);
            }
        });
    }

    private void startCamera() {
        if (!isAdded() || previewView == null) {
            return;
        }
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(requireContext());
        providerFuture.addListener(() -> {
            if (!isAdded() || previewView == null) {
                return;
            }
            try {
                ProcessCameraProvider provider = providerFuture.get();
                Preview preview = new Preview.Builder().build();
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(flashEnabled
                                ? ImageCapture.FLASH_MODE_ON
                                : ImageCapture.FLASH_MODE_OFF)
                        .build();
                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                provider.unbindAll();
                camera = provider.bindToLifecycle(getViewLifecycleOwner(), selector,
                        preview, imageCapture);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                applyFlashMode();
            } catch (Exception exception) {
                Toast.makeText(requireContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void capturePhoto() {
        if (imageCapture == null) {
            return;
        }
        captureButton.setLoading(true);
        File outputFile = new File(requireContext().getCacheDir(),
                "pocket-capture-" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();
        imageCapture.takePicture(options, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        try {
                            Bitmap bitmap = ImageUtils.uriToBitmap(requireContext(),
                                    Uri.fromFile(outputFile));
                            byte[] compressed = ImageUtils.compress(bitmap);
                            postToView(() -> onPhotoCaptured(bitmap, compressed));
                        } catch (IOException exception) {
                            postCaptureError(exception);
                        } finally {
                            if (outputFile.exists()) {
                                outputFile.delete();
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        postCaptureError(exception);
                    }
                });
    }

    private void loadGalleryPhoto(@NonNull Uri uri) {
        cameraExecutor.execute(() -> {
            try {
                Bitmap bitmap = ImageUtils.uriToBitmap(requireContext(), uri);
                byte[] compressed = ImageUtils.compress(bitmap);
                postToView(() -> onPhotoCaptured(bitmap, compressed));
            } catch (IOException exception) {
                postCaptureError(exception);
            }
        });
    }

    private void postCaptureError(@NonNull Exception exception) {
        postToView(() -> {
            captureButton.setLoading(false);
            Toast.makeText(requireContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void postToView(@NonNull Runnable action) {
        if (previewView != null) {
            previewView.post(() -> {
                if (isAdded()) {
                    action.run();
                }
            });
        }
    }

    private void onPhotoCaptured(@NonNull Bitmap bitmap, @NonNull byte[] compressed) {
        capturedJpegBytes = compressed;
        capturedImageView.setImageBitmap(bitmap);
        capturedImageView.setVisibility(View.VISIBLE);
        captionPanel.setVisibility(View.VISIBLE);
        captionInputLayout.setVisibility(View.VISIBLE);
        captureButton.setLoading(false);
        captureButton.setVisibility(View.GONE);
        galleryButton.setVisibility(View.GONE);
        flipButton.setVisibility(View.GONE);
        retakeButton.setVisibility(View.VISIBLE);
        suggestCaptionButton.setVisibility(View.VISIBLE);
        sendPhotoButton.setVisibility(View.VISIBLE);
        savePhotoButton.setVisibility(View.VISIBLE);
        showFriendSelectorSheet();
    }

    private void resetCapture() {
        capturedJpegBytes = null;
        capturedImageView.setImageDrawable(null);
        capturedImageView.setVisibility(View.GONE);
        captionInput.setText(null);
        captionPanel.setVisibility(View.GONE);
        captionInputLayout.setVisibility(View.GONE);
        captureButton.setVisibility(View.VISIBLE);
        galleryButton.setVisibility(View.VISIBLE);
        flipButton.setVisibility(View.VISIBLE);
        retakeButton.setVisibility(View.GONE);
        suggestCaptionButton.setVisibility(View.GONE);
        sendPhotoButton.setVisibility(View.GONE);
        savePhotoButton.setVisibility(View.GONE);
        updateCameraRecipientPill();
    }

    private void restoreCameraPageState() {
        if (capturedJpegBytes == null) {
            resetCapture();
            return;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(capturedJpegBytes, 0,
                capturedJpegBytes.length);
        capturedImageView.setImageBitmap(bitmap);
        capturedImageView.setVisibility(View.VISIBLE);
        captionPanel.setVisibility(View.VISIBLE);
        captionInputLayout.setVisibility(View.VISIBLE);
        captureButton.setVisibility(View.GONE);
        galleryButton.setVisibility(View.GONE);
        flipButton.setVisibility(View.GONE);
        retakeButton.setVisibility(View.VISIBLE);
        suggestCaptionButton.setVisibility(View.VISIBLE);
        sendPhotoButton.setVisibility(View.VISIBLE);
        savePhotoButton.setVisibility(View.VISIBLE);
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
        flashButton.setSelected(flashEnabled);
        flashButton.setContentDescription(getString(flashEnabled
                ? R.string.camera_flash_on
                : R.string.camera_flash_off));
    }

    private void loadProfileAvatar() {
        String avatarUrl = currentProfileAvatarUrl();
        Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.avatar_placeholder)
                .error(R.drawable.avatar_placeholder)
                .into(profileAvatar);
    }

    @Nullable
    private String currentProfileAvatarUrl() {
        String avatarUrl = SharedPrefManager.getInstance(requireContext()).getAvatarUrl();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if ((avatarUrl == null || avatarUrl.trim().isEmpty())
                && user != null && user.getPhotoUrl() != null) {
            avatarUrl = user.getPhotoUrl().toString();
        }
        return avatarUrl;
    }

    private void loadFriends() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        firestore.collection(Constants.COLLECTION_USERS)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    User user = document.toObject(User.class);
                    loadUsersByIds(user == null ? new ArrayList<>() : user.getFriendIds());
                });
    }

    private void loadUsersByIds(@Nullable List<String> userIds) {
        friends.clear();
        if (userIds == null || userIds.isEmpty()) {
            updateCameraRecipientPill();
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
                        updateCameraRecipientPill();
                    });
        }
    }

    private void showFriendSelectorSheet() {
        if (capturedJpegBytes == null || !isAdded()) {
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View content = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_friend_selector, null, false);
        RecyclerView friendList = content.findViewById(R.id.friend_selector_list);
        TextView emptyView = content.findViewById(R.id.friend_selector_empty);
        PocketButton sendButton = content.findViewById(R.id.friend_selector_send_button);
        Set<String> selectedIds = new HashSet<>();
        for (User friend : friends) {
            if (friend.getId() != null && !friend.getId().trim().isEmpty()) {
                selectedIds.add(friend.getId());
            }
        }
        FriendAdapter adapter = new FriendAdapter(friends, selectedIds, () -> {
            sendButton.setEnabled(!selectedIds.isEmpty());
            int count = selectedIds.size();
            recipientPill.setText(count == 0
                    ? getString(R.string.camera_friends)
                    : getResources().getQuantityString(
                            R.plurals.camera_selected_friends, count, count));
        });

        friendList.setLayoutManager(new LinearLayoutManager(requireContext()));
        friendList.setAdapter(adapter);
        emptyView.setVisibility(friends.isEmpty() ? View.VISIBLE : View.GONE);
        friendList.setVisibility(friends.isEmpty() ? View.GONE : View.VISIBLE);
        sendButton.setEnabled(!selectedIds.isEmpty());
        sendButton.setOnClickListener(view -> {
            String caption = captionInput.getText() == null
                    ? ""
                    : captionInput.getText().toString().trim();
            cameraViewModel.sendPhoto(capturedJpegBytes, currentUserId(), currentUserName(),
                    new ArrayList<>(selectedIds), caption);
            dialog.dismiss();
        });
        dialog.setContentView(content);
        dialog.show();
    }

    private void updateCameraRecipientPill() {
        if (recipientPill == null || capturedJpegBytes != null) {
            return;
        }
        int count = friends.size();
        recipientPill.setText(count == 0
                ? getString(R.string.camera_friends)
                : getResources().getQuantityString(R.plurals.camera_selected_friends,
                        count, count));
    }

    private void applyHomeIconPolish() {
        int white = ContextCompat.getColor(requireContext(), R.color.white);
        ColorStateList tint = ColorStateList.valueOf(white);
        flashButton.setIconTint(tint);
        galleryButton.setIconTint(tint);
        flipButton.setIconTint(tint);
        retakeButton.setIconTint(tint);
        savePhotoButton.setIconTint(tint);
        recipientPill.setTextColor(white);
    }

    private void saveCapturedPhoto() {
        if (capturedJpegBytes == null) {
            return;
        }
        byte[] bytes = capturedJpegBytes.clone();
        cameraExecutor.execute(() -> {
            try {
                String fileName = "Pocket-" + System.currentTimeMillis() + ".jpg";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/Pocket");
                    Uri uri = requireContext().getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) {
                        throw new IOException("Unable to create image file");
                    }
                    try (OutputStream output = requireContext().getContentResolver()
                            .openOutputStream(uri)) {
                        if (output == null) {
                            throw new IOException("Unable to open image file");
                        }
                        output.write(bytes);
                    }
                } else {
                    File directory = new File(requireContext().getExternalFilesDir(
                            Environment.DIRECTORY_PICTURES), "Pocket");
                    if (!directory.exists() && !directory.mkdirs()) {
                        throw new IOException("Unable to create Pocket album");
                    }
                    File file = new File(directory, fileName);
                    try (FileOutputStream output = new FileOutputStream(file)) {
                        output.write(bytes);
                    }
                    MediaScannerConnection.scanFile(requireContext(),
                            new String[]{file.getAbsolutePath()},
                            new String[]{"image/jpeg"}, null);
                }
                postToView(() -> Toast.makeText(requireContext(),
                        R.string.camera_saved_to_gallery, Toast.LENGTH_SHORT).show());
            } catch (Exception error) {
                postToView(() -> Toast.makeText(requireContext(),
                        R.string.camera_save_failed, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showCaptionOptionsSheet(@NonNull List<String> captions) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View content = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_caption_options, null, false);
        RecyclerView captionList = content.findViewById(R.id.caption_options_list);
        CaptionAdapter adapter = new CaptionAdapter(captions, caption -> {
            captionInput.setText(caption);
            captionInput.setSelection(caption.length());
            dialog.dismiss();
        });
        captionList.setLayoutManager(new LinearLayoutManager(requireContext()));
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_HOME_PAGE, currentPageIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        cameraObserversBound = false;
        previewView = null;
        previewCard = null;
        capturedImageView = null;
        captionPanel = null;
        homePager = null;
        historyAdapter = null;
        historyHeader = null;
        historyCount = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        super.onDestroy();
    }

    private static class CameraPageAdapter
            extends RecyclerView.Adapter<CameraPageAdapter.CameraPageViewHolder> {
        interface CameraPageListener {
            void onCameraPageBound(@NonNull View view);
        }

        private final CameraPageListener listener;

        CameraPageAdapter(@NonNull CameraPageListener listener) {
            this.listener = listener;
            setHasStableIds(true);
        }

        @NonNull
        @Override
        public CameraPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_home_camera_page, parent, false);
            return new CameraPageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CameraPageViewHolder holder, int position) {
            holder.setIsRecyclable(false);
            listener.onCameraPageBound(holder.itemView);
        }

        @Override
        public int getItemCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return 0L;
        }

        static class CameraPageViewHolder extends RecyclerView.ViewHolder {
            CameraPageViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
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
            return new FriendViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend_selector, parent, false));
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
            holder.checkBox.setOnCheckedChangeListener((button, checked) -> {
                if (id != null) {
                    if (checked) selectedIds.add(id); else selectedIds.remove(id);
                }
                selectionChanged.run();
            });
            holder.itemView.setOnClickListener(view ->
                    holder.checkBox.setChecked(!holder.checkBox.isChecked()));
        }

        @Override
        public int getItemCount() {
            return friends.size();
        }

        static class FriendViewHolder extends RecyclerView.ViewHolder {
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
            return new CaptionViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_caption_option, parent, false));
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

        interface CaptionClickListener {
            void onCaptionClicked(String caption);
        }

        static class CaptionViewHolder extends RecyclerView.ViewHolder {
            final TextView captionView;

            CaptionViewHolder(@NonNull View itemView) {
                super(itemView);
                captionView = itemView.findViewById(R.id.caption_option_text);
            }
        }
    }
}
