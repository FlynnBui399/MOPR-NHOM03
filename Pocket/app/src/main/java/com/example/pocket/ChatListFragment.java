package com.example.pocket;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.pocket.data.model.Conversation;
import com.example.pocket.data.model.Message;
import com.example.pocket.data.model.User;
import com.example.pocket.viewmodel.ChatListViewModel;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatListFragment extends Fragment {
    private ChatListViewModel viewModel;
    private ConversationAdapter adapter;
    private View emptyState;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatListViewModel.class);

        emptyState = view.findViewById(R.id.chat_list_empty_state);
        recyclerView = view.findViewById(R.id.chat_list_recycler);
        progressBar = view.findViewById(R.id.chat_list_progress);

        adapter = new ConversationAdapter(new ArrayList<>(), this::openChat);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.conversations.observe(getViewLifecycleOwner(), list -> {
            adapter.submit(list);
            boolean empty = list == null || list.isEmpty();
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.errorMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openChat(@NonNull User friend) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_UID, friend.getId());
        intent.putExtra(ChatActivity.EXTRA_FRIEND_NAME, displayName(friend));
        intent.putExtra(ChatActivity.EXTRA_FRIEND_AVATAR, friend.getAvatarUrl());
        startActivity(intent);
    }

    @NonNull
    private static String displayName(@NonNull User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName();
        }
        if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
            return user.getPhoneNumber();
        }
        return "Pocket User";
    }

    private interface ConversationClickListener {
        void onClick(@NonNull User friend);
    }

    private static class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.Holder> {
        private final List<Conversation> items;
        private final ConversationClickListener clickListener;

        ConversationAdapter(List<Conversation> items, ConversationClickListener clickListener) {
            this.items = items;
            this.clickListener = clickListener;
        }

        void submit(List<Conversation> nextItems) {
            items.clear();
            if (nextItems != null) {
                items.addAll(nextItems);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_conversation, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Conversation conversation = items.get(position);
            User friend = conversation.getFriend();
            Message lastMessage = conversation.getLastMessage();

            holder.name.setText(displayName(friend));

            String text = "";
            if (lastMessage != null) {
                if ("photo".equalsIgnoreCase(lastMessage.getType())) {
                    text = "[Hình ảnh]";
                } else {
                    text = lastMessage.getText();
                }
            }
            holder.lastMessage.setText(text);
            holder.time.setText(formatTimestamp(lastMessage != null ? lastMessage.getCreatedAt() : null));

            Glide.with(holder.avatar)
                    .load(friend.getAvatarUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_placeholder)
                    .into(holder.avatar);

            holder.itemView.setOnClickListener(v -> clickListener.onClick(friend));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private String formatTimestamp(com.google.firebase.Timestamp timestamp) {
            if (timestamp == null) return "";
            java.util.Date date = timestamp.toDate();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            java.util.Calendar cal1 = java.util.Calendar.getInstance();
            java.util.Calendar cal2 = java.util.Calendar.getInstance();
            cal1.setTime(new java.util.Date());
            cal2.setTime(date);
            if (cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                    cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)) {
                return sdf.format(date);
            } else {
                java.text.SimpleDateFormat dateSdf = new java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault());
                return dateSdf.format(date);
            }
        }

        private static class Holder extends RecyclerView.ViewHolder {
            final CircleImageView avatar;
            final TextView name;
            final TextView lastMessage;
            final TextView time;

            Holder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.conversation_avatar);
                name = itemView.findViewById(R.id.conversation_name);
                lastMessage = itemView.findViewById(R.id.conversation_last_message);
                time = itemView.findViewById(R.id.conversation_time);
            }
        }
    }
}
