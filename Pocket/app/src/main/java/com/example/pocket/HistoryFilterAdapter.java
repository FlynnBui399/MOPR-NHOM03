package com.example.pocket;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pocket.data.model.User;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class HistoryFilterAdapter extends RecyclerView.Adapter<HistoryFilterAdapter.Holder> {
    public interface FilterListener {
        void onFilterSelected(User user);
    }

    private final List<User> friends = new ArrayList<>();
    private final String selectedUserId;
    private final FilterListener listener;

    public HistoryFilterAdapter(@NonNull List<User> friends,
                                String selectedUserId,
                                @NonNull FilterListener listener) {
        this.friends.addAll(friends);
        this.selectedUserId = selectedUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_filter, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        User user = position == 0 ? null : friends.get(position - 1);
        holder.bind(user, user == null ? selectedUserId == null
                : user.getId() != null && user.getId().equals(selectedUserId), listener);
    }

    @Override
    public int getItemCount() {
        return friends.size() + 1;
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final CircleImageView avatar;
        private final TextView name;
        private final TextView subtitle;
        private final ImageView check;

        Holder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.history_filter_avatar);
            name = itemView.findViewById(R.id.history_filter_name);
            subtitle = itemView.findViewById(R.id.history_filter_subtitle);
            check = itemView.findViewById(R.id.history_filter_check);
        }

        void bind(User user, boolean selected, @NonNull FilterListener listener) {
            if (user == null) {
                avatar.setPadding(dp(10), dp(10), dp(10), dp(10));
                avatar.setImageResource(R.drawable.ic_baseline_people_24);
                avatar.setColorFilter(itemView.getContext().getColor(R.color.pocket_text_primary));
                name.setText(R.string.history_filter_everyone);
                subtitle.setText(R.string.history_filter_everyone_hint);
                subtitle.setVisibility(View.VISIBLE);
            } else {
                avatar.setPadding(0, 0, 0, 0);
                avatar.clearColorFilter();
                String displayName = user.getDisplayName();
                name.setText(displayName == null || displayName.trim().isEmpty()
                        ? itemView.getContext().getString(R.string.camera_default_user)
                        : displayName);
                subtitle.setVisibility(View.GONE);
                Glide.with(avatar)
                        .load(user.getAvatarUrl())
                        .circleCrop()
                        .placeholder(R.drawable.avatar_placeholder)
                        .error(R.drawable.avatar_placeholder)
                        .into(avatar);
            }
            check.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
            itemView.setSelected(selected);
            itemView.setOnClickListener(clicked -> listener.onFilterSelected(user));
        }

        private int dp(int value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }
    }
}
