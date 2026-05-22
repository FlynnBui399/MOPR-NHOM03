package com.example.fonos.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fonos.BookDetailActivity;
import com.example.fonos.R;
import com.example.fonos.adapter.BookAdapter;
import com.example.fonos.model.Book;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private EditText etSearch;
    private RecyclerView rvSearchResults;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyState;

    private BookAdapter bookAdapter;
    private List<Book> allBooksList = new ArrayList<>();
    private List<Book> filteredBooksList = new ArrayList<>();

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupRecyclerView();
        loadBooksFromFirestore();
        setupSearchListener();

        return view;
    }

    private void initViews(View view) {
        etSearch = view.findViewById(R.id.etSearch);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
    }

    private void setupRecyclerView() {
        bookAdapter = new BookAdapter(filteredBooksList, this);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(bookAdapter);
    }

    private void loadBooksFromFirestore() {
        db.collection("books")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().isEmpty()) {
                            seedBooksDatabase();
                        } else {
                            allBooksList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Book book = document.toObject(Book.class);
                                allBooksList.add(book);
                            }
                            filterBooks(etSearch.getText().toString());
                        }
                    } else {
                        loadLocalSampleBooks();
                    }
                });
    }

    private void seedBooksDatabase() {
        List<Book> localBooks = getLocalSampleBooksList();
        CollectionReference booksRef = db.collection("books");

        for (Book book : localBooks) {
            booksRef.document(String.valueOf(book.getId())).set(book);
        }

        db.collection("books")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        allBooksList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Book book = document.toObject(Book.class);
                            allBooksList.add(book);
                        }
                        filterBooks(etSearch.getText().toString());
                    } else {
                        loadLocalSampleBooks();
                    }
                });
    }

    private void loadLocalSampleBooks() {
        allBooksList = getLocalSampleBooksList();
        filterBooks(etSearch.getText().toString());
    }

    private List<Book> getLocalSampleBooksList() {
        List<Book> books = new ArrayList<>();
        books.add(new Book(1, getString(R.string.book1_title), getString(R.string.book1_author), getString(R.string.book1_desc), 4.8f, "8h 30m", 12, R.drawable.bg_book_cover_1, "Self-help"));
        books.add(new Book(2, getString(R.string.book2_title), getString(R.string.book2_author), getString(R.string.book2_desc), 4.7f, "5h 45m", 15, R.drawable.bg_book_cover_2, "Fiction"));
        books.add(new Book(3, getString(R.string.book3_title), getString(R.string.book3_author), getString(R.string.book3_desc), 4.6f, "6h 20m", 10, R.drawable.bg_book_cover_3, "Self-help"));
        books.add(new Book(4, getString(R.string.book4_title), getString(R.string.book4_author), getString(R.string.book4_desc), 4.9f, "15h 10m", 20, R.drawable.bg_book_cover_4, "Science"));
        books.add(new Book(5, getString(R.string.book5_title), getString(R.string.book5_author), getString(R.string.book5_desc), 4.5f, "14h 05m", 18, R.drawable.bg_book_cover_5, "Psychology"));
        books.add(new Book(6, getString(R.string.book6_title), getString(R.string.book6_author), getString(R.string.book6_desc), 4.8f, "7h 15m", 11, R.drawable.bg_book_cover_6, "Business"));
        books.add(new Book(7, getString(R.string.book7_title), getString(R.string.book7_author), getString(R.string.book7_desc), 4.4f, "5h 50m", 9, R.drawable.bg_book_cover_7, "Self-help"));
        books.add(new Book(8, getString(R.string.book8_title), getString(R.string.book8_author), getString(R.string.book8_desc), 4.9f, "9h 40m", 16, R.drawable.bg_book_cover_1, "Business"));
        books.add(new Book(9, getString(R.string.book9_title), getString(R.string.book9_author), getString(R.string.book9_desc), 4.7f, "12h 30m", 21, R.drawable.bg_book_cover_2, "Self-help"));
        books.add(new Book(10, getString(R.string.book10_title), getString(R.string.book10_author), getString(R.string.book10_desc), 4.6f, "8h 15m", 14, R.drawable.bg_book_cover_3, "Psychology"));
        return books;
    }

    private void setupSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBooks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterBooks(String query) {
        filteredBooksList.clear();
        String lowercaseQuery = query.toLowerCase().trim();

        if (lowercaseQuery.isEmpty()) {
            rvSearchResults.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText(getString(R.string.search_empty));
        } else {
            for (Book book : allBooksList) {
                if (book.getTitle().toLowerCase().contains(lowercaseQuery) ||
                        book.getAuthor().toLowerCase().contains(lowercaseQuery)) {
                    filteredBooksList.add(book);
                }
            }

            if (filteredBooksList.isEmpty()) {
                rvSearchResults.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("No results found for \"" + query + "\"");
            } else {
                rvSearchResults.setVisibility(View.VISIBLE);
                layoutEmptyState.setVisibility(View.GONE);
            }
        }
        bookAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBookClick(Book book) {
        Intent intent = new Intent(getActivity(), BookDetailActivity.class);
        intent.putExtra("book_id", book.getId());
        intent.putExtra("book_title", book.getTitle());
        intent.putExtra("book_author", book.getAuthor());
        intent.putExtra("book_desc", book.getDescription());
        intent.putExtra("book_rating", book.getRating());
        intent.putExtra("book_duration", book.getDuration());
        intent.putExtra("book_chapters", book.getChapterCount());
        intent.putExtra("book_cover", book.getCoverDrawableRes());
        startActivity(intent);
    }
}
