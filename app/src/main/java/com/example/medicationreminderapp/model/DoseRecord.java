package com.example.medicationreminderapp.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "dose_records", primaryKeys = {"medicineId", "date", "scheduledTime"})
public class DoseRecord {
    public int medicineId;

    @NonNull
    public String date; // Định dạng "yyyy-MM-dd"

    @NonNull
    public String scheduledTime; // Định dạng "hh:mm a"

    public boolean isTaken;
    public String timeTaken;

    public DoseRecord(int medicineId, @NonNull String date, @NonNull String scheduledTime, boolean isTaken, String timeTaken) {
        this.medicineId = medicineId;
        this.date = date;
        this.scheduledTime = scheduledTime;
        this.isTaken = isTaken;
        this.timeTaken = timeTaken;
    }
}