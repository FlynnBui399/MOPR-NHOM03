package com.example.fonos.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {DownloadedBookEntity.class}, version = 2, exportSchema = false)
public abstract class FonosDatabase extends RoomDatabase {
    private static volatile FonosDatabase instance;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE downloaded_books ADD COLUMN userId TEXT NOT NULL DEFAULT ''");
        }
    };

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
                            .addMigrations(MIGRATION_1_2)
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }

        return instance;
    }
}
