package com.example.medicationreminderapp.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.medicationreminderapp.database.Converters;

import java.util.List;

@Entity(tableName = "medicines")
public class Medicine {

    // QUAN TRỌNG: Tên biến phải là medicineId để khớp với code trong Alarm và AlarmWithMedicine
    @PrimaryKey(autoGenerate = true)
    private int medicineId;

    private String name;
    private String frequency;
    private long startDate;
    private long endDate;
    private String notes;

    // QUAN TRỌNG: Cần dòng này để lưu được List vào Database
    @TypeConverters(Converters.class)
    private List<String> times;

    // Constructor mặc định (Bắt buộc cho Room)
    public Medicine() {
    }

    // --- Getters và Setters ---

    public int getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(int medicineId) {
        this.medicineId = medicineId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<String> getTimes() {
        return times;
    }

    public void setTimes(List<String> times) {
        this.times = times;
    }
}