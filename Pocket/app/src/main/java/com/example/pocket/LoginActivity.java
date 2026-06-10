package com.example.pocket;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocket.ui.PocketButton;
import com.example.pocket.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;
    private PocketButton googleButton;
    private PocketButton phoneButton;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
                    authViewModel.signInWithGoogle(result.getData()));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        googleButton = findViewById(R.id.google_sign_in_button);
        phoneButton = findViewById(R.id.phone_sign_in_button);

        googleButton.setOnClickListener(view -> {
            googleButton.setLoading(true);
            googleSignInLauncher.launch(authViewModel.signInWithGoogle());
        });
        phoneButton.setOnClickListener(view ->
                startActivity(new Intent(this, OtpActivity.class)));

        authViewModel.currentUser.observe(this, user -> {
            googleButton.setLoading(false);
            if (user != null) {
                navigateToMain();
            }
        });
        authViewModel.errorMessage.observe(this, message -> {
            googleButton.setLoading(false);
            if (message != null && !message.trim().isEmpty()) {
                showError(message);
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(@NonNull String message) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.login_root), message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.pocket_danger));
        snackbar.setTextColor(ContextCompat.getColor(this, R.color.pocket_on_danger));
        snackbar.show();
    }
}
