package com.example.fonos;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.fonos.model.Book;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to persist and retrieve recently listened books in SharedPreferences using Gson.
 * Newest items are stored first, duplicates are removed, and size is capped to 10 entries.
 */
public final class RecentlyPlayedManager {
    private static final String PREF_NAME = "FonosPref";
    private static final String HISTORY_KEY = "listen_history";
    private static final int MAX_HISTORY_SIZE = 10;

    private RecentlyPlayedManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Adds a book to the listening history. Newest first, duplicate book IDs are removed, capped at 10.
     */
    public static synchronized void addToHistory(Context ctx, Book book) {
        if (book == null || ctx == null) return;

        SharedPreferences pref = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<Book> history = getHistory(ctx);

        Book existingBook = null;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getId() == book.getId()) {
                existingBook = history.remove(i);
                break;
            }
        }

        if (book.getRating() <= 0f && existingBook != null && existingBook.getRating() > 0f) {
            book.setRating(existingBook.getRating());
        }

        // Add to front (index 0)
        history.add(0, book);

        // Capped at 10 entries maximum
        if (history.size() > MAX_HISTORY_SIZE) {
            history = new ArrayList<>(history.subList(0, MAX_HISTORY_SIZE));
        }

        // Serialize and save to SharedPreferences
        try {
            Gson gson = new Gson();
            String json = gson.toJson(history);
            pref.edit().putString(HISTORY_KEY, json).apply();
        } catch (Exception e) {
            // Safe fallback
        }
    }

    /**
     * Retrieves the list of recently listened books.
     */
    public static synchronized List<Book> getHistory(Context ctx) {
        if (ctx == null) {
            return new ArrayList<>();
        }

        SharedPreferences pref = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = pref.getString(HISTORY_KEY, null);
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Gson gson = new Gson();
            Type listType = new TypeToken<ArrayList<Book>>() {}.getType();
            List<Book> history = gson.fromJson(json, listType);
            return history != null ? history : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static synchronized void saveHistory(Context ctx, List<Book> history) {
        if (ctx == null || history == null) return;

        SharedPreferences pref = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        try {
            Gson gson = new Gson();
            String json = gson.toJson(history);
            pref.edit().putString(HISTORY_KEY, json).apply();
        } catch (Exception e) {
            // Safe fallback
        }
    }
}
