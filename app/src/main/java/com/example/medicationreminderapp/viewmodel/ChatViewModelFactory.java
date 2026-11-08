package com.example.medicationreminderapp.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ChatViewModelFactory implements ViewModelProvider.Factory {
    private final String apiKey;

    public ChatViewModelFactory(String apiKey) {
        this.apiKey = apiKey;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (ChatViewModel.class.isAssignableFrom(modelClass)) {
            //noinspection unchecked
            return (T) new ChatViewModel(apiKey);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}

