package com.example.pocket.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class ImageUtils {
    public static final int DEFAULT_JPEG_QUALITY = 82;
    public static final int DEFAULT_MAX_DIMENSION = 1280;
    public static final int CAPTION_JPEG_QUALITY = 70;
    public static final int CAPTION_MAX_DIMENSION = 768;

    private ImageUtils() {
    }

    @NonNull
    public static byte[] compress(@NonNull Bitmap bitmap) {
        return compress(bitmap, DEFAULT_JPEG_QUALITY, DEFAULT_MAX_DIMENSION);
    }

    @NonNull
    public static byte[] compress(@NonNull Bitmap bitmap, int quality, int maxDimension) {
        Bitmap scaled = scaleDown(bitmap, maxDimension);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int boundedQuality = Math.max(1, Math.min(100, quality));
        scaled.compress(Bitmap.CompressFormat.JPEG, boundedQuality, outputStream);
        if (scaled != bitmap) {
            scaled.recycle();
        }
        return outputStream.toByteArray();
    }

    @NonNull
    public static String toBase64(@NonNull byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    @NonNull
    public static String toBase64(@NonNull Bitmap bitmap) {
        return toBase64(compress(bitmap));
    }

    @NonNull
    public static Bitmap uriToBitmap(@NonNull Context context, @NonNull Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source, (decoder, info, src) ->
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE));
        }
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }

    @NonNull
    public static Bitmap bytesToBitmap(@NonNull byte[] bytes) {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @NonNull
    public static byte[] optimizeForCaption(@NonNull byte[] jpegBytes) {
        Bitmap bitmap = bytesToBitmap(jpegBytes);
        if (bitmap == null) {
            throw new IllegalArgumentException("Unable to decode caption image");
        }
        try {
            return compress(bitmap, CAPTION_JPEG_QUALITY, CAPTION_MAX_DIMENSION);
        } finally {
            bitmap.recycle();
        }
    }

    @NonNull
    public static String sha256(@NonNull byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder value = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                value.append(String.format(java.util.Locale.US, "%02x", item & 0xff));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    @NonNull
    private static Bitmap scaleDown(@NonNull Bitmap bitmap, int maxDimension) {
        if (maxDimension <= 0) {
            return bitmap;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= maxDimension) {
            return bitmap;
        }

        float scale = (float) maxDimension / longestSide;
        int targetWidth = Math.max(1, Math.round(width * scale));
        int targetHeight = Math.max(1, Math.round(height * scale));
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
    }
}
