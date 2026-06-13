package com.example.pocket.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPhoto(PhotoEntity photo);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPhotos(List<PhotoEntity> photos);

    @Query("SELECT * FROM photos ORDER BY createdAt DESC")
    List<PhotoEntity> getAllPhotos();

    @Query("DELETE FROM photos WHERE createdAt < :timestamp")
    void deleteOlderThan(long timestamp);
}
