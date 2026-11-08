package com.example.medicationreminderapp.activity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;

import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.database.AppDatabase;
import com.example.medicationreminderapp.model.Alarm;
import com.example.medicationreminderapp.model.Medicine;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class AddMedicineActivity extends AppCompatActivity {

    private static final String TAG = "AddMedicineActivity";

    private TextInputEditText medicineName;
    private AutoCompleteTextView frequencySpinner;
    private TextInputEditText dateRangeText;
    private TextView totalDurationText;
    private ImageView calendarIcon;
    private ChipGroup timeContainer;
    private ImageView addTimeIcon;
    private TextInputEditText notes;
    private Button saveButton;
    private ImageView backArrow;

    private Long startDate;
    private Long endDate;
    private final List<String> times = new ArrayList<>();
    private int editMedicineId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        editMedicineId = getIntent().getIntExtra("EDIT_MEDICINE_ID", -1);

        initViews();
        setupToolbar();
        setupListeners();

        if (editMedicineId != -1) {
            loadMedicineDataForEdit();
            TextView toolbarTitle = findViewById(R.id.tv_toolbar_title);
            if (toolbarTitle != null) {
                toolbarTitle.setText("Sửa đơn thuốc");
            }
            saveButton.setText("Cập nhật");
        }
    }

    private void initViews() {
        medicineName = findViewById(R.id.medicine_name);
        frequencySpinner = findViewById(R.id.frequency_spinner);
        dateRangeText = findViewById(R.id.date_range_text);
        totalDurationText = findViewById(R.id.tv_total_duration);
        calendarIcon = findViewById(R.id.calendar_icon);
        timeContainer = findViewById(R.id.ll_time);
        addTimeIcon = findViewById(R.id.add_time_icon);
        notes = findViewById(R.id.notes);
        saveButton = findViewById(R.id.save_button);
        backArrow = findViewById(R.id.iv_arrow_back);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.frequency_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(adapter);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupListeners() {
        backArrow.setOnClickListener(v -> finish());
        addTimeIcon.setOnClickListener(v -> showTimePicker());
        calendarIcon.setOnClickListener(v -> showCalendarPicker());
        dateRangeText.setOnClickListener(v -> showCalendarPicker());
        saveButton.setOnClickListener(v -> saveOrUpdateMedicine());
    }

    private void loadMedicineDataForEdit() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            Medicine medicine = db.medicineDao().getMedicineById(editMedicineId);
            runOnUiThread(() -> {
                if (medicine != null) {
                    medicineName.setText(medicine.getName());
                    frequencySpinner.setText(medicine.getFrequency(), false);
                    notes.setText(medicine.getNotes());
                    updateDateRange(medicine.getStartDate(), medicine.getEndDate());

                    times.clear();
                    times.addAll(medicine.getTimes());
                    timeContainer.removeAllViews();
                    for (String t : times) {
                        addTimeChip(t);
                    }
                }
            });
        }).start();
    }

    private void saveOrUpdateMedicine() {
        String name = medicineName.getText() != null ? medicineName.getText().toString().trim() : "";
        String frequency = frequencySpinner.getText() != null ? frequencySpinner.getText().toString().trim() : "";
        String note = notes.getText() != null ? notes.getText().toString().trim() : "";

        if (name.isEmpty() || frequency.isEmpty() || startDate == null || endDate == null || times.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(AddMedicineActivity.this);

                if (editMedicineId != -1) {
                    List<Alarm> oldAlarms = db.alarmDao().getAlarmsByMedicineId(editMedicineId);
                    AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                    for (Alarm alarm : oldAlarms) {
                        Intent intent = new Intent(this, com.example.medicationreminderapp.receiver.AlarmReceiver.class);
                        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarm.getId(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
                        if (pendingIntent != null) {
                            alarmManager.cancel(pendingIntent);
                            pendingIntent.cancel();
                        }
                        db.alarmDao().delete(alarm);
                    }
                }

                Medicine medicine = (editMedicineId != -1) ? db.medicineDao().getMedicineById(editMedicineId) : new Medicine();
                medicine.setName(name);
                medicine.setFrequency(frequency);
                medicine.setStartDate(startDate);
                medicine.setEndDate(endDate);
                medicine.setTimes(times);
                medicine.setNotes(note);

                long medicineId;
                if (editMedicineId != -1) {
                    db.medicineDao().update(medicine);
                    medicineId = editMedicineId;
                } else {
                    medicineId = db.medicineDao().insert(medicine);
                }

                setupAlarmsForMedicine((int) medicineId, name, note);

                runOnUiThread(() -> {
                    Toast.makeText(AddMedicineActivity.this, "Đã lưu thành công", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving medicine", e);
            }
        }).start();
    }

    private void setupAlarmsForMedicine(int medicineId, String name, String note) {
        AppDatabase db = AppDatabase.getInstance(this);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);

        for (String timeStr : times) {
            try {
                Date timeDate = timeFormat.parse(timeStr);
                if (timeDate != null) {
                    Calendar timeCal = Calendar.getInstance();
                    timeCal.setTime(timeDate);

                    Calendar alarmCal = Calendar.getInstance();
                    // Bắt đầu từ ngày StartDate đã chọn
                    alarmCal.setTimeInMillis(startDate);
                    alarmCal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
                    alarmCal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
                    alarmCal.set(Calendar.SECOND, 0);
                    alarmCal.set(Calendar.MILLISECOND, 0);

                    // Nếu thời điểm này trong quá khứ so với hiện tại, tìm ngày tiếp theo phù hợp
                    while (alarmCal.getTimeInMillis() <= System.currentTimeMillis()) {
                        alarmCal.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    // Chỉ đặt nếu còn nằm trong khoảng thời gian kết thúc
                    if (alarmCal.getTimeInMillis() <= endDate) {
                        Alarm alarm = new Alarm();
                        alarm.setMedicineId(medicineId);
                        alarm.setTime(alarmCal.getTimeInMillis());
                        alarm.setEnabled(true);
                        alarm.setScheduledTime(timeStr);
                        long alarmId = db.alarmDao().insert(alarm);

                        Intent intent = new Intent(this, com.example.medicationreminderapp.receiver.AlarmReceiver.class);
                        intent.putExtra("MEDICINE_NAME", name);
                        intent.putExtra("MEDICINE_ID", medicineId);
                        intent.putExtra("MEDICINE_NOTE", note);
                        intent.putExtra("SCHEDULED_TIME", timeStr);

                        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) alarmId, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                        if (alarmManager != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                if (alarmManager.canScheduleExactAlarms()) {
                                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pendingIntent);
                                } else {
                                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pendingIntent);
                                }
                            } else {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), pendingIntent);
                            }
                        }
                    }
                }
            } catch (ParseException e) {
                Log.e(TAG, "Parse error", e);
            }
        }
    }

    private void showTimePicker() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_time_picker, null);

        final NumberPicker hourPicker = dialogView.findViewById(R.id.picker_hour);
        final NumberPicker minutePicker = dialogView.findViewById(R.id.picker_minute);
        final NumberPicker amPmPicker = dialogView.findViewById(R.id.picker_am_pm);

        hourPicker.setMinValue(1);
        hourPicker.setMaxValue(12);
        hourPicker.setFormatter(i -> String.format(Locale.US, "%02d", i));

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(i -> String.format(Locale.US, "%02d", i));

        amPmPicker.setMinValue(0);
        amPmPicker.setMaxValue(1);
        amPmPicker.setDisplayedValues(new String[]{"AM", "PM"});

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Chọn giờ uống");
        builder.setPositiveButton("OK", (dialog, which) -> {
            Calendar tempCal = Calendar.getInstance();
            tempCal.set(Calendar.AM_PM, amPmPicker.getValue());
            tempCal.set(Calendar.HOUR, hourPicker.getValue() == 12 ? 0 : hourPicker.getValue());
            tempCal.set(Calendar.MINUTE, minutePicker.getValue());

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);
            String selectedTimeStr = sdf.format(tempCal.getTime());

            if (!times.contains(selectedTimeStr)) {
                times.add(selectedTimeStr);
                addTimeChip(selectedTimeStr);
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.create().show();
    }

    private void addTimeChip(String time) {
        Chip chip = new Chip(this);
        chip.setText(time);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            timeContainer.removeView(chip);
            times.remove(time);
        });
        timeContainer.addView(chip);
    }

    private void showCalendarPicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Chọn khoảng thời gian");
        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first != null && selection.second != null) {
                updateDateRange(selection.first, selection.second);
            }
        });
        picker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void updateDateRange(long newStartDate, long newEndDate) {
        startDate = newStartDate;
        endDate = newEndDate;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateRangeText.setText(String.format("%s - %s", sdf.format(new Date(startDate)), sdf.format(new Date(endDate))));

        long diffInMillis = Math.abs(endDate - startDate);
        long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis) + 1;
        totalDurationText.setText(diffInDays + " ngày");
    }
}
