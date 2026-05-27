package com.example.fonos.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {DownloadedBookEntity.class}, version = 1, exportSchema = false)
public abstract class FonosDatabase extends RoomDatabase {
    private static volatile FonosDatabase instance;

    public abstract DownloadedBookDao downloadedBookDao();

    public static FonosDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (FonosDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    FonosDatabase.class,
                                    "fonos.db"
                            )
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }

        return instance;
    }
}
