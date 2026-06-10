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
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }

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

        topBarAvatar.setOnClickListener(view -> showFragment(new ProfileFragment()));
        topBarChatButton.setOnClickListener(view ->
                Toast.makeText(this, "Sắp ra mắt", Toast.LENGTH_SHORT).show());
        findViewById(R.id.top_bar_filter_pill).setOnClickListener(view ->
                Toast.makeText(this, "Sắp ra mắt", Toast.LENGTH_SHORT).show());
        feedButton.setOnClickListener(view -> selectTab(R.id.nav_feed));
        cameraButton.setOnClickListener(view -> startActivity(new Intent(this, CameraActivity.class)));
        friendsButton.setOnClickListener(view -> selectTab(R.id.nav_friends));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.avatar_placeholder)
                    .into(topBarAvatar);
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

    private void selectTab(int itemId) {
        selectedTabId = itemId;
        feedButton.setSelected(itemId == R.id.nav_feed);
        friendsButton.setSelected(itemId == R.id.nav_friends);

        if (itemId == R.id.nav_feed) {
            showFragment(new FeedFragment());
        } else if (itemId == R.id.nav_friends) {
            showFragment(new FriendListFragment());
        }
    }

    private void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }
}
