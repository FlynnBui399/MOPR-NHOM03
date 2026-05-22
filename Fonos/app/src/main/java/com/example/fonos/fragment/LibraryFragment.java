package com.example.fonos.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fonos.BookDetailActivity;
import com.example.fonos.R;
import com.example.fonos.adapter.BookAdapter;
import com.example.fonos.auth.LoginActivity;
import com.example.fonos.model.Book;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment implements BookAdapter.OnBookClickListener {

    private RecyclerView rvLibrary;
    private LinearLayout layoutLoginRequired, layoutEmptyState;
    private Button btnLibraryLogin;

    private BookAdapter bookAdapter;
    private List<Book> libraryBooks = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration libraryListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupRecyclerView();
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        rvLibrary = view.findViewById(R.id.rvLibrary);
        layoutLoginRequired = view.findViewById(R.id.layoutLoginRequired);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        btnLibraryLogin = view.findViewById(R.id.btnLibraryLogin);
    }

    private void setupRecyclerView() {
        bookAdapter = new BookAdapter(libraryBooks, this);
        rvLibrary.setLayoutManager(new LinearLayoutManager(getContext()));
        rvLibrary.setAdapter(bookAdapter);
    }

    private void setupListeners() {
        btnLibraryLogin.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        checkLoginAndLoadLibrary();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (libraryListener != null) {
            libraryListener.remove();
            libraryListener = null;
        }
    }

    private void checkLoginAndLoadLibrary() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            // User not logged in
            layoutLoginRequired.setVisibility(View.VISIBLE);
            rvLibrary.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);

            if (libraryListener != null) {
                libraryListener.remove();
                libraryListener = null;
            }
        } else {
            // User logged in
            layoutLoginRequired.setVisibility(View.GONE);
            observeLibrary(user.getUid());
        }
    }

    private void observeLibrary(String userId) {
        if (libraryListener != null) {
            libraryListener.remove();
        }

        libraryListener = db.collection("users")
                .document(userId)
                .collection("library")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // Fallback to empty if there's an error loading
                        libraryBooks.clear();
                        bookAdapter.notifyDataSetChanged();
                        showEmptyState();
                        return;
                    }

                    if (value != null) {
                        libraryBooks.clear();
                        for (QueryDocumentSnapshot document : value) {
                            Book book = document.toObject(Book.class);
                            libraryBooks.add(book);
                        }
                        bookAdapter.notifyDataSetChanged();

                        if (libraryBooks.isEmpty()) {
                            showEmptyState();
                        } else {
                            showLibraryList();
                        }
                    }
                });
    }

    private void showEmptyState() {
        rvLibrary.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void showLibraryList() {
        rvLibrary.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
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
