package com.example.fonos.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fonos.BookDetailActivity;
import com.example.fonos.R;
import com.example.fonos.adapter.BookAdapter;
import com.example.fonos.adapter.CategoryAdapter;
import com.example.fonos.model.Book;
import com.example.fonos.model.Category;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements BookAdapter.OnBookClickListener, CategoryAdapter.OnCategoryClickListener {

    private RecyclerView rvCategories, rvTrending, rvNewReleases, rvRecommended;
    private CategoryAdapter categoryAdapter;
    private BookAdapter trendingAdapter, newReleasesAdapter, recommendedAdapter;
    
    private List<Category> categoryList;
    private List<Book> trendingBooks;
    private List<Book> newReleaseBooks;
    private List<Book> recommendedBooks;

    private View progressBar;
    private View homeScrollView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        initViews(view);
        initData();
        setupRecyclerViews();
        loadBooksFromFirestore();
        
        return view;
    }

    private void initViews(View view) {
        rvCategories = view.findViewById(R.id.rvCategories);
        rvTrending = view.findViewById(R.id.rvTrending);
        rvNewReleases = view.findViewById(R.id.rvNewReleases);
        rvRecommended = view.findViewById(R.id.rvRecommended);
        progressBar = view.findViewById(R.id.progressBar);
        homeScrollView = view.findViewById(R.id.homeScrollView);
    }

    private void initData() {
        // Categories
        categoryList = new ArrayList<>();
        categoryList.add(new Category(1, getString(R.string.cat_all), true));
        categoryList.add(new Category(2, getString(R.string.cat_self_help), false));
        categoryList.add(new Category(3, getString(R.string.cat_business), false));
        categoryList.add(new Category(4, getString(R.string.cat_fiction), false));
        categoryList.add(new Category(5, getString(R.string.cat_science), false));
        categoryList.add(new Category(6, getString(R.string.cat_psychology), false));

        trendingBooks = new ArrayList<>();
        newReleaseBooks = new ArrayList<>();
        recommendedBooks = new ArrayList<>();
    }

    private void loadBooksFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("books")
                .get()
                .addOnCompleteListener(task -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (homeScrollView != null) homeScrollView.setVisibility(View.VISIBLE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Book> allBooks = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Book book = document.toObject(Book.class);
                            allBooks.add(book);
                        }
                        if (!allBooks.isEmpty()) {
                            updateUIWithBooks(allBooks);
                        } else {
                            loadLocalSampleBooks();
                        }
                    } else {
                        loadLocalSampleBooks();
                    }
                });
    }

    private void updateUIWithBooks(List<Book> allBooks) {
        trendingBooks.clear();
        newReleaseBooks.clear();
        recommendedBooks.clear();

        for (Book book : allBooks) {
            if (book.isTrending()) {
                trendingBooks.add(book);
            }
            if (book.isNewRelease()) {
                newReleaseBooks.add(book);
            }
            // Add to recommended if not trending and not new release
            if (!book.isTrending() && !book.isNewRelease()) {
                recommendedBooks.add(book);
            }
        }

        trendingAdapter.notifyDataSetChanged();
        newReleasesAdapter.notifyDataSetChanged();
        recommendedAdapter.notifyDataSetChanged();
    }

    private void loadLocalSampleBooks() {
        trendingBooks.clear();
        trendingBooks.add(new Book(1, getString(R.string.book1_title), getString(R.string.book1_author), getString(R.string.book1_desc), 4.8f, "8h 30m", 12, R.drawable.bg_book_cover_1, "Self-help"));
        trendingBooks.add(new Book(2, getString(R.string.book2_title), getString(R.string.book2_author), getString(R.string.book2_desc), 4.7f, "5h 45m", 15, R.drawable.bg_book_cover_2, "Fiction"));
        trendingBooks.add(new Book(3, getString(R.string.book3_title), getString(R.string.book3_author), getString(R.string.book3_desc), 4.6f, "6h 20m", 10, R.drawable.bg_book_cover_3, "Self-help"));
        trendingBooks.add(new Book(4, getString(R.string.book4_title), getString(R.string.book4_author), getString(R.string.book4_desc), 4.9f, "15h 10m", 20, R.drawable.bg_book_cover_4, "Science"));

        newReleaseBooks.clear();
        newReleaseBooks.add(new Book(5, getString(R.string.book5_title), getString(R.string.book5_author), getString(R.string.book5_desc), 4.5f, "14h 05m", 18, R.drawable.bg_book_cover_5, "Psychology"));
        newReleaseBooks.add(new Book(6, getString(R.string.book6_title), getString(R.string.book6_author), getString(R.string.book6_desc), 4.8f, "7h 15m", 11, R.drawable.bg_book_cover_6, "Business"));
        newReleaseBooks.add(new Book(7, getString(R.string.book7_title), getString(R.string.book7_author), getString(R.string.book7_desc), 4.4f, "5h 50m", 9, R.drawable.bg_book_cover_7, "Self-help"));

        recommendedBooks.clear();
        recommendedBooks.add(new Book(8, getString(R.string.book8_title), getString(R.string.book8_author), getString(R.string.book8_desc), 4.9f, "9h 40m", 16, R.drawable.bg_book_cover_1, "Business"));
        recommendedBooks.add(new Book(9, getString(R.string.book9_title), getString(R.string.book9_author), getString(R.string.book9_desc), 4.7f, "12h 30m", 21, R.drawable.bg_book_cover_2, "Self-help"));
        recommendedBooks.add(new Book(10, getString(R.string.book10_title), getString(R.string.book10_author), getString(R.string.book10_desc), 4.6f, "8h 15m", 14, R.drawable.bg_book_cover_3, "Psychology"));

        trendingAdapter.notifyDataSetChanged();
        newReleasesAdapter.notifyDataSetChanged();
        recommendedAdapter.notifyDataSetChanged();
    }

    private void setupRecyclerViews() {
        // Categories
        categoryAdapter = new CategoryAdapter(categoryList, this);
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        // Trending
        trendingAdapter = new BookAdapter(trendingBooks, this);
        rvTrending.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTrending.setAdapter(trendingAdapter);

        // New Releases
        newReleasesAdapter = new BookAdapter(newReleaseBooks, this);
        rvNewReleases.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvNewReleases.setAdapter(newReleasesAdapter);

        // Recommended
        recommendedAdapter = new BookAdapter(recommendedBooks, this);
        rvRecommended.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecommended.setAdapter(recommendedAdapter);
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
        intent.putExtra("book_category", book.getCategory());
        startActivity(intent);
    }

    @Override
    public void onCategoryClick(Category category) {
        // Handle category selection - typically filter book lists
        // For now, we'll just keep the UI update which is handled in adapter
    }
}
