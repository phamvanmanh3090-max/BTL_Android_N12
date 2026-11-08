package com.example.medicationreminderapp.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.database.AppDatabase;
import com.example.medicationreminderapp.model.Alarm;
import com.example.medicationreminderapp.model.Medicine;
import com.example.medicationreminderapp.receiver.AlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MedicineDetailActivity extends AppCompatActivity {

    private TextView tvName, tvFrequency, tvDateRange, tvTimes, tvNotes;
    private Button btnDelete, btnEdit;
    private int medicineId;
    private Medicine currentMedicine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_detail);

        medicineId = getIntent().getIntExtra("MEDICINE_ID", -1);
        if (medicineId == -1) {
            finish();
            return;
        }

        initView();
        loadMedicineDetail();
    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvName = findViewById(R.id.tvDetailName);
        tvFrequency = findViewById(R.id.tvDetailFrequency);
        tvDateRange = findViewById(R.id.tvDetailDateRange);
        tvTimes = findViewById(R.id.tvDetailTimes);
        tvNotes = findViewById(R.id.tvDetailNotes);
        btnDelete = findViewById(R.id.btnDelete);
        btnEdit = findViewById(R.id.btnEdit);

        btnDelete.setOnClickListener(v -> deleteMedicine());
        btnEdit.setOnClickListener(v -> {
            // Chuyển sang màn hình AddMedicineActivity với chế độ sửa
            Intent intent = new Intent(MedicineDetailActivity.this, AddMedicineActivity.class);
            intent.putExtra("EDIT_MEDICINE_ID", medicineId);
            startActivity(intent);
        });
    }

    private void loadMedicineDetail() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            currentMedicine = db.medicineDao().getMedicineById(medicineId);

            runOnUiThread(() -> {
                if (currentMedicine != null) {
                    tvName.setText(currentMedicine.getName());
                    tvFrequency.setText("Tần suất: " + currentMedicine.getFrequency());

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String range = "Thời gian: " + sdf.format(new Date(currentMedicine.getStartDate())) + " - " + sdf.format(new Date(currentMedicine.getEndDate()));
                    tvDateRange.setText(range);

                    List<String> times = currentMedicine.getTimes();
                    if (times != null) {
                        tvTimes.setText(String.join(", ", times));
                    }

                    tvNotes.setText(currentMedicine.getNotes() != null ? currentMedicine.getNotes() : "Không có ghi chú");
                }
            });
        }).start();
    }

    private void deleteMedicine() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa đơn thuốc")
                .setMessage("Bạn có chắc chắn muốn xóa đơn thuốc này không? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    new Thread(() -> {
                        AppDatabase db = AppDatabase.getInstance(MedicineDetailActivity.this);

                        // 1. Hủy các báo thức hệ thống trước
                        List<Alarm> alarms = db.alarmDao().getAlarmsByMedicineId(medicineId);
                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        for (Alarm alarm : alarms) {
                            Intent intent = new Intent(this, AlarmReceiver.class);
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                    this,
                                    (int) alarm.getId(),
                                    intent,
                                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE
                            );
                            if (pendingIntent != null && alarmManager != null) {
                                alarmManager.cancel(pendingIntent);
                                pendingIntent.cancel();
                            }
                        }

                        // 2. Xóa trong database (Cascading sẽ tự xóa Alarms)
                        db.medicineDao().delete(currentMedicine);

                        runOnUiThread(() -> {
                            Toast.makeText(MedicineDetailActivity.this, "Đã xóa đơn thuốc", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }).start();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}