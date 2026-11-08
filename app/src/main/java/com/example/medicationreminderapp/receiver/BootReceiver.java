package com.example.medicationreminderapp.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.medicationreminderapp.database.AppDatabase;
import com.example.medicationreminderapp.model.AlarmWithMedicine;

import java.util.Calendar;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {
            Log.d(TAG, "Điện thoại khởi động lại -> Đang đặt lại báo thức...");
            rescheduleAlarms(context);
        }
    }

    private void rescheduleAlarms(Context context) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(context);
                List<AlarmWithMedicine> alarms = db.alarmDao().getAlarmsWithMedicine();

                if (alarms == null || alarms.isEmpty()) return;

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                for (AlarmWithMedicine item : alarms) {
                    if (item.alarm.isEnabled()) {
                        long alarmTime = item.alarm.getTime();

                        // Nếu thời gian báo thức đã trôi qua (do tắt máy lâu)
                        if (alarmTime < System.currentTimeMillis()) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(alarmTime);

                            // Giữ giờ phút gốc, đẩy ngày lên cho đến khi lớn hơn hiện tại
                            // Lưu ý: Logic này đơn giản hóa, AlarmService sẽ lo việc lặp lại chuẩn xác hơn sau khi kêu
                            while (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                                switch (item.medicine.getFrequency()) {
                                    case "Hàng ngày":
                                        cal.add(Calendar.DAY_OF_YEAR, 1);
                                        break;
                                    case "Mỗi 2 ngày":
                                        cal.add(Calendar.DAY_OF_YEAR, 2);
                                        break;
                                    case "Mỗi 3 ngày":
                                        cal.add(Calendar.DAY_OF_YEAR, 3);
                                        break;
                                    case "Mỗi tuần":
                                        cal.add(Calendar.DAY_OF_YEAR, 7);
                                        break;
                                    case "Mỗi tháng":
                                        cal.add(Calendar.MONTH, 1);
                                        break;
                                    default:
                                        cal.add(Calendar.DAY_OF_YEAR, 1);
                                        break;
                                }
                            }
                            alarmTime = cal.getTimeInMillis();
                            
                            // Cập nhật lại thời gian mới vào database
                            item.alarm.setTime(alarmTime);
                            db.alarmDao().update(item.alarm);
                        }

                        // Chỉ đặt nếu chưa hết hạn thuốc
                        if (alarmTime <= item.medicine.getEndDate()) {
                            Intent i = new Intent(context, AlarmReceiver.class);
                            i.putExtra("MEDICINE_NAME", item.medicine.getName());
                            i.putExtra("MEDICINE_ID", item.medicine.getMedicineId());
                            i.putExtra("MEDICINE_NOTE", item.medicine.getNotes());
                            i.putExtra("SCHEDULED_TIME", item.alarm.getScheduledTime());

                            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    item.alarm.getId(),
                                    i,
                                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                            );

                            if (alarmManager != null) {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        if (alarmManager.canScheduleExactAlarms()) {
                                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                                        } else {
                                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                                        }
                                    } else {
                                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                                    }
                                    Log.d(TAG, "Đã khôi phục báo thức cho: " + item.medicine.getName() + " tại " + alarmTime);
                                } catch (SecurityException e) {
                                    Log.e(TAG, "Thiếu quyền SCHEDULE_EXACT_ALARM khi boot", e);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi đặt lại báo thức trong BootReceiver", e);
            }
        }).start();
    }
}
