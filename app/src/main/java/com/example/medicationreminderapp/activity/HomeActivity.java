package com.example.medicationreminderapp.activity;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.adapter.DateAdapter;
import com.example.medicationreminderapp.adapter.MedicineAdapter;
import com.example.medicationreminderapp.database.AppDatabase;
import com.example.medicationreminderapp.model.Alarm;
import com.example.medicationreminderapp.model.Day;
import com.example.medicationreminderapp.model.DoseRecord;
import com.example.medicationreminderapp.model.Medicine;
import com.example.medicationreminderapp.model.MedicineDose;
import com.example.medicationreminderapp.receiver.AlarmReceiver;
import com.example.medicationreminderapp.service.AlarmService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private TextView tvMonth;
    private RecyclerView rvDate, rvMedicine;
    private FloatingActionButton fabAdd;
    private TextView navHome, navAlarm, navStore, navConsultation;
    private ImageButton btnPrevMonth, btnNextMonth;

    private TextView tvSummary, tvNextLabel, tvNextName, tvNextTime;
    private android.view.View layoutEmpty, cardNextDose;
    private android.widget.Button btnNextTaken;
    private MedicineDose nextDose;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);

    private DateAdapter dateAdapter;
    private MedicineAdapter medicineAdapter;

    private final List<Day> dayList = new ArrayList<>();
    private final List<Medicine> fullMedicineList = new ArrayList<>();
    private final List<MedicineDose> displayedDoseList = new ArrayList<>();

    private Calendar calendar;
    private ActivityResultLauncher<Intent> addMedicineLauncher;

    private final BroadcastReceiver updateUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && AlarmService.BROADCAST_UPDATE_UI.equals(intent.getAction())) {
                Log.d(TAG, "Nhận được broadcast cập nhật UI từ AlarmService.");
                loadAllMedicines(); 
            }
        }
    };

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Bạn cần cấp quyền thông báo để ứng dụng hoạt động!", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initView();
        initRecyclerView();
        initData();
        initLauncher();
        setListener();
        requestPermissionsIfNeeded();

        // Đăng ký receiver để lắng nghe sự kiện cập nhật
        IntentFilter filter = new IntentFilter(AlarmService.BROADCAST_UPDATE_UI);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateUIReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(updateUIReceiver, filter);
        }
    }

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Cần có quyền đặc biệt")
                        .setMessage("Để đảm bảo báo thức kêu đúng giờ, ứng dụng cần quyền \'Báo thức & nhắc nhở\'. Vui lòng cấp quyền trong cài đặt.")
                        .setPositiveButton("Đi đến cài đặt", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy đăng ký receiver để tránh rò rỉ bộ nhớ
        try {
            unregisterReceiver(updateUIReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        navHome.setSelected(true);
        navAlarm.setSelected(false);
        navStore.setSelected(false);
        navConsultation.setSelected(false);
        loadAllMedicines();
    }

    private void initView() {
        tvMonth = findViewById(R.id.tvMonth);
        rvDate = findViewById(R.id.rvDate);
        rvMedicine = findViewById(R.id.rvMedicine);
        fabAdd = findViewById(R.id.fabAdd);
        navHome = findViewById(R.id.navHome);
        navAlarm = findViewById(R.id.navAlarm);
        navStore = findViewById(R.id.navStore);
        navConsultation = findViewById(R.id.navConsultation);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        tvSummary = findViewById(R.id.tvSummary);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        cardNextDose = findViewById(R.id.cardNextDose);
        tvNextLabel = findViewById(R.id.tvNextLabel);
        tvNextName = findViewById(R.id.tvNextName);
        tvNextTime = findViewById(R.id.tvNextTime);
        btnNextTaken = findViewById(R.id.btnNextTaken);
    }

    private void initData() {
        calendar = Calendar.getInstance();
        updateHeader();
        loadWeekDays();
        loadAllMedicines();
    }

    private void initRecyclerView() {
        dateAdapter = new DateAdapter(dayList, 0, (day, position) -> {
            calendar.set(day.getYear(), day.getMonth(), day.getDay());
            updateHeader();
            filterMedicinesForDate(calendar);
        });

        rvDate.setLayoutManager(new GridLayoutManager(this, 7));
        rvDate.setAdapter(dateAdapter);

        medicineAdapter = new MedicineAdapter(displayedDoseList, this::handleUongClick);
        rvMedicine.setLayoutManager(new LinearLayoutManager(this));
        rvMedicine.setAdapter(medicineAdapter);
    }

    private void handleUongClick(MedicineDose dose, int position) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(HomeActivity.this);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

            if (dose.isTaken) {
                // Hoàn tác
                DoseRecord record = new DoseRecord(dose.medicine.getMedicineId(), dateStr, dose.scheduledTime, false, null);
                db.doseRecordDao().insert(record);

                runOnUiThread(() -> {
                    dose.isTaken = false;
                    dose.timeTaken = null;
                    medicineAdapter.notifyItemChanged(position);
                    updateDashboard();
                });
            } else {
                // Xác nhận uống
                Calendar now = Calendar.getInstance();
                String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.getTime());

                DoseRecord record = new DoseRecord(dose.medicine.getMedicineId(), dateStr, dose.scheduledTime, true, currentTime);
                db.doseRecordDao().insert(record);

                runOnUiThread(() -> {
                    dose.isTaken = true;
                    dose.timeTaken = currentTime;
                    medicineAdapter.notifyItemChanged(position);
                    updateDashboard();

                    stopService(new Intent(this, AlarmService.class));

                    huyAlarmHeThong(dose);
                });
            }
        }).start();
    }

    private void huyAlarmHeThong(MedicineDose dose) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(HomeActivity.this);
                Alarm alarm = db.alarmDao().getAlarmByMedicineAndTime(dose.medicine.getMedicineId(), dose.scheduledTime);
                if (alarm != null) {
                    AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    Intent intent = new Intent(HomeActivity.this, AlarmReceiver.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            HomeActivity.this,
                            alarm.getId(),
                            intent,
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                    );
                    if (alarmManager != null) {
                        alarmManager.cancel(pendingIntent);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling alarm", e);
            }
        }).start();
    }

    private void filterMedicinesForDate(Calendar selectedDate) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());
            List<DoseRecord> records = db.doseRecordDao().getRecordsByDate(dateStr);

            runOnUiThread(() -> {
                displayedDoseList.clear();
                for (Medicine medicine : fullMedicineList) {
                    if (isMedicineScheduledForDate(medicine, selectedDate)) {
                        List<String> times = medicine.getTimes();
                        if (times != null) {
                            for (String time : times) {
                                MedicineDose dose = new MedicineDose(medicine, time);
                                for (DoseRecord record : records) {
                                    if (record.medicineId == medicine.getMedicineId() && record.scheduledTime.equals(time)) {
                                        dose.isTaken = record.isTaken;
                                        dose.timeTaken = record.timeTaken;
                                        break;
                                    }
                                }
                                displayedDoseList.add(dose);
                            }
                        }
                    }
                }
                displayedDoseList.sort((o1, o2) -> Integer.compare(toMinutes(o1.scheduledTime), toMinutes(o2.scheduledTime)));

                if (medicineAdapter != null) {
                    medicineAdapter.notifyDataSetChanged();
                    updateDashboard();
                }

                for (Day day : dayList) {
                    Calendar dayCal = Calendar.getInstance();
                    dayCal.set(day.getYear(), day.getMonth(), day.getDay());
                    day.setSelected(isSameDay(dayCal, selectedDate));
                }
                dateAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void loadAllMedicines() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(HomeActivity.this);
            if (db != null) {
                List<Medicine> result = db.medicineDao().getAll();
                fullMedicineList.clear();
                fullMedicineList.addAll(result);
                runOnUiThread(() -> filterMedicinesForDate(calendar));
            }
        });
    }

    private boolean isMedicineScheduledForDate(Medicine medicine, Calendar date) {
        Calendar startDate = Calendar.getInstance();
        startDate.setTimeInMillis(medicine.getStartDate());
        Calendar endDate = Calendar.getInstance();
        endDate.setTimeInMillis(medicine.getEndDate());

        normalizeToStartOfDay(startDate);
        normalizeToStartOfDay(endDate);
        normalizeToStartOfDay(date);

        if (date.before(startDate) || date.after(endDate)) return false;

        long diff = date.getTimeInMillis() - startDate.getTimeInMillis();
        long daysBetween = TimeUnit.MILLISECONDS.toDays(diff);

        switch (medicine.getFrequency()) {
            case "Hàng ngày": return true;
            case "Mỗi 2 ngày": return daysBetween % 2 == 0;
            case "Mỗi 3 ngày": return daysBetween % 3 == 0;
            case "Mỗi tuần": return daysBetween % 7 == 0;
            case "Mỗi tháng": return startDate.get(Calendar.DAY_OF_MONTH) == date.get(Calendar.DAY_OF_MONTH);
            default: return false;
        }
    }

    private void normalizeToStartOfDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void initLauncher() {
        addMedicineLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    loadAllMedicines();
                    loadWeekDays();
                    updateHeader();
                });
    }

    private void setListener() {
        btnNextTaken.setOnClickListener(v -> {
            if (nextDose == null) return;
            int index = displayedDoseList.indexOf(nextDose);
            if (index >= 0) handleUongClick(nextDose, index);
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddMedicineActivity.class);
            addMedicineLauncher.launch(intent);
        });
        navAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ScheduleActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        navStore.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, StoreActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        navConsultation.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AIChatActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        btnPrevMonth.setOnClickListener(v -> {
            calendar.add(Calendar.WEEK_OF_YEAR, -1);
            loadWeekDays();
            updateHeader();
            filterMedicinesForDate(calendar);
        });
        btnNextMonth.setOnClickListener(v -> {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
            loadWeekDays();
            updateHeader();
            filterMedicinesForDate(calendar);
        });
    }

    private void updateHeader() {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));
        String monthText = monthFormat.format(calendar.getTime());
        if (!monthText.isEmpty()) {
            monthText = monthText.substring(0, 1).toUpperCase() + monthText.substring(1);
        }
        tvMonth.setText(monthText);
    }

    private int toMinutes(String time) {
        try {
            java.util.Date d = timeFormat.parse(time);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private void updateDashboard() {
        int total = displayedDoseList.size();
        int taken = 0;
        nextDose = null;

        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        boolean isToday = isSameDay(calendar, now);

        MedicineDose firstPending = null;
        for (MedicineDose d : displayedDoseList) {
            if (d.isTaken) {
                taken++;
                continue;
            }
            if (firstPending == null) firstPending = d;
            if (nextDose == null && isToday && toMinutes(d.scheduledTime) >= nowMinutes) nextDose = d;
        }
        if (nextDose == null) nextDose = firstPending;

        layoutEmpty.setVisibility(total == 0 ? android.view.View.VISIBLE : android.view.View.GONE);
        tvSummary.setVisibility(total == 0 ? android.view.View.GONE : android.view.View.VISIBLE);
        tvSummary.setText(taken + "/" + total + " đã uống");

        if (total == 0) {
            cardNextDose.setVisibility(android.view.View.GONE);
            return;
        }
        cardNextDose.setVisibility(android.view.View.VISIBLE);

        if (nextDose == null) {
            tvNextLabel.setText("HOÀN THÀNH");
            tvNextName.setText("Bạn đã uống đủ thuốc");
            tvNextTime.setText(total + " liều đã hoàn tất");
            btnNextTaken.setVisibility(android.view.View.GONE);
        } else {
            boolean overdue = isToday && toMinutes(nextDose.scheduledTime) < nowMinutes;
            tvNextLabel.setText(overdue ? "QUÁ GIỜ" : "LIỀU TIẾP THEO");
            tvNextName.setText(nextDose.medicine.getName());
            String note = nextDose.medicine.getNotes();
            tvNextTime.setText(nextDose.scheduledTime + (note != null && !note.isEmpty() ? " · " + note : ""));
            btnNextTaken.setVisibility(android.view.View.VISIBLE);
        }
    }

    private void loadWeekDays() {
        dayList.clear();
        Calendar temp = (Calendar) calendar.clone();
        temp.setFirstDayOfWeek(Calendar.MONDAY);
        int dayOfWeek = temp.get(Calendar.DAY_OF_WEEK);
        int delta = Calendar.MONDAY - dayOfWeek;
        if (delta > 0) delta -= 7;
        temp.add(Calendar.DAY_OF_MONTH, delta);

        SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat("EEE", new Locale("vi", "VN"));

        for (int i = 0; i < 7; i++) {
            boolean isSelected = isSameDay(temp, calendar);
            Day day = new Day(
                    temp.get(Calendar.DAY_OF_MONTH),
                    temp.get(Calendar.MONTH),
                    temp.get(Calendar.YEAR),
                    dayOfWeekFormat.format(temp.getTime()),
                    isSelected
            );
            dayList.add(day);
            temp.add(Calendar.DAY_OF_MONTH, 1);
        }
        if (dateAdapter != null) dateAdapter.notifyDataSetChanged();
    }
}
