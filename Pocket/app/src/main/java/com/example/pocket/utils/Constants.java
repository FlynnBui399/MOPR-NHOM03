package com.example.pocket.utils;
import com.example.pocket.BuildConfig;
public class Constants {
    public static final String CLOUDINARY_CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME;
    public static final String CLOUDINARY_UPLOAD_PRESET = BuildConfig.CLOUDINARY_UPLOAD_PRESET;
    public static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    public static final String GEMINI_CAPTION_MODEL = "gemini-3.1-flash-lite";
    public static final String GOOGLE_WEB_CLIENT_ID = BuildConfig.GOOGLE_WEB_CLIENT_ID;

    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_PHOTOS = "photos";
    public static final String COLLECTION_CHATS = "chats";
    public static final String COLLECTION_MESSAGES = "messages";
    public static final String COLLECTION_FCM_TRIGGERS = "fcmTriggers";
}
