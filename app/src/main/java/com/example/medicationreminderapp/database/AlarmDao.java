package com.example.medicationreminderapp.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import com.example.medicationreminderapp.model.Alarm;
import com.example.medicationreminderapp.model.AlarmWithMedicine;

import java.util.List;

@Dao
public interface AlarmDao {
    @Insert
    long insert(Alarm alarm);

    @Insert
    void insertAll(Alarm... alarms);

    @Update
    void update(Alarm alarm);

    @Query("SELECT * FROM alarms")
    List<Alarm> getAllAlarms();

    @Transaction
    @Query("SELECT * FROM alarms")
    List<AlarmWithMedicine> getAlarmsWithMedicine();

    @Query("UPDATE alarms SET enabled = :isEnabled WHERE id = :alarmId")
    void updateEnabled(long alarmId, boolean isEnabled);

    @Query("SELECT * FROM alarms WHERE medicine_id = :medicineId")
    List<Alarm> getAlarmsByMedicineId(int medicineId);

    @Query("SELECT * FROM alarms WHERE medicine_id = :medicineId AND scheduled_time = :scheduledTime LIMIT 1")
    Alarm getAlarmByMedicineAndTime(int medicineId, String scheduledTime);

    @Delete
    void delete(Alarm alarm);
}