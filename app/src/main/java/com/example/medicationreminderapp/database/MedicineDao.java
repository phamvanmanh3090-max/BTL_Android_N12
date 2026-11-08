package com.example.medicationreminderapp.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.medicationreminderapp.model.Medicine;

import java.util.List;

@Dao
public interface MedicineDao {
    @Insert
    long insert(Medicine medicine);

    @Update
    void update(Medicine medicine);

    @Delete
    void delete(Medicine medicine);

    @Query("SELECT * FROM medicines")
    List<Medicine> getAll();

    @Query("SELECT * FROM medicines WHERE medicineId = :id")
    Medicine getMedicineById(int id);
}