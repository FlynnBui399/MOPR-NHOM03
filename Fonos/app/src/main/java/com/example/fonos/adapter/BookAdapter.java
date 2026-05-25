package com.example.fonos.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.fonos.R;
import com.example.fonos.model.Book;

import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private final List<Book> bookList;
    private final OnBookClickListener listener;

    public interface OnBookClickListener {
        void onBookClick(Book book);
    }

    public BookAdapter(List<Book> bookList, OnBookClickListener listener) {
        this.bookList = bookList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book_card, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = bookList.get(position);

        holder.tvBookTitle.setText(book.getTitle());
        holder.tvBookAuthor.setText(book.getAuthor());
        holder.tvBookRating.setText(String.valueOf(book.getRating()));
        holder.tvBookDuration.setText(book.getDuration());
        holder.tvCoverTitle.setText(book.getTitle());

        int fallbackCover = book.getCoverDrawableRes() != 0
                ? book.getCoverDrawableRes()
                : R.drawable.bg_book_cover_1;

        if (isValidUrl(book.getCoverUrl())) {
            holder.tvCoverTitle.setVisibility(View.GONE);
            holder.imgCover.setBackground(null);

            Glide.with(holder.itemView.getContext())
                    .load(book.getCoverUrl())
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
                            holder.tvCoverTitle.setVisibility(View.VISIBLE);
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
                            holder.tvCoverTitle.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(holder.imgCover);
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.imgCover);
            holder.imgCover.setImageDrawable(null);
            holder.imgCover.setBackgroundResource(fallbackCover);
            holder.tvCoverTitle.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookClick(book);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookList != null ? bookList.size() : 0;
    }

    private boolean isValidUrl(String value) {
        return value != null &&
                (value.trim().startsWith("http://") || value.trim().startsWith("https://"));
    }

    public static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookTitle, tvBookAuthor, tvBookRating, tvCoverTitle, tvBookDuration;
        ImageView imgCover;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);

            tvBookTitle = itemView.findViewById(R.id.tvBookTitle);
            tvBookAuthor = itemView.findViewById(R.id.tvBookAuthor);
            tvBookRating = itemView.findViewById(R.id.tvBookRating);
            tvCoverTitle = itemView.findViewById(R.id.tvCoverTitle);
            tvBookDuration = itemView.findViewById(R.id.tvBookDuration);
            imgCover = itemView.findViewById(R.id.imgCover);
        }
    }
}