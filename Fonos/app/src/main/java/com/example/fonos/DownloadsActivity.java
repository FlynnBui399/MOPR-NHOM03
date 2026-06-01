package com.example.fonos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fonos.adapter.DownloadedBookAdapter;
import com.example.fonos.data.DownloadedBookEntity;
import com.example.fonos.model.Book;

import java.util.ArrayList;
import java.util.List;

public class DownloadsActivity extends AppCompatActivity
        implements DownloadedBookAdapter.OnDownloadedBookActionListener,
        AudioDownloadService.DownloadListener {

    private RecyclerView rvDownloads;
    private LinearLayout layoutEmptyDownloads;
    private DownloadedBookAdapter downloadedBookAdapter;
    private final List<DownloadedBookEntity> downloadedBooks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_downloads);

        rvDownloads = findViewById(R.id.rvDownloads);
        layoutEmptyDownloads = findViewById(R.id.layoutEmptyDownloads);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        downloadedBookAdapter = new DownloadedBookAdapter(downloadedBooks, this);
        rvDownloads.setLayoutManager(new LinearLayoutManager(this));
        rvDownloads.setAdapter(downloadedBookAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDownloadedBooks();
    }

    @Override
    protected void onStart() {
        super.onStart();
        AudioDownloadService.addListener(this);
    }

    @Override
    protected void onStop() {
        AudioDownloadService.removeListener(this);
        super.onStop();
    }

    private void loadDownloadedBooks() {
        downloadedBooks.clear();
        downloadedBooks.addAll(OfflineAudioManager.getDownloadedBookEntities(this));
        downloadedBookAdapter.notifyDataSetChanged();

        if (downloadedBooks.isEmpty()) {
            rvDownloads.setVisibility(View.GONE);
            layoutEmptyDownloads.setVisibility(View.VISIBLE);
        } else {
            rvDownloads.setVisibility(View.VISIBLE);
            layoutEmptyDownloads.setVisibility(View.GONE);
        }
    }

    @Override
    public void onOpenDownloadedBook(DownloadedBookEntity downloadedBook) {
        Book book = OfflineAudioManager.toBook(downloadedBook);
        Intent intent = new Intent(this, AudioPlayerActivity.class);
        intent.putExtra("book_id", book.getId());
        intent.putExtra("book_title", book.getTitle());
        intent.putExtra("book_author", book.getAuthor());
        intent.putExtra("book_duration", book.getDuration());
        intent.putExtra("book_cover", book.getCoverDrawableRes());
        intent.putExtra("book_cover_url", book.getCoverUrl());
        intent.putExtra("audio_url", book.getAudioUrl());
        intent.putExtra("book_audio_url", book.getAudioUrl());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_up, R.anim.stay);
    }

    @Override
    public void onDeleteDownloadedBook(DownloadedBookEntity downloadedBook) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.downloads_delete_title))
                .setMessage(getString(R.string.downloads_delete_message))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(getString(R.string.downloads_delete), (dialog, which) -> {
                    OfflineAudioManager.deleteDownloadedBook(this, downloadedBook);
                    Toast.makeText(this, getString(R.string.downloads_deleted), Toast.LENGTH_SHORT).show();
                    loadDownloadedBooks();
                })
                .show();
    }

    @Override
    public void onDownloadStateChanged(AudioDownloadService.DownloadState state) {
        if (state == null || state.isActive()) {
            return;
        }

        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                loadDownloadedBooks();
            }
        });
    }
}
