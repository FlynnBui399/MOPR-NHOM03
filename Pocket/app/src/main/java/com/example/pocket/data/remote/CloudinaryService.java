package com.example.pocket.data.remote;

import androidx.annotation.NonNull;

import com.example.pocket.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.gson.annotations.SerializedName;

import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public class CloudinaryService {
    private static final String BASE_URL = "https://api.cloudinary.com/";
    private static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

    private final CloudinaryApi api;

    public CloudinaryService() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        api = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CloudinaryApi.class);
    }

    @NonNull
    public Task<UploadResult> uploadUnsigned(@NonNull byte[] jpegBytes) {
        TaskCompletionSource<UploadResult> source = new TaskCompletionSource<>();

        RequestBody fileBody = RequestBody.create(MEDIA_TYPE_JPEG, jpegBytes);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", "upload.jpg", fileBody);
        RequestBody presetBody = RequestBody.create(MediaType.parse("text/plain"), Constants.CLOUDINARY_UPLOAD_PRESET);

        api.uploadImage(Constants.CLOUDINARY_CLOUD_NAME, filePart, presetBody)
                .enqueue(new Callback<CloudinaryUploadResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<CloudinaryUploadResponse> call,
                                           @NonNull Response<CloudinaryUploadResponse> response) {
                        CloudinaryUploadResponse body = response.body();
                        if (!response.isSuccessful() || body == null || body.secureUrl == null) {
                            source.setException(new IllegalStateException(
                                    "Cloudinary upload failed: " + response.code()));
                            return;
                        }
                        source.setResult(new UploadResult(
                                body.publicId,
                                body.secureUrl,
                                getThumbnailUrl(body.secureUrl)
                        ));
                    }

                    @Override
                    public void onFailure(@NonNull Call<CloudinaryUploadResponse> call,
                                          @NonNull Throwable throwable) {
                        source.setException(new RuntimeException("Cloudinary upload failed", throwable));
                    }
                });

        return source.getTask();
    }

    @NonNull
    public Task<UploadResult> uploadUnsigned(@NonNull String base64Image) {
        byte[] bytes = android.util.Base64.decode(base64Image, android.util.Base64.NO_WRAP);
        return uploadUnsigned(bytes);
    }

    @NonNull
    public String getThumbnailUrl(@NonNull String imageUrl) {
        int uploadIndex = imageUrl.indexOf("/upload/");
        if (uploadIndex < 0) return imageUrl;
        int insertIndex = uploadIndex + "/upload/".length();
        return imageUrl.substring(0, insertIndex)
                + "c_fill,w_480,h_640,q_auto,f_auto/"
                + imageUrl.substring(insertIndex);
    }

    private interface CloudinaryApi {
        @Multipart
        @POST("v1_1/{cloudName}/image/upload")
        Call<CloudinaryUploadResponse> uploadImage(
                @Path("cloudName") String cloudName,
                @Part MultipartBody.Part file,
                @Part("upload_preset") RequestBody uploadPreset
        );
    }

    private static class CloudinaryUploadResponse {
        @SerializedName("public_id") String publicId;
        @SerializedName("secure_url") String secureUrl;
    }

    public static class UploadResult {
        private final String publicId;
        private final String secureUrl;
        private final String thumbnailUrl;

        public UploadResult(String publicId, String secureUrl, String thumbnailUrl) {
            this.publicId = publicId;
            this.secureUrl = secureUrl;
            this.thumbnailUrl = thumbnailUrl;
        }

        public String getPublicId() { return publicId; }
        public String getSecureUrl() { return secureUrl; }
        public String getThumbnailUrl() { return thumbnailUrl; }
    }
}
