package com.example.pocket;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pocket.data.model.Photo;
import com.example.pocket.data.model.User;
import com.example.pocket.data.repository.UserRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
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
}
