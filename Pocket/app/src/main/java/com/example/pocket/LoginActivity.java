package com.example.pocket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocket.ui.PocketButton;
import com.example.pocket.viewmodel.AuthViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private PocketButton signInButton;
    private PocketButton createAccountButton;
    private PocketButton phoneButton;
    private TextView forgotPasswordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        emailInput = findViewById(R.id.login_email);
        passwordInput = findViewById(R.id.login_password);
        signInButton = findViewById(R.id.login_sign_in_button);
        createAccountButton = findViewById(R.id.login_create_account_button);
        phoneButton = findViewById(R.id.login_phone_button);
        forgotPasswordText = findViewById(R.id.login_forgot_password);

        signInButton.setOnClickListener(v -> {
            String email = getText(emailInput);
            String password = getText(passwordInput);
            signInButton.setLoading(true);
            authViewModel.signInWithEmail(email, password);
        });

        createAccountButton.setOnClickListener(v -> {
            String email = getText(emailInput);
            String password = getText(passwordInput);
            createAccountButton.setLoading(true);
            authViewModel.createAccountWithEmail(email, password);
        });

        phoneButton.setOnClickListener(v ->
                startActivity(new Intent(this, OtpActivity.class)));

        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());

        authViewModel.currentUser.observe(this, user -> {
            signInButton.setLoading(false);
            createAccountButton.setLoading(false);
            if (user != null) {
                navigateToMain();
            }
        });

        authViewModel.errorMessage.observe(this, message -> {
            signInButton.setLoading(false);
            createAccountButton.setLoading(false);
            if (message != null && !message.trim().isEmpty()) {
                showError(message);
            }
        });

        authViewModel.resetEmailSent.observe(this, sent -> {
            if (sent != null && sent) {
                Snackbar snackbar = Snackbar.make(findViewById(R.id.login_root),
                        "Password reset email sent! Check your inbox.",
                        Snackbar.LENGTH_LONG);
                snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.pocket_success));
                snackbar.setTextColor(ContextCompat.getColor(this, R.color.white));
                snackbar.show();
                authViewModel.resetEmailSent.setValue(false);
            }
        });
    }

    private void showForgotPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        TextInputEditText forgotEmailInput = dialogView.findViewById(R.id.forgot_email_input);

        // Pre-fill with any email already entered
        String currentEmail = getText(emailInput);
        if (!currentEmail.isEmpty()) {
            forgotEmailInput.setText(currentEmail);
        }

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Send Reset Link", (dialog, which) -> {
                    String email = forgotEmailInput.getText() != null
                            ? forgotEmailInput.getText().toString().trim() : "";
                    authViewModel.sendPasswordResetEmail(email);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @NonNull
    private String getText(@NonNull TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }

    private void navigateToMain() {
        PocketMessagingService.refreshTokenForCurrentUser();
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
