package com.example.medicationreminderapp.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.activity.AlarmRingActivity;
import com.example.medicationreminderapp.database.AppDatabase;
import com.example.medicationreminderapp.model.Alarm;
import com.example.medicationreminderapp.model.DoseRecord;
import com.example.medicationreminderapp.model.Medicine;
import com.example.medicationreminderapp.receiver.AlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AlarmService extends Service {
    private static final String TAG = "AlarmService";
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private static final String CHANNEL_ID = "ALARM_CHANNEL_ID";

    public static final String ACTION_DISMISS = "ACTION_DISMISS";
    public static final String ACTION_TAKEN = "ACTION_TAKEN";
    public static final String BROADCAST_UPDATE_UI = "com.example.medicationreminderapp.UPDATE_UI";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            String medicineName = intent.getStringExtra("MEDICINE_NAME");
            String scheduledTime = intent.getStringExtra("SCHEDULED_TIME");
            int medicineId = intent.getIntExtra("MEDICINE_ID", -1);

            if (ACTION_DISMISS.equals(action)) {
                rescheduleNextAlarm(medicineId, scheduledTime);
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_TAKEN.equals(action)) {
                saveDoseAndNotify(medicineId, medicineName, scheduledTime);
                rescheduleNextAlarm(medicineId, scheduledTime);
                return START_NOT_STICKY;
            }
        }

        String medicineName = intent.getStringExtra("MEDICINE_NAME");
        String scheduledTime = intent.getStringExtra("SCHEDULED_TIME");
        int medicineId = intent.getIntExtra("MEDICINE_ID", -1);
        if (medicineName == null) medicineName = "Đến giờ uống thuốc!";

        String medicineNote = intent.getStringExtra("MEDICINE_NOTE");

        createNotificationChannel();

        Intent ringIntent = new Intent(this, AlarmRingActivity.class);
        ringIntent.putExtra("MEDICINE_NAME", medicineName);
        ringIntent.putExtra("MEDICINE_NOTE", medicineNote);
        ringIntent.putExtra("SCHEDULED_TIME", scheduledTime);
        ringIntent.putExtra("MEDICINE_ID", medicineId);
        ringIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                ringIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent takenIntent = new Intent(this, AlarmService.class);
        takenIntent.setAction(ACTION_TAKEN);
        takenIntent.putExtra("MEDICINE_NAME", medicineName);
        takenIntent.putExtra("SCHEDULED_TIME", scheduledTime);
        takenIntent.putExtra("MEDICINE_ID", medicineId);
        PendingIntent takenPendingIntent = PendingIntent.getService(this, 1, takenIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent dismissIntent = new Intent(this, AlarmService.class);
        dismissIntent.setAction(ACTION_DISMISS);
        dismissIntent.putExtra("MEDICINE_ID", medicineId);
        dismissIntent.putExtra("SCHEDULED_TIME", scheduledTime);
        PendingIntent dismissPendingIntent = PendingIntent.getService(this, 2, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Đến giờ uống thuốc!")
                .setContentText(medicineName + " (" + (scheduledTime != null ? scheduledTime : "") + ")")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .setFullScreenIntent(pendingIntent, true)
                .addAction(android.R.drawable.ic_menu_add, "ĐÃ UỐNG", takenPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "BỎ QUA", dismissPendingIntent)
                .build();

        startForeground(1, notification);
        startAlarm();

        return START_STICKY;
    }

    private void rescheduleNextAlarm(int medicineId, String scheduledTime) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                Medicine medicine = db.medicineDao().getMedicineById(medicineId);
                Alarm alarm = db.alarmDao().getAlarmByMedicineAndTime(medicineId, scheduledTime);

                if (medicine != null && alarm != null) {
                    Calendar nextAlarmCal = Calendar.getInstance();
                    nextAlarmCal.setTimeInMillis(alarm.getTime());

                    switch (medicine.getFrequency()) {
                        case "Hàng ngày": nextAlarmCal.add(Calendar.DAY_OF_YEAR, 1); break;
                        case "Mỗi 2 ngày": nextAlarmCal.add(Calendar.DAY_OF_YEAR, 2); break;
                        case "Mỗi 3 ngày": nextAlarmCal.add(Calendar.DAY_OF_YEAR, 3); break;
                        case "Mỗi tuần": nextAlarmCal.add(Calendar.DAY_OF_YEAR, 7); break;
                        case "Mỗi tháng": nextAlarmCal.add(Calendar.MONTH, 1); break;
                    }

                    if (nextAlarmCal.getTimeInMillis() <= medicine.getEndDate()) {
                        alarm.setTime(nextAlarmCal.getTimeInMillis());
                        db.alarmDao().update(alarm);

                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        Intent intent = new Intent(this, AlarmReceiver.class);
                        intent.putExtra("MEDICINE_NAME", medicine.getName());
                        intent.putExtra("MEDICINE_ID", medicineId);
                        intent.putExtra("MEDICINE_NOTE", medicine.getNotes());
                        intent.putExtra("SCHEDULED_TIME", scheduledTime);

                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                this, alarm.getId(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

                        if (alarmManager != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (alarmManager.canScheduleExactAlarms()) {
                                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmCal.getTimeInMillis(), pendingIntent);
                                } else {
                                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmCal.getTimeInMillis(), pendingIntent);
                                }
                            } else {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmCal.getTimeInMillis(), pendingIntent);
                            }
                        }
                    } else {
                        alarm.setEnabled(false);
                        db.alarmDao().update(alarm);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error rescheduling next alarm", e);
            }
        }).start();
    }

    private void saveDoseAndNotify(int medicineId, String medicineName, String scheduledTime) {
        new Thread(() -> {
            try {
                if (medicineId != -1 && scheduledTime != null) {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String dateStr = dateFormat.format(cal.getTime());
                    
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String currentTime = timeFormat.format(cal.getTime());

                    DoseRecord record = new DoseRecord(medicineId, dateStr, scheduledTime, true, currentTime);
                    db.doseRecordDao().insert(record);
                    Log.d(TAG, "Saved DoseRecord for " + medicineName + " at " + dateStr);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving dose record", e);
            }

            // Gửi broadcast SAU KHI đã lưu xong vào DB
            sendUpdateBroadcast();
            stopSelf();
        }).start();
    }

    private void sendUpdateBroadcast() {
        Intent intent = new Intent(BROADCAST_UPDATE_UI);
        intent.setPackage(getPackageName()); // Rất quan trọng để Receiver nhận được
        sendBroadcast(intent);
        Log.d(TAG, "Sent update broadcast to HomeActivity");
    }

    private void startAlarm() {
        if (mediaPlayer == null) {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mediaPlayer = MediaPlayer.create(this, alarmUri);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        }
        if (vibrator == null) {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                long[] pattern = {0, 1000, 1000};
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (vibrator != null) vibrator.cancel();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Alarm Channel", NotificationManager.IMPORTANCE_HIGH
            );
            serviceChannel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }
}
