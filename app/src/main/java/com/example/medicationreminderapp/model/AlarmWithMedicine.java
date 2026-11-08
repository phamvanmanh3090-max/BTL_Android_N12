package com.example.medicationreminderapp.model;

import androidx.room.Embedded;
import androidx.room.Relation;

public class AlarmWithMedicine {
    @Embedded
    public Alarm alarm;

    @Relation(
            parentColumn = "medicine_id", // Cột FK trong bảng Alarm (SQL name)
            entityColumn = "medicineId"   // Cột PK trong bảng Medicine
    )
    public Medicine medicine;
}