package com.example.pocket;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pocket.data.model.Photo;
import com.example.pocket.data.model.User;
import com.example.pocket.data.remote.CloudinaryService;
import com.example.pocket.data.repository.UserRepository;
import com.example.pocket.ui.PocketButton;
import com.example.pocket.utils.Constants;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public final class PostActivitySheet {
    private PostActivitySheet() {
    }

    public static void show(@NonNull Context context, @NonNull Photo photo) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View content = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_post_activity, null, false);
        RecyclerView list = content.findViewById(R.id.post_activity_list);
        TextView empty = content.findViewById(R.id.post_activity_empty);
        List<ActivityEntry> entries = buildEntries(photo);
        ActivityAdapter adapter = new ActivityAdapter(entries);
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setAdapter(adapter);
        empty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(entries.isEmpty() ? View.GONE : View.VISIBLE);
        dialog.setContentView(content);
        dialog.show();

        UserRepository repository = UserRepository.getInstance();
        for (int index = 0; index < entries.size(); index++) {
            final int position = index;
            ActivityEntry entry = entries.get(index);
            repository.getUserById(entry.userId, new UserRepository.Callback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (user != null) {
                        entry.displayName = user.getDisplayName();
                        if (entry.displayName == null || entry.displayName.trim().isEmpty()) {
                            entry.displayName = user.getUsername();
                        }
                        entry.avatarUrl = user.getAvatarUrl();
                        adapter.notifyItemChanged(position);
                    }
                }

                @Override
                public void onError(@NonNull Exception error) {
                    // The row already shows a safe fallback identity.
                }
            });
        }
    }

    @NonNull
    private static List<ActivityEntry> buildEntries(@NonNull Photo photo) {
        Set<String> viewedUserIds = new LinkedHashSet<>();
        if (photo.getSeenBy() != null) {
            viewedUserIds.addAll(photo.getSeenBy());
        }
        Map<String, String> reactions = photo.getReactions();
        if (reactions != null) {
            viewedUserIds.addAll(reactions.keySet());
        }
        List<ActivityEntry> entries = new ArrayList<>();
        for (String userId : viewedUserIds) {
            if (userId != null && !userId.trim().isEmpty()) {
                entries.add(new ActivityEntry(userId,
                        reactions == null ? null : reactions.get(userId)));
            }
        }
        return entries;
    }

    static class ActivityEntry {
        final String userId;
        final String reaction;
        String displayName;
        String avatarUrl;

        ActivityEntry(String userId, String reaction) {
            this.userId = userId;
            this.reaction = reaction;
        }
    }

    static class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.Holder> {
        private final List<ActivityEntry> entries;

        ActivityAdapter(List<ActivityEntry> entries) {
            this.entries = entries;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_post_activity, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(entries.get(position));
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            private final CircleImageView avatar;
            private final TextView name;
            private final TextView detail;
            private final TextView reaction;

            Holder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.post_activity_avatar);
                name = itemView.findViewById(R.id.post_activity_name);
                detail = itemView.findViewById(R.id.post_activity_detail);
                reaction = itemView.findViewById(R.id.post_activity_reaction);
            }

            void bind(@NonNull ActivityEntry entry) {
                String displayName = entry.displayName;
                name.setText(displayName == null || displayName.trim().isEmpty()
                        ? itemView.getContext().getString(R.string.history_activity_someone)
                        : displayName);
                String reactionEmoji = normalizeReaction(entry.reaction);
                detail.setText(R.string.history_activity_viewed);
                detail.setTextSize(13f);
                reaction.setText(reactionEmoji == null ? "" : reactionEmoji);
                reaction.setVisibility(reactionEmoji == null ? View.GONE : View.VISIBLE);
                Glide.with(avatar)
                        .load(entry.avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.avatar_placeholder)
                        .error(R.drawable.avatar_placeholder)
                        .into(avatar);
            }

            @Nullable
            private String normalizeReaction(String reaction) {
                if (reaction == null || reaction.trim().isEmpty()) {
                    return null;
                }
                String normalized = reaction.trim().toLowerCase(java.util.Locale.ROOT);
                switch (normalized) {
                    case "smile":
                    case "happy":
                    case "laugh":
                    case "smiley":
                    case "face_smile":
                    case "mặt cười":
                        return itemView.getContext().getString(R.string.reaction_smile);
                    case "heart_eyes":
                    case "love_eyes":
                        return itemView.getContext().getString(R.string.reaction_heart_eyes);
                    case "hearts":
                    case "double_heart":
                        return itemView.getContext().getString(R.string.reaction_hearts);
                    case "heart":
                    case "love":
                        return itemView.getContext().getString(R.string.reaction_heart);
                    case "fire":
                        return itemView.getContext().getString(R.string.reaction_fire);
                    case "hotdog":
                        return itemView.getContext().getString(R.string.reaction_hotdog);
                    case "flower":
                    case "flowers":
                    case "bouquet":
                        return itemView.getContext().getString(R.string.reaction_bouquet);
                    default:
                        return reaction.trim();
                }
            }
        }
    }

    public interface PostCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void showFriendPicker(@NonNull Context context,
                                        @NonNull Uri mediaUri,
                                        @NonNull String type,
                                        @NonNull String captionText,
                                        @NonNull PostCallback callback) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View content = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_friend_selector, null, false);
        RecyclerView friendList = content.findViewById(R.id.friend_selector_list);
        TextView emptyView = content.findViewById(R.id.friend_selector_empty);
        PocketButton sendButton = content.findViewById(R.id.friend_selector_send_button);

        String currentUserId = "local_user";
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            currentUserId = firebaseUser.getUid();
        }
        
        emptyView.setText("Loading friends...");
        emptyView.setVisibility(View.VISIBLE);
        friendList.setVisibility(View.GONE);
        sendButton.setEnabled(false);

        List<User> friends = new ArrayList<>();
        Set<String> selectedIds = new HashSet<>();
        
        final String finalCurrentUserId = currentUserId;
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_USERS).document(finalCurrentUserId).get()
                .addOnSuccessListener(document -> {
                    User currentUser = document.toObject(User.class);
                    List<String> friendIds = currentUser != null ? currentUser.getFriends() : new ArrayList<>();
                    if (friendIds == null || friendIds.isEmpty()) {
                        emptyView.setText(R.string.camera_select_friends_empty);
                        emptyView.setVisibility(View.VISIBLE);
                        return;
                    }
                    
                    List<String> uniqueIds = new ArrayList<>(new LinkedHashSet<>(friendIds));
                    java.util.concurrent.atomic.AtomicInteger remainingChunks = new java.util.concurrent.atomic.AtomicInteger((uniqueIds.size() + 9) / 10);
                    Map<String, User> loadedById = new LinkedHashMap<>();
                    
                    for (int start = 0; start < uniqueIds.size(); start += 10) {
                        int end = Math.min(start + 10, uniqueIds.size());
                        List<String> chunk = uniqueIds.subList(start, end);
                        
                        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_USERS)
                                .whereIn("__name__", chunk)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && task.getResult() != null) {
                                        for (QueryDocumentSnapshot doc : task.getResult()) {
                                            loadedById.put(doc.getId(), doc.toObject(User.class));
                                        }
                                    }
                                    if (remainingChunks.decrementAndGet() == 0) {
                                        friends.clear();
                                        for (String uid : uniqueIds) {
                                            User f = loadedById.get(uid);
                                            if (f != null) {
                                                f.setId(uid);
                                                friends.add(f);
                                            }
                                        }
                                        
                                        if (friends.isEmpty()) {
                                            emptyView.setText(R.string.camera_select_friends_empty);
                                            emptyView.setVisibility(View.VISIBLE);
                                            friendList.setVisibility(View.GONE);
                                        } else {
                                            emptyView.setVisibility(View.GONE);
                                            friendList.setVisibility(View.VISIBLE);
                                            
                                            for (User f : friends) {
                                                selectedIds.add(f.getId());
                                            }
                                            
                                            FriendAdapter adapter = new FriendAdapter(friends, selectedIds, () -> {
                                                sendButton.setEnabled(!selectedIds.isEmpty());
                                            });
                                            friendList.setLayoutManager(new LinearLayoutManager(context));
                                            friendList.setAdapter(adapter);
                                            sendButton.setEnabled(!selectedIds.isEmpty());
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    emptyView.setText("Failed to load friends: " + e.getMessage());
                    emptyView.setVisibility(View.VISIBLE);
                });

        sendButton.setOnClickListener(v -> {
            sendButton.setLoading(true);
            
            String currentUserName = "Pocket User";
            if (firebaseUser != null) {
                if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().trim().isEmpty()) {
                    currentUserName = firebaseUser.getDisplayName();
                } else if (firebaseUser.getEmail() != null) {
                    currentUserName = firebaseUser.getEmail();
                }
            }
            final String finalSenderName = currentUserName;
            final String finalSenderId = finalCurrentUserId;
            
            new Thread(() -> {
                try {
                    Uri localUri = mediaUri;
                    if ("content".equals(mediaUri.getScheme())) {
                        File cacheFile = new File(context.getCacheDir(), "temp-media-" + System.currentTimeMillis() + ("video".equals(type) ? ".mp4" : ".jpg"));
                        try (java.io.InputStream in = context.getContentResolver().openInputStream(mediaUri);
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
                    String secureUrl = "";
                    String thumbnailUrl = "";
                    String publicId = "";
                    
                    if ("video".equals(type)) {
                        com.google.android.gms.tasks.Task<CloudinaryService.UploadResult> uploadTask =
                                cloudinaryService.uploadUnsignedVideo(file);
                        CloudinaryService.UploadResult result = com.google.android.gms.tasks.Tasks.await(uploadTask);
                        secureUrl = result.getSecureUrl();
                        thumbnailUrl = result.getSecureUrl();
                        publicId = result.getPublicId();
                    } else {
                        byte[] fileBytes = new byte[(int) file.length()];
                        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                            fis.read(fileBytes);
                        }
                        com.google.android.gms.tasks.Task<CloudinaryService.UploadResult> uploadTask =
                                cloudinaryService.uploadUnsigned(fileBytes);
                        CloudinaryService.UploadResult result = com.google.android.gms.tasks.Tasks.await(uploadTask);
                        secureUrl = result.getSecureUrl();
                        thumbnailUrl = result.getThumbnailUrl();
                        publicId = result.getPublicId();
                    }
                    
                    DocumentReference photoRef = FirebaseFirestore.getInstance()
                            .collection(Constants.COLLECTION_PHOTOS).document();
                    
                    List<String> receiverList = new ArrayList<>(selectedIds);
                    Photo photo = new Photo(
                            photoRef.getId(),
                            finalSenderId,
                            finalSenderName,
                            secureUrl,
                            thumbnailUrl,
                            publicId,
                            captionText,
                            receiverList,
                            new HashMap<>(),
                            new ArrayList<>(),
                            Timestamp.now()
                    );
                    if ("video".equals(type)) {
                        photo.setType("video");
                        photo.setVideoUrl(secureUrl);
                        photo.setImageUrl("");
                        photo.setThumbnailUrl("");
                    }
                    
                    final File finalFileToDelete = file;
                    
                    FirebaseFirestore.getInstance().runTransaction(transaction -> {
                        transaction.set(photoRef, photo);
                        return null;
                    }).addOnSuccessListener(unused -> {
                        for (String receiverId : receiverList) {
                            com.example.pocket.utils.StreakHelper.updateStreak(finalSenderId, receiverId);
                        }
                        
                        try {
                            if (finalFileToDelete.exists()) {
                                finalFileToDelete.delete();
                            }
                        } catch (Exception ignored) {}
                        
                        createFcmTriggers(photo);

                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            dialog.dismiss();
                            callback.onSuccess();
                        });
                    }).addOnFailureListener(e -> {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            sendButton.setLoading(false);
                            callback.onFailure(e);
                        });
                    });
                    
                } catch (Exception e) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        sendButton.setLoading(false);
                        callback.onFailure(e);
                    });
                }
            }).start();
        });
        
        dialog.setContentView(content);
        dialog.show();
    }

    private static void createFcmTriggers(@NonNull Photo photo) {
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
            holder.nameView.setText(name == null || name.trim().isEmpty() ? "Pocket User" : name);
            
            Glide.with(holder.avatarView)
                    .load(friend.getAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_placeholder)
                    .into(holder.avatarView);

            String id = friend.getId();
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedIds.contains(id));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedIds.add(id);
                } else {
                    selectedIds.remove(id);
                }
                if (selectionChanged != null) {
                    selectionChanged.run();
                }
            });
            holder.itemView.setOnClickListener(v -> holder.checkBox.toggle());
        }

        @Override
        public int getItemCount() {
            return friends.size();
        }

        static class FriendViewHolder extends RecyclerView.ViewHolder {
            final CircleImageView avatarView;
            final TextView nameView;
            final android.widget.CheckBox checkBox;

            FriendViewHolder(@NonNull View itemView) {
                super(itemView);
                avatarView = itemView.findViewById(R.id.friend_avatar);
                nameView = itemView.findViewById(R.id.friend_name);
                checkBox = itemView.findViewById(R.id.friend_checkbox);
            }
        }
    }
}
