package com.example.fonos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fonos.adapter.BookAdapter;
import com.example.fonos.model.Book;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display the user's recently listened books using the standard BookAdapter.
 */
public class HistoryActivity extends AppCompatActivity implements BookAdapter.OnBookClickListener {

    private RecyclerView rvHistory;
    private LinearLayout layoutEmptyHistory;
    private BookAdapter bookAdapter;
    private final List<Book> historyBooks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        rvHistory = findViewById(R.id.rvHistory);
        layoutEmptyHistory = findViewById(R.id.layoutEmptyHistory);

        // Simple back navigation
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Configure RecyclerView & Adapter
        bookAdapter = new BookAdapter(historyBooks, this);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(bookAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistoryData();
    }

    private void loadHistoryData() {
        historyBooks.clear();
        historyBooks.addAll(RecentlyPlayedManager.getHistory(this));
        bookAdapter.notifyDataSetChanged();

        if (historyBooks.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            layoutEmptyHistory.setVisibility(View.VISIBLE);
        } else {
            rvHistory.setVisibility(View.VISIBLE);
            layoutEmptyHistory.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBookClick(Book book) {
        if (book == null) return;

        Intent intent = new Intent(this, BookDetailActivity.class);
        intent.putExtra("book_id", book.getId());
        intent.putExtra("book_title", book.getTitle());
        intent.putExtra("book_author", book.getAuthor());
        intent.putExtra("book_desc", book.getDescription());
        intent.putExtra("book_rating", book.getRating());
        intent.putExtra("book_duration", book.getDuration());
        intent.putExtra("book_chapters", book.getChapterCount());
        intent.putExtra("book_cover", book.getCoverDrawableRes());
        intent.putExtra("book_cover_url", book.getCoverUrl());
        intent.putExtra("book_audio_url", book.getAudioUrl());
        intent.putExtra("book_category", book.getCategory());
        startActivity(intent);
    }
}
