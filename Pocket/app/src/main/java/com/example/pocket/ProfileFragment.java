package com.example.pocket;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.auth.FirebaseAuth;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private ProfileViewModel viewModel;
    private CircleImageView avatarImage;
    private TextView displayNameText;
    private TextView phoneNumberText;
    private PocketButton changeAvatarButton;

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
        LinearLayout privacyRow = view.findViewById(R.id.profile_privacy_row);
        LinearLayout logoutRow = view.findViewById(R.id.profile_logout_row);

        changeAvatarButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        privacyRow.setOnClickListener(v -> Toast.makeText(requireContext(), "Sắp ra mắt", Toast.LENGTH_SHORT).show());
        logoutRow.setOnClickListener(v -> signOut());

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

    @NonNull
    private String nonEmpty(@Nullable String value, @NonNull String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
