package com.example.pocket;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pocket.data.model.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class RecipientSelectionAdapter
        extends RecyclerView.Adapter<RecipientSelectionAdapter.Holder> {
    public static final int ITEM_WIDTH_DP = 68;

    public interface SelectionListener {
        void onSelectionChanged(boolean allSelected, @NonNull Set<String> selectedIds);
    }

    private final List<User> friends = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();
    private final SelectionListener listener;
    private boolean allSelected = true;

    public RecipientSelectionAdapter(@NonNull SelectionListener listener) {
        this.listener = listener;
    }

    public void submitFriends(@NonNull List<User> nextFriends) {
        Map<String, User> uniqueFriends = new LinkedHashMap<>();
        for (User friend : nextFriends) {
            if (friend != null && friend.getId() != null
                    && !friend.getId().trim().isEmpty()) {
                uniqueFriends.put(friend.getId(), friend);
            }
        }
        friends.clear();
        friends.addAll(uniqueFriends.values());
        if (!allSelected) {
            selectedIds.removeIf(id -> !containsFriend(id));
        }
        notifyDataSetChanged();
        notifySelection();
    }

    public void selectAll() {
        allSelected = true;
        selectedIds.clear();
        notifyDataSetChanged();
        notifySelection();
    }

    public boolean isAllSelected() {
        return allSelected;
    }

    @NonNull
    public List<String> selectedReceiverIds() {
        List<String> result = new ArrayList<>();
        if (allSelected) {
            for (User friend : friends) {
                if (friend.getId() != null && !friend.getId().trim().isEmpty()) {
                    result.add(friend.getId());
                }
            }
        } else {
            result.addAll(selectedIds);
        }
        return result;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_capture_recipient, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (position == 0) {
            holder.bindAll(allSelected, clicked -> selectAll());
            return;
        }
        User friend = friends.get(position - 1);
        boolean selected = !allSelected && selectedIds.contains(friend.getId());
        holder.bindFriend(friend, selected, clicked -> toggle(friend));
    }

    @Override
    public int getItemCount() {
        return friends.size() + 1;
    }

    private void toggle(@NonNull User friend) {
        String id = friend.getId();
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        allSelected = false;
        if (!selectedIds.add(id)) {
            selectedIds.remove(id);
        }
        if (selectedIds.isEmpty()) {
            allSelected = true;
        }
        notifyDataSetChanged();
        notifySelection();
    }

    private boolean containsFriend(@NonNull String userId) {
        for (User friend : friends) {
            if (userId.equals(friend.getId())) {
                return true;
            }
        }
        return false;
    }

    private void notifySelection() {
        listener.onSelectionChanged(allSelected, new HashSet<>(selectedIds));
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final CircleImageView avatar;
        private final TextView initials;
        private final TextView name;
        private final View ring;

        Holder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.capture_recipient_avatar);
            initials = itemView.findViewById(R.id.capture_recipient_initials);
            name = itemView.findViewById(R.id.capture_recipient_name);
            ring = itemView.findViewById(R.id.capture_recipient_ring);
        }

        void bindAll(boolean selected, View.OnClickListener listener) {
            avatar.setPadding(dp(10), dp(10), dp(10), dp(10));
            avatar.setImageResource(R.drawable.ic_baseline_people_24);
            avatar.setColorFilter(itemView.getContext().getColor(R.color.pocket_text_primary));
            initials.setVisibility(View.GONE);
            name.setText(R.string.camera_all);
            setSelected(selected);
            itemView.setOnClickListener(listener);
        }

        void bindFriend(@NonNull User user, boolean selected, View.OnClickListener listener) {
            avatar.setPadding(0, 0, 0, 0);
            avatar.clearColorFilter();
            String displayName = user.getDisplayName();
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = itemView.getContext().getString(R.string.camera_default_user);
            }
            name.setText(displayName);
            String first = displayName.substring(0, 1).toUpperCase(java.util.Locale.getDefault());
            initials.setText(first);
            initials.setVisibility(user.getAvatarUrl() == null || user.getAvatarUrl().trim().isEmpty()
                    ? View.VISIBLE : View.GONE);
            Glide.with(avatar)
                    .load(user.getAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_placeholder)
                    .into(avatar);
            setSelected(selected);
            itemView.setOnClickListener(listener);
        }

        private void setSelected(boolean selected) {
            ring.setSelected(selected);
            name.setSelected(selected);
        }

        private int dp(int value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }
    }
}
