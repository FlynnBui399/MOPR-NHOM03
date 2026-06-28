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
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
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
import com.example.pocket.widget.PocketWidgetProvider;
import com.example.pocket.widget.WidgetPhotoUpdater;
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
    private static final String TAG_CAMERA = "PocketCamera";
    private static final String TAG_AI = "PocketAI";
    private static final String STATE_HOME_PAGE = "home_page";
    private static final String STATE_HOME_POST_ID = "home_post_id";

    private enum HomeMode {
        CAMERA,
        POSTS
    }

    private PreviewView previewView;
    private View previewCard;
    private com.example.pocket.ui.ZoomCropImageView capturedImageView;
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
    private ProcessCameraProvider cameraProvider;
    private boolean cameraBound;
    private boolean cameraBinding;
    private int cameraBindGeneration;
    private int boundLensFacing = -1;
    private boolean boundVideoMode;
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
    private boolean timelinePhotosLoaded;
    private boolean friendsLoaded;
    private HomeMode homeMode = HomeMode.CAMERA;
    private boolean homeTabActive = true;
    private boolean cameraErrorShown;
    private final android.os.Handler cameraRecoveryHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable cameraRecoveryRunnable;
    private androidx.lifecycle.Observer<PreviewView.StreamState> previewStreamObserver;
    private PreviewView observedPreviewView;
    private int cameraRecoveryAttempts;
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
    private PlayerView videoPreviewView;
    private ExoPlayer videoPreviewPlayer;
    private Uri currentVideoUri;

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
                Log.d(TAG_CAMERA, "Camera permission result: granted=" + granted
                        + ", fragmentAdded=" + isAdded());
                if (granted) {
                    Log.d(TAG_CAMERA, "Camera permission granted; calling startCamera()");
                    startCamera();
                } else if (isAdded()) {
                    Log.e(TAG_CAMERA, "Camera permission denied");
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

    private Photo pendingDownloadPhoto;

    private final ActivityResultLauncher<String> writeStoragePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    if (pendingDownloadPhoto != null) {
                        performDownload(pendingDownloadPhoto);
                        pendingDownloadPhoto = null;
                    }
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), "Storage permission is required to download files", Toast.LENGTH_LONG).show();
                    pendingDownloadPhoto = null;
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG_CAMERA, "HomeFragment.onCreateView");
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG_CAMERA, "HomeFragment.onViewCreated: savedState="
                + (savedInstanceState != null));
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
                    homePager.setUserInputEnabled(!keyboardVisible && capturedJpegBytes == null);
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
                if (position == 0) {
                    if (capturedJpegBytes == null) {
                        cameraRecoveryAttempts = 0;
                        homePager.postDelayed(() ->
                                rebindCameraToCurrentSurface(
                                        "camera page selected after feed"), 120L);
                    }
                } else {
                    releaseCameraUseCases("feed page selected");
                }
            }
        });

        String userId = currentUserId();
        historyAdapter = new PhotoHistoryAdapter(userId,
                photo -> PostActivitySheet.show(requireContext(), photo),
                (photo, position) -> showPhotoOptionsBottomSheet(photo, position));
        homePager.setAdapter(new ConcatAdapter(new CameraPageAdapter(this::bindCameraPage),
                historyAdapter));
        restorePagerPosition();
    }

    private void deletePhoto(Photo currentPhoto, int position) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setMessage("Delete this Pocket?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", (dialog, which) -> {
                FirebaseFirestore.getInstance()
                    .collection(Constants.COLLECTION_PHOTOS)
                    .document(currentPhoto.getId())
                    .delete()
                    .addOnSuccessListener(unused -> {
                        String photoId = currentPhoto.getId();
                        Photo found = null;
                        for (Photo p : allTimelinePhotos) {
                            if (photoId.equals(p.getId())) {
                                found = p;
                                break;
                            }
                        }
                        if (found != null) {
                            allTimelinePhotos.remove(found);
                        }
                        applyHistoryFilter();
                        Toast.makeText(requireContext(), "Pocket deleted", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            })
            .show();
    }

    private void showPhotoOptionsBottomSheet(@NonNull Photo photo, int position) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_photo_options, null);
        
        View btnShare = view.findViewById(R.id.option_share);
        View btnDownload = view.findViewById(R.id.option_download);
        View btnDelete = view.findViewById(R.id.option_delete);
        View btnReport = view.findViewById(R.id.option_report);
        View btnCancel = view.findViewById(R.id.option_cancel);

        boolean isOwnPost = currentUserId().equals(photo.getSenderId());
        if (isOwnPost) {
            btnDelete.setVisibility(View.VISIBLE);
            btnReport.setVisibility(View.GONE);
        } else {
            btnDelete.setVisibility(View.GONE);
            btnReport.setVisibility(View.VISIBLE);
        }

        btnShare.setOnClickListener(v -> {
            dialog.dismiss();
            sharePhoto(photo);
        });

        btnDownload.setOnClickListener(v -> {
            dialog.dismiss();
            downloadPhoto(photo);
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            deletePhoto(photo, position);
        });

        btnReport.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(requireContext(), "Post reported. Thank you!", Toast.LENGTH_SHORT).show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }

    private void sharePhoto(@NonNull Photo photo) {
        String mediaUrl = "video".equals(photo.getType()) ? photo.getVideoUrl() : photo.getImageUrl();
        if (mediaUrl == null || mediaUrl.trim().isEmpty()) {
            mediaUrl = photo.getThumbnailUrl();
        }
        if (mediaUrl == null || mediaUrl.trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mediaUrl);
        startActivity(Intent.createChooser(intent, "Share post"));
    }

    private void downloadPhoto(@NonNull Photo photo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            performDownload(photo);
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                performDownload(photo);
            } else {
                pendingDownloadPhoto = photo;
                writeStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void performDownload(@NonNull Photo photo) {
        String mediaUrl = "video".equals(photo.getType()) ? photo.getVideoUrl() : photo.getImageUrl();
        if (mediaUrl == null || mediaUrl.trim().isEmpty()) {
            mediaUrl = photo.getThumbnailUrl();
        }
        if (mediaUrl == null || mediaUrl.trim().isEmpty()) {
            Toast.makeText(requireContext(), "No URL found to download", Toast.LENGTH_SHORT).show();
            return;
        }

        final String finalUrl = mediaUrl;
        final boolean isVideo = "video".equals(photo.getType());
        final String mimeType = isVideo ? "video/mp4" : "image/jpeg";
        final String ext = isVideo ? ".mp4" : ".jpg";
        final String filename = "Pocket_" + System.currentTimeMillis() + ext;

        Toast.makeText(requireContext(), "Downloading...", Toast.LENGTH_SHORT).show();

        Context context = requireContext().getApplicationContext();

        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.Request request = new okhttp3.Request.Builder().url(finalUrl).build();
                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new java.io.IOException("Unexpected HTTP code: " + response.code());
                    }
                    byte[] bytes = response.body().bytes();
                    saveMediaToGallery(context, bytes, filename, mimeType, isVideo);
                }
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveMediaToGallery(Context context, byte[] bytes, String filename, String mimeType, boolean isVideo) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        
        Uri collectionUri;
        if (isVideo) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Pocket");
            }
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Pocket");
            }
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        android.content.ContentResolver resolver = context.getContentResolver();
        Uri itemUri = resolver.insert(collectionUri, values);
        if (itemUri != null) {
            try (java.io.OutputStream out = resolver.openOutputStream(itemUri)) {
                out.write(bytes);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Downloaded successfully!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        } else {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "Download failed: Cannot create file", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void bindCameraPage(@NonNull View view) {
        Log.d(TAG_CAMERA, "Binding camera page/view");
        previewView = view.findViewById(R.id.camera_preview);

        // Pinch-to-zoom (Issue 3)
        android.view.ScaleGestureDetector scaleGestureDetector = new android.view.ScaleGestureDetector(requireContext(),
                new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(android.view.ScaleGestureDetector detector) {
                        if (camera != null) {
                            androidx.camera.core.ZoomState zoomState = camera.getCameraInfo().getZoomState().getValue();
                            if (zoomState != null) {
                                float currentZoom = zoomState.getZoomRatio();
                                float delta = detector.getScaleFactor();
                                camera.getCameraControl().setZoomRatio(currentZoom * delta);
                            }
                        }
                        return true;
                    }
                });

        previewView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
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
        Log.d(TAG_CAMERA, "Camera views bound: previewView=" + (previewView != null)
                + ", captureButton=" + (captureButton != null)
                + ", capturedImageView=" + (capturedImageView != null));

        modeToggle = view.findViewById(R.id.modeToggle);
        btnModePhoto = view.findViewById(R.id.btnModePhoto);
        btnModeVideo = view.findViewById(R.id.btnModeVideo);
        recordingBorderView = view.findViewById(R.id.recording_border_view);
        if (recordingBorderView != null) {
            recordingBorderView.setCornerRadius(36f);
        }
        videoPreviewView = view.findViewById(R.id.video_preview);

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
            Log.d(TAG_CAMERA, "Camera permission already granted; calling startCamera()");
            startCamera();
        } else {
            Log.d(TAG_CAMERA, "Camera permission missing; requesting CAMERA permission");
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (previewView == null
                || currentPageIndex != 0
                || capturedJpegBytes != null
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        PreviewView activePreviewView = previewView;
        activePreviewView.post(() -> {
            if (isAdded()
                    && previewView == activePreviewView
                    && getViewLifecycleOwner().getLifecycle().getCurrentState()
                    .isAtLeast(Lifecycle.State.RESUMED)) {
                Log.d(TAG_CAMERA, "Fragment resumed; rebinding camera to the active preview surface");
                cameraRecoveryAttempts = 0;
                rebindCameraToCurrentSurface("fragment resumed");
            }
        });
    }

    @Override
    public void onPause() {
        releaseCameraUseCases("fragment paused");
        super.onPause();
    }

    private void releaseCameraUseCases(@NonNull String reason) {
        stopPreviewStreamMonitoring();
        cameraBindGeneration++;
        cameraBinding = false;
        cameraBound = false;
        boundLensFacing = -1;
        imageCapture = null;
        videoCapture = null;
        camera = null;
        if (cameraProvider != null) {
            Log.d(TAG_CAMERA, "Releasing all CameraX use cases: " + reason);
            cameraProvider.unbindAll();
            cameraProvider = null;
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
            Log.d(TAG_CAMERA, "Capture button clicked: homeMode=" + homeMode
                    + ", isVideoMode=" + isVideoMode
                    + ", capturedPreviewActive=" + (capturedJpegBytes != null)
                    + ", imageCaptureNull=" + (imageCapture == null));
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
            byte[] finalBytes = getTransformedImageBytes();
            Log.d(TAG_AI, "AI suggestion tapped: capturedBytesPresent="
                    + (finalBytes != null)
                    + ", byteLength=" + (finalBytes == null ? 0 : finalBytes.length)
                    + ", imagePathAvailable=false, imageUriAvailable=false"
                    + ", fileExists=N/A, source=in-memory JPEG, mimeType=image/jpeg");
            if (finalBytes == null) {
                Log.e(TAG_AI, "AI suggestion aborted: captured image bytes are missing");
                return;
            }
            waitingForCaptionOptions = true;
            suggestCaptionButton.setLoading(true);
            cameraViewModel.generateCaption(finalBytes);
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
                updateWidgetWithLatestFriendPhoto();
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
            String errorMsg = suggestion.getErrorMessage();

            Log.d(TAG_AI, "Caption result delivered to UI: source=" + source
                    + ", captionCount=" + numCaptions
                    + ", fallbackReason=" + (errorMsg == null ? "none" : errorMsg));

            android.util.Log.d("CaptionDisplay", "--- Suggest Caption Metrics ---");
            android.util.Log.d("CaptionDisplay", "Source: " + source);
            android.util.Log.d("CaptionDisplay", "Count: " + numCaptions);
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
                Toast.makeText(requireContext(),
                        errorMsg == null || errorMsg.trim().isEmpty()
                                ? getString(R.string.camera_caption_fallback_toast)
                                : errorMsg,
                        Toast.LENGTH_SHORT).show();
            }

            showCaptionOptionsSheet(captions);
        });
    }

    private void observeTimeline() {
        String userId = currentUserId();
        timelinePhotosLoaded = false;
        feedViewModel.getTimelinePhotos().observe(getViewLifecycleOwner(), photos -> {
            timelinePhotosLoaded = true;
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
        updateWidgetWithLatestFriendPhoto();
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

    public void openPhotoFromWidget(@NonNull String photoId) {
        String safePhotoId = photoId.trim();
        if (safePhotoId.isEmpty()) {
            return;
        }
        selectedHistorySenderId = null;
        selectedHistorySenderName = null;
        currentPostId = safePhotoId;
        currentPageIndex = 1;
        homeMode = HomeMode.POSTS;
        homeTabActive = true;
        if (historyAdapter != null) {
            applyHistoryFilter();
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
            homePager.post(this::ensureCameraReady);
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
            if (target == 0) {
                cameraRecoveryAttempts = 0;
                rebindCameraToCurrentSurface("home tab restored camera page");
            }
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
        } else if (capturedJpegBytes != null || currentVideoUri != null) {
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
        
        String quotedUrl = currentPhoto.getThumbnailUrl();
        if (quotedUrl == null || quotedUrl.trim().isEmpty()) {
            quotedUrl = currentPhoto.getImageUrl();
        }
        if (quotedUrl == null || quotedUrl.trim().isEmpty()) {
            quotedUrl = currentPhoto.getVideoUrl();
        }
        if (quotedUrl == null) {
            quotedUrl = "";
        }

        chatRepositoryHome.sendPhotoReply(chatId, text, quotedUrl, currentPhoto.getId(),
                currentPhoto.getCaption(), currentPhoto.getType(), new UserRepository.Callback<Void>() {
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
        if (quotedUrl == null || quotedUrl.trim().isEmpty()) {
            quotedUrl = currentPhoto.getVideoUrl();
        }
        if (quotedUrl == null) {
            quotedUrl = "";
        }

        chatRepositoryHome.sendPhotoReply(chatId, emoji, quotedUrl, currentPhoto.getId(),
                currentPhoto.getCaption(), currentPhoto.getType(), new UserRepository.Callback<Void>() {
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
        final boolean requestedVideoMode = isVideoMode;
        final int requestedLensFacing = lensFacing;
        Log.d(TAG_CAMERA, "startCamera() called: fragmentAdded=" + isAdded()
                + ", previewViewNull=" + (previewView == null)
                + ", lensFacing=" + lensFacingName()
                + ", mode=" + (requestedVideoMode ? "VIDEO" : "PHOTO")
                + ", cameraBound=" + cameraBound
                + ", cameraBinding=" + cameraBinding);
        if (!isAdded() || previewView == null) {
            Log.e(TAG_CAMERA, "startCamera() aborted: fragment/view is not ready");
            return;
        }
        boolean expectedUseCaseReady = requestedVideoMode
                ? videoCapture != null : imageCapture != null;
        if (cameraBound && camera != null && expectedUseCaseReady
                && boundLensFacing == requestedLensFacing
                && boundVideoMode == requestedVideoMode) {
            Log.d(TAG_CAMERA, "Camera already bound for current lens/mode; skipping duplicate bind");
            return;
        }

        final int bindGeneration = ++cameraBindGeneration;
        cameraBinding = true;
        PreviewView activePreviewView = previewView;
        Log.d(TAG_CAMERA, "Requesting ProcessCameraProvider: generation=" + bindGeneration);
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(requireContext());
        providerFuture.addListener(() -> {
            if (bindGeneration != cameraBindGeneration) {
                Log.d(TAG_CAMERA, "Ignoring stale camera bind callback: generation="
                        + bindGeneration + ", current=" + cameraBindGeneration);
                return;
            }
            if (!isAdded() || getView() == null || previewView != activePreviewView
                    || !getViewLifecycleOwner().getLifecycle().getCurrentState()
                    .isAtLeast(Lifecycle.State.STARTED)) {
                cameraBinding = false;
                Log.e(TAG_CAMERA, "Camera provider callback ignored: view/lifecycle no longer active");
                return;
            }
            try {
                ProcessCameraProvider provider = providerFuture.get();
                cameraProvider = provider;
                Log.d(TAG_CAMERA, "ProcessCameraProvider available: generation=" + bindGeneration);
                Preview preview = new Preview.Builder().build();
                Log.d(TAG_CAMERA, "Preview use case created=" + (preview != null));
                ImageCapture newImageCapture = null;
                androidx.camera.video.VideoCapture<androidx.camera.video.Recorder>
                        newVideoCapture = null;

                if (requestedVideoMode) {
                    androidx.camera.video.QualitySelector qualitySelector =
                            androidx.camera.video.QualitySelector.fromOrderedList(
                                    java.util.Arrays.asList(
                                            androidx.camera.video.Quality.HD,
                                            androidx.camera.video.Quality.SD),
                                    androidx.camera.video.FallbackStrategy
                                            .lowerQualityOrHigherThan(androidx.camera.video.Quality.SD));
                    androidx.camera.video.Recorder recorder =
                            new androidx.camera.video.Recorder.Builder()
                                    .setExecutor(ContextCompat.getMainExecutor(requireContext()))
                                    .setQualitySelector(qualitySelector)
                                    .setTargetVideoEncodingBitRate(4000000)
                                    .build();
                    newVideoCapture = androidx.camera.video.VideoCapture.withOutput(recorder);
                    Log.d(TAG_CAMERA, "VideoCapture use case created with HD/SD fallback");
                } else {
                    newImageCapture = new ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setFlashMode(flashEnabled
                                    ? ImageCapture.FLASH_MODE_ON
                                    : ImageCapture.FLASH_MODE_OFF)
                            .build();
                    Log.d(TAG_CAMERA, "ImageCapture use case created="
                            + (newImageCapture != null) + ", flashEnabled=" + flashEnabled);
                }

                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(requestedLensFacing)
                        .build();
                Log.d(TAG_CAMERA, "Camera selector created: lensFacing="
                        + (requestedLensFacing == CameraSelector.LENS_FACING_FRONT
                        ? "FRONT" : "BACK"));

                cameraBound = false;
                imageCapture = null;
                videoCapture = null;
                camera = null;
                Log.d(TAG_CAMERA, "Calling unbindAll(); shared use cases cleared");
                provider.unbindAll();

                preview.setSurfaceProvider(activePreviewView.getSurfaceProvider());
                Camera newCamera;
                if (requestedVideoMode) {
                    Log.d(TAG_CAMERA, "Binding VIDEO mode use cases: Preview + VideoCapture");
                    newCamera = provider.bindToLifecycle(getViewLifecycleOwner(), selector,
                            preview, newVideoCapture);
                } else {
                    Log.d(TAG_CAMERA, "Binding PHOTO mode use cases: Preview + ImageCapture");
                    newCamera = provider.bindToLifecycle(getViewLifecycleOwner(), selector,
                            preview, newImageCapture);
                }
                if (bindGeneration != cameraBindGeneration) {
                    Log.d(TAG_CAMERA, "Camera bind became stale after bindToLifecycle; unbinding generation="
                            + bindGeneration);
                    if (requestedVideoMode) {
                        provider.unbind(preview, newVideoCapture);
                    } else {
                        provider.unbind(preview, newImageCapture);
                    }
                    return;
                }

                camera = newCamera;
                imageCapture = newImageCapture;
                videoCapture = newVideoCapture;
                boundLensFacing = requestedLensFacing;
                boundVideoMode = requestedVideoMode;
                cameraBound = true;
                cameraBinding = false;
                Log.d(TAG_CAMERA, "bindToLifecycle succeeded: mode="
                        + (requestedVideoMode ? "VIDEO" : "PHOTO")
                        + ", cameraNull=" + (camera == null)
                        + ", imageCaptureNull=" + (imageCapture == null)
                        + ", videoCaptureNull=" + (videoCapture == null)
                        + ", cameraBound=" + cameraBound);
                if (previewView != activePreviewView) {
                    Log.e(TAG_CAMERA, "Preview view changed after camera binding; skipping surface provider");
                    return;
                }
                Log.d(TAG_CAMERA, "Preview surface provider attached; final imageCaptureNull="
                        + (imageCapture == null) + ", videoCaptureNull="
                        + (videoCapture == null) + ", cameraBound=" + cameraBound);
                if (flashButton != null) {
                    applyFlashMode();
                }
                monitorPreviewStream(activePreviewView, bindGeneration);
                cameraErrorShown = false;
            } catch (Exception exception) {
                if (bindGeneration == cameraBindGeneration) {
                    cameraBinding = false;
                    cameraBound = false;
                    boundVideoMode = false;
                    imageCapture = null;
                    videoCapture = null;
                    camera = null;
                }
                if (requestedVideoMode && bindGeneration == cameraBindGeneration) {
                    Log.e(TAG_CAMERA, "Video mode unavailable; returning to photo mode. Reason="
                            + exception.getMessage(), exception);
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.camera_video_unavailable,
                                Toast.LENGTH_SHORT).show();
                        setCameraMode(false);
                    }
                    return;
                }
                Log.e(TAG_CAMERA, "bindToLifecycle/startCamera failed", exception);
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

    private void ensureCameraReady() {
        boolean expectedUseCaseReady = isVideoMode
                ? videoCapture != null : imageCapture != null;
        PreviewView.StreamState streamState = previewView == null
                ? null : previewView.getPreviewStreamState().getValue();
        if (cameraBinding) {
            Log.d(TAG_CAMERA, "Camera bind already in progress");
            return;
        }
        if (!cameraBound || camera == null || !expectedUseCaseReady
                || boundLensFacing != lensFacing || boundVideoMode != isVideoMode
                || streamState != PreviewView.StreamState.STREAMING) {
            Log.d(TAG_CAMERA, "Camera is not ready; requesting a fresh bind");
            rebindCameraToCurrentSurface("camera readiness check failed");
        }
    }

    private void rebindCameraToCurrentSurface(@NonNull String reason) {
        if (!isAdded() || previewView == null || capturedJpegBytes != null
                || currentPageIndex != 0) {
            return;
        }

        Log.d(TAG_CAMERA, "Rebinding camera: " + reason);
        stopPreviewStreamMonitoring();
        cameraBindGeneration++;
        cameraBinding = false;
        cameraBound = false;
        boundLensFacing = -1;
        imageCapture = null;
        videoCapture = null;
        camera = null;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }

        PreviewView activePreviewView = previewView;
        int rebindGeneration = cameraBindGeneration;
        postCameraBindWhenSurfaceReady(activePreviewView, rebindGeneration, 12);
    }

    private void postCameraBindWhenSurfaceReady(@NonNull PreviewView activePreviewView,
                                                int rebindGeneration,
                                                int attemptsRemaining) {
        activePreviewView.post(() -> {
            if (!isAdded()
                    || previewView != activePreviewView
                    || rebindGeneration != cameraBindGeneration
                    || !getViewLifecycleOwner().getLifecycle().getCurrentState()
                    .isAtLeast(Lifecycle.State.RESUMED)) {
                return;
            }
            if (activePreviewView.isAttachedToWindow()
                    && activePreviewView.getWidth() > 0
                    && activePreviewView.getHeight() > 0) {
                startCamera();
                return;
            }
            if (attemptsRemaining > 0) {
                activePreviewView.postDelayed(() ->
                        postCameraBindWhenSurfaceReady(activePreviewView,
                                rebindGeneration, attemptsRemaining - 1), 50L);
            } else {
                Log.e(TAG_CAMERA, "Preview surface was not ready for camera binding");
            }
        });
    }

    private void monitorPreviewStream(@NonNull PreviewView activePreviewView,
                                      int bindGeneration) {
        stopPreviewStreamMonitoring();
        observedPreviewView = activePreviewView;
        previewStreamObserver = streamState -> {
            if (previewView != activePreviewView
                    || bindGeneration != cameraBindGeneration) {
                return;
            }
            Log.d(TAG_CAMERA, "Preview stream state=" + streamState
                    + ", generation=" + bindGeneration);
            if (streamState == PreviewView.StreamState.STREAMING) {
                cameraRecoveryAttempts = 0;
                if (cameraRecoveryRunnable != null) {
                    cameraRecoveryHandler.removeCallbacks(cameraRecoveryRunnable);
                    cameraRecoveryRunnable = null;
                }
            }
        };
        activePreviewView.getPreviewStreamState()
                .observe(getViewLifecycleOwner(), previewStreamObserver);

        cameraRecoveryRunnable = () -> {
            if (!isAdded()
                    || previewView != activePreviewView
                    || bindGeneration != cameraBindGeneration
                    || currentPageIndex != 0
                    || capturedJpegBytes != null
                    || !getViewLifecycleOwner().getLifecycle().getCurrentState()
                    .isAtLeast(Lifecycle.State.RESUMED)) {
                return;
            }
            PreviewView.StreamState streamState =
                    activePreviewView.getPreviewStreamState().getValue();
            if (streamState == PreviewView.StreamState.STREAMING) {
                return;
            }
            if (cameraRecoveryAttempts >= 2) {
                Log.e(TAG_CAMERA, "Preview remained idle after camera recovery attempts");
                return;
            }
            cameraRecoveryAttempts++;
            Log.w(TAG_CAMERA, "Preview did not start streaming; retry="
                    + cameraRecoveryAttempts);
            rebindCameraToCurrentSurface("preview stream stayed idle");
        };
        cameraRecoveryHandler.postDelayed(cameraRecoveryRunnable, 1500L);
    }

    private void stopPreviewStreamMonitoring() {
        if (cameraRecoveryRunnable != null) {
            cameraRecoveryHandler.removeCallbacks(cameraRecoveryRunnable);
            cameraRecoveryRunnable = null;
        }
        if (observedPreviewView != null && previewStreamObserver != null) {
            observedPreviewView.getPreviewStreamState().removeObserver(previewStreamObserver);
        }
        observedPreviewView = null;
        previewStreamObserver = null;
    }

    @NonNull
    private String lensFacingName() {
        return lensFacing == CameraSelector.LENS_FACING_FRONT ? "FRONT" : "BACK";
    }

    private void capturePhoto() {
        Log.d(TAG_CAMERA, "capturePhoto() entered: imageCaptureNull=" + (imageCapture == null)
                + ", cameraBound=" + cameraBound
                + ", cameraBinding=" + cameraBinding
                + ", homeMode=" + homeMode + ", isVideoMode=" + isVideoMode);
        if (imageCapture == null || !cameraBound || camera == null) {
            Log.e(TAG_CAMERA, "Capture aborted: camera/ImageCapture is not currently bound");
            ensureCameraReady();
            Toast.makeText(requireContext(), R.string.camera_not_ready,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        ImageCapture activeImageCapture = imageCapture;
        captureButton.setLoading(true);
        File outputFile;
        try {
            Log.d(TAG_CAMERA, "Creating temporary capture output file");
            outputFile = new File(requireContext().getCacheDir(),
                    "pocket-capture-" + System.currentTimeMillis() + ".jpg");
            Log.d(TAG_CAMERA, "Capture output path=" + outputFile.getAbsolutePath());
        } catch (RuntimeException exception) {
            Log.e(TAG_CAMERA, "Failed to create capture output file", exception);
            postCaptureError(exception);
            return;
        }
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();
        Log.d(TAG_CAMERA, "Calling ImageCapture.takePicture()");
        activeImageCapture.takePicture(options, cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                        Uri savedUri = results.getSavedUri();
                        Log.d(TAG_CAMERA, "onImageSaved: savedUri=" + savedUri
                                + ", outputPath=" + outputFile.getAbsolutePath()
                                + ", fileExists=" + outputFile.exists()
                                + ", fileBytes=" + outputFile.length());
                        try {
                            Log.d(TAG_CAMERA, "Decoding saved capture into Bitmap");
                            Bitmap bitmap = ImageUtils.uriToBitmap(requireContext(),
                                    Uri.fromFile(outputFile));
                            Log.d(TAG_CAMERA, "Capture Bitmap decoded: bitmapNull=" + (bitmap == null)
                                    + (bitmap == null ? "" : ", size=" + bitmap.getWidth()
                                    + "x" + bitmap.getHeight()));
                            byte[] compressed = ImageUtils.compress(bitmap);
                            Log.d(TAG_CAMERA, "Capture image compressed: bytes=" + compressed.length
                                    + "; posting captured preview UI");
                            postToView(() -> onPhotoCaptured(bitmap, compressed));
                        } catch (IOException exception) {
                            Log.e(TAG_CAMERA, "Failed to load saved capture for preview", exception);
                            postCaptureError(exception);
                        } finally {
                            if (outputFile.exists()) {
                                outputFile.delete();
                            }
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG_CAMERA, "ImageCapture.onError: code="
                                + exception.getImageCaptureError()
                                + ", type=" + exception.getClass().getName()
                                + ", message=" + exception.getMessage()
                                + ", cause=" + exception.getCause(), exception);
                        if (exception.getImageCaptureError() == ImageCapture.ERROR_INVALID_CAMERA) {
                            cameraBound = false;
                            imageCapture = null;
                            postToView(HomeFragment.this::ensureCameraReady);
                        }
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
        Log.e(TAG_CAMERA, "Capture pipeline failed: type=" + exception.getClass().getName()
                + ", message=" + exception.getMessage()
                + ", cause=" + exception.getCause(), exception);
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
        } else {
            Log.e(TAG_CAMERA, "Cannot post capture result: previewView is null");
        }
    }

    private void onPhotoCaptured(@NonNull Bitmap bitmap, @NonNull byte[] compressed) {
        Log.d(TAG_CAMERA, "onPhotoCaptured: bitmap=" + bitmap.getWidth() + "x"
                + bitmap.getHeight() + ", compressedBytes=" + compressed.length
                + ", capturedImageViewNull=" + (capturedImageView == null));
        capturedJpegBytes = compressed;
        try {
            capturedImageView.setImage(bitmap);
            Log.d(TAG_CAMERA, "Captured bitmap loaded into preview ImageView");
        } catch (RuntimeException exception) {
            Log.e(TAG_CAMERA, "Failed to load captured bitmap into preview ImageView", exception);
            postCaptureError(exception);
            return;
        }
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
        if (homePager != null) {
            homePager.setUserInputEnabled(false);
        }
        Log.d(TAG_CAMERA, "Switched to captured preview/send UI: capturedBytesPresent="
                + (capturedJpegBytes != null));
    }

    private void resetCapture() {
        resetCapture(true);
    }

    private void resetCapture(boolean ensureCameraBound) {
        if (videoPreviewPlayer != null) {
            videoPreviewPlayer.stop();
            videoPreviewPlayer.release();
            videoPreviewPlayer = null;
        }
        if (videoPreviewView != null) {
            videoPreviewView.setVisibility(View.GONE);
        }
        if (previewView != null) {
            previewView.setVisibility(View.VISIBLE);
        }

        if (currentVideoUri != null) {
            if ("file".equals(currentVideoUri.getScheme())) {
                try {
                    File file = new File(currentVideoUri.getPath());
                    if (file.exists()) {
                        file.delete();
                    }
                } catch (Exception ignored) {}
            }
            currentVideoUri = null;
        }

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
        if (homePager != null) {
            homePager.setUserInputEnabled(!keyboardVisible);
        }
        if (ensureCameraBound) {
            ensureCameraReady();
        }
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
            resetCapture(false);
            return;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(capturedJpegBytes, 0,
                capturedJpegBytes.length);
        capturedImageView.setImage(bitmap);
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
        if (homePager != null) {
            homePager.setUserInputEnabled(false);
        }
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
        friendsLoaded = false;
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
        friendsLoaded = true;
        updateWidgetWithLatestFriendPhoto();
        updateCameraRecipientPill();
        if (recipientSelectionAdapter != null) {
            recipientSelectionAdapter.submitFriends(friends);
            centerCaptureRecipients();
        }
    }

    private void sendCapturedPhoto() {
        if (currentVideoUri != null) {
            sendCapturedVideo();
            return;
        }
        byte[] finalBytes = getTransformedImageBytes();
        if (finalBytes == null || recipientSelectionAdapter == null) {
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
        cameraViewModel.sendPhoto(finalBytes, currentUserId(), currentUserName(),
                receiverIds, caption);
    }

    private void showVideoPreview(Uri videoUri) {
        currentVideoUri = videoUri;
        
        if (previewView != null) {
            previewView.setVisibility(View.GONE);
        }
        if (videoPreviewView != null) {
            videoPreviewView.setVisibility(View.VISIBLE);
            
            if (videoPreviewPlayer != null) {
                videoPreviewPlayer.release();
            }
            
            videoPreviewPlayer = new ExoPlayer.Builder(requireContext()).build();
            videoPreviewView.setPlayer(videoPreviewPlayer);
            
            MediaItem mediaItem = MediaItem.fromUri(videoUri);
            videoPreviewPlayer.setMediaItem(mediaItem);
            videoPreviewPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
            videoPreviewPlayer.prepare();
            videoPreviewPlayer.play();
        }

        captionPanel.setVisibility(View.VISIBLE);
        captionInputLayout.setVisibility(View.VISIBLE);
        
        captureButton.setVisibility(View.GONE);
        captureButtonSurface.setVisibility(View.GONE);
        galleryButton.setVisibility(View.GONE);
        flipButton.setVisibility(View.GONE);
        
        retakeButton.setVisibility(View.VISIBLE);
        
        styleCapturedActionButtons();
        sendPhotoButton.setVisibility(View.VISIBLE);
        savePhotoButton.setVisibility(View.GONE);
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

    private void sendCapturedVideo() {
        if (currentVideoUri == null || recipientSelectionAdapter == null) {
            return;
        }
        
        List<String> receiverIds = recipientSelectionAdapter.selectedReceiverIds();
        if (receiverIds.isEmpty()) {
            Toast.makeText(requireContext(), R.string.photo_send_no_friend, Toast.LENGTH_SHORT).show();
            return;
        }

        String caption = captionInput.getText() == null
                ? "" : captionInput.getText().toString().trim();
                
        sendPhotoButton.setEnabled(false);
        sendPhotoButton.setAlpha(0.55f);
        captureButton.setEnabled(false);
        retakeButton.setEnabled(false);
        
        Context context = requireContext();
        String currentUserId = currentUserId();
        String currentUserName = currentUserName();

        new Thread(() -> {
            try {
                Uri localUri = currentVideoUri;
                if ("content".equals(localUri.getScheme())) {
                    File cacheFile = new File(context.getCacheDir(), "temp-media-" + System.currentTimeMillis() + ".mp4");
                    try (java.io.InputStream in = context.getContentResolver().openInputStream(localUri);
                         java.io.OutputStream out = new java.io.FileOutputStream(cacheFile)) {
                        byte[] buf = new byte[4096];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    }
                    localUri = Uri.fromFile(cacheFile);
                }
                
                File file = new File(localUri.getPath());
                if (!file.exists() || file.length() == 0) {
                    throw new Exception("Media file does not exist or is empty");
                }
                
                CloudinaryService cloudinaryService = new CloudinaryService();
                com.google.android.gms.tasks.Task<CloudinaryService.UploadResult> uploadTask =
                        cloudinaryService.uploadUnsignedVideo(file);
                CloudinaryService.UploadResult result = com.google.android.gms.tasks.Tasks.await(uploadTask);
                String secureUrl = result.getSecureUrl();
                String thumbnailUrl = result.getSecureUrl();
                String publicId = result.getPublicId();
                
                DocumentReference photoRef = FirebaseFirestore.getInstance()
                        .collection(Constants.COLLECTION_PHOTOS).document();
                
                Photo photo = new Photo(
                        photoRef.getId(),
                        currentUserId,
                        currentUserName,
                        secureUrl,
                        thumbnailUrl,
                        publicId,
                        caption,
                        receiverIds,
                        new HashMap<>(),
                        new ArrayList<>(),
                        Timestamp.now()
                );
                String thumbnailFromVideo = "https://res.cloudinary.com/" + Constants.CLOUDINARY_CLOUD_NAME
                        + "/video/upload/so_0,w_300,h_300,c_fill,f_jpg/" + publicId + ".jpg";
                photo.setType("video");
                photo.setVideoUrl(secureUrl);
                photo.setImageUrl(thumbnailFromVideo);
                photo.setThumbnailUrl(thumbnailFromVideo);
                
                final File finalFileToDelete = file;
                
                FirebaseFirestore.getInstance().runTransaction(transaction -> {
                    transaction.set(photoRef, photo);
                    return null;
                }).addOnSuccessListener(unused -> {
                    for (String receiverId : receiverIds) {
                        com.example.pocket.utils.StreakHelper.updateStreak(currentUserId, receiverId);
                    }
                    
                    try {
                        if (finalFileToDelete.exists()) {
                            finalFileToDelete.delete();
                        }
                    } catch (Exception ignored) {}
                    
                    createFcmTriggers(photo);

                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        if (isAdded()) {
                            if (videoPreviewPlayer != null) {
                                videoPreviewPlayer.stop();
                                videoPreviewPlayer.release();
                                videoPreviewPlayer = null;
                            }
                            if (videoPreviewView != null) {
                                videoPreviewView.setVisibility(View.GONE);
                            }
                            if (previewView != null) {
                                previewView.setVisibility(View.VISIBLE);
                            }
                            currentVideoUri = null;
                            
                            sendPhotoButton.setEnabled(true);
                            sendPhotoButton.setAlpha(1f);
                            captureButton.setEnabled(true);
                            retakeButton.setEnabled(true);
                            
                            Toast.makeText(requireContext(), R.string.camera_upload_success, Toast.LENGTH_SHORT).show();
                            resetCapture();
                            openHistoryAfterUpload = true;
                            scrollToHistory();
                        }
                    });
                }).addOnFailureListener(e -> {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        if (isAdded()) {
                            sendPhotoButton.setEnabled(true);
                            sendPhotoButton.setAlpha(1f);
                            captureButton.setEnabled(true);
                            retakeButton.setEnabled(true);
                            Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                });
                
            } catch (Exception e) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (isAdded()) {
                        sendPhotoButton.setEnabled(true);
                        sendPhotoButton.setAlpha(1f);
                        captureButton.setEnabled(true);
                        retakeButton.setEnabled(true);
                        Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void createFcmTriggers(@NonNull Photo photo) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        for (String recipientId : photo.getReceiverIds()) {
            Map<String, Object> trigger = new HashMap<>();
            trigger.put("type", "photo_received");
            trigger.put("photoId", photo.getId());
            trigger.put("senderId", photo.getSenderId());
            trigger.put("senderName", photo.getSenderName());
            trigger.put("recipientId", recipientId);
            trigger.put("imageUrl", photo.getThumbnailUrl());
            trigger.put("caption", photo.getCaption());
            trigger.put("createdAt", Timestamp.now());
            trigger.put("processed", false);

            firestore.collection(Constants.COLLECTION_FCM_TRIGGERS)
                    .document()
                    .set(trigger);
        }
    }

    private void updateCapturedRecipientLabel() {
        if (capturedJpegBytes == null && currentVideoUri == null || recipientSelectionAdapter == null) {
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
        ColorStateList foreground = ColorStateList.valueOf(iconColor);
        if (flashButton != null) flashButton.setIconTint(foreground);
        styleCameraSideButton(galleryButton, foreground);
        styleCameraSideButton(flipButton, foreground);
        if (retakeButton != null) retakeButton.setIconTint(foreground);
        if (savePhotoButton != null) savePhotoButton.setIconTint(foreground);
        if (recipientPill != null) recipientPill.setTextColor(iconColor);
    }

    private void styleCameraSideButton(@Nullable PocketButton button,
                                       @NonNull ColorStateList foreground) {
        if (button == null) {
            return;
        }
        button.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.pocket_surface_variant)));
        button.setStrokeColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.pocket_outline)));
        button.setStrokeWidth(dp(1));
        button.setIconTint(foreground);
    }

    private void saveCapturedPhoto() {
        byte[] transformedBytes = getTransformedImageBytes();
        if (transformedBytes == null) {
            return;
        }
        byte[] bytes = transformedBytes.clone();
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
        captionList.setLayoutManager(new LinearLayoutManager(
                requireContext(), RecyclerView.HORIZONTAL, false));
        captionList.setAdapter(adapter);
        dialog.setContentView(content);
        dialog.show();
    }

    private void updateWidgetWithPhoto(@Nullable Photo photo) {
        if (photo == null) {
            return;
        }
        WidgetPhotoUpdater.updateLatestFriendPhoto(requireContext(), photo,
                currentUserId(), friends, displayNameForWidgetPhoto(photo));
    }

    private void updateWidgetWithLatestFriendPhoto() {
        if (!timelinePhotosLoaded || !friendsLoaded) {
            return;
        }
        Photo latestFriendPhoto = latestFriendPhotoForWidget();
        if (latestFriendPhoto == null) {
            PocketWidgetProvider.clearLatestPhoto(requireContext());
            return;
        }
        updateWidgetWithPhoto(latestFriendPhoto);
    }

    @Nullable
    private Photo latestFriendPhotoForWidget() {
        for (Photo photo : allTimelinePhotos) {
            if (WidgetPhotoUpdater.isFriendPhoto(photo, currentUserId(), friends)) {
                return photo;
            }
        }
        return null;
    }

    @Nullable
    private String displayNameForWidgetPhoto(@NonNull Photo photo) {
        if (isRealDisplayName(photo.getSenderName())) {
            return photo.getSenderName().trim();
        }
        String senderId = photo.getSenderId();
        for (User friend : friends) {
            if (senderId != null && senderId.equals(friend.getId())) {
                if (isRealDisplayName(friend.getDisplayName())) {
                    return friend.getDisplayName().trim();
                }
                if (friend.getUsername() != null && !friend.getUsername().trim().isEmpty()) {
                    return friend.getUsername().trim();
                }
            }
        }
        return null;
    }

    @NonNull
    private String currentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user == null ? "local_user" : user.getUid();
    }

    @NonNull
    private String currentUserName() {
        SharedPrefManager sharedPrefManager = SharedPrefManager.getInstance(requireContext());
        String savedDisplayName = sharedPrefManager.getDisplayName();
        if (isRealDisplayName(savedDisplayName)) {
            return savedDisplayName.trim();
        }
        String savedUsername = sharedPrefManager.getUsername();
        if (savedUsername != null && !savedUsername.trim().isEmpty()) {
            return savedUsername.trim();
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return getString(R.string.camera_default_user);
        }
        if (isRealDisplayName(user.getDisplayName())) {
            return user.getDisplayName().trim();
        }
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            return user.getEmail();
        }
        return getString(R.string.camera_default_user);
    }

    private boolean isRealDisplayName(@Nullable String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return false;
        }
        String trimmed = displayName.trim();
        return !trimmed.equals(getString(R.string.camera_default_user))
                && !"Pocket User".equals(trimmed);
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
        stopPreviewStreamMonitoring();
        homeTabActive = false;
        cameraObserversBound = false;
        cameraBindGeneration++;
        cameraBinding = false;
        cameraBound = false;
        boundLensFacing = -1;
        boundVideoMode = false;
        if (cameraProvider != null) {
            Log.d(TAG_CAMERA, "onDestroyView: calling unbindAll() and clearing camera use cases");
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        imageCapture = null;
        videoCapture = null;
        camera = null;
        previewView = null;
        previewCard = null;
        capturedImageView = null;
        captionPanel = null;
        homePager = null;
        historyAdapter = null;
        historyHeader = null;
        historyCount = null;
        if (videoPreviewPlayer != null) {
            videoPreviewPlayer.release();
            videoPreviewPlayer = null;
        }
        videoPreviewView = null;
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
        if (isRecordingVideo) {
            return;
        }
        if (!cameraBound || !boundVideoMode || videoCapture == null || camera == null) {
            Log.e(TAG_CAMERA, "Video recording unavailable: cameraBound=" + cameraBound
                    + ", boundVideoMode=" + boundVideoMode
                    + ", videoCaptureNull=" + (videoCapture == null));
            ensureCameraReady();
            Toast.makeText(requireContext(), R.string.camera_video_unavailable,
                    Toast.LENGTH_SHORT).show();
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
            recordingBorderAnimator.setDuration(5000);
            recordingBorderAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
            recordingBorderAnimator.addUpdateListener(animator -> {
                if (recordingBorderView != null) {
                    float sweepAngle = (float) animator.getAnimatedValue();
                    recordingBorderView.setSweepAngle(sweepAngle);
                    if (Math.round(sweepAngle) % 36 == 0) {
                        Log.d("Border", "sweep=" + sweepAngle);
                    }
                }
            });
            recordingBorderAnimator.start();
        }

        if (captureButtonSurface != null) {
            captureButtonSurface.setBackgroundResource(R.drawable.bg_shutter_button_stop);
        }

        File videoFile = new File(requireContext().getCacheDir(), "pocket-video-" + System.currentTimeMillis() + ".mp4");
        androidx.camera.video.FileOutputOptions options = new androidx.camera.video.FileOutputOptions.Builder(videoFile)
                .setDurationLimitMillis(5000)
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

                                showVideoPreview(outputUri);
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

        // Start countdown progress limit (5 seconds)
        recordProgressHandler.postDelayed(stopVideoRecordingRunnable, 5000);
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
        if (captureButton != null) {
            captureButton.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
        }

        if (activeRecording != null) {
            activeRecording.stop();
            activeRecording = null;
        }
    }

    private void setCameraMode(boolean videoMode) {
        boolean modeChanged = isVideoMode != videoMode;
        isVideoMode = videoMode;
        Log.d(TAG_CAMERA, "Camera mode selected: " + (isVideoMode ? "VIDEO" : "PHOTO")
                + ", changed=" + modeChanged);
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
        if (modeChanged && previewView != null && capturedJpegBytes == null) {
            startCamera();
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

    private byte[] getTransformedImageBytes() {
        if (capturedImageView == null || capturedImageView.getVisibility() != View.VISIBLE) {
            return capturedJpegBytes;
        }
        try {
            Bitmap bitmap = capturedImageView.getCroppedBitmap(1024, 1024);
            if (bitmap == null) {
                return capturedJpegBytes;
            }
            byte[] compressed = ImageUtils.compress(bitmap);
            return compressed;
        } catch (Exception e) {
            Log.e(TAG_CAMERA, "Failed to get transformed image bytes", e);
            return capturedJpegBytes;
        }
    }

    private void centerCropMatrix(ImageView imageView, Bitmap bitmap) {
        if (imageView == null || bitmap == null) return;
        
        imageView.setScaleType(ImageView.ScaleType.MATRIX);
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        
        float viewWidth = imageView.getWidth();
        float viewHeight = imageView.getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) {
            imageView.post(() -> centerCropMatrix(imageView, bitmap));
            return;
        }
        
        float drawableWidth = bitmap.getWidth();
        float drawableHeight = bitmap.getHeight();
        
        float scale;
        float dx = 0, dy = 0;
        
        if (drawableWidth * viewHeight > viewWidth * drawableHeight) {
            scale = viewHeight / drawableHeight;
            dx = (viewWidth - drawableWidth * scale) * 0.5f;
        } else {
            scale = viewWidth / drawableWidth;
            dy = (viewHeight - drawableHeight * scale) * 0.5f;
        }
        
        matrix.setScale(scale, scale);
        matrix.postTranslate(dx, dy);
        imageView.setImageMatrix(matrix);
    }

    private void initPhotoTouchListener(ImageView imageView) {
        imageView.setScaleType(ImageView.ScaleType.MATRIX);
        
        final android.view.ScaleGestureDetector scaleDetector = new android.view.ScaleGestureDetector(
                requireContext(),
                new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(android.view.ScaleGestureDetector detector) {
                        android.graphics.Matrix matrix = new android.graphics.Matrix(imageView.getImageMatrix());
                        float scaleFactor = detector.getScaleFactor();
                        matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                        imageView.setImageMatrix(matrix);
                        return true;
                    }
                }
        );

        imageView.setOnTouchListener(new View.OnTouchListener() {
            private final android.graphics.Matrix matrix = new android.graphics.Matrix();
            private final android.graphics.Matrix savedMatrix = new android.graphics.Matrix();
            private final android.graphics.PointF start = new android.graphics.PointF();
            private final AtomicInteger mode = new AtomicInteger(0); // 0 = NONE, 1 = DRAG

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                scaleDetector.onTouchEvent(event);
                
                switch (event.getAction() & android.view.MotionEvent.ACTION_MASK) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        matrix.set(imageView.getImageMatrix());
                        savedMatrix.set(matrix);
                        start.set(event.getX(), event.getY());
                        mode.set(1); // DRAG
                        break;
                        
                    case android.view.MotionEvent.ACTION_POINTER_DOWN:
                        mode.set(0); // Stop dragging when pinch begins
                        break;
                        
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_POINTER_UP:
                        mode.set(0);
                        break;
                        
                    case android.view.MotionEvent.ACTION_MOVE:
                        if (mode.get() == 1) {
                            matrix.set(savedMatrix);
                            matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                            imageView.setImageMatrix(matrix);
                        }
                        break;
                }
                return true;
            }
        });
    }
}
