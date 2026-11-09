package com.example.medicationreminderapp.database;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

// Application-scoped work continues to save a pending response after leaving the screen.
public final class ChatRepository {
    private static volatile ChatRepository instance;
    public final ChatDao dao;
    public final SharedPreferences preferences;
    public final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Set<Long> pending = new HashSet<>();
    private final MutableLiveData<Set<Long>> pendingState =
            new MutableLiveData<>(Collections.emptySet());

    private ChatRepository(Context context) {
        dao = ChatDatabase.getInstance(context).chatDao();
        preferences = context.getSharedPreferences("consultation_history", Context.MODE_PRIVATE);
    }

    public static ChatRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (ChatRepository.class) {
                if (instance == null) instance = new ChatRepository(context.getApplicationContext());
            }
        }
        return instance;
    }

    public LiveData<Set<Long>> getPending() { return pendingState; }

    public synchronized boolean beginRequest(long id) {
        if (!pending.add(id)) return false;
        pendingState.postValue(new HashSet<>(pending));
        return true;
    }

    public synchronized void finishRequest(long id) {
        pending.remove(id);
        pendingState.postValue(new HashSet<>(pending));
    }
}
