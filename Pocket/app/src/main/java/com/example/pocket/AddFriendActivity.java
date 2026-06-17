package com.example.pocket;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.pocket.data.model.User;
import com.example.pocket.data.repository.UserRepository;
import com.example.pocket.ui.PocketButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddFriendActivity extends AppCompatActivity {
    private TextInputEditText phoneInput;
    private View resultCard;
    private CircleImageView resultAvatar;
    private TextView resultName;
    private TextView resultPhone;
    private PocketButton sendRequestButton;
    private User resultUser;
    private UserRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        repository = UserRepository.getInstance();
        ImageButton backButton = findViewById(R.id.add_friend_back_button);
        phoneInput = findViewById(R.id.add_friend_phone_input);
        resultCard = findViewById(R.id.add_friend_result_card);
        resultAvatar = findViewById(R.id.add_friend_result_avatar);
        resultName = findViewById(R.id.add_friend_result_name);
        resultPhone = findViewById(R.id.add_friend_result_phone);
        sendRequestButton = findViewById(R.id.add_friend_send_request_button);
        PocketButton searchButton = findViewById(R.id.add_friend_search_button);

        backButton.setOnClickListener(v -> finish());
        com.example.pocket.utils.ViewUtils.applyPressAnimation(backButton);
        searchButton.setOnClickListener(v -> search());
        com.example.pocket.utils.ViewUtils.applyPressAnimation(searchButton);
        sendRequestButton.setOnClickListener(v -> sendRequest());
        com.example.pocket.utils.ViewUtils.applyPressAnimation(sendRequestButton);
    }

    private void search() {
        String phone = phoneInput.getText() == null ? "" : phoneInput.getText().toString().trim();
        if (phone.isEmpty()) {
            showError(getString(R.string.add_friend_not_found));
            return;
        }

        repository.getUserByPhone(phone, new UserRepository.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                if (result == null) {
                    resultUser = null;
                    resultCard.setVisibility(View.GONE);
                    showError(getString(R.string.add_friend_not_found));
                    return;
                }
                resultUser = result;
                resultCard.setVisibility(View.VISIBLE);
                resultName.setText(displayName(result));
                resultPhone.setVisibility(View.GONE);
                sendRequestButton.setEnabled(true);
                sendRequestButton.setText(R.string.add_friend_send_request);
                Glide.with(AddFriendActivity.this)
                        .load(result.getAvatarUrl())
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.avatar_placeholder)
                        .error(R.drawable.avatar_placeholder)
                        .into(resultAvatar);
            }

            @Override
            public void onError(@NonNull Exception error) {
                showError(error.getMessage() == null ? getString(R.string.add_friend_not_found) : error.getMessage());
            }
        });
    }

    private void sendRequest() {
        if (resultUser == null || resultUser.getId() == null) {
            showError(getString(R.string.add_friend_not_found));
            return;
        }

        repository.sendFriendRequest(resultUser.getId(), new UserRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                sendRequestButton.setText(R.string.add_friend_sent);
                sendRequestButton.setEnabled(false);
            }

            @Override
            public void onError(@NonNull Exception error) {
                showError(error.getMessage() == null ? getString(R.string.add_friend_request_failed) : error.getMessage());
            }
        });
    }

    @NonNull
    private String displayName(@NonNull User user) {
        return user.getDisplayName() == null || user.getDisplayName().trim().isEmpty()
                ? getString(R.string.camera_default_user)
                : user.getDisplayName();
    }

    private void showError(@NonNull String message) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.add_friend_root), message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.pocket_danger));
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.pocket_on_danger));
        snackbar.show();
    }
}
