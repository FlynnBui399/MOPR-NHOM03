package com.example.pocket;

import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.pocket.data.remote.CloudinaryService;
import com.example.pocket.ui.PocketButton;
import com.example.pocket.utils.ImageUtils;
import com.example.pocket.utils.SharedPrefManager;
import com.example.pocket.viewmodel.ProfileViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private ProfileViewModel viewModel;
    private CircleImageView avatarImage;
    private TextView displayNameText;
    private TextView phoneNumberText;
    private PocketButton changeAvatarButton;
    private PocketButton editNameButton;
    private EditText nameEditText;
    private PocketButton saveNameButton;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::uploadAvatar);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        avatarImage = view.findViewById(R.id.profile_avatar);
        displayNameText = view.findViewById(R.id.profile_display_name);
        phoneNumberText = view.findViewById(R.id.profile_phone_number);
        changeAvatarButton = view.findViewById(R.id.profile_change_avatar_button);
        editNameButton = view.findViewById(R.id.profile_edit_name_button);
        nameEditText = view.findViewById(R.id.profile_name_edit_text);
        saveNameButton = view.findViewById(R.id.profile_save_name_button);
        LinearLayout privacyRow = view.findViewById(R.id.profile_privacy_row);
        LinearLayout logoutRow = view.findViewById(R.id.profile_logout_row);
        LinearLayout themeRow = view.findViewById(R.id.profile_theme_row);
        TextView themeStatusText = view.findViewById(R.id.profile_theme_status);
        Switch themeSwitch = view.findViewById(R.id.profile_theme_switch);
        LinearLayout languageRow = view.findViewById(R.id.profile_language_row);
        TextView languageStatusText = view.findViewById(R.id.profile_language_status);

        changeAvatarButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        editNameButton.setOnClickListener(v -> showNameEditor());
        saveNameButton.setOnClickListener(v -> saveDisplayName());
        privacyRow.setOnClickListener(v -> Toast.makeText(requireContext(), "Sắp ra mắt", Toast.LENGTH_SHORT).show());
        logoutRow.setOnClickListener(v -> signOut());

        // Theme Toggle Setup
        int currentMode = SharedPrefManager.getInstance(requireContext()).getThemeMode();
        boolean isDark = currentMode == AppCompatDelegate.MODE_NIGHT_YES;
        themeSwitch.setChecked(isDark);
        themeStatusText.setText(isDark ? R.string.profile_theme_dark : R.string.profile_theme_light);

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int newMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            SharedPrefManager.getInstance(requireContext()).setThemeMode(newMode);
            AppCompatDelegate.setDefaultNightMode(newMode);
        });
        themeRow.setOnClickListener(v -> themeSwitch.toggle());

        // Language Toggle Setup
        String currentLang = "en";
        String savedLanguage = SharedPrefManager.getInstance(requireContext()).getLanguageLocale();
        if (savedLanguage != null) {
            currentLang = savedLanguage;
        } else {
            LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
            if (!currentLocales.isEmpty()) {
                currentLang = currentLocales.get(0).getLanguage();
            } else {
                currentLang = java.util.Locale.getDefault().getLanguage();
            }
        }
        boolean isVi = "vi".equalsIgnoreCase(currentLang);
        languageStatusText.setText(isVi ? R.string.profile_language_vi : R.string.profile_language_en);

        languageRow.setOnClickListener(v -> {
            String currLang = "en";
            String savedLang = SharedPrefManager.getInstance(requireContext()).getLanguageLocale();
            if (savedLang != null) {
                currLang = savedLang;
            } else {
                LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
                if (!currentLocales.isEmpty()) {
                    currLang = currentLocales.get(0).getLanguage();
                } else {
                    currLang = java.util.Locale.getDefault().getLanguage();
                }
            }
            String newLang = "vi".equalsIgnoreCase(currLang) ? "en" : "vi";
            SharedPrefManager.getInstance(requireContext()).setLanguageLocale(newLang);
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newLang));
            languageStatusText.setText("vi".equalsIgnoreCase(newLang) ? R.string.profile_language_vi : R.string.profile_language_en);
        });

        viewModel.currentUser.observe(getViewLifecycleOwner(), user -> {
            if (user == null) {
                return;
            }
            displayNameText.setText(nonEmpty(user.getDisplayName(), "Pocket User"));
            phoneNumberText.setText(nonEmpty(user.getPhoneNumber(), nonEmpty(user.getEmail(), "")));
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_placeholder)
                    .into(avatarImage);

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).updateTopBarAvatar(user.getAvatarUrl());
            }
        });
        viewModel.errorMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadAvatar(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }

        changeAvatarButton.setLoading(true);
        try {
            Bitmap bitmap = ImageUtils.uriToBitmap(requireContext(), uri);
            byte[] bytes = ImageUtils.compress(bitmap);
            new CloudinaryService().uploadUnsigned(bytes)
                    .addOnSuccessListener(result -> {
                        changeAvatarButton.setLoading(false);
                        viewModel.updateAvatar(result.getSecureUrl());
                    })
                    .addOnFailureListener(error -> {
                        changeAvatarButton.setLoading(false);
                        Log.e(TAG, "Failed to upload avatar", error);
                        Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (Exception error) {
            changeAvatarButton.setLoading(false);
            Log.e(TAG, "Failed to read avatar image", error);
            Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        SharedPrefManager.getInstance(requireContext()).clear();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showNameEditor() {
        nameEditText.setVisibility(View.VISIBLE);
        saveNameButton.setVisibility(View.VISIBLE);
        nameEditText.setText(displayNameText.getText());
        nameEditText.setSelection(nameEditText.getText().length());
        nameEditText.requestFocus();

        InputMethodManager inputMethodManager =
                (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(nameEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void saveDisplayName() {
        String newName = nameEditText.getText() == null ? "" : nameEditText.getText().toString().trim();
        if (newName.isEmpty()) {
            showSnackbar("Tên không được để trống");
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showSnackbar("Bạn chưa đăng nhập");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update("displayName", newName)
                .addOnSuccessListener(unused -> {
                    displayNameText.setText(newName);
                    nameEditText.setVisibility(View.GONE);
                    saveNameButton.setVisibility(View.GONE);
                    showSnackbar("Đã cập nhật tên");

                    com.example.pocket.data.model.User currentUserModel = viewModel.currentUser.getValue();
                    if (currentUserModel != null) {
                        currentUserModel.setDisplayName(newName);
                        viewModel.currentUser.setValue(currentUserModel);
                        SharedPrefManager.getInstance(requireContext()).saveUser(currentUserModel);
                    }
                })
                .addOnFailureListener(error ->
                        showSnackbar(error.getMessage() == null ? "Không thể cập nhật tên" : error.getMessage()));
    }

    private void showSnackbar(@NonNull String message) {
        View view = getView();
        if (view == null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            return;
        }
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }

    @NonNull
    private String nonEmpty(@Nullable String value, @NonNull String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
