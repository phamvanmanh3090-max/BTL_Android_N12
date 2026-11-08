package com.example.medicationreminderapp.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.medicationreminderapp.model.DoseRecord;

import java.util.List;

@Dao
public interface DoseRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DoseRecord record);

    @Query("SELECT * FROM dose_records WHERE date = :date")
    List<DoseRecord> getRecordsByDate(String date);

    @Query("SELECT * FROM dose_records WHERE medicineId = :medicineId AND date = :date AND scheduledTime = :scheduledTime LIMIT 1")
    DoseRecord getRecord(int medicineId, String date, String scheduledTime);
}