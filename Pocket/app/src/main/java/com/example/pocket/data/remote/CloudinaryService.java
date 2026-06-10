package com.example.pocket.data.remote;

import androidx.annotation.NonNull;

import com.example.pocket.utils.Constants;
import com.example.pocket.utils.ImageUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.gson.annotations.SerializedName;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class CloudinaryService {
    private static final String BASE_URL = "https://api.cloudinary.com/";
    private static final String DATA_URI_PREFIX = "data:image/jpeg;base64,";

    private final CloudinaryApi api;

    public CloudinaryService() {
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
                .create(CloudinaryApi.class);
    }

    @NonNull
    public Task<UploadResult> uploadUnsigned(@NonNull byte[] jpegBytes) {
        return uploadUnsigned(ImageUtils.toBase64(jpegBytes));
    }

    @NonNull
    public Task<UploadResult> uploadUnsigned(@NonNull String base64Image) {
        TaskCompletionSource<UploadResult> source = new TaskCompletionSource<>();
        api.uploadImage(
                Constants.CLOUDINARY_CLOUD_NAME,
                Constants.CLOUDINARY_UPLOAD_PRESET,
                DATA_URI_PREFIX + base64Image
        ).enqueue(new Callback<CloudinaryUploadResponse>() {
            @Override
            public void onResponse(@NonNull Call<CloudinaryUploadResponse> call,
                                   @NonNull Response<CloudinaryUploadResponse> response) {
                CloudinaryUploadResponse body = response.body();
                if (!response.isSuccessful() || body == null || body.secureUrl == null) {
                    source.setException(new IllegalStateException("Cloudinary upload failed: "
                            + response.code()));
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
    public String getThumbnailUrl(@NonNull String imageUrl) {
        int uploadIndex = imageUrl.indexOf("/upload/");
        if (uploadIndex < 0) {
            return imageUrl;
        }
        int insertIndex = uploadIndex + "/upload/".length();
        return imageUrl.substring(0, insertIndex)
                + "c_fill,w_480,h_640,q_auto,f_auto/"
                + imageUrl.substring(insertIndex);
    }

    private interface CloudinaryApi {
        @FormUrlEncoded
        @POST("v1_1/{cloudName}/image/upload")
        Call<CloudinaryUploadResponse> uploadImage(
                @Path("cloudName") String cloudName,
                @Field("upload_preset") String uploadPreset,
                @Field("file") String file
        );
    }

    private static class CloudinaryUploadResponse {
        @SerializedName("public_id")
        String publicId;

        @SerializedName("secure_url")
        String secureUrl;
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

        public String getPublicId() {
            return publicId;
        }

        public String getSecureUrl() {
            return secureUrl;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }
    }
}
