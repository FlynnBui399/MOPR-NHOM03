package com.example.pocket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocket.utils.SharedPrefManager;
import com.example.pocket.viewmodel.ChatUnreadViewModel;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_HISTORY = "open_history";

    private static final String STATE_SELECTED_TAB = "selected_tab";
    private static final String TAG_MEMORIES = "main_memories";
    private static final String TAG_HOME = "main_home";
    private static final String TAG_CHAT = "main_chat";
    private static final String TAG_PROFILE = "main_profile";
    private static final String TAG_FRIENDS = "main_friends";

    private int selectedTabId = View.NO_ID;
    private LinearLayout memoriesTab;
    private LinearLayout homeTab;
    private LinearLayout chatTab;
    private TextView chatUnreadBadge;

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
        bindViews();
        applyWindowInsets();

        memoriesTab.setOnClickListener(view -> selectTab(R.id.nav_memories_tab));
        homeTab.setOnClickListener(view -> handleHomeTabClick());
        chatTab.setOnClickListener(view -> selectTab(R.id.nav_chat_tab));

        new ViewModelProvider(this).get(ChatUnreadViewModel.class)
                .getUnreadCount().observe(this, this::showUnreadBadge);

        if (savedInstanceState == null) {
            selectTab(R.id.nav_home_tab);
        } else {
            selectTab(savedInstanceState.getInt(STATE_SELECTED_TAB, R.id.nav_home_tab));
        }

        PocketMessagingService.refreshTokenForCurrentUser();
    }

    private void bindViews() {
        memoriesTab = findViewById(R.id.nav_memories_tab);
        homeTab = findViewById(R.id.nav_home_tab);
        chatTab = findViewById(R.id.nav_chat_tab);
        chatUnreadBadge = findViewById(R.id.chat_unread_badge);
    }

    private void showUnreadBadge(Integer countValue) {
        int count = countValue == null ? 0 : countValue;
        chatUnreadBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        chatUnreadBadge.setText(count > 99 ? "99+" : String.valueOf(count));
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.main);
        View content = findViewById(R.id.main_fragment_container);
        View bottomNavigation = findViewById(R.id.main_bottom_navigation);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            content.setPadding(0, statusBar, 0, 0);

            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) bottomNavigation.getLayoutParams();
            params.bottomMargin = navigationBar + dp(8);
            bottomNavigation.setLayoutParams(params);

            return insets;
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void selectTab(int itemId) {
        int previousTabId = selectedTabId;
        Fragment current = getSupportFragmentManager().getPrimaryNavigationFragment();
        boolean returningToHome = itemId == R.id.nav_home_tab
                && previousTabId != R.id.nav_home_tab;
        if (previousTabId == R.id.nav_home_tab && itemId != R.id.nav_home_tab
                && current instanceof HomeFragment) {
            ((HomeFragment) current).onHomeTabUnselected();
        }
        selectedTabId = itemId;

        memoriesTab.setSelected(itemId == R.id.nav_memories_tab);
        homeTab.setSelected(itemId == R.id.nav_home_tab);
        chatTab.setSelected(itemId == R.id.nav_chat_tab);

        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment;
        String tag;

        if (itemId == R.id.nav_memories_tab) {
            tag = TAG_MEMORIES;
            fragment = manager.findFragmentByTag(tag);
            if (fragment == null) {
                fragment = new MemoriesFragment();
            }
        } else if (itemId == R.id.nav_chat_tab) {
            tag = TAG_CHAT;
            fragment = manager.findFragmentByTag(tag);
            if (fragment == null) {
                fragment = new ChatListFragment();
            }
        } else {
            tag = TAG_HOME;
            fragment = manager.findFragmentByTag(tag);
            if (fragment == null) {
                fragment = new HomeFragment();
            }
        }

        Runnable onShown = returningToHome && fragment instanceof HomeFragment
                ? ((HomeFragment) fragment)::onHomeTabSelectedFromAnotherTab
                : null;
        showFragment(fragment, tag, onShown);
    }

    private void handleHomeTabClick() {
        Fragment current = getSupportFragmentManager().getPrimaryNavigationFragment();
        if (selectedTabId == R.id.nav_home_tab && current instanceof HomeFragment) {
            ((HomeFragment) current).returnToCameraMode();
            return;
        }
        selectTab(R.id.nav_home_tab);
    }

    public void openProfile() {
        notifyHomeUnselected();
        memoriesTab.setSelected(false);
        homeTab.setSelected(false);
        chatTab.setSelected(false);

        Fragment profile = getSupportFragmentManager().findFragmentByTag(TAG_PROFILE);
        showFragment(profile == null ? new ProfileFragment() : profile, TAG_PROFILE);
    }

    public void openHome() {
        selectTab(R.id.nav_home_tab);
    }

    public void openFriends() {
        notifyHomeUnselected();
        memoriesTab.setSelected(false);
        homeTab.setSelected(false);
        chatTab.setSelected(false);

        Fragment friends = getSupportFragmentManager().findFragmentByTag(TAG_FRIENDS);
        showFragment(friends == null ? new FriendListFragment() : friends, TAG_FRIENDS);
    }

    private void notifyHomeUnselected() {
        Fragment current = getSupportFragmentManager().getPrimaryNavigationFragment();
        if (current instanceof HomeFragment) {
            ((HomeFragment) current).onHomeTabUnselected();
        }
    }

    public void updateTopBarAvatar(String url) {
        SharedPrefManager.getInstance(this).updateAvatarUrl(url);
    }

    private void showFragment(@NonNull Fragment fragment, @NonNull String tag) {
        showFragment(fragment, tag, null);
    }

    private void showFragment(@NonNull Fragment fragment,
                              @NonNull String tag,
                              @Nullable Runnable onShown) {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction().setReorderingAllowed(true);

        for (Fragment existing : manager.getFragments()) {
            if (existing.getId() == R.id.main_fragment_container && existing != fragment) {
                transaction.hide(existing);
                transaction.setMaxLifecycle(existing, Lifecycle.State.CREATED);
            }
        }

        if (fragment.isAdded()) {
            transaction.show(fragment);
        } else {
            transaction.add(R.id.main_fragment_container, fragment, tag);
        }

        transaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED);
        transaction.setPrimaryNavigationFragment(fragment);
        if (onShown != null) {
            transaction.runOnCommit(onShown);
        }
        transaction.commit();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, selectedTabId);
    }
}

