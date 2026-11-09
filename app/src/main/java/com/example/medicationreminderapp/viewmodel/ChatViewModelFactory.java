package com.example.medicationreminderapp.viewmodel;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ChatViewModelFactory implements ViewModelProvider.Factory {
    private final String apiKey;
    private final Context context;

    public ChatViewModelFactory(Context context, String apiKey) {
        this.context = context.getApplicationContext();
        this.apiKey = apiKey;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (ChatViewModel.class.isAssignableFrom(modelClass)) {
            //noinspection unchecked
            return (T) new ChatViewModel(context, apiKey);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}

