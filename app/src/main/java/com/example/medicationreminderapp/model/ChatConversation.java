package com.example.medicationreminderapp.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_conversations")
public class ChatConversation {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String title = "Cuộc trò chuyện mới";
    public long updatedAt;
}
