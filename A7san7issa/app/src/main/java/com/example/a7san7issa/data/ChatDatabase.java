package com.example.a7san7issa.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ChatMessageEntity.class}, version = 2, exportSchema = false)   // ← version passée à 2
public abstract class ChatDatabase extends RoomDatabase {
    private static ChatDatabase instance;

    public abstract ChatDao chatDao();

    public static synchronized ChatDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            ChatDatabase.class, "chat_database")
                    .fallbackToDestructiveMigration()   // déjà présent
                    .build();
        }
        return instance;
    }
}