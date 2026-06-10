package com.example.pocket;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private int selectedTabId = R.id.nav_feed;
    private ImageButton feedButton;
    private ImageButton cameraButton;
    private ImageButton friendsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        feedButton = findViewById(R.id.nav_feed_button);
        cameraButton = findViewById(R.id.nav_camera_button);
        friendsButton = findViewById(R.id.nav_friends_button);

        feedButton.setOnClickListener(view -> selectTab(R.id.nav_feed));
        cameraButton.setOnClickListener(view -> startActivity(new Intent(this, CameraActivity.class)));
        friendsButton.setOnClickListener(view -> selectTab(R.id.nav_friends));

        if (savedInstanceState == null) {
            selectTab(R.id.nav_feed);
        } else {
            selectedTabId = savedInstanceState.getInt("selectedTabId", R.id.nav_feed);
            selectTab(selectedTabId);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selectedTabId", selectedTabId);
    }

    private void selectTab(int itemId) {
        selectedTabId = itemId;
        feedButton.setSelected(itemId == R.id.nav_feed);
        friendsButton.setSelected(itemId == R.id.nav_friends);

        if (itemId == R.id.nav_feed) {
            showFragment(new FeedFragment());
        } else if (itemId == R.id.nav_friends) {
            showFragment(new FriendsFragment());
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }
}
