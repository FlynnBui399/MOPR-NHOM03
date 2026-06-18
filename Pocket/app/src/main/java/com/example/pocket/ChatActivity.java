package com.example.pocket;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.pocket.data.model.Message;
import com.example.pocket.data.repository.UserRepository;
import com.example.pocket.utils.FcmHelper;
import com.example.pocket.viewmodel.ChatViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_FRIEND_UID = "friendUid";
    public static final String EXTRA_FRIEND_NAME = "friendName";
    public static final String EXTRA_FRIEND_AVATAR = "friendAvatar";

    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    private EditText messageInput;
    private RecyclerView messagesRecycler;
    private ExecutorService notificationExecutor;
    private String friendUid;
    private String friendName;
    private com.google.firebase.firestore.ListenerRegistration streakListenerRegistration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        View chatRoot = findViewById(R.id.chat_root);
        MaterialToolbar toolbar = findViewById(R.id.chat_toolbar);
        LinearLayout composerContainer = findViewById(R.id.chat_composer_container);

        ViewCompat.setOnApplyWindowInsetsListener(chatRoot, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;

            toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    statusBarHeight,
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom()
            );

            int basePaddingBottom = (int) (16 * v.getResources().getDisplayMetrics().density);
            int bottomPadding = Math.max(navBarHeight, imeHeight);
            composerContainer.setPadding(
                    composerContainer.getPaddingLeft(),
                    composerContainer.getPaddingTop(),
                    composerContainer.getPaddingRight(),
                    basePaddingBottom + bottomPadding
            );

            return insets;
        });

        notificationExecutor = Executors.newSingleThreadExecutor();

        friendUid = getIntent().getStringExtra(EXTRA_FRIEND_UID);
        friendName = getIntent().getStringExtra(EXTRA_FRIEND_NAME);
        String friendAvatar = getIntent().getStringExtra(EXTRA_FRIEND_AVATAR);

        if (friendUid == null || friendUid.trim().isEmpty()) {
            finish();
            return;
        }

        ImageButton backButton = findViewById(R.id.chat_back_button);
        CircleImageView avatar = findViewById(R.id.chat_other_avatar);
        TextView name = findViewById(R.id.chat_other_name);
        TextView streakBadge = findViewById(R.id.chat_streak_badge);
        messageInput = findViewById(R.id.chat_message_input);
        ImageButton sendButton = findViewById(R.id.chat_send_button);
        messagesRecycler = findViewById(R.id.chat_messages_recycler);

        backButton.setOnClickListener(v -> finish());

        name.setText(friendName == null || friendName.trim().isEmpty()
                ? getString(R.string.camera_default_user)
                : friendName);

        Glide.with(this)
                .load(friendAvatar)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.avatar_placeholder)
                .error(R.drawable.avatar_placeholder)
                .into(avatar);

        adapter = new MessageAdapter(currentUid());
        messagesRecycler.setLayoutManager(new LinearLayoutManager(this));
        messagesRecycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        viewModel.initialize(friendUid);

        viewModel.messages.observe(this, messages -> {
            adapter.submit(messages);
            if (messages != null && !messages.isEmpty()) {
                messagesRecycler.scrollToPosition(messages.size() - 1);
            }
        });

        viewModel.errorMessage.observe(this, message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });

        sendButton.setOnClickListener(v -> sendText());
        com.example.pocket.utils.ViewUtils.applyPressAnimation(sendButton);

        messageInput.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                messageInput.setHint(null);
            } else if (messageInput.getText() == null
                    || messageInput.getText().toString().trim().isEmpty()) {
                messageInput.setHint(R.string.chat_message_hint);
            }
        });

        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendText();
                return true;
            }
            return false;
        });

        int[] emojiIds = {
                R.id.quick_reply_smile,
                R.id.quick_reply_laugh,
                R.id.quick_reply_thumb,
                R.id.quick_reply_heart,
                R.id.quick_reply_surprise
        };

        for (int emojiId : emojiIds) {
            TextView emoji = findViewById(emojiId);
            emoji.setOnClickListener(v -> sendEmoji(((TextView) v).getText().toString()));
            com.example.pocket.utils.ViewUtils.applyPressAnimation(emoji);
        }

        UserRepository.getInstance().getUserById(friendUid, new UserRepository.Callback<com.example.pocket.data.model.User>() {
            @Override
            public void onSuccess(com.example.pocket.data.model.User result) {
                if (result == null) {
                    return;
                }

                name.setText(result.getDisplayName() == null ? name.getText() : result.getDisplayName());

                Glide.with(ChatActivity.this)
                        .load(result.getAvatarUrl())
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.avatar_placeholder)
                        .error(R.drawable.avatar_placeholder)
                        .into(avatar);
            }

            @Override
            public void onError(@NonNull Exception error) {
                Toast.makeText(ChatActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        if (!currentUid().isEmpty() && friendUid != null) {
            streakListenerRegistration = com.example.pocket.utils.StreakHelper.listenStreak(currentUid(), friendUid, count -> {
                if (streakBadge != null) {
                    if (count >= 2) {
                        streakBadge.setText("🔥 " + count);
                        streakBadge.setVisibility(View.VISIBLE);
                    } else {
                        streakBadge.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    private void sendText() {
        String content = messageInput.getText() == null ? "" : messageInput.getText().toString().trim();
        if (!content.isEmpty()) {
            viewModel.sendMessage(content, () -> sendFcmNotification(content));
            messageInput.setText(null);
        }
    }

    private void sendEmoji(@NonNull String emoji) {
        if (!emoji.trim().isEmpty()) {
            viewModel.sendEmoji(emoji, () -> sendFcmNotification(emoji));
        }
    }

    private void sendFcmNotification(@NonNull String content) {
        if (notificationExecutor == null
                || notificationExecutor.isShutdown()
                || friendUid == null
                || friendUid.trim().isEmpty()) {
            return;
        }

        String senderName = currentSenderName();
        notificationExecutor.execute(() ->
                FcmHelper.sendMessageNotification(friendUid, senderName, content));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.markMessagesRead();
        }
    }

    @NonNull
    private String currentUid() {
        return FirebaseAuth.getInstance().getCurrentUser() == null
                ? ""
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    private String currentSenderName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName();
        }
        return getString(R.string.camera_default_user);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (streakListenerRegistration != null) {
            streakListenerRegistration.remove();
            streakListenerRegistration = null;
        }
        if (notificationExecutor != null) {
            notificationExecutor.shutdown();
        }
    }

    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.Holder> {
        private static final int VIEW_TYPE_TEXT = 1;
        private static final int VIEW_TYPE_PHOTO_REPLY = 2;

        private final List<Message> messages = new ArrayList<>();
        private final String currentUid;

        MessageAdapter(String currentUid) {
            this.currentUid = currentUid;
        }

        void submit(List<Message> nextMessages) {
            messages.clear();
            if (nextMessages != null) {
                messages.addAll(nextMessages);
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            Message message = messages.get(position);
            if (message.getType() != null && message.getType().equals("photo_reply")) {
                return VIEW_TYPE_PHOTO_REPLY;
            }
            return VIEW_TYPE_TEXT;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_PHOTO_REPLY) {
                return new Holder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_message_photo_reply, parent, false));
            }
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false));
        }

        private boolean isEmojiOnly(String text) {
            if (text == null) return false;
            text = text.trim();
            if (text.isEmpty()) return false;
            
            int count = 0;
            for (int i = 0; i < text.length(); ) {
                int codePoint = text.codePointAt(i);
                if (!isEmojiCodePoint(codePoint) && !Character.isWhitespace(codePoint) && codePoint != 0xFE0F) {
                    return false;
                }
                if (!Character.isWhitespace(codePoint) && codePoint != 0xFE0F) {
                    count++;
                }
                i += Character.charCount(codePoint);
            }
            return count >= 1 && count <= 4;
        }

        private boolean isEmojiCodePoint(int codePoint) {
            return (codePoint >= 0x1F300 && codePoint <= 0x1F9FF) || 
                   (codePoint >= 0x2600 && codePoint <= 0x27BF) ||
                   (codePoint >= 0x1F000 && codePoint <= 0x1F0FF) ||
                   (codePoint >= 0x1F100 && codePoint <= 0x1F1FF) ||
                   (codePoint >= 0x1F200 && codePoint <= 0x1F2FF) ||
                   (codePoint >= 0x1F900 && codePoint <= 0x1F9FF) ||
                   (codePoint >= 0x1FA00 && codePoint <= 0x1FAFF) ||
                   (codePoint >= 0xE0000 && codePoint <= 0xE007F) ||
                   (codePoint == 0x2705) || (codePoint == 0x270A) || (codePoint == 0x270B) ||
                   (codePoint == 0x2728) || (codePoint == 0x274C) || (codePoint == 0x274E) ||
                   (codePoint == 0x2753) || (codePoint == 0x2757) || (codePoint == 0x2795) ||
                   (codePoint == 0x2796) || (codePoint == 0x2797) || (codePoint == 0x27B0) ||
                   (codePoint == 0x27BF) || (codePoint == 0x2934) || (codePoint == 0x2935) ||
                   (codePoint == 0x2B05) || (codePoint == 0x2B06) || (codePoint == 0x2B07) ||
                   (codePoint == 0x2B1B) || (codePoint == 0x2B1C) || (codePoint == 0x2B50) ||
                   (codePoint == 0x2B55) || (codePoint == 0x3030) || (codePoint == 0x303D) ||
                   (codePoint == 0x3297) || (codePoint == 0x3299);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Message message = messages.get(position);
            boolean sent = currentUid.equals(message.getSenderId());

            if (getItemViewType(position) == VIEW_TYPE_PHOTO_REPLY) {
                if (holder.quotedBox != null) {
                    holder.quotedBox.setVisibility(View.VISIBLE);
                }
                
                if (holder.ivQuotedThumb != null) {
                    Glide.with(ChatActivity.this)
                            .load(message.getQuotedPhotoUrl())
                            .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade(250))
                            .placeholder(new android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#1C1C1E")))
                            .error(R.drawable.placeholder_pocket)
                            .into(holder.ivQuotedThumb);
                }

                if (holder.ivQuotedPlayIcon != null) {
                    boolean isVideo = "video".equals(message.getQuotedPhotoType());
                    holder.ivQuotedPlayIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);
                }

                if (holder.tvQuotedCaption != null) {
                    String caption = message.getQuotedPhotoCaption();
                    if (caption != null && !caption.trim().isEmpty()) {
                        holder.tvQuotedCaption.setText(caption);
                        holder.tvQuotedCaption.setVisibility(View.VISIBLE);
                    } else {
                        holder.tvQuotedCaption.setVisibility(View.GONE);
                    }
                }

                String senderName = sent ? "You" : (friendName != null ? friendName : "Friend");
                if (holder.tvQuotedLabel != null) {
                    holder.tvQuotedLabel.setText("Pocket from " + senderName);
                }

                String replyText = message.getText() == null ? "" : message.getText();
                boolean isEmoji = isEmojiOnly(replyText);

                if (holder.tvReplyText != null) {
                    holder.tvReplyText.setText(replyText);
                    if (isEmoji) {
                        holder.tvReplyText.setTextSize(36f);
                        holder.tvReplyText.setBackground(null);
                        holder.tvReplyText.setPadding(0, 0, 0, 0);
                    } else {
                        holder.tvReplyText.setTextSize(15f);
                        float density = getResources().getDisplayMetrics().density;
                        int horizontalPadding = (int) (12 * density);
                        int verticalPadding = (int) (8 * density);
                        holder.tvReplyText.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
                        if (sent) {
                            holder.tvReplyText.setBackgroundResource(R.drawable.bg_chat_bubble_sent);
                            holder.tvReplyText.setTextColor(android.graphics.Color.WHITE);
                        } else {
                            holder.tvReplyText.setBackgroundResource(R.drawable.bg_chat_bubble_received_dark);
                            holder.tvReplyText.setTextColor(android.graphics.Color.WHITE);
                        }
                    }
                    android.widget.LinearLayout.LayoutParams textParams =
                            (android.widget.LinearLayout.LayoutParams) holder.tvReplyText.getLayoutParams();
                    textParams.gravity = sent ? android.view.Gravity.END : android.view.Gravity.START;
                    holder.tvReplyText.setLayoutParams(textParams);
                }

                if (holder.replyContainer != null) {
                    android.widget.FrameLayout.LayoutParams params =
                            (android.widget.FrameLayout.LayoutParams) holder.replyContainer.getLayoutParams();
                    params.gravity = sent ? android.view.Gravity.END : android.view.Gravity.START;
                    float density = getResources().getDisplayMetrics().density;
                    int largeMargin = (int) (64 * density);
                    int smallMargin = (int) (16 * density);
                    params.leftMargin = sent ? largeMargin : smallMargin;
                    params.rightMargin = sent ? smallMargin : largeMargin;
                    holder.replyContainer.setLayoutParams(params);
                }
            } else {
                String content = message.getContent() == null ? "" : message.getContent();
                boolean isEmoji = isEmojiOnly(content);

                if (holder.sentBubble != null) {
                    holder.sentBubble.setVisibility(sent ? View.VISIBLE : View.GONE);
                    if (sent) {
                        holder.sentBubble.setText(content);
                        if (isEmoji) {
                            holder.sentBubble.setTextSize(36f);
                            holder.sentBubble.setBackground(null);
                            holder.sentBubble.setPadding(0, 0, 0, 0);
                        } else {
                            holder.sentBubble.setTextSize(15f);
                            holder.sentBubble.setBackgroundResource(R.drawable.bg_chat_bubble_sent);
                            float density = getResources().getDisplayMetrics().density;
                            int horizontalPadding = (int) (12 * density);
                            int verticalPadding = (int) (8 * density);
                            holder.sentBubble.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
                        }
                    }
                }
                if (holder.receivedBubble != null) {
                    holder.receivedBubble.setVisibility(sent ? View.GONE : View.VISIBLE);
                    if (!sent) {
                        holder.receivedBubble.setText(content);
                        if (isEmoji) {
                            holder.receivedBubble.setTextSize(36f);
                            holder.receivedBubble.setBackground(null);
                            holder.receivedBubble.setPadding(0, 0, 0, 0);
                        } else {
                            holder.receivedBubble.setTextSize(15f);
                            holder.receivedBubble.setBackgroundResource(R.drawable.bg_chat_bubble_received_dark);
                            float density = getResources().getDisplayMetrics().density;
                            int horizontalPadding = (int) (12 * density);
                            int verticalPadding = (int) (8 * density);
                            holder.receivedBubble.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
                        }
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        private class Holder extends RecyclerView.ViewHolder {
            final TextView receivedBubble;
            final TextView sentBubble;
            final View replyContainer;
            final View quotedBox;
            final ImageView ivQuotedThumb;
            final ImageView ivQuotedPlayIcon;
            final TextView tvQuotedCaption;
            final TextView tvQuotedLabel;
            final TextView tvReplyText;

            Holder(@NonNull View itemView) {
                super(itemView);
                receivedBubble = itemView.findViewById(R.id.chat_received_bubble);
                sentBubble = itemView.findViewById(R.id.chat_sent_bubble);
                replyContainer = itemView.findViewById(R.id.chat_reply_container);
                quotedBox = itemView.findViewById(R.id.quotedBox);
                ivQuotedThumb = itemView.findViewById(R.id.ivQuotedThumb);
                ivQuotedPlayIcon = itemView.findViewById(R.id.ivQuotedPlayIcon);
                tvQuotedCaption = itemView.findViewById(R.id.tvQuotedCaption);
                tvQuotedLabel = itemView.findViewById(R.id.tvQuotedLabel);
                tvReplyText = itemView.findViewById(R.id.tvReplyText);
            }
        }
    }
}

