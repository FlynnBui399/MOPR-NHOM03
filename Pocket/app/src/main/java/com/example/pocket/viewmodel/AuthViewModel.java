package com.example.pocket.viewmodel;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.pocket.R;
import com.example.pocket.data.model.User;
import com.example.pocket.utils.Constants;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
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

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final GoogleSignInClient googleSignInClient;
    private String verificationId;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        GoogleSignInOptions.Builder signInOptionsBuilder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestProfile();
        String webClientId = googleWebClientId(application);
        if (!webClientId.isEmpty()) {
            signInOptionsBuilder.requestIdToken(webClientId);
        }
        GoogleSignInOptions signInOptions = signInOptionsBuilder.build();
        googleSignInClient = GoogleSignIn.getClient(application, signInOptions);

        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser != null) {
            saveUserToFirestore(firebaseUser);
        }
    }

    @NonNull
    public Intent signInWithGoogle() {
        return googleSignInClient.getSignInIntent();
    }

    public void signInWithGoogle(@Nullable Intent data) {
        if (googleWebClientId(getApplication()).isEmpty()) {
            errorMessage.setValue(getApplication().getString(R.string.auth_google_client_missing));
            return;
        }

        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(this::signInWithGoogleAccount)
                .addOnFailureListener(error -> {
                    String message = error instanceof ApiException
                            ? getApplication().getString(R.string.auth_google_failed)
                            : safeMessage(error, R.string.auth_google_failed);
                    errorMessage.setValue(message);
                });
    }

    public void sendOtp(@NonNull String phoneNumber, @NonNull Activity activity) {
        String trimmedPhone = phoneNumber.trim();
        if (trimmedPhone.isEmpty()) {
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

    public void saveUserToFirestore(@NonNull FirebaseUser firebaseUser) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(tokenTask -> {
                    String token = tokenTask.isSuccessful() ? tokenTask.getResult() : null;
                    upsertUser(firebaseUser, token);
                });
    }

    private void signInWithGoogleAccount(@NonNull GoogleSignInAccount account) {
        String idToken = account.getIdToken();
        if (idToken == null || idToken.trim().isEmpty()) {
            errorMessage.setValue(getApplication().getString(R.string.auth_google_failed));
            return;
        }

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        errorMessage.setValue(getApplication().getString(R.string.auth_google_failed));
                        return;
                    }
                    saveUserToFirestore(firebaseUser);
                })
                .addOnFailureListener(error ->
                        errorMessage.setValue(safeMessage(error, R.string.auth_google_failed)));
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
                    values.put("displayName", displayName(firebaseUser));
                    values.put("username", username(firebaseUser));
                    values.put("email", firebaseUser.getEmail());
                    values.put("phoneNumber", firebaseUser.getPhoneNumber());
                    values.put("avatarUrl", firebaseUser.getPhotoUrl() == null
                            ? null
                            : firebaseUser.getPhotoUrl().toString());
                    values.put("fcmToken", fcmToken);
                    values.put("updatedAt", now);
                    if (!snapshot.exists()) {
                        values.put("friendIds", new ArrayList<String>());
                        values.put("friends", new ArrayList<String>());
                        values.put("createdAt", now);
                    }

                    userRef.set(values, SetOptions.merge())
                            .addOnSuccessListener(unused -> userRef.get()
                                    .addOnSuccessListener(updatedSnapshot -> {
                                        User user = updatedSnapshot.toObject(User.class);
                                        if (user != null) {
                                            user.setId(updatedSnapshot.getId());
                                            currentUser.setValue(user);
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
    private String googleWebClientId(@NonNull Application application) {
        if (Constants.GOOGLE_WEB_CLIENT_ID != null && !Constants.GOOGLE_WEB_CLIENT_ID.trim().isEmpty()) {
            return Constants.GOOGLE_WEB_CLIENT_ID.trim();
        }

        int resourceId = application.getResources()
                .getIdentifier("default_web_client_id", "string", application.getPackageName());
        if (resourceId == 0) {
            return "";
        }
        String value = application.getString(resourceId);
        return value == null ? "" : value.trim();
    }

    @NonNull
    private String safeMessage(@NonNull Exception error, int fallbackStringRes) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? getApplication().getString(fallbackStringRes)
                : message;
    }
}
