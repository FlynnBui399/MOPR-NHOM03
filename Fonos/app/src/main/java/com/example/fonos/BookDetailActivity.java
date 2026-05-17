package com.example.fonos;


import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Button;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class BookDetailActivity extends AppCompatActivity {

    private TextView tvDetailTitle, tvDetailAuthor, tvDetailRating, tvDetailDuration;
    private TextView tvDetailChapters, tvDetailDescription, tvDetailCoverTitle;
    private View viewDetailCover;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        initViews();
        setupToolbar();
        loadBookData();
        setupListenNowButton();
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
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        //toolbar.setNavigationOnClickListener(v -> onBackPressed());
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadBookData() {
        if (getIntent() != null) {
            String title = getIntent().getStringExtra("book_title");
            String author = getIntent().getStringExtra("book_author");
            String desc = getIntent().getStringExtra("book_desc");
            float rating = getIntent().getFloatExtra("book_rating", 0f);
            String duration = getIntent().getStringExtra("book_duration");
            int chapters = getIntent().getIntExtra("book_chapters", 0);
            int coverRes = getIntent().getIntExtra("book_cover", R.drawable.bg_book_cover_1);

            tvDetailTitle.setText(title);
            tvDetailAuthor.setText(author);
            tvDetailDescription.setText(desc);
            tvDetailRating.setText(String.valueOf(rating));
            tvDetailDuration.setText(duration);
            //tvDetailChapters.setText(chapters + " " + getString(R.string.detail_chapters));
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

            int coverRes = getIntent().getIntExtra("book_cover", R.drawable.bg_book_cover_1);
            intent.putExtra("book_cover", coverRes);

            startActivity(intent);
        });
    }
}
