package com.example.fonos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        setupMiniPlayer();

        // Do not seed placeholder data from the app.
        // Firestore data should be managed from Firebase Console or official seed scripts.
        // FirestoreSeeder.seedBooks(this);

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

    private void setupMiniPlayer() {
        updateMiniPlayer();
    }

    private void updateMiniPlayer() {
        SharedPreferences sharedPref = getSharedPreferences("FonosPref", MODE_PRIVATE);

        String activeTitle = sharedPref.getString("active_book_title", null);
        String activeAuthor = sharedPref.getString("active_book_author", null);
        String activeDuration = sharedPref.getString("active_book_duration", null);
        int activeCover = sharedPref.getInt("active_book_cover", R.drawable.bg_book_cover_1);
        String activeCoverUrl = sharedPref.getString("active_book_cover_url", null);
        String activeAudioUrl = sharedPref.getString("active_book_audio_url", null);

        TextView tvTitle = findViewById(R.id.tvMiniPlayerTitle);
        TextView tvSubtitle = findViewById(R.id.tvMiniPlayerSubtitle);
        ImageView imgCover = findViewById(R.id.imgMiniPlayerCover);
        TextView tvCoverTitle = findViewById(R.id.tvMiniPlayerCoverTitle);

        if (tvTitle == null ||
                tvSubtitle == null ||
                imgCover == null ||
                tvCoverTitle == null ||
                findViewById(R.id.miniPlayer) == null) {
            return;
        }

        if (activeTitle != null && !activeTitle.trim().isEmpty()) {
            tvTitle.setText(activeTitle);
            tvSubtitle.setText("Tiếp tục nghe • " + (activeAuthor != null ? activeAuthor : ""));
            tvCoverTitle.setText(activeTitle);

            if (isValidUrl(activeCoverUrl)) {
                Glide.with(this)
                        .load(activeCoverUrl)
                        .placeholder(activeCover)
                        .error(activeCover)
                        .into(imgCover);
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
                intent.putExtra("audio_url", activeAudioUrl);
                intent.putExtra("book_audio_url", activeAudioUrl);
                startActivity(intent);
            });
        } else {
            tvTitle.setText(getString(R.string.mini_player_title));
            tvSubtitle.setText(getString(R.string.mini_player_subtitle));
            tvCoverTitle.setText("Fonos");
            imgCover.setImageResource(R.drawable.bg_book_cover_1);

            findViewById(R.id.miniPlayer).setOnClickListener(v -> {
                Toast.makeText(this, "Hãy chọn một sách để bắt đầu nghe", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private boolean isValidUrl(String value) {
        return value != null &&
                (value.trim().startsWith("http://") || value.trim().startsWith("https://"));
    }
}