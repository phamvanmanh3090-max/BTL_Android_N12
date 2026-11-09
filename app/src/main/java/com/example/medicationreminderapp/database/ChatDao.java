package com.example.medicationreminderapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.medicationreminderapp.model.ChatConversation;
import com.example.medicationreminderapp.model.SavedChatMessage;

import java.util.List;

@Dao
public abstract class ChatDao {
    @Query("SELECT * FROM chat_conversations ORDER BY updatedAt DESC, id DESC")
    public abstract LiveData<List<ChatConversation>> observeConversations();

    @Query("SELECT * FROM chat_messages WHERE conversationId = :id ORDER BY id ASC")
    public abstract LiveData<List<SavedChatMessage>> observeMessages(long id);

    @Query("SELECT * FROM chat_messages WHERE conversationId = :id ORDER BY id ASC")
    public abstract List<SavedChatMessage> getMessages(long id);

    @Query("SELECT * FROM chat_conversations WHERE id = :id")
    public abstract ChatConversation getConversation(long id);

    @Query("SELECT * FROM chat_conversations ORDER BY updatedAt DESC, id DESC LIMIT 1")
    public abstract ChatConversation getLatestConversation();

    @Insert
    public abstract long insertConversation(ChatConversation conversation);

    @Insert
    protected abstract void insertMessage(SavedChatMessage message);

    @Query("UPDATE chat_conversations SET title = :title, updatedAt = :now WHERE id = :id")
    protected abstract void updateConversation(long id, String title, long now);

    @Query("DELETE FROM chat_conversations WHERE id = :id")
    public abstract void deleteConversation(long id);

    @Query("DELETE FROM chat_conversations")
    public abstract void deleteAll();

    // The existence check and insert share a transaction: a late AI reply cannot
    // recreate a conversation that the user has deleted.
    @Transaction
    public boolean appendMessage(long id, String text, boolean user) {
        ChatConversation conversation = getConversation(id);
        if (conversation == null) return false;
        SavedChatMessage message = new SavedChatMessage();
        message.conversationId = id;
        message.text = text;
        message.user = user;
        message.createdAt = System.currentTimeMillis();
        boolean firstQuestion = user && getMessages(id).stream().noneMatch(m -> m.user);
        insertMessage(message);
        String title = firstQuestion ? text.replaceAll("\\s+", " ").trim() : conversation.title;
        if (title.length() > 60) title = title.substring(0, 60) + "…";
        updateConversation(id, title, message.createdAt);
        return true;
    }
}
