package com.example.pocket;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pocket.data.model.Photo;
import com.google.android.material.card.MaterialCardView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MemoriesViewerDialogFragment extends DialogFragment {
    private static final String ARG_URLS = "urls";
    private static final String ARG_THUMBNAILS = "thumbnails";
    private static final String ARG_TIMESTAMPS = "timestamps";
    private static final String ARG_SELECTED = "selected";

    private static final String ARG_TYPES = "types";
    private static final String ARG_VIDEO_URLS = "video_urls";

    private final List<MemoryItem> items = new ArrayList<>();
    private TextView year;
    private TextView date;
    private TextView time;
    private RecyclerView photoPager;
    private ThumbnailAdapter thumbnailAdapter;
    private int selectedIndex;
    private ExoPlayer player;

    @NonNull
    public static MemoriesViewerDialogFragment newInstance(@NonNull ArrayList<Photo> photos,
                                                            int selectedIndex) {
        ArrayList<String> urls = new ArrayList<>();
        ArrayList<String> thumbnails = new ArrayList<>();
        ArrayList<Long> timestamps = new ArrayList<>();
        ArrayList<String> captions = new ArrayList<>();
        ArrayList<String> types = new ArrayList<>();
        ArrayList<String> videoUrls = new ArrayList<>();
        for (Photo photo : photos) {
            String url = photo.getImageUrl();
            if (url == null || url.trim().isEmpty()) {
                url = photo.getThumbnailUrl();
            }
            String thumbnail = photo.getThumbnailUrl();
            if (thumbnail == null || thumbnail.trim().isEmpty()) {
                thumbnail = url;
            }
            urls.add(url == null ? "" : url);
            thumbnails.add(thumbnail == null ? "" : thumbnail);
            timestamps.add(photo.getCreatedAt() == null
                    ? System.currentTimeMillis()
                    : photo.getCreatedAt().toDate().getTime());
            captions.add(photo.getCaption() == null ? "" : photo.getCaption());
            types.add(photo.getType() == null ? "" : photo.getType());
            videoUrls.add(photo.getVideoUrl() == null ? "" : photo.getVideoUrl());
        }
        MemoriesViewerDialogFragment fragment = new MemoriesViewerDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putStringArrayList(ARG_URLS, urls);
        arguments.putStringArrayList(ARG_THUMBNAILS, thumbnails);
        arguments.putSerializable(ARG_TIMESTAMPS, timestamps);
        arguments.putStringArrayList("captions", captions);
        arguments.putStringArrayList(ARG_TYPES, types);
        arguments.putStringArrayList(ARG_VIDEO_URLS, videoUrls);
        arguments.putInt(ARG_SELECTED, selectedIndex);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.Theme_Pocket);
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }
        ArrayList<String> urls = arguments.getStringArrayList(ARG_URLS);
        ArrayList<String> thumbnails = arguments.getStringArrayList(ARG_THUMBNAILS);
        @SuppressWarnings("unchecked")
        ArrayList<Long> timestamps = (ArrayList<Long>) arguments.getSerializable(ARG_TIMESTAMPS);
        ArrayList<String> captions = arguments.getStringArrayList("captions");
        ArrayList<String> types = arguments.getStringArrayList(ARG_TYPES);
        ArrayList<String> videoUrls = arguments.getStringArrayList(ARG_VIDEO_URLS);
        if (urls != null && thumbnails != null && timestamps != null) {
            int count = Math.min(urls.size(), Math.min(thumbnails.size(), timestamps.size()));
            for (int index = 0; index < count; index++) {
                String caption = (captions != null && index < captions.size()) ? captions.get(index) : "";
                String type = (types != null && index < types.size()) ? types.get(index) : "";
                String videoUrl = (videoUrls != null && index < videoUrls.size()) ? videoUrls.get(index) : "";
                items.add(new MemoryItem(urls.get(index), thumbnails.get(index),
                        timestamps.get(index), caption, type, videoUrl));
            }
        }
        selectedIndex = Math.max(0, Math.min(arguments.getInt(ARG_SELECTED, 0),
                Math.max(0, items.size() - 1)));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_memories_viewer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        year = view.findViewById(R.id.memory_viewer_year);
        date = view.findViewById(R.id.memory_viewer_date);
        time = view.findViewById(R.id.memory_viewer_time);
        photoPager = view.findViewById(R.id.memory_viewer_pager);
        RecyclerView thumbnails = view.findViewById(R.id.memory_viewer_thumbnails);

        view.findViewById(R.id.memory_viewer_close).setOnClickListener(clicked -> dismiss());
        view.findViewById(R.id.memory_viewer_share).setOnClickListener(clicked -> shareSelected());

        player = new ExoPlayer.Builder(requireContext()).build();
        player.setRepeatMode(Player.REPEAT_MODE_ALL);

        LinearLayoutManager pagerLayout = new LinearLayoutManager(requireContext(),
                RecyclerView.HORIZONTAL, false);
        photoPager.setLayoutManager(pagerLayout);
        photoPager.setAdapter(new PhotoPagerAdapter(items, this));
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(photoPager);
        photoPager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }
                View snapped = snapHelper.findSnapView(pagerLayout);
                if (snapped != null) {
                    int pos = pagerLayout.getPosition(snapped);
                    showPosition(pos, true);
                    playVideoAtPosition(pos);
                }
            }
        });

        thumbnailAdapter = new ThumbnailAdapter(items, index -> {
            showPosition(index, false);
            photoPager.smoothScrollToPosition(index);
            playVideoAtPosition(index);
        });
        thumbnails.setLayoutManager(new LinearLayoutManager(requireContext(),
                RecyclerView.HORIZONTAL, false));
        thumbnails.setAdapter(thumbnailAdapter);
        photoPager.scrollToPosition(selectedIndex);
        showPosition(selectedIndex, false);
        photoPager.post(() -> playVideoAtPosition(selectedIndex));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        Window window = dialog == null ? null : dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(R.color.pocket_background);
        }
    }

    private void playVideoAtPosition(int position) {
        if (player == null) return;
        player.stop();
        player.clearMediaItems();

        for (int i = 0; i < photoPager.getChildCount(); i++) {
            View child = photoPager.getChildAt(i);
            RecyclerView.ViewHolder holder = photoPager.getChildViewHolder(child);
            if (holder instanceof PhotoPagerAdapter.Holder) {
                PhotoPagerAdapter.Holder h = (PhotoPagerAdapter.Holder) holder;
                h.videoView.setPlayer(null);
            }
        }

        if (position < 0 || position >= items.size()) return;
        MemoryItem item = items.get(position);
        if ("video".equals(item.type) && item.videoUrl != null && !item.videoUrl.isEmpty()) {
            RecyclerView.ViewHolder holder = photoPager.findViewHolderForAdapterPosition(position);
            if (holder instanceof PhotoPagerAdapter.Holder) {
                PhotoPagerAdapter.Holder h = (PhotoPagerAdapter.Holder) holder;
                h.videoView.setPlayer(player);
                MediaItem mediaItem = MediaItem.fromUri(Uri.parse(item.videoUrl));
                player.setMediaItem(mediaItem);
                player.prepare();
                player.play();
            }
        }
    }

    private void showPosition(int position, boolean centerThumbnail) {
        if (position < 0 || position >= items.size()) {
            return;
        }
        selectedIndex = position;
        Date selectedDate = new Date(items.get(position).timestamp);
        year.setText(new SimpleDateFormat("yyyy", Locale.getDefault()).format(selectedDate));
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.getDefault());
        int day = Integer.parseInt(new SimpleDateFormat("d", Locale.US).format(selectedDate));
        date.setText(getString(R.string.memories_viewer_date,
                monthFormat.format(selectedDate), day, ordinalSuffix(day)));
        time.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedDate));
        thumbnailAdapter.setSelectedIndex(position);
        if (centerThumbnail) {
            RecyclerView list = requireView().findViewById(R.id.memory_viewer_thumbnails);
            list.smoothScrollToPosition(position);
        }
    }

    private String ordinalSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }

    private void shareSelected() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) {
            return;
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, items.get(selectedIndex).url);
        startActivity(Intent.createChooser(share, getString(R.string.memories_share)));
    }

    static class MemoryItem {
        final String url;
        final String thumbnail;
        final long timestamp;
        final String caption;
        final String type;
        final String videoUrl;

        MemoryItem(String url, String thumbnail, long timestamp, String caption, String type, String videoUrl) {
            this.url = url;
            this.thumbnail = thumbnail;
            this.timestamp = timestamp;
            this.caption = caption;
            this.type = type;
            this.videoUrl = videoUrl;
        }
    }

    static class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.Holder> {
        private final List<MemoryItem> items;
        private final MemoriesViewerDialogFragment fragment;

        PhotoPagerAdapter(List<MemoryItem> items, MemoriesViewerDialogFragment fragment) {
            this.items = items;
            this.fragment = fragment;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_memory_viewer_photo, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            MemoryItem item = items.get(position);
            boolean isVideo = "video".equals(item.type);
            
            holder.videoView.setVisibility(isVideo ? View.VISIBLE : View.GONE);
            
            if (isVideo && position == fragment.selectedIndex && fragment.player != null) {
                holder.videoView.setPlayer(fragment.player);
                if (fragment.player.getPlaybackState() == Player.STATE_IDLE || fragment.player.getMediaItemCount() == 0) {
                    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(item.videoUrl));
                    fragment.player.setMediaItem(mediaItem);
                    fragment.player.prepare();
                    fragment.player.play();
                }
            } else {
                holder.videoView.setPlayer(null);
            }

            Glide.with(holder.image)
                    .load(isVideo && item.videoUrl != null ? item.videoUrl : item.url)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_pocket)
                    .error(R.drawable.placeholder_pocket)
                    .into(holder.image);
            
            if (item.caption != null && !item.caption.trim().isEmpty()) {
                holder.captionView.setText(item.caption);
                holder.captionView.setVisibility(View.VISIBLE);
            } else {
                holder.captionView.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ImageView image;
            final PlayerView videoView;
            final TextView captionView;

            Holder(@NonNull View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.memory_viewer_photo);
                videoView = itemView.findViewById(R.id.memory_viewer_video);
                captionView = itemView.findViewById(R.id.memory_viewer_caption);
                itemView.post(() -> {
                    ViewGroup.LayoutParams params = itemView.getLayoutParams();
                    View parent = (View) itemView.getParent();
                    if (parent != null && parent.getWidth() > 0) {
                        params.width = parent.getWidth();
                        itemView.setLayoutParams(params);
                    }
                });
            }
        }
    }

    static class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailAdapter.Holder> {
        interface ClickListener { void onClick(int index); }

        private final List<MemoryItem> items;
        private final ClickListener clickListener;
        private int selectedIndex;

        ThumbnailAdapter(List<MemoryItem> items, ClickListener clickListener) {
            this.items = items;
            this.clickListener = clickListener;
        }

        void setSelectedIndex(int nextIndex) {
            int oldIndex = selectedIndex;
            selectedIndex = nextIndex;
            notifyItemChanged(oldIndex);
            notifyItemChanged(nextIndex);
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_memory_viewer_thumbnail, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Glide.with(holder.image)
                    .load(items.get(position).thumbnail)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_pocket)
                    .error(R.drawable.placeholder_pocket)
                    .into(holder.image);
            boolean selected = position == selectedIndex;
            holder.card.setStrokeWidth(selected ? dp(holder.itemView, 4) : 0);
            holder.card.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(),
                    R.color.pocket_primary));
            holder.itemView.setOnClickListener(view -> clickListener.onClick(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private static int dp(@NonNull View view, int value) {
            return Math.round(value * view.getResources().getDisplayMetrics().density);
        }

        static class Holder extends RecyclerView.ViewHolder {
            final MaterialCardView card;
            final ImageView image;

            Holder(@NonNull View itemView) {
                super(itemView);
                card = (MaterialCardView) itemView;
                image = itemView.findViewById(R.id.memory_viewer_thumbnail);
            }
        }
    }
}
