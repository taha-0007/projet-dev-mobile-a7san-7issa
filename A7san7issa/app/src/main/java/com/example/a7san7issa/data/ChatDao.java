package com.example.a7san7issa.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ChatDao {
    // Récupère les messages d'un utilisateur donné
    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp ASC")
    List<ChatMessageEntity> getMessagesByUser(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(ChatMessageEntity message);

    // Supprime tous les messages d'un utilisateur (optionnel)
    @Query("DELETE FROM chat_messages WHERE userId = :userId")
    void deleteAllByUser(String userId);
}