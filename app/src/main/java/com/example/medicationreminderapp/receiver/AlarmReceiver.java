package com.example.medicationreminderapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.medicationreminderapp.service.AlarmService;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String medicineName = intent.getStringExtra("MEDICINE_NAME");
        String medicineNote = intent.getStringExtra("MEDICINE_NOTE");
        String scheduledTime = intent.getStringExtra("SCHEDULED_TIME");
        int medicineId = intent.getIntExtra("MEDICINE_ID", -1);

        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra("MEDICINE_NAME", medicineName);
        serviceIntent.putExtra("MEDICINE_NOTE", medicineNote);
        serviceIntent.putExtra("SCHEDULED_TIME", scheduledTime);
        serviceIntent.putExtra("MEDICINE_ID", medicineId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}