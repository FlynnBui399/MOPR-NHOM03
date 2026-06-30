package com.example.pocket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocket.ui.PocketButton;
import com.example.pocket.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class OtpActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;
    private TextInputEditText phoneInput;
    private TextInputEditText codeInput;
    private TextInputLayout codeInputLayout;
    private PocketButton sendButton;
    private PocketButton verifyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        ImageButton backButton = findViewById(R.id.otp_back);
        phoneInput = findViewById(R.id.otp_phone_input);
        codeInput = findViewById(R.id.otp_code_input);
        codeInputLayout = findViewById(R.id.otp_code_layout);
        sendButton = findViewById(R.id.otp_send_button);
        verifyButton = findViewById(R.id.otp_verify_button);

        backButton.setOnClickListener(v -> finish());

        sendButton.setOnClickListener(v -> {
            String phone = getText(phoneInput);
            sendButton.setLoading(true);
            authViewModel.sendOtp(phone, this);
        });

        verifyButton.setOnClickListener(v -> {
            String code = getText(codeInput);
            verifyButton.setLoading(true);
            authViewModel.verifyOtp(code);
        });

        authViewModel.otpSent.observe(this, sent -> {
            sendButton.setLoading(false);
            if (sent != null && sent) {
                codeInputLayout.setVisibility(View.VISIBLE);
                verifyButton.setVisibility(View.VISIBLE);
                Snackbar.make(findViewById(R.id.otp_root),
                        getString(R.string.otp_sent), Snackbar.LENGTH_SHORT).show();
            }
        });

        authViewModel.currentUser.observe(this, user -> {
            sendButton.setLoading(false);
            verifyButton.setLoading(false);
            if (user != null) {
                navigateToMain();
            }
        });

        authViewModel.errorMessage.observe(this, message -> {
            sendButton.setLoading(false);
            verifyButton.setLoading(false);
            if (message != null && !message.trim().isEmpty()) {
                Snackbar snackbar = Snackbar.make(findViewById(R.id.otp_root), message, Snackbar.LENGTH_LONG);
                snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.pocket_danger));
                snackbar.setTextColor(ContextCompat.getColor(this, R.color.pocket_on_danger));
                snackbar.show();
            }
        });
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
}
