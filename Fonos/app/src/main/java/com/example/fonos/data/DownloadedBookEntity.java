package com.example.fonos.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloaded_books")
public class DownloadedBookEntity {
    @PrimaryKey
    @NonNull
    public String downloadKey;

    public int bookId;
    public String title;
    public String author;
    public String description;
    public float rating;
    public String duration;
    public int chapterCount;
    public int coverDrawableRes;
    public String category;
    public String coverUrl;
    public String audioUrl;
    public String localFilePath;
    public long fileSizeBytes;
    public String status;
    public long downloadedAt;

    public DownloadedBookEntity(
            @NonNull String downloadKey,
            int bookId,
            String title,
            String author,
            String description,
            float rating,
            String duration,
            int chapterCount,
            int coverDrawableRes,
            String category,
            String coverUrl,
            String audioUrl,
            String localFilePath,
            long fileSizeBytes,
            String status,
            long downloadedAt
    ) {
        this.downloadKey = downloadKey;
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.description = description;
        this.rating = rating;
        this.duration = duration;
        this.chapterCount = chapterCount;
        this.coverDrawableRes = coverDrawableRes;
        this.category = category;
        this.coverUrl = coverUrl;
        this.audioUrl = audioUrl;
        this.localFilePath = localFilePath;
        this.fileSizeBytes = fileSizeBytes;
        this.status = status;
        this.downloadedAt = downloadedAt;
    }
}
