package com.example.pocket;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.pocket.data.model.Photo;
import com.example.pocket.data.model.User;
import com.example.pocket.data.repository.UserRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class PhotoHistoryAdapter extends ListAdapter<Photo, PhotoHistoryAdapter.PhotoViewHolder> {

    public interface ActivityClickListener {
        void onActivityClick(@NonNull Photo photo);
    }

    private final String currentUserId;
    private final ActivityClickListener activityClickListener;
    private final UserRepository userRepository = UserRepository.getInstance();
    private final Map<String, User> senderCache = new HashMap<>();
    private final Set<String> requestedSenderIds = new HashSet<>();

    public PhotoHistoryAdapter(@NonNull String currentUserId,
                               ActivityClickListener activityClickListener) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
        this.activityClickListener = activityClickListener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo_history, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = getItem(position);
        User sender = photo.getSenderId() == null ? null : senderCache.get(photo.getSenderId());
        holder.bind(photo, sender, currentUserId, activityClickListener);
        requestSenderIfNeeded(photo.getSenderId());
    }

    @Override
    public void onViewRecycled(@NonNull PhotoViewHolder holder) {
        super.onViewRecycled(holder);
        holder.releasePlayer();
    }

    private void requestSenderIfNeeded(String senderId) {
        if (senderId == null || senderId.trim().isEmpty()
                || senderCache.containsKey(senderId) || !requestedSenderIds.add(senderId)) {
            return;
        }
        userRepository.getUserById(senderId, new UserRepository.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                senderCache.put(senderId, user);
                notifySenderChanged(senderId);
            }

            @Override
            public void onError(@NonNull Exception error) {
                senderCache.put(senderId, null);
                notifySenderChanged(senderId);
            }
        });
    }

    private void notifySenderChanged(@NonNull String senderId) {
        for (int index = 0; index < getCurrentList().size(); index++) {
            if (senderId.equals(getCurrentList().get(index).getSenderId())) {
                notifyItemChanged(index);
            }
        }
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView senderAvatar;
        private final View photoCard;
        private final TextView senderName;
        private final TextView timestamp;
        private final ImageView photoImage;
        private final PlayerView videoPlayer;
        private final TextView tvCaption;
        private final TextView activityButton;
        
        private ExoPlayer exoPlayer;
        private String boundPhotoId;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            senderAvatar = itemView.findViewById(R.id.history_sender_avatar);
            photoCard = itemView.findViewById(R.id.history_photo_card);
            senderName = itemView.findViewById(R.id.history_sender_name);
            timestamp = itemView.findViewById(R.id.history_timestamp);
            photoImage = itemView.findViewById(R.id.history_photo_image);
            videoPlayer = itemView.findViewById(R.id.history_video_player);
            tvCaption = itemView.findViewById(R.id.tvCaption);
            activityButton = itemView.findViewById(R.id.history_activity_button);
        }

        void bind(@NonNull Photo photo,
                  User sender,
                  @NonNull String currentUserId,
                  ActivityClickListener activityListener) {
            boundPhotoId = photo.getId();
            String displayName = sender == null ? null : sender.getDisplayName();
            if ((displayName == null || displayName.trim().isEmpty()) && sender != null) {
                displayName = sender.getUsername();
            }
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = photo.getSenderName();
            }
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = itemView.getContext().getString(R.string.camera_default_user);
            }
            senderName.setText(currentUserId.equals(photo.getSenderId())
                    ? itemView.getContext().getString(R.string.history_sender_you)
                    : displayName);

            String avatarUrl = (sender != null && sender.getAvatarUrl() != null && !sender.getAvatarUrl().trim().isEmpty())
                    ? sender.getAvatarUrl() : null;

            if (avatarUrl != null) {
                Glide.with(senderAvatar.getContext())
                        .load(avatarUrl)
                        .transition(DrawableTransitionOptions.withCrossFade(250))
                        .placeholder(R.drawable.avatar_placeholder)
                        .error(R.drawable.avatar_placeholder)
                        .circleCrop()
                        .into(senderAvatar);
            } else {
                senderAvatar.setImageResource(R.drawable.avatar_placeholder);
            }

            if (photo.getCreatedAt() == null) {
                timestamp.setText(R.string.history_time_now);
            } else {
                timestamp.setText(DateUtils.getRelativeTimeSpanString(
                        photo.getCreatedAt().toDate().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS));
            }

            // Video rendering vs Photo image rendering (Task 9)
            if ("video".equals(photo.getType()) && photo.getVideoUrl() != null) {
                photoImage.setVisibility(View.GONE);
                videoPlayer.setVisibility(View.VISIBLE);
                initializePlayer(photo.getVideoUrl());
            } else {
                videoPlayer.setVisibility(View.GONE);
                photoImage.setVisibility(View.VISIBLE);
                releasePlayer();

                String imageUrl = photo.getImageUrl();
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    imageUrl = photo.getThumbnailUrl();
                }
                Glide.with(photoImage.getContext())
                        .load(imageUrl)
                        .transition(DrawableTransitionOptions.withCrossFade(250))
                        .placeholder(new ColorDrawable(Color.parseColor("#1C1C1E")))
                        .error(R.drawable.placeholder_pocket)
                        .centerCrop()
                        .into(photoImage);
            }

            String captionText = photo.getCaption();
            boolean hasCaption = captionText != null && !captionText.trim().isEmpty();
            tvCaption.setVisibility(hasCaption ? View.VISIBLE : View.GONE);
            tvCaption.setText(hasCaption ? captionText : "");

            int activityCount = activityCount(photo);
            boolean ownPhoto = currentUserId.equals(photo.getSenderId());
            activityButton.setVisibility(ownPhoto ? View.VISIBLE : View.GONE);
            activityButton.setText(activityCount == 0
                    ? itemView.getContext().getString(R.string.history_no_activity)
                    : itemView.getResources().getQuantityString(
                            R.plurals.history_activity_count, activityCount, activityCount));
            activityButton.setEnabled(ownPhoto);
            activityButton.setTextColor(itemView.getContext().getColor(activityCount > 0
                    ? R.color.pocket_primary : R.color.pocket_text_secondary));
            activityButton.setOnClickListener(clicked -> {
                if (ownPhoto && activityListener != null) {
                    activityListener.onActivityClick(photo);
                }
            });
        }

        private void initializePlayer(String videoUrl) {
            if (exoPlayer == null) {
                exoPlayer = new ExoPlayer.Builder(itemView.getContext()).build();
                videoPlayer.setPlayer(exoPlayer);
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
                exoPlayer.setVolume(1.0f); // Play with sound
            }
            exoPlayer.clearMediaItems();
            exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl));
            exoPlayer.prepare();
            exoPlayer.play();
        }

        void releasePlayer() {
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }
        }

        private int activityCount(@NonNull Photo photo) {
            Set<String> userIds = new HashSet<>();
            if (photo.getSeenBy() != null) {
                userIds.addAll(photo.getSeenBy());
            }
            if (photo.getReactions() != null) {
                userIds.addAll(photo.getReactions().keySet());
            }
            userIds.remove(null);
            userIds.remove("");
            return userIds.size();
        }
    }

    private static final DiffUtil.ItemCallback<Photo> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Photo>() {
                @Override
                public boolean areItemsTheSame(@NonNull Photo oldItem, @NonNull Photo newItem) {
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Photo oldItem, @NonNull Photo newItem) {
                    return Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl())
                            && Objects.equals(oldItem.getCaption(), newItem.getCaption())
                            && Objects.equals(oldItem.getCreatedAt(), newItem.getCreatedAt())
                            && Objects.equals(oldItem.getSeenBy(), newItem.getSeenBy())
                            && Objects.equals(oldItem.getReactions(), newItem.getReactions())
                            && Objects.equals(oldItem.getType(), newItem.getType())
                            && Objects.equals(oldItem.getVideoUrl(), newItem.getVideoUrl());
                }
            };
}
