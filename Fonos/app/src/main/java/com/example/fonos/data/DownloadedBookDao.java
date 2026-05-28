package com.example.fonos.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DownloadedBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(DownloadedBookEntity book);

    @Query("SELECT * FROM downloaded_books ORDER BY downloadedAt DESC")
    List<DownloadedBookEntity> getAll();

    @Query("SELECT * FROM downloaded_books WHERE userId = :userId ORDER BY downloadedAt DESC")
    List<DownloadedBookEntity> getAllForUser(String userId);

    @Query("SELECT * FROM downloaded_books WHERE downloadKey = :downloadKey LIMIT 1")
    DownloadedBookEntity getByKey(String downloadKey);

    @Delete
    void delete(DownloadedBookEntity book);

    @Query("DELETE FROM downloaded_books WHERE downloadKey = :downloadKey")
    void deleteByKey(String downloadKey);
}
