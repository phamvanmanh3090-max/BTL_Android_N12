package com.example.medicationreminderapp.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages",
        foreignKeys = @ForeignKey(entity = ChatConversation.class, parentColumns = "id",
                childColumns = "conversationId", onDelete = ForeignKey.CASCADE),
        indices = @Index("conversationId"))
public class SavedChatMessage {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long conversationId;
    @NonNull
    public String text = "";
    public boolean user;
    public long createdAt;
}
