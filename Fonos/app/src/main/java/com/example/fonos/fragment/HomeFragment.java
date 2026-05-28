package com.example.fonos.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.fonos.BookDetailActivity;
import com.example.fonos.MainActivity;
import com.example.fonos.RecentlyPlayedManager;
import com.example.fonos.R;
import com.example.fonos.SearchCache;
import com.example.fonos.adapter.BookAdapter;
import com.example.fonos.adapter.CategoryAdapter;
import com.example.fonos.model.Book;
import com.example.fonos.model.Category;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements BookAdapter.OnBookClickListener, CategoryAdapter.OnCategoryClickListener {

    private RecyclerView rvCategories, rvTrending, rvNewReleases, rvRecommended, rvRecentlyPlayed;
    private View layoutRecentlyPlayed;
    private CategoryAdapter categoryAdapter;
    private BookAdapter trendingAdapter, newReleasesAdapter, recommendedAdapter, recentlyPlayedAdapter;
    
    private List<Category> categoryList;
    private List<Book> trendingBooks;
    private List<Book> newReleaseBooks;
    private List<Book> recommendedBooks;
    private List<Book> recentlyPlayedBooks;
    private final List<Book> allBooksFromFirestore = new ArrayList<>();

    private com.facebook.shimmer.ShimmerFrameLayout shimmerHome;
    private View homeScrollView;
    private View cardHomeSearch;
    private SwipeRefreshLayout swipeRefreshHome;

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

    @Override
    public void onResume() {
        super.onResume();
        if (shimmerHome != null && shimmerHome.getVisibility() == View.VISIBLE) {
            shimmerHome.startShimmer();
        }
        // Refresh recently played books list when returning to Home tab
        loadRecentlyPlayed();
    }

    @Override
    public void onPause() {
        if (shimmerHome != null) {
            shimmerHome.stopShimmer();
        }
        super.onPause();
    }

    private void initViews(View view) {
        rvCategories = view.findViewById(R.id.rvCategories);
        rvTrending = view.findViewById(R.id.rvTrending);
        rvNewReleases = view.findViewById(R.id.rvNewReleases);
        rvRecommended = view.findViewById(R.id.rvRecommended);
        rvRecentlyPlayed = view.findViewById(R.id.rvRecentlyPlayed);
        layoutRecentlyPlayed = view.findViewById(R.id.layoutRecentlyPlayed);
        shimmerHome = view.findViewById(R.id.shimmerHome);
        homeScrollView = view.findViewById(R.id.homeScrollView);
        cardHomeSearch = view.findViewById(R.id.cardHomeSearch);
        swipeRefreshHome = view.findViewById(R.id.swipeRefreshHome);

        if (cardHomeSearch != null) {
            cardHomeSearch.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectSearchTab();
                }
            });
        }

        if (swipeRefreshHome != null) {
            swipeRefreshHome.setColorSchemeResources(R.color.colorPrimary);
            swipeRefreshHome.setOnRefreshListener(() -> {
                trendingBooks.clear();
                newReleaseBooks.clear();
                recommendedBooks.clear();
                allBooksFromFirestore.clear();
                trendingAdapter.notifyDataSetChanged();
                newReleasesAdapter.notifyDataSetChanged();
                recommendedAdapter.notifyDataSetChanged();
                loadBooksFromFirestore();
                loadRecentlyPlayed();
            });
        }
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
        recentlyPlayedBooks = new ArrayList<>();
    }

    private void loadBooksFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("books")
                .get()
                .addOnCompleteListener(task -> {
                    // Safety check: skip callback if fragment is detached
                    if (!isAdded() || getContext() == null) {
                        Log.d("HomeFragment", "Fragment not attached. Skipping callback.");
                        return;
                    }

                    if (shimmerHome != null) {
                        shimmerHome.stopShimmer();
                        shimmerHome.setVisibility(View.GONE);
                    }
                    if (swipeRefreshHome != null) {
                        swipeRefreshHome.setVisibility(View.VISIBLE);
                    }
                    if (homeScrollView != null) homeScrollView.setVisibility(View.VISIBLE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        List<Book> allBooks = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Book book = document.toObject(Book.class);
                            allBooks.add(book);
                        }
                        if (!allBooks.isEmpty()) {
                            allBooksFromFirestore.clear();
                            allBooksFromFirestore.addAll(allBooks);
                            // Populate SearchCache so SearchFragment can reuse this data
                            SearchCache.set(allBooksFromFirestore);
                            updateUIWithBooks(allBooksFromFirestore);
                        } else {
                            loadLocalSampleBooks();
                        }
                    } else {
                        if (task.getException() != null) {
                            Log.e("HomeFragment", "Firestore query failed: ", task.getException());
                        }
                        loadLocalSampleBooks();
                    }

                    if (swipeRefreshHome != null) {
                        swipeRefreshHome.setRefreshing(false);
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
        allBooksFromFirestore.clear();
        allBooksFromFirestore.add(new Book(1, getString(R.string.book1_title), getString(R.string.book1_author), getString(R.string.book1_desc), 4.8f, "8h 30m", 12, R.drawable.bg_book_cover_1, "Self-help"));
        allBooksFromFirestore.add(new Book(2, getString(R.string.book2_title), getString(R.string.book2_author), getString(R.string.book2_desc), 4.7f, "5h 45m", 15, R.drawable.bg_book_cover_2, "Fiction"));
        allBooksFromFirestore.add(new Book(3, getString(R.string.book3_title), getString(R.string.book3_author), getString(R.string.book3_desc), 4.6f, "6h 20m", 10, R.drawable.bg_book_cover_3, "Self-help"));
        allBooksFromFirestore.add(new Book(4, getString(R.string.book4_title), getString(R.string.book4_author), getString(R.string.book4_desc), 4.9f, "15h 10m", 20, R.drawable.bg_book_cover_4, "Science"));
        allBooksFromFirestore.add(new Book(5, getString(R.string.book5_title), getString(R.string.book5_author), getString(R.string.book5_desc), 4.5f, "14h 05m", 18, R.drawable.bg_book_cover_5, "Psychology"));
        allBooksFromFirestore.add(new Book(6, getString(R.string.book6_title), getString(R.string.book6_author), getString(R.string.book6_desc), 4.8f, "7h 15m", 11, R.drawable.bg_book_cover_6, "Business"));
        allBooksFromFirestore.add(new Book(7, getString(R.string.book7_title), getString(R.string.book7_author), getString(R.string.book7_desc), 4.4f, "5h 50m", 9, R.drawable.bg_book_cover_7, "Self-help"));
        allBooksFromFirestore.add(new Book(8, getString(R.string.book8_title), getString(R.string.book8_author), getString(R.string.book8_desc), 4.9f, "9h 40m", 16, R.drawable.bg_book_cover_1, "Business"));
        allBooksFromFirestore.add(new Book(9, getString(R.string.book9_title), getString(R.string.book9_author), getString(R.string.book9_desc), 4.7f, "12h 30m", 21, R.drawable.bg_book_cover_2, "Self-help"));
        allBooksFromFirestore.add(new Book(10, getString(R.string.book10_title), getString(R.string.book10_author), getString(R.string.book10_desc), 4.6f, "8h 15m", 14, R.drawable.bg_book_cover_3, "Psychology"));

        updateUIWithBooks(allBooksFromFirestore);
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

        // Recently Played
        recentlyPlayedAdapter = new BookAdapter(recentlyPlayedBooks, this);
        rvRecentlyPlayed.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecentlyPlayed.setAdapter(recentlyPlayedAdapter);
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

    @Override
    public void onCategoryClick(Category category) {
        String targetCategory = null;
        switch (category.getId()) {
            case 2: targetCategory = "Self-help"; break;
            case 3: targetCategory = "Business"; break;
            case 4: targetCategory = "Fiction"; break;
            case 5: targetCategory = "Science"; break;
            case 6: targetCategory = "Psychology"; break;
        }

        trendingBooks.clear();
        newReleaseBooks.clear();
        recommendedBooks.clear();

        for (Book book : allBooksFromFirestore) {
            boolean matchesCategory = (targetCategory == null) || 
                (book.getCategory() != null && book.getCategory().equalsIgnoreCase(targetCategory));
            
            if (matchesCategory) {
                if (book.isTrending()) {
                    trendingBooks.add(book);
                }
                if (book.isNewRelease()) {
                    newReleaseBooks.add(book);
                }
                if (!book.isTrending() && !book.isNewRelease()) {
                    recommendedBooks.add(book);
                }
            }
        }

        trendingAdapter.notifyDataSetChanged();
        newReleasesAdapter.notifyDataSetChanged();
        recommendedAdapter.notifyDataSetChanged();
    }

    /**
     * Loads recently played books using RecentlyPlayedManager and dynamically toggles 
     * the visibility of the "Nghe gần đây" section based on whether history exists.
     */
    private void loadRecentlyPlayed() {
        if (!isAdded() || getContext() == null) return;

        List<Book> history = RecentlyPlayedManager.getHistory(requireContext());
        if (history != null && !history.isEmpty()) {
            recentlyPlayedBooks.clear();
            recentlyPlayedBooks.addAll(history);
            if (recentlyPlayedAdapter != null) {
                recentlyPlayedAdapter.notifyDataSetChanged();
            }
            if (layoutRecentlyPlayed != null) {
                layoutRecentlyPlayed.setVisibility(View.VISIBLE);
            }
        } else {
            recentlyPlayedBooks.clear();
            if (recentlyPlayedAdapter != null) {
                recentlyPlayedAdapter.notifyDataSetChanged();
            }
            if (layoutRecentlyPlayed != null) {
                layoutRecentlyPlayed.setVisibility(View.GONE);
            }
        }
    }
}
