package com.example.fonos;

import com.example.fonos.model.Book;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory cache for book data loaded from Firestore.
 * Avoids redundant network requests when switching between tabs.
 * Both HomeFragment and SearchFragment share this cache.
 */
public class SearchCache {
    private static List<Book> cachedBooks = null;

    /** Store a copy of books into the cache */
    public static void set(List<Book> books) {
        cachedBooks = new ArrayList<>(books);
    }

    /** Retrieve a copy of cached books, or empty list if cache is empty */
    public static List<Book> get() {
        return cachedBooks != null ? new ArrayList<>(cachedBooks) : new ArrayList<>();
    }

    /** Check if the cache has any data */
    public static boolean isEmpty() {
        return cachedBooks == null || cachedBooks.isEmpty();
    }

    /** Clear the cache (e.g., on pull-to-refresh) */
    public static void invalidate() {
        cachedBooks = null;
    }
}
