package com.example.medicationreminderapp.model;

public class MedicineDose {
    public Medicine medicine; // Thuốc gì
    public String scheduledTime; // Giờ dự kiến (VD: "08:00")
    public String timeTaken; // Giờ thực tế đã uống (VD: "08:05"), null nếu chưa uống
    public boolean isTaken; // Đã uống chưa

    public MedicineDose(Medicine medicine, String scheduledTime) {
        this.medicine = medicine;
        this.scheduledTime = scheduledTime;
        this.isTaken = false;
        this.timeTaken = null;
    }
}