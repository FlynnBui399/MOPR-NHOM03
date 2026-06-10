package com.example.pocket;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.pocket.data.model.Message;
import com.example.pocket.data.repository.UserRepository;
import com.example.pocket.viewmodel.ChatViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    public static final String EXTRA_FRIEND_UID = "friendUid";
    public static final String EXTRA_FRIEND_NAME = "friendName";
    public static final String EXTRA_FRIEND_AVATAR = "friendAvatar";

    private ChatViewModel viewModel;
    private MessageAdapter adapter;
    private EditText messageInput;
    private RecyclerView messagesRecycler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String friendUid = getIntent().getStringExtra(EXTRA_FRIEND_UID);
        String friendName = getIntent().getStringExtra(EXTRA_FRIEND_NAME);
        String friendAvatar = getIntent().getStringExtra(EXTRA_FRIEND_AVATAR);
        if (friendUid == null || friendUid.trim().isEmpty()) {
            finish();
            return;
        }

        ImageButton backButton = findViewById(R.id.chat_back_button);
        CircleImageView avatar = findViewById(R.id.chat_other_avatar);
        TextView name = findViewById(R.id.chat_other_name);
        messageInput = findViewById(R.id.chat_message_input);
        ImageButton sendButton = findViewById(R.id.chat_send_button);
        messagesRecycler = findViewById(R.id.chat_messages_recycler);

        backButton.setOnClickListener(v -> finish());
        name.setText(friendName == null || friendName.trim().isEmpty() ? getString(R.string.camera_default_user) : friendName);
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
                R.id.quick_reply_surprise,
                R.id.quick_reply_heart
        };
        for (int emojiId : emojiIds) {
            TextView emoji = findViewById(emojiId);
            emoji.setOnClickListener(v -> viewModel.sendEmoji(((TextView) v).getText().toString()));
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
    }

    private void sendText() {
        String content = messageInput.getText() == null ? "" : messageInput.getText().toString().trim();
        if (!content.isEmpty()) {
            viewModel.sendMessage(content);
            messageInput.setText(null);
        }
    }

    @NonNull
    private String currentUid() {
        return FirebaseAuth.getInstance().getCurrentUser() == null
                ? ""
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.Holder> {
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

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat_message, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Message message = messages.get(position);
            boolean sent = currentUid.equals(message.getSenderId());
            String content = message.getContent() == null ? "" : message.getContent();
            holder.sentBubble.setVisibility(sent ? View.VISIBLE : View.GONE);
            holder.receivedBubble.setVisibility(sent ? View.GONE : View.VISIBLE);
            holder.sentBubble.setText(content);
            holder.receivedBubble.setText(content);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        private static class Holder extends RecyclerView.ViewHolder {
            final TextView receivedBubble;
            final TextView sentBubble;

            Holder(@NonNull View itemView) {
                super(itemView);
                receivedBubble = itemView.findViewById(R.id.chat_received_bubble);
                sentBubble = itemView.findViewById(R.id.chat_sent_bubble);
            }
        }
    }
}
