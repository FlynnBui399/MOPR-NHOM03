package com.example.fonos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

        if (isValidUrl(book.getCoverUrl())) {
            Glide.with(holder.itemView.getContext())
                    .load(book.getCoverUrl())
                    .placeholder(book.getCoverDrawableRes())
                    .error(book.getCoverDrawableRes())
                    .into(holder.imgCover);
        } else {
            Glide.with(holder.itemView.getContext()).clear(holder.imgCover);
            holder.imgCover.setImageDrawable(null);

            if (book.getCoverDrawableRes() != 0) {
                holder.imgCover.setBackgroundResource(book.getCoverDrawableRes());
            } else {
                holder.imgCover.setBackgroundResource(R.drawable.bg_book_cover_1);
            }
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