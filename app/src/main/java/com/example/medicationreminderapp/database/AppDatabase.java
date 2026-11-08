package com.example.medicationreminderapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.medicationreminderapp.model.Alarm;
import com.example.medicationreminderapp.model.DoseRecord;
import com.example.medicationreminderapp.model.Medicine;

@Database(entities = {Medicine.class, Alarm.class, DoseRecord.class}, version = 7)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public abstract MedicineDao medicineDao();
    public abstract AlarmDao alarmDao();
    public abstract DoseRecordDao doseRecordDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "medication_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
