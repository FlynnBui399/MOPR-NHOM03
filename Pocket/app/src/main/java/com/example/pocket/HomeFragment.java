package com.example.pocket;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.inputmethod.InputMethodManager;

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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.widget.GridLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.Timestamp;
import java.util.HashMap;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import nl.dionsegijn.konfetti.xml.KonfettiView;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Position;
import nl.dionsegijn.konfetti.core.models.Size;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.models.Shape;
import java.util.concurrent.TimeUnit;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Color;

import com.bumptech.glide.Glide;
import com.example.pocket.data.model.Photo;
import com.example.pocket.data.model.User;
import com.example.pocket.data.remote.CloudinaryService;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final String STATE_HOME_PAGE = "home_page";
    private static final String STATE_HOME_POST_ID = "home_post_id";

    private enum HomeMode {
        CAMERA,
        POSTS
    }

    private PreviewView previewView;
    private View previewCard;
    private ImageView capturedImageView;
    private View captionPanel;
    private TextInputLayout captionInputLayout;
    private TextInputEditText captionInput;
    private PocketButton captureButton;
    private PocketButton retakeButton;
    private PocketButton suggestCaptionButton;
    private ImageButton sendPhotoButton;
    private PocketButton flipButton;
    private PocketButton flashButton;
    private PocketButton galleryButton;
    private PocketButton savePhotoButton;
    private PocketButton recipientPill;
    private CircleImageView profileAvatar;
    private View captureButtonSurface;
    private View captureRecipientsContainer;
    private RecyclerView captureRecipientList;
    private RecipientSelectionAdapter recipientSelectionAdapter;
    private ViewPager2 homePager;
    private View historyHeader;
    private TextView historyCount;
    private PhotoHistoryAdapter historyAdapter;

    private CameraViewModel cameraViewModel;
    private FeedViewModel feedViewModel;
    private ChatRepository chatRepository;
    private FirebaseFirestore firestore;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private androidx.camera.video.VideoCapture<androidx.camera.video.Recorder> videoCapture;
    private androidx.camera.video.Recording activeRecording;
    private boolean isRecordingVideo = false;
    private final android.os.Handler recordProgressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable stopVideoRecordingRunnable = this::stopVideoRecording;
    private int recordProgressSeconds = 0;
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
    private int unseenCount;
    private int currentPageIndex;
    private String currentPostId;
    private int friendsLoadGeneration;
    private HomeMode homeMode = HomeMode.CAMERA;
    private boolean homeTabActive = true;
    private boolean cameraErrorShown;
    private final Set<String> locallySeenPhotoIds = new HashSet<>();

    // Persistent reply bar & Emoji rain
    private View replyBar;
    private android.widget.EditText etQuickReply;
    private android.widget.ImageView btnSendReply;
    private TextView btnReactionHearts;
    private TextView btnReactionShock;
    private TextView btnReactionFire;
    private TextView btnReactionMore;
    private KonfettiView konfettiView;
    private Photo currentPhoto;
    private String currentReceiverId;
    private boolean keyboardVisible;
    private android.widget.ProgressBar videoRecordProgress;
    private ChatRepository chatRepositoryHome = new ChatRepository();

    // Mode toggle and recording progress animation
    private View modeToggle;
    private ImageButton btnModePhoto;
    private ImageButton btnModeVideo;
    private boolean isVideoMode = false;
    private com.example.pocket.ui.RecordingBorderView recordingBorderView;
    private android.animation.ValueAnimator recordingBorderAnimator;

    private final ActivityResultLauncher<Intent> videoPreviewLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    resetCapture();
                    scrollToHistory();
                } else {
                    resetCapture();
                }
            });

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
        currentPostId = savedInstanceState == null
                ? null
                : savedInstanceState.getString(STATE_HOME_POST_ID);

        // Bind fixed reply bar and Konfetti
        replyBar = view.findViewById(R.id.replyBar);
        etQuickReply = view.findViewById(R.id.etQuickReply);
        btnSendReply = view.findViewById(R.id.btnSendReply);
        btnReactionHearts = view.findViewById(R.id.btnReactionHearts);
        btnReactionShock = view.findViewById(R.id.btnReactionShock);
        btnReactionFire = view.findViewById(R.id.btnReactionFire);
        btnReactionMore = view.findViewById(R.id.btnReactionMore);
        konfettiView = view.findViewById(R.id.konfettiView);
        View bottomNavPlaceholder = view.findViewById(R.id.bottomNavigation);
        if (bottomNavPlaceholder != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNavPlaceholder, (v, insets) -> {
                int navHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom;
                float density = v.getResources().getDisplayMetrics().density;
                ViewGroup.LayoutParams lp = v.getLayoutParams();
                lp.height = navHeight + Math.round(84 * density);
                v.setLayoutParams(lp);
                return insets;
            });
        }

        // Apply press animations
        com.example.pocket.utils.ViewUtils.applyPressAnimation(btnSendReply);
        com.example.pocket.utils.ViewUtils.applyPressAnimation(btnReactionHearts);
        com.example.pocket.utils.ViewUtils.applyPressAnimation(btnReactionShock);
        com.example.pocket.utils.ViewUtils.applyPressAnimation(btnReactionFire);
        com.example.pocket.utils.ViewUtils.applyPressAnimation(btnReactionMore);

        // TextWatcher to toggle send vs emojis
        etQuickReply.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean empty = s.toString().trim().isEmpty();
                btnSendReply.setVisibility(empty ? View.GONE : View.VISIBLE);
                btnReactionHearts.setVisibility(empty ? View.VISIBLE : View.GONE);
                btnReactionShock.setVisibility(empty ? View.VISIBLE : View.GONE);
                btnReactionFire.setVisibility(empty ? View.VISIBLE : View.GONE);
                btnReactionMore.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // Click listeners
        btnSendReply.setOnClickListener(v -> sendFixedTextReply());
        View.OnClickListener emojiClickListener = v -> {
            String emoji = v.getTag() != null ? (String) v.getTag() : ((TextView) v).getText().toString();
            sendFixedEmojiReply(emoji);
        };
        btnReactionHearts.setOnClickListener(emojiClickListener);
        btnReactionShock.setOnClickListener(emojiClickListener);
        btnReactionFire.setOnClickListener(emojiClickListener);
        btnReactionMore.setOnClickListener(v -> showFixedEmojiPicker());

        etQuickReply.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendFixedTextReply();
                return true;
            }
            return false;
        });

        // Lift bar above keyboard
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(replyBar, (v, insets) -> {
            int imeHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime()).bottom;
            int navHeight = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setTranslationY(-(imeHeight > 0 ? imeHeight : 0));
            return insets;
        });

        // Keyboard visibility and pager lock
        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!isAdded()) return;
            Rect r = new Rect();
            view.getWindowVisibleDisplayFrame(r);
            int screenHeight = view.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;
            boolean visible = keypadHeight > screenHeight * 0.15;
            if (visible != keyboardVisible) {
                keyboardVisible = visible;
                if (homePager != null) {
                    homePager.setUserInputEnabled(!keyboardVisible);
                }
            }
        });

        bindFixedTopBar(view);
        bindPager(view);
        observeTimeline();
    }

    private void bindFixedTopBar(@NonNull View view) {
        flashButton = view.findViewById(R.id.flash_button);
        recipientPill = view.findViewById(R.id.recipient_pill);
        profileAvatar = view.findViewById(R.id.camera_profile_avatar);
        applyHomeIconPolish();
        loadProfileAvatar();
        profileAvatar.setOnClickListener(clicked -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openProfile();
            }
        });
        recipientPill.setOnClickListener(clicked -> {
            if (homeMode == HomeMode.POSTS || currentPageIndex > 0) {
                showHistoryFilterSheet();
            } else if (capturedJpegBytes == null && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openFriends();
            }
        });
        flashButton.setOnClickListener(clicked -> {
            flashEnabled = !flashEnabled;
            applyFlashMode();
        });
        updateTopBar();
    }

    private void bindPager(@NonNull View view) {
        homePager = view.findViewById(R.id.home_pager);
        homePager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        homePager.setUserInputEnabled(capturedJpegBytes == null);

        // Task 5b ViewPager2 scale transformer
        homePager.setPageTransformer((page, position) -> {
            float scale = 1f - 0.05f * Math.abs(position);
            page.setScaleX(scale);
            page.setScaleY(scale);
            page.setAlpha(1f - 0.3f * Math.abs(position));
        });

        // ViewPager2 page change callback
        homePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPageIndex = position;
                updateCurrentPostId(position);
                updateModeForPage(position);
                markVisiblePostSeen(position);
                updateReplyBarVisibilityForCurrentPage(position);
            }
        });

        String userId = currentUserId();
        historyAdapter = new PhotoHistoryAdapter(userId,
                photo -> PostActivitySheet.show(requireContext(), photo));
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
        galleryButton = view.findViewById(R.id.gallery_button);
        savePhotoButton = view.findViewById(R.id.save_photo_button);
        captureButtonSurface = view.findViewById(R.id.capture_button_surface);
        captureRecipientsContainer = view.findViewById(R.id.capture_recipients_container);
        captureRecipientList = view.findViewById(R.id.capture_recipient_list);
        historyHeader = view.findViewById(R.id.history_header);
        historyCount = view.findViewById(R.id.history_count);
        historyCount.setText(String.valueOf(timelineCount));
        videoRecordProgress = view.findViewById(R.id.video_record_progress);

        modeToggle = view.findViewById(R.id.modeToggle);
        btnModePhoto = view.findViewById(R.id.btnModePhoto);
        btnModeVideo = view.findViewById(R.id.btnModeVideo);
        recordingBorderView = view.findViewById(R.id.recording_border_view);

        if (btnModePhoto != null) {
            btnModePhoto.setOnClickListener(v -> setCameraMode(false));
        }
        if (btnModeVideo != null) {
            btnModeVideo.setOnClickListener(v -> setCameraMode(true));
        }
        setCameraMode(false);

        // Apply press animations to camera controls (excluding captureButton which has custom touch handler)
        com.example.pocket.utils.ViewUtils.applyPressAnimation(retakeButton);
        com.example.pocket.utils.ViewUtils.applyPressAnimation(suggestCaptionButton);
        com.example.pocket.utils.ViewUtils.applyPressAnimation(sendPhotoButton);
        com.example.pocket.utils.ViewUtils.applyPressAnimation(flipButton);
        com.example.pocket.utils.ViewUtils.applyPressAnimation(galleryButton);
        com.example.pocket.utils.ViewUtils.applyPressAnimation(savePhotoButton);

        bindActions();
        applyHomeIconPolish();
        recipientSelectionAdapter = new RecipientSelectionAdapter((allSelected, selectedIds) ->
                updateCapturedRecipientLabel());
        captureRecipientList.setLayoutManager(new LinearLayoutManager(requireContext(),
                RecyclerView.HORIZONTAL, false));
        captureRecipientList.setAdapter(recipientSelectionAdapter);
        recipientSelectionAdapter.submitFriends(friends);
        centerCaptureRecipients();
        loadFriends();
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
        captionInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                captionInput.setHint(null);
            } else if (captionInput.getText() == null
                    || captionInput.getText().toString().trim().isEmpty()) {
                captionInput.setHint(R.string.camera_add_message);
            }
        });

        // Click to take photo
        captureButton.setOnClickListener(view -> {
            if (!isVideoMode) {
                capturePhoto();
            }
        });

        // Custom touch listener for click zoom animation + video recording long-press (Task 2 & 9)
        captureButton.setOnTouchListener(new View.OnTouchListener() {
            private static final long LONG_PRESS_THRESHOLD = 300; // ms
            private long downTime;
            private boolean isLongPressed = false;
            private final Runnable longPressRunnable = () -> {
                if (isVideoMode) {
                    isLongPressed = true;
                    startVideoRecording();
                }
            };

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        downTime = System.currentTimeMillis();
                        isLongPressed = false;
                        v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(80).start();
                        if (isVideoMode) {
                            v.postDelayed(longPressRunnable, LONG_PRESS_THRESHOLD);
                        }
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.removeCallbacks(longPressRunnable);
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        if (isVideoMode) {
                            if (isLongPressed) {
                                stopVideoRecording();
                            } else {
                                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                                    Toast.makeText(requireContext(), "Hold to record video", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                                v.performClick();
                            }
                        }
                        break;
                }
                return true;
            }
        });

        retakeButton.setOnClickListener(view -> resetCapture());
        sendPhotoButton.setOnClickListener(view -> sendCapturedPhoto());
        galleryButton.setOnClickListener(view -> galleryLauncher.launch("image/*"));
        savePhotoButton.setOnClickListener(view -> saveCapturedPhoto());
        historyHeader.setOnClickListener(view -> scrollToHistory());
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
    }

    private void observeCameraStateOnce() {
        if (cameraObserversBound) {
            return;
        }
        cameraObserversBound = true;
        cameraViewModel.getUploadStatus().observe(getViewLifecycleOwner(), status -> {
            boolean loading = status.getState() == CameraViewModel.UploadStatus.State.LOADING;
            sendPhotoButton.setEnabled(!loading);
            sendPhotoButton.setAlpha(loading ? 0.55f : 1f);
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
                Log.e(TAG, "Photo upload failed: " + status.getMessage());
                Toast.makeText(requireContext(), R.string.camera_upload_error,
                        Toast.LENGTH_LONG).show();
            }
        });

        cameraViewModel.getCaptionSuggestion().observe(getViewLifecycleOwner(), suggestion -> {
            List<String> captions = suggestion == null ? null : suggestion.getCaptions();
            if (!waitingForCaptionOptions || captions == null || captions.isEmpty()) {
                return;
            }
            waitingForCaptionOptions = false;
            suggestCaptionButton.setLoading(false);

            // Logging safe details to Logcat
            String source = suggestion.getSource().name();
            int numCaptions = captions.size();
            String modelName = Constants.GEMINI_CAPTION_MODEL;
            boolean isApiKeyPresent = Constants.GEMINI_API_KEY != null && !Constants.GEMINI_API_KEY.trim().isEmpty();
            String errorMsg = suggestion.getErrorMessage();

            android.util.Log.d("CaptionDisplay", "--- Suggest Caption Metrics ---");
            android.util.Log.d("CaptionDisplay", "Source: " + source);
            android.util.Log.d("CaptionDisplay", "Count: " + numCaptions);
            android.util.Log.d("CaptionDisplay", "Model: " + modelName);
            android.util.Log.d("CaptionDisplay", "API Key Present: " + isApiKeyPresent);
            if (suggestion.getSource() == CameraViewModel.CaptionSource.FALLBACK) {
                android.util.Log.w("CaptionDisplay", "Error message: " + (errorMsg != null ? errorMsg : "Unknown/Internal error"));
            }
            android.util.Log.d("CaptionDisplay", "-------------------------------");

            // Display Toast notification for development and user awareness
            if (suggestion.getSource() == CameraViewModel.CaptionSource.AI) {
                Toast.makeText(requireContext(), R.string.camera_caption_ai_generated, Toast.LENGTH_SHORT).show();
            } else if (suggestion.getSource() == CameraViewModel.CaptionSource.CACHE) {
                Toast.makeText(requireContext(), R.string.camera_caption_loaded_cache, Toast.LENGTH_SHORT).show();
            } else if (suggestion.getSource() == CameraViewModel.CaptionSource.FALLBACK) {
                Toast.makeText(requireContext(), R.string.camera_caption_fallback_toast, Toast.LENGTH_SHORT).show();
            }

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
        if (selectedHistorySenderId != null && visiblePhotos.isEmpty()
                && !allTimelinePhotos.isEmpty()) {
            selectedHistorySenderId = null;
            selectedHistorySenderName = null;
            visiblePhotos.addAll(allTimelinePhotos);
        }
        timelineCount = visiblePhotos.size();
        updateUnseenCount();
        updateTopBar();
        historyAdapter.submitList(visiblePhotos,
                    () -> {
                        updateHistoryBadge();
                        if (openHistoryAfterUpload && timelineCount > 0) {
                            openHistoryAfterUpload = false;
                            scrollToHistory();
                        } else {
                            restorePagerPosition();
                        }
                        updateReplyBarVisibilityForCurrentPage(currentPageIndex);
                    });
    }

    private void showHistoryFilterSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View content = getLayoutInflater()
                .inflate(R.layout.bottom_sheet_history_filter, null, false);
        RecyclerView list = content.findViewById(R.id.history_filter_list);
        HistoryFilterAdapter adapter = new HistoryFilterAdapter(friends,
                selectedHistorySenderId, user -> {
                    selectedHistorySenderId = user == null ? null : user.getId();
                    selectedHistorySenderName = user == null ? null
                            : (user.getDisplayName() == null || user.getDisplayName().trim().isEmpty()
                            ? getString(R.string.camera_default_user) : user.getDisplayName());
                    currentPostId = null;
                    currentPageIndex = 1;
                    applyHistoryFilter();
                    dialog.dismiss();
                });
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        dialog.setContentView(content);
        dialog.show();
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
            updateCurrentPostId(1);
            updateModeForPage(1);
            homePager.setCurrentItem(1, true);
        }
    }

    public void returnToCameraMode() {
        homeTabActive = true;
        if (homeMode == HomeMode.POSTS && homePager != null) {
            currentPageIndex = 0;
            currentPostId = null;
            homeMode = HomeMode.CAMERA;
            updateTopBar();
            homePager.setCurrentItem(0, true);
        }
    }

    public boolean isInPostMode() {
        return homeMode == HomeMode.POSTS;
    }

    public void onHomeTabUnselected() {
        captureCurrentPagerPosition();
        homeTabActive = false;
    }

    public void onHomeTabSelectedFromAnotherTab() {
        homeTabActive = false;
        if (homePager == null || homePager.getAdapter() == null) {
            return;
        }
        int target = resolveRestorePage();
        homePager.post(() -> {
            if (homePager == null) {
                return;
            }
            homePager.setCurrentItem(target, false);
            currentPageIndex = target;
            updateCurrentPostId(target);
            updateModeForPage(target);
            markVisiblePostSeen(target);
            homeTabActive = true;
        });
    }

    private void captureCurrentPagerPosition() {
        if (homePager == null) {
            return;
        }
        int position = homePager.getCurrentItem();
        // A hidden pager can briefly report page 0 while Home is being switched out.
        // Keep the confirmed post position in that transition.
        if (position == 0 && homeMode == HomeMode.POSTS && currentPageIndex > 0) {
            return;
        }
        currentPageIndex = position;
        updateCurrentPostId(position);
        updateModeForPage(position);
    }

    private void updateCurrentPostId(int pageIndex) {
        if (pageIndex <= 0 || historyAdapter == null) {
            currentPostId = null;
            return;
        }
        int adapterIndex = pageIndex - 1;
        if (adapterIndex >= 0 && adapterIndex < historyAdapter.getCurrentList().size()) {
            currentPostId = historyAdapter.getCurrentList().get(adapterIndex).getId();
        }
    }

    private int resolveRestorePage() {
        if (homePager == null || homePager.getAdapter() == null) {
            return 0;
        }
        if (currentPostId != null && historyAdapter != null) {
            for (int index = 0; index < historyAdapter.getCurrentList().size(); index++) {
                if (currentPostId.equals(historyAdapter.getCurrentList().get(index).getId())) {
                    return index + 1;
                }
            }
        }
        int lastPosition = Math.max(0, homePager.getAdapter().getItemCount() - 1);
        return Math.min(currentPageIndex, lastPosition);
    }

    private void updateModeForPage(int pageIndex) {
        homeMode = pageIndex == 0 ? HomeMode.CAMERA : HomeMode.POSTS;
        updateTopBar();
    }

    private void updateTopBar() {
        if (recipientPill == null || flashButton == null) {
            return;
        }
        if (homeMode == HomeMode.POSTS) {
            flashButton.setVisibility(View.INVISIBLE);
            recipientPill.setText(selectedHistorySenderName == null
                    ? getString(R.string.history_filter_everyone) : selectedHistorySenderName);
        } else if (capturedJpegBytes != null) {
            flashButton.setVisibility(View.INVISIBLE);
            recipientPill.setText(R.string.camera_send_to);
        } else {
            flashButton.setVisibility(View.VISIBLE);
            updateCameraRecipientPill();
        }
    }

    private void updateUnseenCount() {
        String userId = currentUserId();
        int count = 0;
        for (Photo photo : allTimelinePhotos) {
            boolean ownPhoto = userId.equals(photo.getSenderId());
            boolean seen = locallySeenPhotoIds.contains(photo.getId())
                    || photo.getSeenBy() != null && photo.getSeenBy().contains(userId);
            if (!ownPhoto && !seen) {
                count++;
            }
        }
        unseenCount = count;
        updateHistoryBadge();
    }

    private void updateHistoryBadge() {
        if (historyCount == null) {
            return;
        }
        historyCount.setText(String.valueOf(unseenCount));
        historyCount.setVisibility(unseenCount > 0 ? View.VISIBLE : View.GONE);
    }

    private void markVisiblePostSeen(int pageIndex) {
        if (pageIndex <= 0 || historyAdapter == null) {
            return;
        }
        int adapterIndex = pageIndex - 1;
        if (adapterIndex < 0 || adapterIndex >= historyAdapter.getCurrentList().size()) {
            return;
        }
        Photo photo = historyAdapter.getCurrentList().get(adapterIndex);
        String userId = currentUserId();
        if (userId.equals(photo.getSenderId()) || photo.getId() == null) {
            return;
        }
        List<String> seenBy = photo.getSeenBy();
        if (seenBy != null && seenBy.contains(userId)) {
            return;
        }
        locallySeenPhotoIds.add(photo.getId());
        if (seenBy == null) {
            seenBy = new ArrayList<>();
        }
        seenBy.add(userId);
        photo.setSeenBy(seenBy);
        unseenCount = Math.max(0, unseenCount - 1);
        updateHistoryBadge();
        historyAdapter.notifyItemChanged(adapterIndex);
        feedViewModel.markPhotoSeen(photo.getId(), userId);
    }

    private void restorePagerPosition() {
        if (homePager == null || homePager.getAdapter() == null) {
            return;
        }
        int target = resolveRestorePage();
        homePager.post(() -> {
            if (homePager != null) {
                homePager.setCurrentItem(target, false);
                currentPageIndex = target;
                updateCurrentPostId(target);
                updateModeForPage(target);
                markVisiblePostSeen(target);
            }
        });
    }

    private void sendFixedTextReply() {
        if (currentReceiverId == null || currentPhoto == null) return;
        String text = etQuickReply.getText().toString().trim();
        if (text.isEmpty()) return;

        etQuickReply.setText("");
        hideKeyboard(etQuickReply);

        String chatId = chatRepositoryHome.getChatId(currentUserId(), currentReceiverId);
        
        chatRepositoryHome.sendPhotoReply(chatId, text, currentPhoto.getThumbnailUrl(), currentPhoto.getId(), new UserRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void ignored) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.history_reply_sent, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onError(@NonNull Exception error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.history_reply_failed, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void sendFixedEmojiReply(String emoji) {
        if (currentReceiverId == null || currentPhoto == null) return;
        
        triggerEmojiRain(konfettiView, emoji);

        String chatId = chatRepositoryHome.getChatId(currentUserId(), currentReceiverId);
        
        String quotedUrl = currentPhoto.getThumbnailUrl();
        if (quotedUrl == null || quotedUrl.trim().isEmpty()) {
            quotedUrl = currentPhoto.getImageUrl();
        }
        if (quotedUrl == null) {
            quotedUrl = "";
        }

        chatRepositoryHome.sendPhotoReply(chatId, emoji, quotedUrl, currentPhoto.getId(), new UserRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void ignored) {
                // Update parent chat document
                java.util.Map<String, Object> chatUpdates = new java.util.HashMap<>();
                chatUpdates.put("lastMessage", emoji);
                chatUpdates.put("lastUpdated", com.google.firebase.Timestamp.now());
                FirebaseFirestore.getInstance().collection("chats").document(chatId)
                        .set(chatUpdates, SetOptions.merge());

                if (currentPhoto.getId() != null) {
                    feedViewModel.reactToPhoto(currentPhoto.getId(), currentUserId(), emoji);
                    Map<String, String> reactions = currentPhoto.getReactions();
                    if (reactions == null) {
                        reactions = new java.util.LinkedHashMap<>();
                    }
                    reactions.put(currentUserId(), emoji);
                    currentPhoto.setReactions(reactions);
                }
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.history_reply_sent, Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onError(@NonNull Exception error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.history_reply_failed, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showFixedEmojiPicker() {
        if (currentReceiverId == null || currentPhoto == null) return;
        Context context = requireContext();
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View content = getLayoutInflater().inflate(R.layout.bottom_sheet_emoji_picker, null, false);
        GridLayout grid = content.findViewById(R.id.history_emoji_grid);
        
        String[] pickerEmojis = {
                "\uD83D\uDC95", "\uD83D\uDE31", "\uD83D\uDD25", "\uD83D\uDE02",
                "\uD83E\uDD23", "\uD83D\uDE0D", "\uD83E\uDD79", "\uD83D\uDE2D",
                "\uD83E\uDEF6", "\uD83D\uDC40", "\uD83D\uDC80", "\uD83D\uDE0E",
                "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83C\uDF89", "\uD83D\uDC90",
                "\u2728", "\uD83D\uDE2E\u200D\uD83D\uDCA8", "\uD83E\uDD21", "\uD83D\uDE0B",
                "\u2764\uFE0F\u200D\uD83D\uDD25", "\uD83D\uDE24", "\uD83D\uDE43", "\uD83E\uDD70",
                "\uD83E\uDD1D", "\uD83E\uDD29", "\uD83D\uDE4C", "\uD83E\uDD73"
        };

        for (int index = 0; index < pickerEmojis.length; index++) {
            String emoji = pickerEmojis[index];
            TextView option = new TextView(context);
            option.setText(emoji);
            option.setTextSize(27f);
            option.setGravity(android.view.Gravity.CENTER);
            option.setBackgroundResource(R.drawable.bg_filter_row);
            option.setContentDescription(emoji);
            int margin = Math.round(4 * getResources().getDisplayMetrics().density);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(index / 4), GridLayout.spec(index % 4, 1, 1f));
            params.width = 0;
            params.height = Math.round(58 * getResources().getDisplayMetrics().density);
            params.setMargins(margin, margin, margin, margin);
            option.setLayoutParams(params);
            option.setOnClickListener(clicked -> {
                dialog.dismiss();
                sendFixedEmojiReply(emoji);
            });
            grid.addView(option);
        }
        dialog.setContentView(content);
        dialog.show();
    }

    private void triggerEmojiRain(KonfettiView konfettiView, String emoji) {
        if (konfettiView == null) return;
        try {
            int size = 80;
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            Paint paint = new Paint();
            paint.setTextSize(50f);
            paint.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = paint.getFontMetrics();
            float y = (size / 2f) - ((fm.descent + fm.ascent) / 2f);
            canvas.drawText(emoji, size / 2f, y, paint);
            Drawable drawable = new BitmapDrawable(getResources(), bmp);

            konfettiView.start(
                new PartyFactory(new Emitter(1500L, TimeUnit.MILLISECONDS).max(80))
                    .angle(270).spread(40)
                    .shapes(java.util.Collections.singletonList(new Shape.DrawableShape(drawable, false, false)))
                    .setSpeedBetween(1f, 4f).setDamping(0.85f).sizes(new Size(24, 5f, 0.2f))
                    .position(new Position.Relative(0.0, 0.0)
                        .between(new Position.Relative(1.0, 0.0)))
                    .build()
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to run emoji rain", e);
        }
    }

    private void startCamera() {
        if (!isAdded() || previewView == null) {
            return;
        }
        PreviewView activePreviewView = previewView;
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(requireContext());
        providerFuture.addListener(() -> {
            if (!isAdded() || getView() == null || previewView != activePreviewView
                    || !getViewLifecycleOwner().getLifecycle().getCurrentState()
                    .isAtLeast(Lifecycle.State.STARTED)) {
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
                
                androidx.camera.video.Recorder recorder = new androidx.camera.video.Recorder.Builder()
                        .setExecutor(ContextCompat.getMainExecutor(requireContext()))
                        .setQualitySelector(androidx.camera.video.QualitySelector.from(androidx.camera.video.Quality.SD))
                        .setTargetVideoEncodingBitRate(4000000)
                        .build();
                videoCapture = androidx.camera.video.VideoCapture.withOutput(recorder);

                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                provider.unbindAll();
                camera = provider.bindToLifecycle(getViewLifecycleOwner(), selector,
                        preview, imageCapture, videoCapture);
                if (previewView != activePreviewView) {
                    return;
                }
                preview.setSurfaceProvider(activePreviewView.getSurfaceProvider());
                if (flashButton != null) {
                    applyFlashMode();
                }
                cameraErrorShown = false;
            } catch (Exception exception) {
                Log.e(TAG, "Unable to start CameraX preview", exception);
                if (!cameraErrorShown && isAdded() && isResumed()
                        && homeMode == HomeMode.CAMERA) {
                    cameraErrorShown = true;
                    Toast.makeText(requireContext(), R.string.camera_start_failed,
                            Toast.LENGTH_SHORT).show();
                }
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
        Log.e(TAG, "Photo capture failed", exception);
        postToView(() -> {
            if (captureButton != null) {
                captureButton.setLoading(false);
            }
            Toast.makeText(requireContext(), R.string.camera_capture_failed,
                    Toast.LENGTH_LONG).show();
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
        captureButtonSurface.setVisibility(View.GONE);
        galleryButton.setVisibility(View.GONE);
        flipButton.setVisibility(View.GONE);
        retakeButton.setVisibility(View.VISIBLE);
        suggestCaptionButton.setVisibility(View.VISIBLE);
        styleCapturedActionButtons();
        sendPhotoButton.setVisibility(View.VISIBLE);
        savePhotoButton.setVisibility(View.VISIBLE);
        captureRecipientsContainer.setVisibility(View.VISIBLE);
        historyHeader.setVisibility(View.GONE);
        if (modeToggle != null) {
            modeToggle.setVisibility(View.GONE);
        }
        recipientSelectionAdapter.selectAll();
        captureRecipientList.scrollToPosition(0);
        centerCaptureRecipients();
        updateTopBar();
    }

    private void resetCapture() {
        capturedJpegBytes = null;
        waitingForCaptionOptions = false;
        capturedImageView.setImageDrawable(null);
        capturedImageView.setVisibility(View.GONE);
        captionInput.setText(null);
        captionInput.clearFocus();
        captionInput.setHint(R.string.camera_add_message);
        hideKeyboard(captionInput);
        captionPanel.setVisibility(View.GONE);
        captionInputLayout.setVisibility(View.GONE);
        captureButton.setLoading(false);
        captureButton.setVisibility(View.VISIBLE);
        captureButtonSurface.setVisibility(View.VISIBLE);
        captureButtonSurface.setBackgroundResource(R.drawable.bg_shutter_button);
        galleryButton.setVisibility(View.VISIBLE);
        flipButton.setVisibility(View.VISIBLE);
        retakeButton.setVisibility(View.GONE);
        suggestCaptionButton.setVisibility(View.GONE);
        suggestCaptionButton.setLoading(false);
        sendPhotoButton.setVisibility(View.GONE);
        savePhotoButton.setVisibility(View.GONE);
        captureRecipientsContainer.setVisibility(View.GONE);
        historyHeader.setVisibility(View.VISIBLE);
        if (modeToggle != null) {
            modeToggle.setVisibility(View.VISIBLE);
        }
        if (recipientSelectionAdapter != null) {
            recipientSelectionAdapter.selectAll();
        }
        homeMode = HomeMode.CAMERA;
        updateTopBar();
    }

    private void hideKeyboard(@NonNull View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
        captureButtonSurface.setVisibility(View.GONE);
        galleryButton.setVisibility(View.GONE);
        flipButton.setVisibility(View.GONE);
        retakeButton.setVisibility(View.VISIBLE);
        suggestCaptionButton.setVisibility(View.VISIBLE);
        styleCapturedActionButtons();
        sendPhotoButton.setVisibility(View.VISIBLE);
        savePhotoButton.setVisibility(View.VISIBLE);
        captureRecipientsContainer.setVisibility(View.VISIBLE);
        historyHeader.setVisibility(View.GONE);
        if (modeToggle != null) {
            modeToggle.setVisibility(View.GONE);
        }
        captureRecipientList.scrollToPosition(0);
        centerCaptureRecipients();
        updateTopBar();
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
        int generation = ++friendsLoadGeneration;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            publishFriends(generation, new ArrayList<>());
            return;
        }
        firestore.collection(Constants.COLLECTION_USERS)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (generation != friendsLoadGeneration) {
                        return;
                    }
                    User user = document.toObject(User.class);
                    loadUsersByIds(user == null ? new ArrayList<>() : user.getFriendIds(),
                            generation);
                });
    }

    private void loadUsersByIds(@Nullable List<String> userIds, int generation) {
        Set<String> uniqueIds = new LinkedHashSet<>();
        if (userIds != null) {
            for (String userId : userIds) {
                if (userId != null && !userId.trim().isEmpty()) {
                    uniqueIds.add(userId);
                }
            }
        }
        List<String> orderedIds = new ArrayList<>(uniqueIds);
        if (orderedIds.isEmpty()) {
            publishFriends(generation, new ArrayList<>());
            return;
        }
        Map<String, User> loadedById = new LinkedHashMap<>();
        AtomicInteger remainingChunks = new AtomicInteger((orderedIds.size() + 9) / 10);
        for (int start = 0; start < orderedIds.size(); start += 10) {
            int end = Math.min(start + 10, orderedIds.size());
            List<String> chunk = new ArrayList<>(orderedIds.subList(start, end));
            firestore.collection(Constants.COLLECTION_USERS)
                    .whereIn("__name__", chunk)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (generation != friendsLoadGeneration) {
                            return;
                        }
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                loadedById.put(document.getId(), document.toObject(User.class));
                            }
                        } else if (task.getException() != null) {
                            Log.w(TAG, "Unable to load a friend batch", task.getException());
                        }
                        if (remainingChunks.decrementAndGet() == 0) {
                            List<User> loadedFriends = new ArrayList<>();
                            for (String userId : orderedIds) {
                                User friend = loadedById.get(userId);
                                if (friend != null) {
                                    loadedFriends.add(friend);
                                }
                            }
                            publishFriends(generation, loadedFriends);
                        }
                    });
        }
    }

    private void publishFriends(int generation, @NonNull List<User> loadedFriends) {
        if (generation != friendsLoadGeneration) {
            return;
        }
        friends.clear();
        friends.addAll(loadedFriends);
        updateCameraRecipientPill();
        if (recipientSelectionAdapter != null) {
            recipientSelectionAdapter.submitFriends(friends);
            centerCaptureRecipients();
        }
    }

    private void sendCapturedPhoto() {
        if (capturedJpegBytes == null || recipientSelectionAdapter == null) {
            return;
        }
        List<String> receiverIds = recipientSelectionAdapter.selectedReceiverIds();
        if (receiverIds.isEmpty()) {
            Toast.makeText(requireContext(), R.string.photo_send_no_friend,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String caption = captionInput.getText() == null
                ? "" : captionInput.getText().toString().trim();
        cameraViewModel.sendPhoto(capturedJpegBytes, currentUserId(), currentUserName(),
                receiverIds, caption);
    }

    private void updateCapturedRecipientLabel() {
        if (capturedJpegBytes == null || recipientSelectionAdapter == null) {
            return;
        }
        List<String> selected = recipientSelectionAdapter.selectedReceiverIds();
        if (recipientSelectionAdapter.isAllSelected()) {
            recipientPill.setText(R.string.camera_send_to);
        } else {
            recipientPill.setText(getResources().getQuantityString(
                    R.plurals.camera_selected_friends, selected.size(), selected.size()));
        }
    }

    private void centerCaptureRecipients() {
        if (captureRecipientList == null || recipientSelectionAdapter == null) {
            return;
        }
        captureRecipientList.post(() -> {
            if (captureRecipientList == null || recipientSelectionAdapter == null) {
                return;
            }
            int minimumPadding = dp(12);
            int contentWidth = recipientSelectionAdapter.getItemCount()
                    * dp(RecipientSelectionAdapter.ITEM_WIDTH_DP);
            int availableWidth = captureRecipientList.getWidth();
            int sidePadding = contentWidth < availableWidth
                    ? Math.max(minimumPadding, (availableWidth - contentWidth) / 2)
                    : minimumPadding;
            captureRecipientList.setPadding(sidePadding, 0, sidePadding, 0);
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void styleCapturedActionButtons() {
        ColorStateList surface = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.pocket_surface_variant));
        ColorStateList foreground = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.pocket_text_primary));
        retakeButton.setBackgroundTintList(surface);
        retakeButton.setIconTint(foreground);
        suggestCaptionButton.setBackgroundTintList(surface);
        suggestCaptionButton.setTextColor(foreground);
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
        int iconColor = ContextCompat.getColor(requireContext(), R.color.pocket_text_primary);
        ColorStateList tint = ColorStateList.valueOf(iconColor);
        if (flashButton != null) flashButton.setIconTint(tint);
        if (galleryButton != null) galleryButton.setIconTint(tint);
        if (flipButton != null) flipButton.setIconTint(tint);
        if (retakeButton != null) retakeButton.setIconTint(tint);
        if (savePhotoButton != null) savePhotoButton.setIconTint(tint);
        if (recipientPill != null) recipientPill.setTextColor(iconColor);
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
        outState.putString(STATE_HOME_POST_ID, currentPostId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        captureCurrentPagerPosition();
        homeTabActive = false;
        cameraObserversBound = false;
        imageCapture = null;
        camera = null;
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

    private void startVideoRecording() {
        if (videoCapture == null || isRecordingVideo) {
            return;
        }
        isRecordingVideo = true;
        recordProgressSeconds = 0;

        if (modeToggle != null) {
            modeToggle.setVisibility(View.GONE);
        }

        if (recordingBorderView != null) {
            recordingBorderView.setVisibility(View.VISIBLE);
            recordingBorderView.setSweepAngle(0f);
            if (recordingBorderAnimator != null) {
                recordingBorderAnimator.cancel();
            }
            recordingBorderAnimator = android.animation.ValueAnimator.ofFloat(0f, 360f);
            recordingBorderAnimator.setDuration(10000);
            recordingBorderAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
            recordingBorderAnimator.addUpdateListener(animator -> {
                if (recordingBorderView != null) {
                    recordingBorderView.setSweepAngle((float) animator.getAnimatedValue());
                }
            });
            recordingBorderAnimator.start();
        }

        if (captureButtonSurface != null) {
            captureButtonSurface.setBackgroundResource(R.drawable.bg_shutter_button_stop);
        }

        File videoFile = new File(requireContext().getCacheDir(), "pocket-video-" + System.currentTimeMillis() + ".mp4");
        androidx.camera.video.FileOutputOptions options = new androidx.camera.video.FileOutputOptions.Builder(videoFile)
                .setDurationLimitMillis(10000)
                .build();

        try {
            activeRecording = videoCapture.getOutput()
                    .prepareRecording(requireContext(), options)
                    .start(ContextCompat.getMainExecutor(requireContext()), event -> {
                        if (event instanceof androidx.camera.video.VideoRecordEvent.Finalize) {
                            androidx.camera.video.VideoRecordEvent.Finalize finalizeEvent = (androidx.camera.video.VideoRecordEvent.Finalize) event;
                            if (finalizeEvent.hasError()) {
                                Log.e(TAG, "Video record failed: " + finalizeEvent.getError());
                                if (videoFile.exists()) {
                                    videoFile.delete();
                                }
                                isRecordingVideo = false;
                                recordProgressHandler.removeCallbacks(stopVideoRecordingRunnable);
                                if (recordingBorderAnimator != null) {
                                    recordingBorderAnimator.cancel();
                                    recordingBorderAnimator = null;
                                }
                                if (recordingBorderView != null) {
                                    recordingBorderView.setVisibility(View.GONE);
                                }
                                if (modeToggle != null) {
                                    modeToggle.setVisibility(View.VISIBLE);
                                }
                                if (captureButtonSurface != null) {
                                    captureButtonSurface.setBackgroundResource(R.drawable.bg_shutter_button);
                                }
                                Toast.makeText(requireContext(), "Failed to record video", Toast.LENGTH_SHORT).show();
                            } else {
                                Uri outputUri = finalizeEvent.getOutputResults().getOutputUri();
                                Log.d(TAG, "Video recording stopped. Local URI: " + outputUri);
                                
                                isRecordingVideo = false;
                                recordProgressHandler.removeCallbacks(stopVideoRecordingRunnable);
                                if (recordingBorderAnimator != null) {
                                    recordingBorderAnimator.cancel();
                                    recordingBorderAnimator = null;
                                }
                                if (recordingBorderView != null) {
                                    recordingBorderView.setVisibility(View.GONE);
                                }
                                if (captureButtonSurface != null) {
                                    captureButtonSurface.setBackgroundResource(R.drawable.bg_shutter_button);
                                }

                                // Navigate to VideoPreviewActivity
                                List<String> receiverIds = recipientSelectionAdapter != null
                                        ? recipientSelectionAdapter.selectedReceiverIds()
                                        : new ArrayList<>();
                                String caption = captionInput != null && captionInput.getText() != null
                                        ? captionInput.getText().toString().trim()
                                        : "";

                                Intent intent = new Intent(requireContext(), VideoPreviewActivity.class);
                                intent.putExtra(VideoPreviewActivity.EXTRA_VIDEO_URI, outputUri.toString());
                                intent.putStringArrayListExtra(VideoPreviewActivity.EXTRA_RECEIVER_IDS, new ArrayList<>(receiverIds));
                                intent.putExtra(VideoPreviewActivity.EXTRA_CAPTION, caption);
                                videoPreviewLauncher.launch(intent);
                            }
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to start camera recording", e);
            isRecordingVideo = false;
            recordProgressHandler.removeCallbacks(stopVideoRecordingRunnable);
            if (recordingBorderAnimator != null) {
                recordingBorderAnimator.cancel();
                recordingBorderAnimator = null;
            }
            if (recordingBorderView != null) {
                recordingBorderView.setVisibility(View.GONE);
            }
            if (modeToggle != null) {
                modeToggle.setVisibility(View.VISIBLE);
            }
            if (captureButtonSurface != null) {
                captureButtonSurface.setBackgroundResource(R.drawable.bg_shutter_button);
            }
            return;
        }

        // Start countdown progress limit (10 seconds)
        recordProgressHandler.postDelayed(stopVideoRecordingRunnable, 10000);
    }

    private void stopVideoRecording() {
        if (!isRecordingVideo) {
            return;
        }
        isRecordingVideo = false;
        recordProgressHandler.removeCallbacks(stopVideoRecordingRunnable);
        if (recordingBorderAnimator != null) {
            recordingBorderAnimator.cancel();
            recordingBorderAnimator = null;
        }
        if (recordingBorderView != null) {
            recordingBorderView.setVisibility(View.GONE);
        }
        if (captureButtonSurface != null) {
            captureButtonSurface.setBackgroundResource(R.drawable.bg_shutter_button);
        }

        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        }
    }

    private void setCameraMode(boolean videoMode) {
        isVideoMode = videoMode;
        if (btnModePhoto != null && btnModeVideo != null && isAdded() && getContext() != null) {
            if (isVideoMode) {
                btnModePhoto.setBackground(null);
                btnModePhoto.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.pocket_text_muted)));
                btnModeVideo.setBackgroundResource(R.drawable.bg_toggle_circle);
                btnModeVideo.setImageTintList(ColorStateList.valueOf(Color.parseColor("#00E5FF")));
            } else {
                btnModePhoto.setBackgroundResource(R.drawable.bg_toggle_circle);
                btnModePhoto.setImageTintList(ColorStateList.valueOf(Color.parseColor("#00E5FF")));
                btnModeVideo.setBackground(null);
                btnModeVideo.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.pocket_text_muted)));
            }
        }
    }

    private void updateReplyBarVisibilityForCurrentPage(int position) {
        if (replyBar == null) return;
        if (position == 0) {
            replyBar.setVisibility(View.GONE);
            currentPhoto = null;
            currentReceiverId = null;
        } else {
            int adapterIndex = position - 1;
            if (historyAdapter != null && adapterIndex >= 0 
                    && adapterIndex < historyAdapter.getCurrentList().size()) {
                Photo photo = historyAdapter.getCurrentList().get(adapterIndex);
                currentPhoto = photo;
                currentReceiverId = photo.getSenderId();
                
                if (currentPhoto != null) {
                    String currentUserId = currentUserId();
                    boolean isMyOwnPhoto = currentPhoto.getSenderId().equals(currentUserId);
                    if (isMyOwnPhoto) {
                        replyBar.setVisibility(View.GONE);
                    } else {
                        replyBar.setVisibility(View.VISIBLE);
                    }
                } else {
                    replyBar.setVisibility(View.GONE);
                }
            } else {
                replyBar.setVisibility(View.GONE);
                currentPhoto = null;
                currentReceiverId = null;
            }
        }
    }
}
