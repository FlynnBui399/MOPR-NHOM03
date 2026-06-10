package com.example.pocket;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.pocket.ui.PocketButton;
import com.example.pocket.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class OtpActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;
    private TextInputEditText phoneInput;
    private TextInputEditText otpInput;
    private PocketButton sendOtpButton;
    private PocketButton verifyOtpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        phoneInput = findViewById(R.id.phone_input);
        otpInput = findViewById(R.id.otp_input);
        sendOtpButton = findViewById(R.id.send_otp_button);
        verifyOtpButton = findViewById(R.id.verify_otp_button);

        sendOtpButton.setOnClickListener(view -> sendOtp());
        verifyOtpButton.setOnClickListener(view -> verifyOtp());
        phoneInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendOtp();
                return true;
            }
            return false;
        });
        otpInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                verifyOtp();
                return true;
            }
            return false;
        });

        authViewModel.otpSent.observe(this, sent -> {
            sendOtpButton.setLoading(false);
            if (Boolean.TRUE.equals(sent)) {
                showMessage(getString(R.string.otp_sent), false);
                otpInput.requestFocus();
            }
        });
        authViewModel.currentUser.observe(this, user -> {
            verifyOtpButton.setLoading(false);
            if (user != null) {
                navigateToMain();
            }
        });
        authViewModel.errorMessage.observe(this, message -> {
            sendOtpButton.setLoading(false);
            verifyOtpButton.setLoading(false);
            if (message != null && !message.trim().isEmpty()) {
                showMessage(message, true);
            }
        });
    }

    private void sendOtp() {
        String phone = phoneInput.getText() == null ? "" : phoneInput.getText().toString();
        sendOtpButton.setLoading(true);
        authViewModel.sendOtp(phone, this);
    }

    private void verifyOtp() {
        String code = otpInput.getText() == null ? "" : otpInput.getText().toString();
        verifyOtpButton.setLoading(true);
        authViewModel.verifyOtp(code);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showMessage(@NonNull String message, boolean error) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.otp_root), message, Snackbar.LENGTH_LONG);
        int backgroundColor = error ? R.color.pocket_danger : R.color.pocket_primary;
        int textColor = error ? R.color.pocket_on_danger : R.color.pocket_on_primary;
        snackbar.setBackgroundTint(ContextCompat.getColor(this, backgroundColor));
        snackbar.setTextColor(ContextCompat.getColor(this, textColor));
        snackbar.show();
    }
}
