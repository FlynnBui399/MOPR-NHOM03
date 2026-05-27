package com.example.fonos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.tvDownloadedPath.setText(book.localFilePath);

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

    @Override
    public int getItemCount() {
        return downloadedBooks != null ? downloadedBooks.size() : 0;
    }

    public static class DownloadedBookViewHolder extends RecyclerView.ViewHolder {
        TextView tvDownloadedTitle, tvDownloadedAuthor, tvDownloadedSize, tvDownloadedPath;
        Button btnOpenDownload, btnDeleteDownload;

        public DownloadedBookViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDownloadedTitle = itemView.findViewById(R.id.tvDownloadedTitle);
            tvDownloadedAuthor = itemView.findViewById(R.id.tvDownloadedAuthor);
            tvDownloadedSize = itemView.findViewById(R.id.tvDownloadedSize);
            tvDownloadedPath = itemView.findViewById(R.id.tvDownloadedPath);
            btnOpenDownload = itemView.findViewById(R.id.btnOpenDownload);
            btnDeleteDownload = itemView.findViewById(R.id.btnDeleteDownload);
        }
    }
}
