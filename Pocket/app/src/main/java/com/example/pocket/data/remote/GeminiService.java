package com.example.pocket.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import com.example.pocket.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
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
    private static final String TAG_AI = "PocketAI";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";
    private static final String LEGACY_CAPTION_PROMPT =
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
    private static final String CAPTION_PROMPT =
            "Analyze this photo carefully and generate exactly 4 short Vietnamese captions "
            + "for a close-friends photo sharing app like Locket. The first 3 captions must each "
            + "clearly describe or relate to different visible details: objects, animals, people, "
            + "food, furniture, space, lighting, activity, mood, or scene context. Do not make "
            + "these first 3 captions generic. The last caption may be generic, casual, and social. "
            + "Each caption should normally contain 4 to 8 words, must contain at least 3 words "
            + "and 8 visible characters, and must sound natural for Vietnamese Gen Z without "
            + "being abrupt or cringe. Start every caption with an uppercase letter. Do not use "
            + "hashtags, numbering, markdown, explanations, labels, or introductory text. Return "
            + "ONLY valid JSON in this exact format: "
            + "{\"imageSpecific\":[\"caption 1\",\"caption 2\",\"caption 3\"],"
            + "\"generic\":\"caption 4\"}";

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
        Log.d(TAG_AI, "Gemini request preparation: apiKeyPresent=" + !apiKey.isEmpty()
                + ", apiKeyLength=" + apiKey.length()
                + ", model=" + Constants.GEMINI_CAPTION_MODEL
                + ", endpoint=/v1beta/models/" + Constants.GEMINI_CAPTION_MODEL
                + ":generateContent");
        if (apiKey.isEmpty()) {
            Log.e(TAG_AI, "Gemini request aborted: API key is missing; fallbackReason=missing_api_key");
            android.util.Log.e("GeminiService", "Gemini API key is missing");
            source.setException(new IllegalStateException("Gemini API key is missing"));
            return source.getTask();
        }

        String imageBase64 = android.util.Base64.encodeToString(
                optimizedJpegBytes, android.util.Base64.NO_WRAP);
        GeminiRequest request = new GeminiRequest(imageBase64);
        Log.d(TAG_AI, "Gemini request created: jpegBytes=" + optimizedJpegBytes.length
                + ", imageBase64Length=" + imageBase64.length()
                + ", promptLength=" + CAPTION_PROMPT.length()
                + ", mimeType=image/jpeg");
        api.generateContent(Constants.GEMINI_CAPTION_MODEL, apiKey,
                        request)
                .enqueue(new Callback<GeminiResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<GeminiResponse> call,
                                           @NonNull Response<GeminiResponse> response) {
                        Log.d(TAG_AI, "Gemini HTTP response: statusCode=" + response.code()
                                + ", successful=" + response.isSuccessful()
                                + ", message=" + response.message());
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
                            String errorBody = readErrorBodySummary(response, apiKey);
                            Log.e(TAG_AI, "Gemini HTTP failure: " + errorDetail
                                    + ", errorBody=" + errorBody
                                    + ", fallbackReason=http_" + code);
                            android.util.Log.e("GeminiService", "Gemini API error: " + errorDetail);
                            source.setException(new IllegalStateException(
                                    errorDetail + "; errorBody=" + errorBody));
                            return;
                        }

                        String responseText = body.firstText();
                        Log.d(TAG_AI, "Gemini response body parsed: candidates="
                                + body.candidateCount() + ", firstTextLength="
                                + (responseText == null ? 0 : responseText.length())
                                + ", finishReason=" + body.firstFinishReason()
                                + ", safetyRatings=" + body.firstSafetySummary()
                                + ", promptBlockReason=" + body.promptBlockReason());
                        List<String> captions = parseCaptions(responseText);
                        Log.d(TAG_AI, "Gemini caption parsing complete: captionCount="
                                + captions.size());
                        if (captions.isEmpty()) {
                            String errorDetail = "Gemini returned no usable captions";
                            Log.e(TAG_AI, errorDetail
                                    + "; fallbackReason=empty_or_unparseable_response"
                                    + ", responseTextLength="
                                    + (responseText == null ? 0 : responseText.length())
                                    + ", finishReason=" + body.firstFinishReason()
                                    + ", safetyRatings=" + body.firstSafetySummary()
                                    + ", promptBlockReason=" + body.promptBlockReason());
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
                        Log.e(TAG_AI, "Gemini request exception: type="
                                + throwable.getClass().getName() + ", message=" + errorMsg
                                + ", rootCause=" + rootCauseSummary(throwable)
                                + ", fallbackReason=network_or_transport_failure", throwable);
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
                .replaceAll("(?i)```json", "")
                .replace("```", "")
                .trim();
        List<String> imageSpecific = new ArrayList<>();
        List<String> legacyCaptions = new ArrayList<>();
        String generic = null;
        boolean structuredResponse = false;
        JsonElement root = parseJsonPayload(text);
        if (root != null) {
            if (root.isJsonObject()) {
                JsonElement specificElement = root.getAsJsonObject().get("imageSpecific");
                JsonElement genericElement = root.getAsJsonObject().get("generic");
                if (specificElement != null && specificElement.isJsonArray()) {
                    structuredResponse = true;
                    JsonArray array = specificElement.getAsJsonArray();
                    for (JsonElement item : array) {
                        if (item.isJsonPrimitive()) {
                            addImageSpecificCaption(imageSpecific, item.getAsString());
                        }
                    }
                }
                if (genericElement != null && genericElement.isJsonPrimitive()) {
                    structuredResponse = true;
                    generic = cleanCaption(genericElement.getAsString());
                }

                if (!structuredResponse) {
                    JsonElement captionsElement = root.getAsJsonObject().get("captions");
                    if (captionsElement != null && captionsElement.isJsonArray()) {
                        for (JsonElement item : captionsElement.getAsJsonArray()) {
                            if (item.isJsonPrimitive()) {
                                addRawCaption(legacyCaptions, item.getAsString());
                            }
                        }
                    }
                }
            } else if (root.isJsonArray()) {
                JsonArray array = root.getAsJsonArray();
                for (JsonElement item : array) {
                    if (item.isJsonPrimitive()) {
                        addRawCaption(legacyCaptions, item.getAsString());
                    }
                }
            }
        }

        if (structuredResponse) {
            return completeCaptionSet(imageSpecific, generic);
        }

        if (legacyCaptions.isEmpty()) {
            String normalized = text
                    .replace('[', '\n')
                    .replace(']', '\n')
                    .replace("\",\"", "\n")
                    .replace("\", \"", "\n")
                    .replace('|', '\n');
            for (String line : normalized.split("\\r?\\n")) {
                addRawCaption(legacyCaptions, line);
            }
        }

        // Preserve legacy formats while preventing them from becoming mostly generic.
        Set<String> filteredCaptions = new LinkedHashSet<>();
        int genericCount = 0;
        for (String caption : legacyCaptions) {
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
        List<String> legacy = new ArrayList<>(filteredCaptions);
        List<String> legacySpecific = new ArrayList<>();
        for (int index = 0; index < legacy.size() && index < 3; index++) {
            legacySpecific.add(legacy.get(index));
        }
        String legacyGeneric = legacy.size() > 3 ? legacy.get(3) : null;
        return completeCaptionSet(legacySpecific, legacyGeneric);
    }

    @NonNull
    private static List<String> completeCaptionSet(@NonNull List<String> specificCandidates,
                                                   @Nullable String genericCandidate) {
        List<String> result = new ArrayList<>();
        for (String caption : specificCandidates) {
            addImageSpecificCaption(result, caption);
            if (result.size() == 3) {
                break;
            }
        }
        for (String backup : imageSpecificBackups()) {
            if (result.size() == 3) {
                break;
            }
            addImageSpecificCaption(result, backup);
        }

        String cleanedGeneric = cleanCaption(genericCandidate);
        if (cleanedGeneric != null && !containsNormalized(result, cleanedGeneric)) {
            result.add(cleanedGeneric);
        } else {
            for (String backup : genericBackups()) {
                String cleanedBackup = cleanCaption(backup);
                if (cleanedBackup != null && !containsNormalized(result, cleanedBackup)) {
                    result.add(cleanedBackup);
                    break;
                }
            }
        }
        return new ArrayList<>(result.subList(0, Math.min(4, result.size())));
    }

    @NonNull
    public static List<String> fallbackCaptionSet() {
        return new ArrayList<>(completeCaptionSet(Collections.emptyList(), null));
    }

    @Nullable
    private static JsonElement parseJsonPayload(@NonNull String text) {
        try {
            return JsonParser.parseString(text);
        } catch (RuntimeException ignored) {
            // Try extracting JSON from a short preamble such as "Here is the JSON requested:".
        }

        int arrayStart = text.indexOf('[');
        int arrayEnd = text.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            try {
                return JsonParser.parseString(text.substring(arrayStart, arrayEnd + 1));
            } catch (RuntimeException ignored) {
                // Continue with legacy object extraction or tolerant line parsing.
            }
        }

        int objectStart = text.indexOf('{');
        int objectEnd = text.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            try {
                return JsonParser.parseString(text.substring(objectStart, objectEnd + 1));
            } catch (RuntimeException ignored) {
                // Tolerant line parsing handles the remaining response formats.
            }
        }
        return null;
    }

    @NonNull
    private static String readErrorBodySummary(@NonNull Response<?> response,
                                               @NonNull String apiKey) {
        if (response.errorBody() == null) {
            return "<empty>";
        }
        try {
            String raw = response.errorBody().string();
            String redacted = raw.replace(apiKey, "<redacted-api-key>")
                    .replaceAll("(?i)([?&]key=)[^&\\s\"']+", "$1<redacted>")
                    .replaceAll("(?i)(\"key\"\\s*:\\s*\")[^\"]+", "$1<redacted>");
            return redacted.length() > 1200 ? redacted.substring(0, 1200) + "..." : redacted;
        } catch (IOException error) {
            return "<unable to read error body: " + error.getMessage() + ">";
        }
    }

    @NonNull
    private static String rootCauseSummary(@NonNull Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getName() + ": " + root.getMessage();
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
        String cleanedCaption = finalizeCaption(caption, 3);
        if (cleanedCaption != null && !containsNormalized(list, cleanedCaption)) {
            list.add(cleanedCaption);
        }
    }

    private static void addImageSpecificCaption(@NonNull List<String> list,
                                                @Nullable String value) {
        String cleanedCaption = finalizeCaption(cleanCaption(value), 4);
        if (cleanedCaption != null && !containsNormalized(list, cleanedCaption)) {
            list.add(cleanedCaption);
        }
    }

    @Nullable
    private static String cleanCaption(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String caption = value.trim()
                .replaceFirst("^[-*]\\s*", "")
                .replaceFirst("^\\d+\\s*[.)-]\\s*", "")
                .replaceFirst("^[\"']", "")
                .replaceFirst("[\"',]$", "")
                .trim();
        return finalizeCaption(caption, 3);
    }

    @Nullable
    private static String finalizeCaption(@Nullable String caption, int minimumWords) {
        if (caption == null) {
            return null;
        }
        String trimmed = caption.trim();
        if (trimmed.length() < 8
                || trimmed.split("\\s+").length < minimumWords
                || isParserNoise(trimmed)) {
            return null;
        }
        return capitalizeFirstLetter(trimmed);
    }

    @NonNull
    private static String capitalizeFirstLetter(@NonNull String caption) {
        for (int offset = 0; offset < caption.length();) {
            int codePoint = caption.codePointAt(offset);
            if (Character.isLetter(codePoint)) {
                int upper = Character.toUpperCase(codePoint);
                return caption.substring(0, offset)
                        + new String(Character.toChars(upper))
                        + caption.substring(offset + Character.charCount(codePoint));
            }
            offset += Character.charCount(codePoint);
        }
        return caption;
    }

    private static boolean containsNormalized(@NonNull List<String> captions,
                                              @NonNull String candidate) {
        String candidateKey = normalizedCaptionKey(candidate);
        for (String caption : captions) {
            if (normalizedCaptionKey(caption).equals(candidateKey)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String normalizedCaptionKey(@NonNull String caption) {
        return caption.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[\\p{Punct}\\p{S}\\s]+", " ")
                .trim();
    }

    @NonNull
    private static List<String> imageSpecificBackups() {
        return java.util.Arrays.asList(
                "G\u00f3c n\u00e0y nh\u00ecn xinh th\u1eadt \u0111\u00f3",
                "Khung c\u1ea3nh n\u00e0y l\u00e0m m\u00ecnh mu\u1ed1n l\u01b0u l\u1ea1i",
                "Nh\u00ecn t\u1ea5m n\u00e0y th\u1ea5y r\u1ea5t c\u00f3 mood",
                "Chi ti\u1ebft nh\u1ecf m\u00e0 d\u1ec5 th\u01b0\u01a1ng gh\u00ea",
                "B\u1ee9c n\u00e0y nh\u00ecn y\u00ean b\u00ecnh qu\u00e1"
        );
    }

    @NonNull
    private static List<String> genericBackups() {
        return java.util.Arrays.asList(
                "M\u1ed9t ch\u00fat h\u00f4m nay",
                "V\u1eeba ch\u1ee5p n\u00e8 \ud83d\udcf8",
                "G\u1eedi b\u1ea1n kho\u1ea3nh kh\u1eafc n\u00e0y",
                "L\u01b0u l\u1ea1i m\u1ed9t ng\u00e0y b\u00ecnh th\u01b0\u1eddng"
        );
    }

    private static boolean isParserNoise(@NonNull String caption) {
        String lower = caption.toLowerCase(java.util.Locale.ROOT).trim();
        if (lower.equals("json") || lower.equals("json:")
                || lower.equals("captions") || lower.equals("captions:")) {
            return true;
        }
        if (lower.startsWith("here is the json") || lower.startsWith("here's the json")
                || lower.startsWith("here is your json") || lower.startsWith("requested json")
                || lower.startsWith("caption suggestions:") || lower.startsWith("response:")) {
            return true;
        }
        if (lower.startsWith("{") || lower.startsWith("}")
                || lower.startsWith("[") || lower.startsWith("]")
                || lower.startsWith("\"visualdetails\"")
                || lower.startsWith("\"captions\"")) {
            return true;
        }
        return caption.length() > 120 || caption.split("\\s+").length > 18;
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
        final double temperature = 0.5;

        @SerializedName("maxOutputTokens")
        final int maxOutputTokens = 512;

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
        final List<String> required = java.util.Arrays.asList("imageSpecific", "generic");
    }

    private static class Properties {
        @SerializedName("imageSpecific")
        final ArraySchema imageSpecific = new ArraySchema();

        @SerializedName("generic")
        final SchemaItem generic = new SchemaItem();
    }

    private static class ArraySchema {
        @SerializedName("type")
        final String type = "ARRAY";

        @SerializedName("items")
        final SchemaItem items = new SchemaItem();

        @SerializedName("minItems")
        final int minItems = 3;

        @SerializedName("maxItems")
        final int maxItems = 3;
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

        @SerializedName("promptFeedback")
        PromptFeedback promptFeedback;

        int candidateCount() {
            return candidates == null ? 0 : candidates.size();
        }

        @NonNull
        String firstFinishReason() {
            if (candidates == null || candidates.isEmpty()
                    || candidates.get(0).finishReason == null) {
                return "<none>";
            }
            return candidates.get(0).finishReason;
        }

        @NonNull
        String firstSafetySummary() {
            if (candidates == null || candidates.isEmpty()) {
                return "<none>";
            }
            return safetySummary(candidates.get(0).safetyRatings);
        }

        @NonNull
        String promptBlockReason() {
            return promptFeedback == null || promptFeedback.blockReason == null
                    ? "<none>" : promptFeedback.blockReason;
        }

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

        @SerializedName("finishReason")
        String finishReason;

        @SerializedName("safetyRatings")
        List<SafetyRating> safetyRatings;
    }

    private static class PromptFeedback {
        @SerializedName("blockReason")
        String blockReason;
    }

    private static class SafetyRating {
        @SerializedName("category")
        String category;

        @SerializedName("probability")
        String probability;

        @SerializedName("blocked")
        boolean blocked;
    }

    @NonNull
    private static String safetySummary(@Nullable List<SafetyRating> ratings) {
        if (ratings == null || ratings.isEmpty()) {
            return "<none>";
        }
        StringBuilder summary = new StringBuilder();
        for (SafetyRating rating : ratings) {
            if (summary.length() > 0) {
                summary.append('|');
            }
            summary.append(rating.category == null ? "unknown" : rating.category)
                    .append(':')
                    .append(rating.probability == null ? "unknown" : rating.probability);
            if (rating.blocked) {
                summary.append(":blocked");
            }
        }
        return summary.toString();
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
