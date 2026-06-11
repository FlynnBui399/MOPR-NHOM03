package com.example.pocket;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.pocket.utils.SharedPrefManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {
    private int selectedTabId = R.id.nav_feed;
    private ImageButton feedButton;
    private ImageButton cameraButton;
    private ImageButton friendsButton;
    private CircleImageView topBarAvatar;
    private ImageButton topBarChatButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        View rootView = findViewById(R.id.main);
        LinearLayout topBar = findViewById(R.id.top_bar);
        LinearLayout bottomNavPill = findViewById(R.id.main_bottom_nav_pill);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            topBar.setPadding(
                    topBar.getPaddingLeft(),
                    statusBarHeight,
                    topBar.getPaddingRight(),
                    topBar.getPaddingBottom()
            );

            int basePaddingBottom = (int) (16 * v.getResources().getDisplayMetrics().density);
            bottomNavPill.setPadding(
                    bottomNavPill.getPaddingLeft(),
                    bottomNavPill.getPaddingTop(),
                    bottomNavPill.getPaddingRight(),
                    basePaddingBottom + navBarHeight
            );

            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) bottomNavPill.getLayoutParams();
            params.bottomMargin = 0;
            bottomNavPill.setLayoutParams(params);

            return insets;
        });

        topBarAvatar = findViewById(R.id.top_bar_avatar);
        topBarChatButton = findViewById(R.id.top_bar_chat_button);
        feedButton = findViewById(R.id.nav_feed_button);
        cameraButton = findViewById(R.id.nav_camera_button);
        friendsButton = findViewById(R.id.nav_friends_button);

        topBarAvatar.setOnClickListener(view -> selectTab(R.id.top_bar_avatar));
        topBarChatButton.setOnClickListener(view -> selectTab(R.id.top_bar_chat_button));
        findViewById(R.id.top_bar_filter_pill).setOnClickListener(view ->
                Toast.makeText(this, "Sắp ra mắt", Toast.LENGTH_SHORT).show());
        feedButton.setOnClickListener(view -> selectTab(R.id.nav_feed));
        cameraButton.setOnClickListener(view -> startActivity(new Intent(this, CameraActivity.class)));
        friendsButton.setOnClickListener(view -> selectTab(R.id.nav_friends));

        String cachedAvatar = SharedPrefManager.getInstance(this).getAvatarUrl();
        if (cachedAvatar != null) {
            updateTopBarAvatar(cachedAvatar);
        } else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getPhotoUrl() != null) {
                updateTopBarAvatar(user.getPhotoUrl().toString());
            }
        }

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

    public void updateTopBarAvatar(String url) {
        if (topBarAvatar != null && url != null) {
            Glide.with(this)
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.avatar_placeholder)
                    .into(topBarAvatar);
        }
    }

    private void selectTab(int itemId) {
        selectedTabId = itemId;
        feedButton.setSelected(itemId == R.id.nav_feed);
        friendsButton.setSelected(itemId == R.id.nav_friends);

        if (itemId == R.id.nav_feed) {
            showFragment(new FeedFragment());
        } else if (itemId == R.id.nav_friends) {
            showFragment(new FriendListFragment());
        } else if (itemId == R.id.top_bar_avatar) {
            showFragment(new ProfileFragment());
        } else if (itemId == R.id.top_bar_chat_button) {
            showFragment(new ChatListFragment());
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }
}
