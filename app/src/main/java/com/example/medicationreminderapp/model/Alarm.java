package com.example.medicationreminderapp.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "alarms",
        foreignKeys = @ForeignKey(
                entity = Medicine.class,
                parentColumns = "medicineId",
                childColumns = "medicine_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("medicine_id")}
)
public class Alarm {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "medicine_id")
    private int medicineId;

    private long time;
    private boolean enabled;

    @ColumnInfo(name = "scheduled_time")
    private String scheduledTime; // Thêm trường này để dễ dàng tìm kiếm và hủy báo thức

    public Alarm() {
        this.enabled = true;
    }

    @Ignore
    public Alarm(int medicineId, long time, boolean enabled, String scheduledTime) {
        this.medicineId = medicineId;
        this.time = time;
        this.enabled = enabled;
        this.scheduledTime = scheduledTime;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }
}