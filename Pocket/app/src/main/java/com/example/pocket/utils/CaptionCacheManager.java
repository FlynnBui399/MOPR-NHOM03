package com.example.pocket.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CaptionCacheManager {
    private static final String TAG_AI = "PocketAI";
    public static final String CAPTION_PROMPT_VERSION = "v6_three_specific_one_generic_caps";
    private static final String PREFERENCES = "caption_cache";
    private static final String INDEX_KEY = "cached_image_hashes";
    private static final String ENTRY_PREFIX = "captions_";
    private static final int MAX_ENTRIES = 20;

    private final SharedPreferences preferences;

    public CaptionCacheManager(@NonNull Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    @NonNull
    public List<String> get(@NonNull String imageHash) {
        String cacheKey = CAPTION_PROMPT_VERSION + "_" + imageHash;
        Log.d(TAG_AI, "Caption cache lookup: keySource=promptVersion+optimizedJPEG_SHA256"
                + ", promptVersion=" + CAPTION_PROMPT_VERSION
                + ", hashPrefix=" + hashPrefix(imageHash));
        String value = preferences.getString(ENTRY_PREFIX + cacheKey, null);
        if (value == null) {
            Log.d(TAG_AI, "Caption cache MISS: hashPrefix=" + hashPrefix(imageHash));
            return new ArrayList<>();
        }
        try {
            JSONArray array = new JSONArray(value);
            List<String> captions = new ArrayList<>();
            for (int index = 0; index < array.length() && captions.size() < 4; index++) {
                String caption = array.optString(index, "").trim();
                if (!caption.isEmpty()) {
                    captions.add(caption);
                }
            }
            Log.d(TAG_AI, "Caption cache HIT: captionCount=" + captions.size()
                    + ", source=previous successful Gemini result");
            return captions;
        } catch (JSONException error) {
            Log.e(TAG_AI, "Caption cache entry invalid; removing it. type="
                    + error.getClass().getName() + ", message=" + error.getMessage(), error);
            preferences.edit().remove(ENTRY_PREFIX + cacheKey).apply();
            return new ArrayList<>();
        }
    }

    public void put(@NonNull String imageHash, @NonNull List<String> captions) {
        JSONArray array = new JSONArray();
        for (String caption : captions) {
            if (caption != null && !caption.trim().isEmpty() && array.length() < 4) {
                array.put(caption.trim());
            }
        }
        if (array.length() == 0) {
            Log.d(TAG_AI, "Caption cache write skipped: no usable captions");
            return;
        }

        String cacheKey = CAPTION_PROMPT_VERSION + "_" + imageHash;
        List<String> hashes = readIndex();
        hashes.remove(cacheKey);
        hashes.add(cacheKey);
        SharedPreferences.Editor editor = preferences.edit()
                .putString(ENTRY_PREFIX + cacheKey, array.toString());
        while (hashes.size() > MAX_ENTRIES) {
            String oldest = hashes.remove(0);
            editor.remove(ENTRY_PREFIX + oldest);
        }
        editor.putString(INDEX_KEY, joinHashes(hashes)).apply();
        Log.d(TAG_AI, "Caption cache SAVE: captionCount=" + array.length()
                + ", hashPrefix=" + hashPrefix(imageHash));
    }

    @NonNull
    private static String hashPrefix(@NonNull String imageHash) {
        return imageHash.substring(0, Math.min(12, imageHash.length()));
    }

    @NonNull
    private List<String> readIndex() {
        String raw = preferences.getString(INDEX_KEY, "");
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(raw.split(",")));
    }

    @NonNull
    private String joinHashes(@NonNull List<String> hashes) {
        StringBuilder result = new StringBuilder();
        for (String hash : hashes) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(hash);
        }
        return result.toString();
    }
}
