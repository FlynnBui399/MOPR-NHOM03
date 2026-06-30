package com.example.pocket.viewmodel;

import android.app.Activity;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.pocket.R;
import com.example.pocket.data.model.User;
import com.example.pocket.utils.Constants;
import com.example.pocket.utils.NotificationPreferenceHelper;
import com.example.pocket.utils.SharedPrefManager;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AuthViewModel extends AndroidViewModel {
    public final MutableLiveData<User> currentUser = new MutableLiveData<>();
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public final MutableLiveData<Boolean> otpSent = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> resetEmailSent = new MutableLiveData<>(false);

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private String verificationId;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            saveUserToFirestore(firebaseUser);
        }
    }

    // ── Email / Password ────────────────────────────────────────────────

    public void signInWithEmail(@NonNull String email, @NonNull String password) {
        String trimmedEmail = email.trim();
        String trimmedPassword = password.trim();
        if (trimmedEmail.isEmpty()) {
            errorMessage.setValue("Please enter your email address.");
            return;
        }
        if (trimmedPassword.isEmpty()) {
            errorMessage.setValue("Please enter your password.");
            return;
        }

        auth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        errorMessage.setValue("Sign-in failed. Please try again.");
                        return;
                    }
                    saveUserToFirestore(firebaseUser);
                })
                .addOnFailureListener(error ->
                        errorMessage.setValue(safeMessage(error, "Sign-in failed. Please check your credentials.")));
    }

    public void createAccountWithEmail(@NonNull String email, @NonNull String password) {
        String trimmedEmail = email.trim();
        String trimmedPassword = password.trim();
        if (trimmedEmail.isEmpty()) {
            errorMessage.setValue("Please enter your email address.");
            return;
        }
        if (trimmedPassword.length() < 6) {
            errorMessage.setValue("Password must be at least 6 characters.");
            return;
        }

        auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        errorMessage.setValue("Account creation failed. Please try again.");
                        return;
                    }
                    saveUserToFirestore(firebaseUser);
                })
                .addOnFailureListener(error ->
                        errorMessage.setValue(safeMessage(error, "Account creation failed.")));
    }

    public void sendPasswordResetEmail(@NonNull String email) {
        String trimmedEmail = email.trim();
        if (trimmedEmail.isEmpty()) {
            errorMessage.setValue("Please enter your email address.");
            return;
        }

        auth.sendPasswordResetEmail(trimmedEmail)
                .addOnSuccessListener(unused -> resetEmailSent.setValue(true))
                .addOnFailureListener(error ->
                        errorMessage.setValue(safeMessage(error, "Failed to send reset email.")));
    }

    // ── Phone / OTP ─────────────────────────────────────────────────────

    public void sendOtp(@NonNull String phoneNumber, @NonNull Activity activity) {
        String trimmedPhone = normalizePhoneNumber(phoneNumber);
        if (trimmedPhone.isEmpty() || trimmedPhone.replaceAll("[^0-9]", "").length() < 9) {
            errorMessage.setValue(getApplication().getString(R.string.otp_invalid_phone));
            return;
        }

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(trimmedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        signInWithPhoneCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull com.google.firebase.FirebaseException exception) {
                        otpSent.setValue(false);
                        errorMessage.setValue(safeMessage(exception, R.string.auth_phone_failed));
                    }

                    @Override
                    public void onCodeSent(@NonNull String id,
                                           @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        verificationId = id;
                        otpSent.setValue(true);
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private String normalizePhoneNumber(String raw) {
        String phone = raw.trim().replaceAll("[\\s\\-()]", "");
        if (phone.startsWith("0") && phone.length() == 10) {
            return "+84" + phone.substring(1);
        }
        if (phone.startsWith("84") && phone.length() == 11) {
            return "+" + phone;
        }
        return phone;
    }

    public void verifyOtp(@NonNull String code) {
        String trimmedCode = code.trim();
        if (trimmedCode.length() != 6) {
            errorMessage.setValue(getApplication().getString(R.string.otp_invalid_code));
            return;
        }
        if (verificationId == null || verificationId.trim().isEmpty()) {
            errorMessage.setValue(getApplication().getString(R.string.auth_phone_failed));
            return;
        }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, trimmedCode);
        signInWithPhoneCredential(credential);
    }

    // ── Firestore ───────────────────────────────────────────────────────

    public void saveUserToFirestore(@NonNull FirebaseUser firebaseUser) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(tokenTask -> {
                    String token = tokenTask.isSuccessful() ? tokenTask.getResult() : null;
                    upsertUser(firebaseUser, token);
                });
    }

    private void signInWithPhoneCredential(@NonNull PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        errorMessage.setValue(getApplication().getString(R.string.auth_phone_failed));
                        return;
                    }
                    saveUserToFirestore(firebaseUser);
                })
                .addOnFailureListener(error ->
                        errorMessage.setValue(safeMessage(error, R.string.auth_phone_failed)));
    }

    private void upsertUser(@NonNull FirebaseUser firebaseUser, @Nullable String fcmToken) {
        DocumentReference userRef = firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.getUid());
        userRef.get()
                .addOnSuccessListener(snapshot -> {
                    Timestamp now = Timestamp.now();
                    Map<String, Object> values = new HashMap<>();

                    if (snapshot.exists()) {
                        String existingName = snapshot.getString("displayName");
                        String existingAvatar = snapshot.getString("avatarUrl");

                        if (existingName != null && !existingName.trim().isEmpty()) {
                            values.put("displayName", existingName);
                        } else {
                            values.put("displayName", displayName(firebaseUser));
                        }

                        if (existingAvatar != null && !existingAvatar.trim().isEmpty()) {
                            values.put("avatarUrl", existingAvatar);
                        } else {
                            values.put("avatarUrl", firebaseUser.getPhotoUrl() == null
                                    ? null
                                    : firebaseUser.getPhotoUrl().toString());
                        }
                    } else {
                        values.put("displayName", displayName(firebaseUser));
                        values.put("avatarUrl", firebaseUser.getPhotoUrl() == null
                                ? null
                                : firebaseUser.getPhotoUrl().toString());
                        values.put("friendIds", new ArrayList<String>());
                        values.put("friends", new ArrayList<String>());
                        values.put("createdAt", now);
                    }

                    values.put("username", username(firebaseUser));
                    values.put("email", firebaseUser.getEmail());
                    values.put("phoneNumber", firebaseUser.getPhoneNumber());
                    values.put("fcmToken", fcmToken);
                    if (!snapshot.exists()
                            || !snapshot.contains(NotificationPreferenceHelper.FIELD_NOTIFICATIONS_ENABLED)) {
                        values.put(NotificationPreferenceHelper.FIELD_NOTIFICATIONS_ENABLED,
                                SharedPrefManager.getInstance(getApplication()).areNotificationsEnabled());
                    }
                    values.put("updatedAt", now);

                    userRef.set(values, SetOptions.merge())
                            .addOnSuccessListener(unused -> userRef.get()
                                    .addOnSuccessListener(updatedSnapshot -> {
                                        User user = updatedSnapshot.toObject(User.class);
                                        if (user != null) {
                                            user.setId(updatedSnapshot.getId());
                                            currentUser.setValue(user);
                                            SharedPrefManager.getInstance(getApplication()).saveUser(user);
                                        }
                                    })
                                    .addOnFailureListener(error ->
                                            errorMessage.setValue(safeMessage(error, R.string.auth_user_save_failed))))
                            .addOnFailureListener(error ->
                                    errorMessage.setValue(safeMessage(error, R.string.auth_user_save_failed)));
                })
                .addOnFailureListener(error ->
                        errorMessage.setValue(safeMessage(error, R.string.auth_user_save_failed)));
    }

    @NonNull
    private String displayName(@NonNull FirebaseUser firebaseUser) {
        if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().trim().isEmpty()) {
            return firebaseUser.getDisplayName();
        }
        if (firebaseUser.getPhoneNumber() != null && !firebaseUser.getPhoneNumber().trim().isEmpty()) {
            return firebaseUser.getPhoneNumber();
        }
        if (firebaseUser.getEmail() != null && !firebaseUser.getEmail().trim().isEmpty()) {
            return firebaseUser.getEmail();
        }
        return getApplication().getString(R.string.camera_default_user);
    }

    @NonNull
    private String username(@NonNull FirebaseUser firebaseUser) {
        String email = firebaseUser.getEmail();
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        String phone = firebaseUser.getPhoneNumber();
        if (phone != null && !phone.trim().isEmpty()) {
            return phone.replace("+", "");
        }
        return firebaseUser.getUid();
    }

    @NonNull
    private String safeMessage(@NonNull Exception error, int fallbackStringRes) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? getApplication().getString(fallbackStringRes)
                : message;
    }

    @NonNull
    private String safeMessage(@NonNull Exception error, @NonNull String fallback) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? fallback
                : message;
    }
}
