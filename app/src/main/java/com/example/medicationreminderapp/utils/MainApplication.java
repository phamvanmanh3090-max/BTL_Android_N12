package com.example.medicationreminderapp.utils;

import android.app.Application;
import com.example.medicationreminderapp.database.AppDatabase;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize database or other global components
        AppDatabase.getInstance(this);
    }
}
