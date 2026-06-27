package com.example.pocket.data.remote;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GeminiService {
    private static final String TAG_AI = "PocketAI";

    private final FirebaseFunctions functions;

    public GeminiService() {
        this(FirebaseFunctions.getInstance());
    }

    GeminiService(@NonNull FirebaseFunctions functions) {
        this.functions = functions;
    }

    @NonNull
    public Task<List<String>> generateCaptions(@NonNull byte[] optimizedJpegBytes) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG_AI, "generateCaption auth check: signedIn=" + (currentUser != null)
                + ", uid=" + (currentUser == null ? "<none>" : currentUser.getUid()));
        if (currentUser == null) {
            return Tasks.forException(new IllegalStateException("User not authenticated"));
        }

        String imageBase64 = Base64.encodeToString(optimizedJpegBytes, Base64.NO_WRAP);
        Map<String, Object> data = new HashMap<>();
        data.put("imageBase64", imageBase64);
        data.put("language", preferredLanguage());

        return currentUser.getIdToken(true)
                .continueWithTask(tokenTask -> {
                    if (!tokenTask.isSuccessful()) {
                        Exception exception = tokenTask.getException();
                        return Tasks.forException(exception == null
                                ? new IllegalStateException("Unable to refresh authentication token")
                                : exception);
                    }
                    return functions
                            .getHttpsCallable("generateCaption")
                            .call(data);
                })
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        throw exception == null
                                ? new IllegalStateException("Caption generation failed")
                                : exception;
                    }
                    Object result = task.getResult().getData();
                    return parseFunctionCaptions(result);
                });
    }

    @NonNull
    public static List<String> fallbackCaptionSet() {
        return new ArrayList<>(Arrays.asList(
                "V\u1eeba ch\u1ee5p n\u00e8 \ud83d\udcf8",
                "M\u1ed9t ch\u00fat h\u00f4m nay",
                "G\u1eedi b\u1ea1n kho\u1ea3nh kh\u1eafc n\u00e0y"
        ));
    }

    @NonNull
    private static String preferredLanguage() {
        return "vi".equalsIgnoreCase(Locale.getDefault().getLanguage()) ? "vi" : "en";
    }

    @NonNull
    private static List<String> parseFunctionCaptions(Object result) {
        List<String> captions = new ArrayList<>();
        if (!(result instanceof Map)) {
            return captions;
        }

        Object rawCaptions = ((Map<?, ?>) result).get("captions");
        if (!(rawCaptions instanceof List)) {
            return captions;
        }

        for (Object value : (List<?>) rawCaptions) {
            if (!(value instanceof String)) {
                continue;
            }
            String caption = ((String) value).trim();
            if (!caption.isEmpty() && !captions.contains(caption)) {
                captions.add(caption);
            }
            if (captions.size() == 3) {
                break;
            }
        }
        return captions;
    }
}
