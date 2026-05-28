package com.example.fonos.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.fonos.OfflineAudioManager;
import com.example.fonos.R;
import com.example.fonos.data.DownloadedBookEntity;

import java.util.List;

public class DownloadedBookAdapter extends RecyclerView.Adapter<DownloadedBookAdapter.DownloadedBookViewHolder> {

    private final List<DownloadedBookEntity> downloadedBooks;
    private final OnDownloadedBookActionListener listener;

    public interface OnDownloadedBookActionListener {
        void onOpenDownloadedBook(DownloadedBookEntity book);
        void onDeleteDownloadedBook(DownloadedBookEntity book);
    }

    public DownloadedBookAdapter(
            List<DownloadedBookEntity> downloadedBooks,
            OnDownloadedBookActionListener listener
    ) {
        this.downloadedBooks = downloadedBooks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DownloadedBookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_downloaded_book, parent, false);
        return new DownloadedBookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadedBookViewHolder holder, int position) {
        DownloadedBookEntity book = downloadedBooks.get(position);

        holder.tvDownloadedTitle.setText(book.title);
        holder.tvDownloadedAuthor.setText(book.author);
        holder.tvDownloadedSize.setText(OfflineAudioManager.formatFileSize(book.fileSizeBytes));
        holder.tvDownloadedMeta.setText(book.duration != null && !book.duration.trim().isEmpty()
                ? book.duration
                : book.status);
        holder.tvDownloadedCoverTitle.setText(book.title);
        bindCover(holder, book);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenDownloadedBook(book);
            }
        });

        holder.btnOpenDownload.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOpenDownloadedBook(book);
            }
        });

        holder.btnDeleteDownload.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteDownloadedBook(book);
            }
        });
    }

    private void bindCover(@NonNull DownloadedBookViewHolder holder, @NonNull DownloadedBookEntity book) {
        int fallbackCover = book.coverDrawableRes != 0
                ? book.coverDrawableRes
                : R.drawable.bg_book_cover_1;

        if (isValidUrl(book.coverUrl)) {
            holder.tvDownloadedCoverTitle.setVisibility(View.GONE);
            holder.imgDownloadedCover.setBackground(null);

            Glide.with(holder.itemView.getContext())
                    .load(book.coverUrl)
                    .placeholder(fallbackCover)
                    .error(fallbackCover)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(
                                @Nullable GlideException e,
                                Object model,
                                Target<Drawable> target,
                                boolean isFirstResource
                        ) {
                            holder.tvDownloadedCoverTitle.setVisibility(View.VISIBLE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(
                                Drawable resource,
                                Object model,
                                Target<Drawable> target,
                                DataSource dataSource,
                                boolean isFirstResource
                        ) {
                            holder.tvDownloadedCoverTitle.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(holder.imgDownloadedCover);
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.imgDownloadedCover);
            holder.imgDownloadedCover.setImageDrawable(null);
            holder.imgDownloadedCover.setBackgroundResource(fallbackCover);
            holder.tvDownloadedCoverTitle.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return downloadedBooks != null ? downloadedBooks.size() : 0;
    }

    public static class DownloadedBookViewHolder extends RecyclerView.ViewHolder {
        ImageView imgDownloadedCover;
        TextView tvDownloadedTitle, tvDownloadedAuthor, tvDownloadedSize, tvDownloadedMeta, tvDownloadedCoverTitle;
        Button btnOpenDownload, btnDeleteDownload;

        public DownloadedBookViewHolder(@NonNull View itemView) {
            super(itemView);

            imgDownloadedCover = itemView.findViewById(R.id.imgDownloadedCover);
            tvDownloadedTitle = itemView.findViewById(R.id.tvDownloadedTitle);
            tvDownloadedAuthor = itemView.findViewById(R.id.tvDownloadedAuthor);
            tvDownloadedSize = itemView.findViewById(R.id.tvDownloadedSize);
            tvDownloadedMeta = itemView.findViewById(R.id.tvDownloadedMeta);
            tvDownloadedCoverTitle = itemView.findViewById(R.id.tvDownloadedCoverTitle);
            btnOpenDownload = itemView.findViewById(R.id.btnOpenDownload);
            btnDeleteDownload = itemView.findViewById(R.id.btnDeleteDownload);
        }
    }

    private boolean isValidUrl(String value) {
        return value != null &&
                (value.trim().startsWith("http://") || value.trim().startsWith("https://"));
    }
}
