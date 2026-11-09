package com.example.medicationreminderapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.medicationreminderapp.model.ChatConversation;
import com.example.medicationreminderapp.model.SavedChatMessage;

@Database(entities = {ChatConversation.class, SavedChatMessage.class}, version = 1,
        exportSchema = false)
public abstract class ChatDatabase extends RoomDatabase {
    public abstract ChatDao chatDao();
    private static volatile ChatDatabase instance;

    public static ChatDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (ChatDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            ChatDatabase.class, "consultation_history.db").build();
                }
            }
        }
        return instance;
    }
}
