package com.example.fonos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fonos.auth.LoginActivity;
import com.example.fonos.model.Book;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class BookDetailActivity extends AppCompatActivity {

    private TextView tvDetailTitle, tvDetailAuthor, tvDetailRating, tvDetailDuration;
    private TextView tvDetailChapters, tvDetailDescription, tvDetailCoverTitle;
    private View viewDetailCover;
    private Toolbar toolbar;
    private Button btnAddLibrary;

    private int bookId;
    private String title, author, desc, duration, category;
    private float rating;
    private int chapters, coverRes;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isAddedToLibrary = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_detail);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        initViews();
        setupToolbar();
        loadBookData();
        setupListenNowButton();
        setupLibraryButton();
    }

    private void initViews() {
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailAuthor = findViewById(R.id.tvDetailAuthor);
        tvDetailRating = findViewById(R.id.tvDetailRating);
        tvDetailDuration = findViewById(R.id.tvDetailDuration);
        tvDetailChapters = findViewById(R.id.tvDetailChapters);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailCoverTitle = findViewById(R.id.tvDetailCoverTitle);
        viewDetailCover = findViewById(R.id.viewDetailCover);
        toolbar = findViewById(R.id.toolbar);
        btnAddLibrary = findViewById(R.id.btnAddLibrary);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadBookData() {
        if (getIntent() != null) {
            bookId = getIntent().getIntExtra("book_id", 0);
            title = getIntent().getStringExtra("book_title");
            author = getIntent().getStringExtra("book_author");
            desc = getIntent().getStringExtra("book_desc");
            rating = getIntent().getFloatExtra("book_rating", 0f);
            duration = getIntent().getStringExtra("book_duration");
            chapters = getIntent().getIntExtra("book_chapters", 0);
            coverRes = getIntent().getIntExtra("book_cover", R.drawable.bg_book_cover_1);
            category = getIntent().getStringExtra("book_category");
            if (category == null) {
                category = "General";
            }

            tvDetailTitle.setText(title);
            tvDetailAuthor.setText(author);
            tvDetailDescription.setText(desc);
            tvDetailRating.setText(String.valueOf(rating));
            tvDetailDuration.setText(duration);
            tvDetailChapters.setText(getString(R.string.detail_chapter_count, chapters));

            tvDetailCoverTitle.setText(title);
            viewDetailCover.setBackgroundResource(coverRes);
        }
    }

    private void setupListenNowButton() {
        Button btnListenNow = findViewById(R.id.btnListenNow);
        btnListenNow.setOnClickListener(v -> {
            Intent intent = new Intent(BookDetailActivity.this, AudioPlayerActivity.class);
            intent.putExtra("book_title", tvDetailTitle.getText().toString());
            intent.putExtra("book_author", tvDetailAuthor.getText().toString());
            intent.putExtra("book_duration", tvDetailDuration.getText().toString());
            int cover = getIntent().getIntExtra("book_cover", R.drawable.bg_book_cover_1);
            intent.putExtra("book_cover", cover);
            startActivity(intent);
        });
    }

    private void setupLibraryButton() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users")
                    .document(user.getUid())
                    .collection("library")
                    .document(String.valueOf(bookId))
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            isAddedToLibrary = true;
                            btnAddLibrary.setText("Xóa khỏi thư viện");
                        } else {
                            isAddedToLibrary = false;
                            btnAddLibrary.setText(getString(R.string.detail_add_library));
                        }
                    });
        } else {
            isAddedToLibrary = false;
            btnAddLibrary.setText(getString(R.string.detail_add_library));
        }

        btnAddLibrary.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, "Please log in to save books to your library", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(BookDetailActivity.this, LoginActivity.class);
                startActivity(intent);
                return;
            }

            btnAddLibrary.setEnabled(false);
            if (isAddedToLibrary) {
                db.collection("users")
                        .document(currentUser.getUid())
                        .collection("library")
                        .document(String.valueOf(bookId))
                        .delete()
                        .addOnCompleteListener(task -> {
                            btnAddLibrary.setEnabled(true);
                            if (task.isSuccessful()) {
                                isAddedToLibrary = false;
                                btnAddLibrary.setText(getString(R.string.detail_add_library));
                                Toast.makeText(BookDetailActivity.this, "Removed from library", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(BookDetailActivity.this, "Failed to remove book", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Book book = new Book(bookId, title, author, desc, rating, duration, chapters, coverRes, category);
                db.collection("users")
                        .document(currentUser.getUid())
                        .collection("library")
                        .document(String.valueOf(bookId))
                        .set(book)
                        .addOnCompleteListener(task -> {
                            btnAddLibrary.setEnabled(true);
                            if (task.isSuccessful()) {
                                isAddedToLibrary = true;
                                btnAddLibrary.setText("Xóa khỏi thư viện");
                                Toast.makeText(BookDetailActivity.this, "Added to library", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(BookDetailActivity.this, "Failed to add book", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
}
