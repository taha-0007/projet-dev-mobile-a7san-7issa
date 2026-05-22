package com.example.a7san7issa.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String userId;
    @NonNull
    public String role;     // "user" ou "assistant"
    @NonNull
    public String content;
    public long timestamp;

    public ChatMessageEntity(@NonNull String userId, @NonNull String role,
                             @NonNull String content, long timestamp) {
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }
}