package com.example.pocket.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocket.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class GeminiService {
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private static final String CAPTION_PROMPT =
            "Hay viet 3 caption tieng Viet ngan gon, than mat cho anh nay. "
                    + "Chi tra ve dung 3 caption, ngan cach bang ky tu |, khong danh so.";

    private final GeminiApi api;

    public GeminiService() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .writeTimeout(45, TimeUnit.SECONDS)
                .build();

        api = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GeminiApi.class);
    }

    @NonNull
    public Task<List<String>> generateCaptions(@Nullable String imageBase64) {
        TaskCompletionSource<List<String>> source = new TaskCompletionSource<>();
        api.generateContent(Constants.GEMINI_API_KEY, new GeminiRequest(imageBase64))
                .enqueue(new Callback<GeminiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GeminiResponse> call,
                                           @NonNull Response<GeminiResponse> response) {
                        GeminiResponse body = response.body();
                        if (!response.isSuccessful() || body == null) {
                            source.setException(new IllegalStateException("Gemini request failed: "
                                    + response.code()));
                            return;
                        }

                        List<String> captions = parseCaptions(body.firstText());
                        if (captions.isEmpty()) {
                            source.setException(new IllegalStateException("Gemini returned no captions"));
                            return;
                        }
                        source.setResult(captions);
                    }

                    @Override
                    public void onFailure(@NonNull Call<GeminiResponse> call,
                                          @NonNull Throwable throwable) {
                        source.setException(new RuntimeException("Gemini request failed", throwable));
                    }
                });
        return source.getTask();
    }

    @NonNull
    private List<String> parseCaptions(@Nullable String text) {
        if (text == null) {
            return Collections.emptyList();
        }

        String[] pieces = text.replace("\n", "|").split("\\|");
        List<String> captions = new ArrayList<>();
        for (String piece : pieces) {
            String caption = piece.replaceFirst("^\\s*\\d+[.)-]?\\s*", "").trim();
            if (!caption.isEmpty()) {
                captions.add(caption);
            }
            if (captions.size() == 3) {
                break;
            }
        }
        return captions;
    }

    private interface GeminiApi {
        @POST("v1beta/models/gemini-1.5-flash:generateContent")
        Call<GeminiResponse> generateContent(
                @Query("key") String apiKey,
                @Body GeminiRequest request
        );
    }

    private static class GeminiRequest {
        @SerializedName("contents")
        final List<Content> contents;

        GeminiRequest(@Nullable String imageBase64) {
            List<Part> parts = new ArrayList<>();
            parts.add(Part.text(CAPTION_PROMPT));
            if (imageBase64 != null && !imageBase64.trim().isEmpty()) {
                parts.add(Part.inlineImage(imageBase64));
            }
            contents = Collections.singletonList(new Content(parts));
        }
    }

    private static class Content {
        @SerializedName("parts")
        final List<Part> parts;

        Content(List<Part> parts) {
            this.parts = parts;
        }
    }

    private static class Part {
        @SerializedName("text")
        final String text;

        @SerializedName("inline_data")
        final InlineData inlineData;

        private Part(String text, InlineData inlineData) {
            this.text = text;
            this.inlineData = inlineData;
        }

        static Part text(String text) {
            return new Part(text, null);
        }

        static Part inlineImage(String imageBase64) {
            return new Part(null, new InlineData("image/jpeg", imageBase64));
        }
    }

    private static class InlineData {
        @SerializedName("mime_type")
        final String mimeType;

        @SerializedName("data")
        final String data;

        InlineData(String mimeType, String data) {
            this.mimeType = mimeType;
            this.data = data;
        }
    }

    private static class GeminiResponse {
        @SerializedName("candidates")
        List<Candidate> candidates;

        @Nullable
        String firstText() {
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            Candidate candidate = candidates.get(0);
            if (candidate.content == null || candidate.content.parts == null
                    || candidate.content.parts.isEmpty()) {
                return null;
            }
            return candidate.content.parts.get(0).text;
        }
    }

    private static class Candidate {
        @SerializedName("content")
        ContentResponse content;
    }

    private static class ContentResponse {
        @SerializedName("parts")
        List<TextPart> parts;
    }

    private static class TextPart {
        @SerializedName("text")
        String text;
    }
}
