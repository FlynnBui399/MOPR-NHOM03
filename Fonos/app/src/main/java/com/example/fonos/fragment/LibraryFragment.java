package com.example.fonos.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fonos.BookDetailActivity;
import com.example.fonos.OfflineAudioManager;
import com.example.fonos.R;
import com.example.fonos.adapter.BookAdapter;
import com.example.fonos.auth.LoginActivity;
import com.example.fonos.model.Book;
import com.google.android.material.tabs.TabLayout;
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
    private final List<Book> libraryBooks = new ArrayList<>();
    private final List<Book> savedBooks = new ArrayList<>();
    private int currentTab = 0; // 0 = Saved, 1 = Downloaded

    private TabLayout tabLayoutLibrary;
    private TextView tvEmptyTitle, tvEmptySubtitle;

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
        tabLayoutLibrary = view.findViewById(R.id.tabLayoutLibrary);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle);

        if (tabLayoutLibrary != null) {
            tabLayoutLibrary.removeAllTabs();
            tabLayoutLibrary.addTab(tabLayoutLibrary.newTab().setText(R.string.library_tab_saved));
            tabLayoutLibrary.addTab(tabLayoutLibrary.newTab().setText(R.string.library_tab_downloaded));
            
            TabLayout.Tab tab = tabLayoutLibrary.getTabAt(currentTab);
            if (tab != null) {
                tab.select();
            }
        }
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

        if (tabLayoutLibrary != null) {
            tabLayoutLibrary.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    currentTab = tab.getPosition();
                    loadActiveTabData();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }
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
            layoutLoginRequired.setVisibility(View.VISIBLE);
            if (tabLayoutLibrary != null) {
                tabLayoutLibrary.setVisibility(View.GONE);
            }
            rvLibrary.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.GONE);

            if (libraryListener != null) {
                libraryListener.remove();
                libraryListener = null;
            }
        } else {
            layoutLoginRequired.setVisibility(View.GONE);
            if (tabLayoutLibrary != null) {
                tabLayoutLibrary.setVisibility(View.VISIBLE);
            }
            observeLibrary(user.getUid());
            loadActiveTabData();
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
                        savedBooks.clear();
                        if (currentTab == 0) {
                            loadActiveTabData();
                        }
                        return;
                    }

                    if (value != null) {
                        savedBooks.clear();

                        for (QueryDocumentSnapshot document : value) {
                            Book book = document.toObject(Book.class);
                            savedBooks.add(book);
                        }

                        if (currentTab == 0) {
                            loadActiveTabData();
                        }
                    }
                });
    }

    private void loadActiveTabData() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }

        libraryBooks.clear();
        if (currentTab == 0) {
            // Saved books
            libraryBooks.addAll(savedBooks);
            bookAdapter.notifyDataSetChanged();

            if (libraryBooks.isEmpty()) {
                if (tvEmptyTitle != null) tvEmptyTitle.setText(R.string.library_empty);
                if (tvEmptySubtitle != null) tvEmptySubtitle.setText(R.string.library_empty_sub);
                showEmptyState();
            } else {
                showLibraryList();
            }
        } else {
            // Downloaded books
            List<Book> downloaded = OfflineAudioManager.getDownloadedBooks(getContext());
            libraryBooks.addAll(downloaded);
            bookAdapter.notifyDataSetChanged();

            if (libraryBooks.isEmpty()) {
                if (tvEmptyTitle != null) tvEmptyTitle.setText(R.string.library_empty_downloaded);
                if (tvEmptySubtitle != null) tvEmptySubtitle.setText(R.string.library_empty_downloaded_sub);
                showEmptyState();
            } else {
                showLibraryList();
            }
        }
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
        intent.putExtra("book_cover_url", book.getCoverUrl());
        intent.putExtra("book_audio_url", book.getAudioUrl());
        intent.putExtra("book_category", book.getCategory());

        startActivity(intent);
    }
}