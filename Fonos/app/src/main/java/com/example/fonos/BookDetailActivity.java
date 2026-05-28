package com.example.fonos;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.fonos.auth.LoginActivity;
import com.example.fonos.model.Book;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BookDetailActivity extends AppCompatActivity {

    private TextView tvDetailTitle, tvDetailAuthor, tvDetailRating, tvDetailDuration;
    private TextView tvDetailChapters, tvDetailDescription, tvDetailCoverTitle;
    private ImageView imgDetailCover;
    private Toolbar toolbar;
    private Button btnAddLibrary, btnDownloadAudio;

    private int bookId;
    private String title, author, desc, duration, category, coverUrl, audioUrl;
    private float rating;
    private int chapters, coverRes;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isAddedToLibrary = false;
    private boolean isDownloadingAudio = false;
    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_book_detail);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        initViews();
        setupToolbar();
        loadBookData();
        setupListenNowButton();
        setupLibraryButton();
        setupDownloadButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDownloadButtonState();
    }

    private void initViews() {
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailAuthor = findViewById(R.id.tvDetailAuthor);
        tvDetailRating = findViewById(R.id.tvDetailRating);
        tvDetailDuration = findViewById(R.id.tvDetailDuration);
        tvDetailChapters = findViewById(R.id.tvDetailChapters);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailCoverTitle = findViewById(R.id.tvDetailCoverTitle);
        imgDetailCover = findViewById(R.id.imgDetailCover);
        toolbar = findViewById(R.id.toolbar);
        btnAddLibrary = findViewById(R.id.btnAddLibrary);
        btnDownloadAudio = findViewById(R.id.btnDownloadAudio);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadBookData() {
        if (getIntent() == null) return;

        bookId = getIntent().getIntExtra("book_id", 0);
        title = getIntent().getStringExtra("book_title");
        author = getIntent().getStringExtra("book_author");
        desc = getIntent().getStringExtra("book_desc");
        rating = getIntent().getFloatExtra("book_rating", 0f);
        duration = getIntent().getStringExtra("book_duration");
        chapters = getIntent().getIntExtra("book_chapters", 0);
        coverRes = getIntent().getIntExtra("book_cover", R.drawable.bg_book_cover_1);
        coverUrl = getIntent().getStringExtra("book_cover_url");
        audioUrl = getIntent().getStringExtra("book_audio_url");
        category = getIntent().getStringExtra("book_category");

        if (title == null) title = getString(R.string.app_name);
        if (author == null) author = "";
        if (desc == null) desc = "";
        if (duration == null) duration = "00:00";
        if (category == null || category.trim().isEmpty()) category = "General";

        tvDetailTitle.setText(title);
        tvDetailAuthor.setText(author);
        tvDetailDescription.setText(desc);
        tvDetailRating.setText(String.valueOf(rating));
        tvDetailDuration.setText(duration);
        tvDetailChapters.setText(getString(R.string.detail_chapter_count, chapters));
        tvDetailCoverTitle.setText(title);

        renderCover();
    }

    private void renderCover() {
        if (isValidUrl(coverUrl)) {
            Glide.with(this)
                    .load(coverUrl)
                    .placeholder(coverRes)
                    .error(coverRes)
                    .into(imgDetailCover);

            tvDetailCoverTitle.setVisibility(View.GONE);
        } else {
            imgDetailCover.setImageResource(coverRes);
            tvDetailCoverTitle.setVisibility(View.VISIBLE);
        }
    }

    private void setupListenNowButton() {
        Button btnListenNow = findViewById(R.id.btnListenNow);

        btnListenNow.setOnClickListener(v -> {
            if (!OfflineAudioManager.isRemoteAudioSource(audioUrl)) {
                Toast.makeText(this, "Sách này chưa có audioUrl hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            saveActiveBookToMiniPlayer();
            openAudioPlayer();
        });
    }

    private void openAudioPlayer() {
        Intent intent = new Intent(BookDetailActivity.this, AudioPlayerActivity.class);

        intent.putExtra("book_id", bookId);
        intent.putExtra("book_title", title);
        intent.putExtra("book_author", author);
        intent.putExtra("book_duration", duration);
        intent.putExtra("book_cover", coverRes);
        intent.putExtra("book_cover_url", coverUrl);
        intent.putExtra("audio_url", audioUrl);
        intent.putExtra("book_audio_url", audioUrl);
        intent.putExtra("book_desc", desc);
        intent.putExtra("book_rating", rating);
        intent.putExtra("book_chapters", chapters);
        intent.putExtra("book_category", category);

        startActivity(intent);
    }

    private void saveActiveBookToMiniPlayer() {
        SharedPreferences sharedPref = getSharedPreferences("FonosPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putInt("active_book_id", bookId);
        editor.putString("active_book_title", title);
        editor.putString("active_book_author", author);
        editor.putString("active_book_duration", duration);
        editor.putInt("active_book_cover", coverRes);
        editor.putString("active_book_cover_url", coverUrl);
        editor.putString("active_book_audio_url", audioUrl);
        editor.putString("active_book_category", category);
        editor.putString("active_book_desc", desc);
        editor.putFloat("active_book_rating", rating);
        editor.putInt("active_book_chapters", chapters);

        editor.apply();
    }

    private void setupDownloadButton() {
        updateDownloadButtonState();

        btnDownloadAudio.setOnClickListener(v -> {
            if (isDownloadingAudio) return;

            if (!OfflineAudioManager.isRemoteAudioSource(audioUrl)) {
                Toast.makeText(this, getString(R.string.detail_download_unavailable), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!OfflineAudioManager.canUseOfflineDownloads()) {
                Toast.makeText(this, "Please log in to download audio", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(BookDetailActivity.this, LoginActivity.class));
                return;
            }

            if (OfflineAudioManager.hasDownloadedAudio(this, bookId, audioUrl)) {
                updateDownloadButtonState();
                saveActiveBookToMiniPlayer();
                openAudioPlayer();
                return;
            }

            downloadAudioForOffline();
        });
    }

    private void downloadAudioForOffline() {
        if (!OfflineAudioManager.canUseOfflineDownloads()) {
            Toast.makeText(this, "Please log in to download audio", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(BookDetailActivity.this, LoginActivity.class));
            return;
        }

        if (!OfflineAudioManager.ensureAudioDirectory(this)) {
            Toast.makeText(this, getString(R.string.detail_audio_download_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        Context appContext = getApplicationContext();
        File targetFile = OfflineAudioManager.getDownloadTargetFile(appContext, bookId, audioUrl);
        if (targetFile == null) {
            Toast.makeText(this, "Please log in to download audio", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(BookDetailActivity.this, LoginActivity.class));
            return;
        }

        setAudioDownloadInProgress(0);

        if (OfflineAudioManager.isDirectStreamingUrl(audioUrl)) {
            downloadAudioFromHttpUrl(appContext, targetFile);
            return;
        }

        StorageReference audioRef = OfflineAudioManager.createStorageReference(audioUrl);
        if (audioRef == null) {
            isDownloadingAudio = false;
            updateDownloadButtonState();
            Toast.makeText(this, getString(R.string.detail_audio_download_unsupported), Toast.LENGTH_SHORT).show();
            return;
        }

        audioRef.getFile(targetFile)
                .addOnProgressListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) return;

                    long totalBytes = snapshot.getTotalByteCount();
                    if (totalBytes <= 0) return;

                    int progress = (int) ((snapshot.getBytesTransferred() * 100) / totalBytes);
                    setAudioDownloadInProgress(progress);
                })
                .addOnSuccessListener(taskSnapshot -> {
                    saveDownloadedBookMetadata(appContext, targetFile);
                    isDownloadingAudio = false;
                    if (isFinishing() || isDestroyed()) return;

                    updateDownloadButtonState();
                    Toast.makeText(this, getString(R.string.detail_audio_download_success), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (targetFile.exists()) {
                        targetFile.delete();
                    }

                    OfflineAudioManager.clearDownloadedAudio(appContext, bookId, audioUrl);
                    isDownloadingAudio = false;
                    if (isFinishing() || isDestroyed()) return;

                    updateDownloadButtonState();
                    Toast.makeText(this, getString(R.string.detail_audio_download_failed), Toast.LENGTH_SHORT).show();
                });
    }

    private void downloadAudioFromHttpUrl(Context appContext, File targetFile) {
        String source = audioUrl.trim();

        downloadExecutor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(source);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(15_000);
                connection.setReadTimeout(30_000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    throw new IOException("Unexpected HTTP response " + responseCode);
                }

                long totalBytes = connection.getContentLengthLong();
                long downloadedBytes = 0;
                int lastProgress = -1;

                try (InputStream input = new BufferedInputStream(connection.getInputStream());
                     FileOutputStream output = new FileOutputStream(targetFile, false)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = input.read(buffer)) != -1) {
                        if (Thread.currentThread().isInterrupted()) {
                            throw new IOException("Download interrupted");
                        }

                        output.write(buffer, 0, bytesRead);
                        downloadedBytes += bytesRead;

                        if (totalBytes > 0) {
                            int progress = (int) ((downloadedBytes * 100) / totalBytes);
                            if (progress != lastProgress) {
                                lastProgress = progress;
                                runOnUiThread(() -> {
                                    if (!isFinishing() && !isDestroyed()) {
                                        setAudioDownloadInProgress(progress);
                                    }
                                });
                            }
                        }
                    }
                }

                if (targetFile.length() <= 0) {
                    throw new IOException("Downloaded file is empty");
                }

                saveDownloadedBookMetadata(appContext, targetFile);
                runOnUiThread(() -> {
                    isDownloadingAudio = false;
                    if (isFinishing() || isDestroyed()) return;

                    updateDownloadButtonState();
                    Toast.makeText(this, getString(R.string.detail_audio_download_success), Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                if (targetFile.exists()) {
                    targetFile.delete();
                }

                OfflineAudioManager.clearDownloadedAudio(appContext, bookId, audioUrl);
                runOnUiThread(() -> {
                    isDownloadingAudio = false;
                    if (isFinishing() || isDestroyed()) return;

                    updateDownloadButtonState();
                    Toast.makeText(this, getString(R.string.detail_audio_download_failed), Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void updateDownloadButtonState() {
        if (btnDownloadAudio == null || isDownloadingAudio) return;

        if (!OfflineAudioManager.isRemoteAudioSource(audioUrl)) {
            btnDownloadAudio.setEnabled(false);
            btnDownloadAudio.setText(getString(R.string.detail_download_unavailable));
            return;
        }

        if (!OfflineAudioManager.canUseOfflineDownloads()) {
            btnDownloadAudio.setEnabled(true);
            btnDownloadAudio.setText(getString(R.string.detail_download_audio));
            return;
        }

        boolean hasOfflineAudio = OfflineAudioManager.hasDownloadedAudio(this, bookId, audioUrl);
        if (hasOfflineAudio) {
            String actualDuration = OfflineAudioManager.getDownloadedAudioDuration(this, bookId, audioUrl);
            if (!actualDuration.isEmpty()) {
                duration = actualDuration;
                tvDetailDuration.setText(actualDuration);
            }

            File localFile = OfflineAudioManager.getDownloadedAudioFile(this, bookId, audioUrl);
            if (localFile != null) {
                saveDownloadedBookMetadata(getApplicationContext(), localFile);
            }
        }

        btnDownloadAudio.setEnabled(true);
        btnDownloadAudio.setText(hasOfflineAudio
                ? getString(R.string.detail_downloaded_audio)
                : getString(R.string.detail_download_audio));
    }

    private void setAudioDownloadInProgress(int progress) {
        isDownloadingAudio = true;
        btnDownloadAudio.setEnabled(false);
        btnDownloadAudio.setText(getString(R.string.detail_downloading_audio, progress));
    }

    private void saveDownloadedBookMetadata(Context appContext, File targetFile) {
        OfflineAudioManager.saveDownloadedBook(
                appContext,
                bookId,
                title,
                author,
                desc,
                rating,
                duration,
                chapters,
                coverRes,
                category,
                coverUrl,
                audioUrl,
                targetFile
        );
    }

    private void setupLibraryButton() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            db.collection("users")
                    .document(user.getUid())
                    .collection("library")
                    .document(String.valueOf(bookId))
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            isAddedToLibrary = true;
                            btnAddLibrary.setText("Xóa khỏi thư viện");
                        } else {
                            isAddedToLibrary = false;
                            btnAddLibrary.setText(getString(R.string.detail_add_library));
                        }
                    });
        } else {
            isAddedToLibrary = false;
            btnAddLibrary.setText(getString(R.string.detail_add_library));
        }

        btnAddLibrary.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser == null) {
                Toast.makeText(this, "Please log in to save books to your library", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(BookDetailActivity.this, LoginActivity.class);
                startActivity(intent);
                return;
            }

            btnAddLibrary.setEnabled(false);

            if (isAddedToLibrary) {
                db.collection("users")
                        .document(currentUser.getUid())
                        .collection("library")
                        .document(String.valueOf(bookId))
                        .delete()
                        .addOnCompleteListener(task -> {
                            btnAddLibrary.setEnabled(true);

                            if (task.isSuccessful()) {
                                isAddedToLibrary = false;
                                btnAddLibrary.setText(getString(R.string.detail_add_library));
                                Toast.makeText(BookDetailActivity.this, "Removed from library", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(BookDetailActivity.this, "Failed to remove book", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Book book = new Book(bookId, title, author, desc, rating, duration, chapters, coverRes, category);
                book.setCoverUrl(coverUrl);
                book.setAudioUrl(audioUrl);

                db.collection("users")
                        .document(currentUser.getUid())
                        .collection("library")
                        .document(String.valueOf(bookId))
                        .set(book)
                        .addOnCompleteListener(task -> {
                            btnAddLibrary.setEnabled(true);

                            if (task.isSuccessful()) {
                                isAddedToLibrary = true;
                                btnAddLibrary.setText("Xóa khỏi thư viện");
                                Toast.makeText(BookDetailActivity.this, "Added to library", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(BookDetailActivity.this, "Failed to add book", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private boolean isValidUrl(String value) {
        return value != null &&
                (value.trim().startsWith("http://") || value.trim().startsWith("https://"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloadExecutor.shutdownNow();
    }
}
