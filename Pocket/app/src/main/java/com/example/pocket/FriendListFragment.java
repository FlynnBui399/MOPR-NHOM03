package com.example.pocket;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.pocket.data.model.User;
import com.example.pocket.ui.PocketButton;
import com.example.pocket.viewmodel.FriendViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendListFragment extends Fragment {
    private FriendViewModel viewModel;
    private FriendAdapter friendAdapter;
    private RequestAdapter requestAdapter;
    private View emptyState;
    private RecyclerView friendsRecycler;
    private RecyclerView requestsRecycler;
    private TextView requestsTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FriendViewModel.class);
        emptyState = view.findViewById(R.id.friend_empty_state);
        friendsRecycler = view.findViewById(R.id.friend_list_recycler);
        requestsRecycler = view.findViewById(R.id.friend_requests_recycler);
        requestsTitle = view.findViewById(R.id.friend_requests_title);
        FloatingActionButton addButton = view.findViewById(R.id.friend_add_fab);
        PocketButton emptyAddButton = view.findViewById(R.id.friend_empty_add_button);

        friendAdapter = new FriendAdapter(new ArrayList<>(), new FriendActionListener() {
            @Override
            public void onMessage(@NonNull User user) {
                openChat(user);
            }

            @Override
            public void onRemove(@NonNull User user) {
                viewModel.removeFriend(user.getId());
            }
        });
        requestAdapter = new RequestAdapter(new ArrayList<>(), new RequestActionListener() {
            @Override
            public void onAccept(@NonNull User user) {
                viewModel.acceptRequest(user.getId());
            }

            @Override
            public void onDecline(@NonNull User user) {
                viewModel.declineRequest(user.getId());
            }
        });

        friendsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        friendsRecycler.setAdapter(friendAdapter);
        requestsRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        requestsRecycler.setAdapter(requestAdapter);

        addButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddFriendActivity.class)));
        emptyAddButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddFriendActivity.class)));

        viewModel.friends.observe(getViewLifecycleOwner(), users -> {
            friendAdapter.submit(users);
            boolean empty = users == null || users.isEmpty();
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            friendsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
        viewModel.pendingRequests.observe(getViewLifecycleOwner(), users -> {
            requestAdapter.submit(users);
            boolean empty = users == null || users.isEmpty();
            requestsTitle.setVisibility(empty ? View.GONE : View.VISIBLE);
            requestsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
        viewModel.errorMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openChat(@NonNull User user) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_FRIEND_UID, user.getId());
        intent.putExtra(ChatActivity.EXTRA_FRIEND_NAME, displayName(user));
        intent.putExtra(ChatActivity.EXTRA_FRIEND_AVATAR, user.getAvatarUrl());
        startActivity(intent);
    }

    @NonNull
    private static String displayName(@NonNull User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            return user.getDisplayName();
        }
        return "Pocket User";
    }

    private interface FriendActionListener {
        void onMessage(@NonNull User user);

        void onRemove(@NonNull User user);
    }

    private interface RequestActionListener {
        void onAccept(@NonNull User user);

        void onDecline(@NonNull User user);
    }

    private static class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.Holder> {
        private final List<User> users;
        private final FriendActionListener listener;

        FriendAdapter(List<User> users, FriendActionListener listener) {
            this.users = users;
            this.listener = listener;
        }

        void submit(List<User> nextUsers) {
            users.clear();
            if (nextUsers != null) {
                users.addAll(nextUsers);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            User user = users.get(position);
            holder.name.setText(displayName(user));
            holder.phone.setText(user.getPhoneNumber() == null ? "" : user.getPhoneNumber());
            holder.phone.setVisibility(View.GONE);
            Glide.with(holder.avatar)
                    .load(user.getAvatarUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_placeholder)
                    .into(holder.avatar);
            holder.messageButton.setOnClickListener(v -> listener.onMessage(user));
            holder.removeButton.setOnClickListener(v -> listener.onRemove(user));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        private static class Holder extends RecyclerView.ViewHolder {
            final CircleImageView avatar;
            final TextView name;
            final TextView phone;
            final PocketButton messageButton;
            final View removeButton;

            Holder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.friend_item_avatar);
                name = itemView.findViewById(R.id.friend_item_name);
                phone = itemView.findViewById(R.id.friend_item_phone);
                messageButton = itemView.findViewById(R.id.friend_item_message_button);
                removeButton = itemView.findViewById(R.id.friend_item_remove_button);
            }
        }
    }

    private static class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.Holder> {
        private final List<User> users;
        private final RequestActionListener listener;

        RequestAdapter(List<User> users, RequestActionListener listener) {
            this.users = users;
            this.listener = listener;
        }

        void submit(List<User> nextUsers) {
            users.clear();
            if (nextUsers != null) {
                users.addAll(nextUsers);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend_request, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            User user = users.get(position);
            holder.name.setText(displayName(user));
            Glide.with(holder.avatar)
                    .load(user.getAvatarUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_placeholder)
                    .into(holder.avatar);
            holder.acceptButton.setOnClickListener(v -> listener.onAccept(user));
            holder.declineButton.setOnClickListener(v -> listener.onDecline(user));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        private static class Holder extends RecyclerView.ViewHolder {
            final CircleImageView avatar;
            final TextView name;
            final PocketButton acceptButton;
            final PocketButton declineButton;

            Holder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.request_item_avatar);
                name = itemView.findViewById(R.id.request_item_name);
                acceptButton = itemView.findViewById(R.id.request_item_accept_button);
                declineButton = itemView.findViewById(R.id.request_item_decline_button);
            }
        }
    }
}
