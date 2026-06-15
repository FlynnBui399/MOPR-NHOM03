package com.example.pocket.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.pocket.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class GeminiService {
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private static final String CAPTION_PROMPT =
            "Analyze this photo carefully. Identify the main visible scene, objects, animals/people if present, lighting, and mood.\n\n"
            + "Generate exactly 4 short Vietnamese captions for a close-friends photo sharing app like Locket.\n\n"
            + "Caption mix:\n"
            + "- captions[0] and captions[1]: general casual Gen Z captions, but still suitable for this photo.\n"
            + "- captions[2] and captions[3]: image-specific captions that mention or clearly imply visible details from the photo.\n\n"
            + "Rules:\n"
            + "- Vietnamese only.\n"
            + "- Each caption under 12 Vietnamese words.\n"
            + "- Natural Gen Z style, not formal, not poetic.\n"
            + "- Friendly, casual, playful if appropriate.\n"
            + "- No hashtags.\n"
            + "- No explanation.\n"
            + "- Avoid overly generic captions like 'một chút hôm nay', 'khoảnh khắc thật đẹp', 'lưu lại ngày này' unless combined with a concrete detail.\n"
            + "- If the photo has a cat, TV, room, food, drink, laptop, street, sky, or other clear object, at least two captions must refer to those details.\n\n"
            + "Return only valid JSON in this exact format:\n"
            + "{\n"
            + "  \"visualDetails\": [\"detail 1\", \"detail 2\", \"detail 3\"],\n"
            + "  \"captions\": [\n"
            + "    \"general caption 1\",\n"
            + "    \"general caption 2\",\n"
            + "    \"image-specific caption 1\",\n"
            + "    \"image-specific caption 2\"\n"
            + "  ]\n"
            + "}";

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
    public Task<List<String>> generateCaptions(@NonNull byte[] optimizedJpegBytes) {
        TaskCompletionSource<List<String>> source = new TaskCompletionSource<>();
        String apiKey = Constants.GEMINI_API_KEY == null ? "" : Constants.GEMINI_API_KEY.trim();
        if (apiKey.isEmpty()) {
            android.util.Log.e("GeminiService", "Gemini API key is missing");
            source.setException(new IllegalStateException("Gemini API key is missing"));
            return source.getTask();
        }

        String imageBase64 = android.util.Base64.encodeToString(
                optimizedJpegBytes, android.util.Base64.NO_WRAP);
        api.generateContent(Constants.GEMINI_CAPTION_MODEL, apiKey,
                        new GeminiRequest(imageBase64))
                .enqueue(new Callback<GeminiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GeminiResponse> call,
                                           @NonNull Response<GeminiResponse> response) {
                        GeminiResponse body = response.body();
                        if (!response.isSuccessful() || body == null) {
                            int code = response.code();
                            String msg = response.message();
                            String errorDetail = "HTTP " + code + " (" + msg + ")";
                            if (code == 400) {
                                errorDetail += " - Bad Request (check model name or request format)";
                            } else if (code == 403) {
                                errorDetail += " - Forbidden (check API key restrictions or validity)";
                            } else if (code == 429) {
                                errorDetail += " - Too Many Requests / Rate Limit Exceeded";
                            } else if (code == 404) {
                                errorDetail += " - Not Found (model name unavailable)";
                            }
                            android.util.Log.e("GeminiService", "Gemini API error: " + errorDetail);
                            source.setException(new IllegalStateException(errorDetail));
                            return;
                        }

                        List<String> captions = parseCaptions(body.firstText());
                        if (captions.isEmpty()) {
                            String errorDetail = "Gemini returned no usable captions. Raw response: " + body.firstText();
                            android.util.Log.e("GeminiService", errorDetail);
                            source.setException(new IllegalStateException("Gemini returned no usable captions"));
                            return;
                        }
                        source.setResult(captions);
                    }

                    @Override
                    public void onFailure(@NonNull Call<GeminiResponse> call,
                                          @NonNull Throwable throwable) {
                        String errorMsg = throwable.getMessage();
                        android.util.Log.e("GeminiService", "Gemini network/request failure: " + errorMsg, throwable);
                        source.setException(new RuntimeException("Gemini request failed: " + errorMsg, throwable));
                    }
                });
        return source.getTask();
    }

    @NonNull
    static List<String> parseCaptions(@Nullable String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String text = rawText.trim()
                .replaceFirst("(?s)^```(?:json)?\\s*", "")
                .replaceFirst("(?s)\\s*```$", "")
                .trim();
        List<String> rawCaptions = new ArrayList<>();
        try {
            JsonElement root = JsonParser.parseString(text);
            if (root.isJsonObject()) {
                JsonElement captionsElem = root.getAsJsonObject().get("captions");
                if (captionsElem != null && captionsElem.isJsonArray()) {
                    JsonArray array = captionsElem.getAsJsonArray();
                    for (JsonElement item : array) {
                        if (item.isJsonPrimitive()) {
                            addRawCaption(rawCaptions, item.getAsString());
                        }
                    }
                }
            } else if (root.isJsonArray()) {
                JsonArray array = root.getAsJsonArray();
                for (JsonElement item : array) {
                    if (item.isJsonPrimitive()) {
                        addRawCaption(rawCaptions, item.getAsString());
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Fall through to tolerant line parsing
        }

        if (rawCaptions.size() < 4) {
            String normalized = text
                    .replace('[', '\n')
                    .replace(']', '\n')
                    .replace("\",\"", "\n")
                    .replace("\", \"", "\n")
                    .replace('|', '\n');
            for (String line : normalized.split("\\r?\\n")) {
                addRawCaption(rawCaptions, line);
            }
        }

        // Apply filtering rule: keep up to 2 generic captions, and keep non-generic ones, up to 4 total
        Set<String> filteredCaptions = new LinkedHashSet<>();
        int genericCount = 0;
        for (String caption : rawCaptions) {
            if (isGenericCaption(caption)) {
                if (genericCount < 2) {
                    filteredCaptions.add(caption);
                    genericCount++;
                }
            } else {
                filteredCaptions.add(caption);
            }
            if (filteredCaptions.size() == 4) {
                break;
            }
        }

        return new ArrayList<>(filteredCaptions);
    }

    private static void addRawCaption(@NonNull List<String> list, @Nullable String value) {
        if (value == null) {
            return;
        }
        String caption = value.trim()
                .replaceFirst("^[-*•]\\s*", "")
                .replaceFirst("^\\d+\\s*[.)-]\\s*", "")
                .replaceFirst("^[\"']", "")
                .replaceFirst("[\"',]$", "")
                .trim();
        if (!caption.isEmpty() && !caption.equalsIgnoreCase("json") && !list.contains(caption)) {
            list.add(caption);
        }
    }

    public static boolean isGenericCaption(@Nullable String caption) {
        if (caption == null) {
            return true;
        }
        String normalized = caption.toLowerCase()
                .replaceAll("[\\p{Punct}\\s\\p{InEmoticons}\\p{So}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        String noAccents = removeAccents(normalized);

        List<String> genericCaptions = java.util.Arrays.asList(
            "mot khoa nhac that dep", // in case of typo "một khoảnh khắc thật đẹp" normalization
            "mot khoanh khac that dep",
            "vua chup ne",
            "gui ban khoanh khac nay",
            "nhin cung on ap do",
            "luu lai mot ngay binh thuong",
            "hom nay that vui",
            "mot chut hom nay"
        );

        for (String generic : genericCaptions) {
            if (noAccents.equals(generic) || noAccents.contains(generic)) {
                return true;
            }
        }
        return false;
    }

    private static String removeAccents(String src) {
        if (src == null) return "";
        String nfdNormalizedString = java.text.Normalizer.normalize(src, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    private interface GeminiApi {
        @POST("v1beta/models/{model}:generateContent")
        Call<GeminiResponse> generateContent(
                @Path("model") String model,
                @Query("key") String apiKey,
                @Body GeminiRequest request
        );
    }

    private static class GeminiRequest {
        @SerializedName("contents")
        final List<Content> contents;

        @SerializedName("generationConfig")
        final GenerationConfig generationConfig;

        GeminiRequest(@NonNull String imageBase64) {
            List<Part> parts = new ArrayList<>();
            parts.add(Part.text(CAPTION_PROMPT));
            parts.add(Part.inlineImage(imageBase64));
            contents = Collections.singletonList(new Content(parts));
            generationConfig = new GenerationConfig();
        }
    }

    private static class GenerationConfig {
        @SerializedName("temperature")
        final double temperature = 0.65;

        @SerializedName("maxOutputTokens")
        final int maxOutputTokens = 256;

        @SerializedName("responseMimeType")
        final String responseMimeType = "application/json";

        @SerializedName("responseSchema")
        final ResponseSchema responseSchema = new ResponseSchema();
    }

    private static class ResponseSchema {
        @SerializedName("type")
        final String type = "OBJECT";

        @SerializedName("properties")
        final Properties properties = new Properties();

        @SerializedName("required")
        final List<String> required = java.util.Arrays.asList("visualDetails", "captions");
    }

    private static class Properties {
        @SerializedName("visualDetails")
        final ArraySchema visualDetails = new ArraySchema();

        @SerializedName("captions")
        final ArraySchema captions = new ArraySchema();
    }

    private static class ArraySchema {
        @SerializedName("type")
        final String type = "ARRAY";

        @SerializedName("items")
        final SchemaItem items = new SchemaItem();
    }

    private static class SchemaItem {
        @SerializedName("type")
        final String type = "STRING";
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
            if (candidate.content == null || candidate.content.parts == null) {
                return null;
            }
            StringBuilder result = new StringBuilder();
            for (TextPart part : candidate.content.parts) {
                if (part.text != null) {
                    result.append(part.text);
                }
            }
            return result.length() == 0 ? null : result.toString();
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
