package com.example.medicationreminderapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.adapter.ScheduleMedicineAdapter;
import com.example.medicationreminderapp.database.AppDatabase;
import com.example.medicationreminderapp.model.Medicine;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class ScheduleActivity extends AppCompatActivity {

    private static final String TAG = "ScheduleActivity";
    private RecyclerView rvAlarms;
    private ScheduleMedicineAdapter medicineAdapter;
    private final List<Medicine> medicineList = new ArrayList<>();
    private TextView navHome, navAlarm, navStore, navConsultation;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        initView();
        setupToolbar();
        initRecyclerView();
        setListener();
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        rvAlarms = findViewById(R.id.rvAlarms);
        navHome = findViewById(R.id.navHome);
        navAlarm = findViewById(R.id.navAlarm);
        navStore = findViewById(R.id.navStore);
        navConsultation = findViewById(R.id.navConsultation);
        navAlarm.setSelected(true);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Lịch trình");
        }
    }

    private void initRecyclerView() {
        medicineAdapter = new ScheduleMedicineAdapter(medicineList, medicine -> {
            Intent intent = new Intent(ScheduleActivity.this, MedicineDetailActivity.class);
            intent.putExtra("MEDICINE_ID", medicine.getMedicineId());
            startActivity(intent);
        });
        rvAlarms.setLayoutManager(new LinearLayoutManager(this));
        rvAlarms.setAdapter(medicineAdapter);
    }

    private void loadMedicines() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(ScheduleActivity.this);
                List<Medicine> result = db.medicineDao().getAll();
                runOnUiThread(() -> {
                    if (result != null) {
                        medicineList.clear();
                        medicineList.addAll(result);
                        medicineAdapter.notifyDataSetChanged();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading medicines", e);
                runOnUiThread(() -> Toast.makeText(ScheduleActivity.this, "Lỗi khi tải dữ liệu", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void setListener() {
        navHome.setOnClickListener(v -> {
            startActivity(new Intent(ScheduleActivity.this, HomeActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        navStore.setOnClickListener(v -> {
            startActivity(new Intent(ScheduleActivity.this, StoreActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        navConsultation.setOnClickListener(v -> {
            startActivity(new android.content.Intent(ScheduleActivity.this, AIChatActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        navAlarm.setSelected(true);
        loadMedicines();
    }
}