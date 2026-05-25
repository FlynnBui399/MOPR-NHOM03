package com.example.fonos;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class FirestoreSeeder {
    private static final String TAG = "FirestoreSeeder";

    public static void seedBooks(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference booksRef = db.collection("books");

        // Check if collection is empty
        booksRef.limit(1).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    QuerySnapshot result = task.getResult();
                    if (result != null && result.isEmpty()) {
                        Log.d(TAG, "Books collection is empty. Seeding 10 audiobooks...");
                        seedData(context, booksRef);
                    } else {
                        Log.d(TAG, "Books collection is not empty. Skipping seeding.");
                    }
                } else {
                    Log.e(TAG, "Error checking if books collection is empty: ", task.getException());
                }
            }
        });
    }

    private static void seedData(Context context, CollectionReference booksRef) {
        // 1. Pride and Prejudice
        insertBook(booksRef, 1, "Pride and Prejudice", "Jane Austen", 
                "A classic romantic novel of manners written by Jane Austen. The story charts the emotional development of Elizabeth Bennet.", 
                4.8f, "8h 30m", 12, "Fiction", true, false, 
                "placeholder_audio_url_pride_and_prejudice", "placeholder_cover_url_pride_and_prejudice");

        // 2. Alice's Adventures in Wonderland
        insertBook(booksRef, 2, "Alice's Adventures in Wonderland", "Lewis Carroll", 
                "An 1865 fantasy novel by English author Lewis Carroll. It tells of a young girl named Alice falling through a rabbit hole.", 
                4.7f, "5h 45m", 15, "Fiction", true, false, 
                "placeholder_audio_url_alices_adventures_in_wonderland", "placeholder_cover_url_alices_adventures_in_wonderland");

        // 3. Dracula
        insertBook(booksRef, 3, "Dracula", "Bram Stoker", 
                "An 1897 Gothic horror novel by Irish author Bram Stoker. It introduced the character of Count Dracula and established many conventions of subsequent vampire fantasy.", 
                4.6f, "6h 20m", 10, "Fiction", true, false, 
                "placeholder_audio_url_dracula", "placeholder_cover_url_dracula");

        // 4. The Adventures of Tom Sawyer
        insertBook(booksRef, 4, "The Adventures of Tom Sawyer", "Mark Twain", 
                "An 1876 novel by Mark Twain about a boy growing up along the Mississippi River. Tom Sawyer has several adventures, often with his friend Huckleberry Finn.", 
                4.9f, "15h 10m", 20, "Fiction", true, false, 
                "placeholder_audio_url_tom_sawyer", "placeholder_cover_url_tom_sawyer");

        // 5. Meditations
        insertBook(booksRef, 5, "Meditations", "Marcus Aurelius", 
                "A series of personal writings by Marcus Aurelius, Roman Emperor from 161 to 180 AD, recording his private notes to himself and ideas on Stoic philosophy.", 
                4.5f, "14h 05m", 18, "Self-help", false, true, 
                "placeholder_audio_url_meditations", "placeholder_cover_url_meditations");

        // 6. The Art of War
        insertBook(booksRef, 6, "The Art of War", "Sun Tzu", 
                "An ancient Chinese military treatise dating from the Late Spring and Autumn Period. The work, which is attributed to the ancient Chinese military strategist Sun Tzu.", 
                4.8f, "7h 15m", 11, "Self-help", false, true, 
                "placeholder_audio_url_art_of_war", "placeholder_cover_url_art_of_war");

        // 7. The Richest Man in Babylon
        insertBook(booksRef, 7, "The Richest Man in Babylon", "George S. Clason", 
                "A 1926 book by George S. Clason that dispenses financial advice through a collection of parables set 4,000 years ago in ancient Babylon.", 
                4.4f, "5h 50m", 9, "Business", false, true, 
                "placeholder_audio_url_richest_man_in_babylon", "placeholder_cover_url_richest_man_in_babylon");

        // 8. Flatland
        insertBook(booksRef, 8, "Flatland", "Edwin Abbott", 
                "A satirical novella by the English schoolmaster Edwin A. Abbott, first published in 1884. Written pseudonymously by 'A Square', the book used the fictional two-dimensional world of Flatland.", 
                4.9f, "9h 40m", 16, "Science", false, false, 
                "placeholder_audio_url_flatland", "placeholder_cover_url_flatland");

        // 9. The Importance of Being Earnest
        insertBook(booksRef, 9, "The Importance of Being Earnest", "Oscar Wilde", 
                "A play by Oscar Wilde. First performed on 14 February 1895 at the St James's Theatre in London, it is a farcical comedy in which the protagonists maintain fictitious personae.", 
                4.7f, "12h 30m", 21, "Fiction", false, false, 
                "placeholder_audio_url_importance_of_being_earnest", "placeholder_cover_url_importance_of_being_earnest");

        // 10. As a Man Thinketh
        insertBook(booksRef, 10, "As a Man Thinketh", "James Allen", 
                "A self-help book by James Allen, published in 1903. It was described by Allen as dealing with the power of thought, and particularly with the use and application of thought to happy and beautiful issues.", 
                4.6f, "8h 15m", 14, "Psychology", false, false, 
                "placeholder_audio_url_as_a_man_thinketh", "placeholder_cover_url_as_a_man_thinketh");

        Toast.makeText(context, "Audiobooks seeded successfully!", Toast.LENGTH_SHORT).show();
    }

    private static void insertBook(CollectionReference booksRef, int id, String title, String author, 
                                  String description, float rating, String duration, int chapterCount, 
                                  String category, boolean isTrending, boolean isNewRelease, 
                                  String audioUrl, String coverUrl) {
        booksRef.document(String.valueOf(id)).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().exists()) {
                Map<String, Object> book = new HashMap<>();
                book.put("id", id);
                book.put("title", title);
                book.put("author", author);
                book.put("description", description);
                book.put("rating", rating);
                book.put("duration", duration);
                book.put("chapterCount", chapterCount);
                book.put("category", category);
                book.put("isTrending", isTrending);
                book.put("isNewRelease", isNewRelease);
                book.put("audioUrl", audioUrl);
                book.put("coverUrl", coverUrl);

                booksRef.document(String.valueOf(id)).set(book)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Audiobook '" + title + "' seeded successfully with ID: " + id))
                        .addOnFailureListener(e -> Log.e(TAG, "Error seeding audiobook '" + title + "'", e));
            } else if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                Log.d(TAG, "Audiobook '" + title + "' (ID: " + id + ") already exists. Skipping write.");
            } else {
                Log.e(TAG, "Failed to check if audiobook '" + title + "' exists: ", task.getException());
            }
        });
    }
}
