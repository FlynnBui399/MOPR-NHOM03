package com.example.pocket;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pocket.data.model.Photo;
import com.example.pocket.data.model.User;
import com.example.pocket.data.repository.UserRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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

    public interface ActivityClickListener {
        void onActivityClick(@NonNull Photo photo);
    }

    private final String currentUserId;
    private final QuickReplyListener quickReplyListener;
    private final ActivityClickListener activityClickListener;
    private final UserRepository userRepository = UserRepository.getInstance();
    private final Map<String, User> senderCache = new HashMap<>();
    private final Set<String> requestedSenderIds = new HashSet<>();

    public PhotoHistoryAdapter(@NonNull String currentUserId,
                               QuickReplyListener quickReplyListener,
                               ActivityClickListener activityClickListener) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
        this.quickReplyListener = quickReplyListener;
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
        holder.bind(photo, sender, currentUserId, quickReplyListener, activityClickListener);
        requestSenderIfNeeded(photo.getSenderId());
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
        private static final String[] PICKER_EMOJIS = {
                "\uD83D\uDC95", "\uD83D\uDE31", "\uD83D\uDD25", "\uD83D\uDE02",
                "\uD83E\uDD23", "\uD83D\uDE0D", "\uD83E\uDD79", "\uD83D\uDE2D",
                "\uD83E\uDEF6", "\uD83D\uDC40", "\uD83D\uDC80", "\uD83D\uDE0E",
                "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83C\uDF89", "\uD83D\uDC90",
                "\u2728", "\uD83D\uDE2E\u200D\uD83D\uDCA8", "\uD83E\uDD21", "\uD83D\uDE0B",
                "\u2764\uFE0F\u200D\uD83D\uDD25", "\uD83D\uDE24", "\uD83D\uDE43", "\uD83E\uDD70",
                "\uD83E\uDD1D", "\uD83E\uDD29", "\uD83D\uDE4C", "\uD83E\uDD73"
        };

        private final CircleImageView senderAvatar;
        private final View photoCard;
        private final TextView senderName;
        private final TextView timestamp;
        private final ImageView photoImage;
        private final TextView caption;
        private final TextView activityButton;
        private final View replyRow;
        private final EditText replyInput;
        private final TextView heartsReply;
        private final TextView shockReply;
        private final TextView fireReply;
        private final TextView moreReply;
        private String boundPhotoId;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            senderAvatar = itemView.findViewById(R.id.history_sender_avatar);
            photoCard = itemView.findViewById(R.id.history_photo_card);
            senderName = itemView.findViewById(R.id.history_sender_name);
            timestamp = itemView.findViewById(R.id.history_timestamp);
            photoImage = itemView.findViewById(R.id.history_photo_image);
            caption = itemView.findViewById(R.id.history_caption);
            activityButton = itemView.findViewById(R.id.history_activity_button);
            replyRow = itemView.findViewById(R.id.history_reply_row);
            replyInput = itemView.findViewById(R.id.history_reply_input);
            heartsReply = itemView.findViewById(R.id.history_reaction_hearts);
            shockReply = itemView.findViewById(R.id.history_reaction_shock);
            fireReply = itemView.findViewById(R.id.history_reaction_fire);
            moreReply = itemView.findViewById(R.id.history_reaction_more);
            photoCard.post(() -> {
                ViewGroup.LayoutParams params = photoCard.getLayoutParams();
                params.height = photoCard.getWidth();
                photoCard.setLayoutParams(params);
            });
        }

        void bind(@NonNull Photo photo,
                  User sender,
                  @NonNull String currentUserId,
                  QuickReplyListener listener,
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
            String avatarUrl = sender == null ? null : sender.getAvatarUrl();
            Glide.with(senderAvatar)
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_placeholder)
                    .into(senderAvatar);

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
            String captionText = photo.getCaption();
            boolean hasCaption = captionText != null && !captionText.trim().isEmpty();
            caption.setVisibility(hasCaption ? View.VISIBLE : View.GONE);
            caption.setText(hasCaption ? captionText : "");

            boolean canReply = photo.getSenderId() != null
                    && !photo.getSenderId().trim().isEmpty()
                    && !currentUserId.equals(photo.getSenderId())
                    && listener != null;
            replyRow.setVisibility(canReply ? View.VISIBLE : View.GONE);
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

            bindEmoji(heartsReply, photo, canReply, listener);
            bindEmoji(shockReply, photo, canReply, listener);
            bindEmoji(fireReply, photo, canReply, listener);
            moreReply.setEnabled(canReply);
            moreReply.setAlpha(canReply ? 1f : 0.35f);
            moreReply.setOnClickListener(clicked -> {
                if (canReply && listener != null) {
                    showEmojiPicker(photo, listener);
                }
            });

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

        private int activityCount(@NonNull Photo photo) {
            java.util.Set<String> userIds = new java.util.HashSet<>();
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

        private void showEmojiPicker(@NonNull Photo photo,
                                     @NonNull QuickReplyListener listener) {
            Context context = itemView.getContext();
            BottomSheetDialog dialog = new BottomSheetDialog(context);
            View content = LayoutInflater.from(context)
                    .inflate(R.layout.bottom_sheet_emoji_picker, null, false);
            GridLayout grid = content.findViewById(R.id.history_emoji_grid);
            for (int index = 0; index < PICKER_EMOJIS.length; index++) {
                String emoji = PICKER_EMOJIS[index];
                TextView option = new TextView(context);
                option.setText(emoji);
                option.setTextSize(27f);
                option.setGravity(Gravity.CENTER);
                option.setBackgroundResource(R.drawable.bg_filter_row);
                option.setContentDescription(emoji);
                int margin = dp(context, 4);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(index / 4), GridLayout.spec(index % 4, 1, 1f));
                params.width = 0;
                params.height = dp(context, 58);
                params.setMargins(margin, margin, margin, margin);
                option.setLayoutParams(params);
                option.setOnClickListener(clicked -> {
                    dialog.dismiss();
                    sendReply(photo, emoji, "emoji", listener);
                });
                grid.addView(option);
            }
            dialog.setContentView(content);
            dialog.show();
        }

        private static int dp(@NonNull Context context, int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
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
            heartsReply.setEnabled(enabled);
            shockReply.setEnabled(enabled);
            fireReply.setEnabled(enabled);
            moreReply.setEnabled(enabled);
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
                            && Objects.equals(oldItem.getReactions(), newItem.getReactions());
                }
            };
}
