package com.example.pocket;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pocket.data.model.Photo;
import com.example.pocket.ui.AvatarView;
import com.example.pocket.ui.PocketButton;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class PhotoHistoryAdapter extends ListAdapter<Photo, PhotoHistoryAdapter.PhotoViewHolder> {
    public interface ReplyResult {
        void complete(boolean success);
    }

    public interface QuickReplyListener {
        void onQuickReply(@NonNull Photo photo,
                          @NonNull String content,
                          @NonNull String type,
                          @NonNull ReplyResult result);
    }

    private final String currentUserId;
    private final String currentUserAvatarUrl;
    private final QuickReplyListener quickReplyListener;
    private final View.OnClickListener profileClickListener;
    private final View.OnClickListener filterClickListener;
    private String filterLabel;

    public PhotoHistoryAdapter(@NonNull String currentUserId,
                               String currentUserAvatarUrl,
                               QuickReplyListener quickReplyListener,
                               View.OnClickListener profileClickListener,
                               View.OnClickListener filterClickListener,
                               @NonNull String filterLabel) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
        this.currentUserAvatarUrl = currentUserAvatarUrl;
        this.quickReplyListener = quickReplyListener;
        this.profileClickListener = profileClickListener;
        this.filterClickListener = filterClickListener;
        this.filterLabel = filterLabel;
    }

    public void setFilterLabel(@NonNull String filterLabel) {
        this.filterLabel = filterLabel;
        notifyItemRangeChanged(0, getItemCount());
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
        holder.bind(getItem(position), currentUserId, currentUserAvatarUrl,
                quickReplyListener, profileClickListener, filterClickListener, filterLabel);
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final AvatarView senderAvatar;
        private final CircleImageView profileAvatar;
        private final PocketButton recipientPill;
        private final View photoCard;
        private final TextView senderName;
        private final TextView timestamp;
        private final ImageView photoImage;
        private final TextView caption;
        private final EditText replyInput;
        private final TextView hotdogReply;
        private final TextView heartEyesReply;
        private final TextView heartsReply;
        private final TextView smileReply;
        private String boundPhotoId;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            senderAvatar = itemView.findViewById(R.id.history_sender_avatar);
            profileAvatar = itemView.findViewById(R.id.history_profile_avatar);
            recipientPill = itemView.findViewById(R.id.history_recipient_pill);
            photoCard = itemView.findViewById(R.id.history_photo_card);
            senderName = itemView.findViewById(R.id.history_sender_name);
            timestamp = itemView.findViewById(R.id.history_timestamp);
            photoImage = itemView.findViewById(R.id.history_photo_image);
            caption = itemView.findViewById(R.id.history_caption);
            replyInput = itemView.findViewById(R.id.history_reply_input);
            hotdogReply = itemView.findViewById(R.id.history_reaction_hotdog);
            heartEyesReply = itemView.findViewById(R.id.history_reaction_heart_eyes);
            heartsReply = itemView.findViewById(R.id.history_reaction_hearts);
            smileReply = itemView.findViewById(R.id.history_reaction_smile);
            photoCard.post(() -> {
                ViewGroup.LayoutParams params = photoCard.getLayoutParams();
                params.height = photoCard.getWidth();
                photoCard.setLayoutParams(params);
            });
        }

        void bind(@NonNull Photo photo,
                  @NonNull String currentUserId,
                  String currentUserAvatarUrl,
                  QuickReplyListener listener,
                  View.OnClickListener profileListener,
                  View.OnClickListener filterListener,
                  String filterLabel) {
            boundPhotoId = photo.getId();
            String displayName = photo.getSenderName();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = itemView.getContext().getString(R.string.camera_default_user);
            }
            senderAvatar.setAvatarText(displayName);
            senderName.setText(currentUserId.equals(photo.getSenderId())
                    ? itemView.getContext().getString(R.string.history_sender_you)
                    : displayName);

            if (photo.getCreatedAt() == null) {
                timestamp.setText(R.string.history_time_now);
            } else {
                timestamp.setText(DateUtils.getRelativeTimeSpanString(
                        photo.getCreatedAt().toDate().getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS));
            }

            String imageUrl = photo.getImageUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                imageUrl = photo.getThumbnailUrl();
            }
            Glide.with(photoImage)
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_pocket)
                    .error(R.drawable.placeholder_pocket)
                    .into(photoImage);
            Glide.with(profileAvatar)
                    .load(currentUserAvatarUrl)
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_placeholder)
                    .into(profileAvatar);
            profileAvatar.setOnClickListener(profileListener);
            recipientPill.setText(filterLabel);
            recipientPill.setOnClickListener(filterListener);

            String captionText = photo.getCaption();
            boolean hasCaption = captionText != null && !captionText.trim().isEmpty();
            caption.setVisibility(hasCaption ? View.VISIBLE : View.GONE);
            caption.setText(hasCaption ? captionText : "");

            boolean canReply = photo.getSenderId() != null
                    && !photo.getSenderId().trim().isEmpty()
                    && !currentUserId.equals(photo.getSenderId())
                    && listener != null;
            replyInput.setText(null);
            replyInput.setEnabled(canReply);
            replyInput.setHint(canReply
                    ? R.string.history_reply_hint
                    : R.string.history_reply_own_photo);
            replyInput.setOnEditorActionListener((view, actionId, event) -> {
                boolean sendAction = actionId == EditorInfo.IME_ACTION_SEND
                        || (event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        && event.getAction() == KeyEvent.ACTION_DOWN);
                if (!sendAction || !canReply) {
                    return false;
                }
                String content = replyInput.getText().toString().trim();
                if (content.isEmpty()) {
                    return true;
                }
                sendReply(photo, content, "text", listener);
                return true;
            });

            bindEmoji(hotdogReply, photo, canReply, listener);
            bindEmoji(heartEyesReply, photo, canReply, listener);
            bindEmoji(heartsReply, photo, canReply, listener);
            bindEmoji(smileReply, photo, canReply, listener);
        }

        private void bindEmoji(@NonNull TextView view,
                               @NonNull Photo photo,
                               boolean enabled,
                               QuickReplyListener listener) {
            view.setEnabled(enabled);
            view.setAlpha(enabled ? 1f : 0.35f);
            view.setOnClickListener(clicked -> {
                if (enabled) {
                    sendReply(photo, view.getText().toString(), "emoji", listener);
                }
            });
        }

        private void sendReply(@NonNull Photo photo,
                               @NonNull String content,
                               @NonNull String type,
                               @NonNull QuickReplyListener listener) {
            String sendingPhotoId = photo.getId();
            setReplyControlsEnabled(false);
            listener.onQuickReply(photo, content, type, success -> {
                if (!Objects.equals(boundPhotoId, sendingPhotoId)) {
                    return;
                }
                setReplyControlsEnabled(true);
                if (success) {
                    replyInput.setText(null);
                }
            });
        }

        private void setReplyControlsEnabled(boolean enabled) {
            replyInput.setEnabled(enabled);
            hotdogReply.setEnabled(enabled);
            heartEyesReply.setEnabled(enabled);
            heartsReply.setEnabled(enabled);
            smileReply.setEnabled(enabled);
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
                            && Objects.equals(oldItem.getReactions(), newItem.getReactions());
                }
            };
}
