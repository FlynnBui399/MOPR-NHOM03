package com.example.fonos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.fonos.fragment.HomeFragment;
import com.example.fonos.fragment.LibraryFragment;
import com.example.fonos.fragment.ProfileFragment;
import com.example.fonos.fragment.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        setupMiniPlayer();
        
        // Seed books collection in Firestore if empty
        FirestoreSeeder.seedBooks(this);
        
        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_search) {
                fragment = new SearchFragment();
            } else if (itemId == R.id.nav_library) {
                fragment = new LibraryFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateMiniPlayer();
    }

    private void updateMiniPlayer() {
        SharedPreferences sharedPref = getSharedPreferences("FonosPref", MODE_PRIVATE);
        String activeTitle = sharedPref.getString("active_book_title", null);
        String activeAuthor = sharedPref.getString("active_book_author", null);
        String activeDuration = sharedPref.getString("active_book_duration", null);
        int activeCover = sharedPref.getInt("active_book_cover", R.drawable.bg_book_cover_1);
        String activeCoverUrl = sharedPref.getString("active_book_cover_url", null);

        TextView tvTitle = findViewById(R.id.tvMiniPlayerTitle);
        TextView tvSubtitle = findViewById(R.id.tvMiniPlayerSubtitle);
        ImageView imgCover = findViewById(R.id.imgMiniPlayerCover);
        TextView tvCoverTitle = findViewById(R.id.tvMiniPlayerCoverTitle);

        if (activeTitle != null) {
            tvTitle.setText(activeTitle);
            tvSubtitle.setText("Tiếp tục nghe • " + activeAuthor);
            tvCoverTitle.setText(activeTitle);

            if (activeCoverUrl != null && !activeCoverUrl.isEmpty()) {
                Glide.with(this).load(activeCoverUrl).into(imgCover);
            } else {
                imgCover.setImageResource(activeCover);
            }

            findViewById(R.id.miniPlayer).setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AudioPlayerActivity.class);
                intent.putExtra("book_title", activeTitle);
                intent.putExtra("book_author", activeAuthor);
                intent.putExtra("book_duration", activeDuration);
                intent.putExtra("book_cover", activeCover);
                intent.putExtra("book_cover_url", activeCoverUrl);
                startActivity(intent);
            });
        } else {
            // Default placeholder state if no book has been played yet
            tvTitle.setText(getString(R.string.mini_player_title));
            tvSubtitle.setText(getString(R.string.mini_player_subtitle));
            tvCoverTitle.setText("Fonos");
            imgCover.setImageResource(R.drawable.bg_book_cover_1);

            findViewById(R.id.miniPlayer).setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AudioPlayerActivity.class);
                intent.putExtra("book_title", getString(R.string.book1_title));
                intent.putExtra("book_author", getString(R.string.book1_author));
                intent.putExtra("book_duration", "8h 30m");
                intent.putExtra("book_cover", R.drawable.bg_book_cover_1);
                startActivity(intent);
            });
        }
    }

    private void setupMiniPlayer() {
        // Initial setup, will be updated by updateMiniPlayer in onResume
        updateMiniPlayer();
    }
}