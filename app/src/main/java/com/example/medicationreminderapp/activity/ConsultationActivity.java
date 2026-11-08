package com.example.medicationreminderapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medicationreminderapp.R;

public class ConsultationActivity extends AppCompatActivity {

    private TextView navHome, navAlarm, navStore, navConsultation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation);

        initView();
        setListener();
    }

    private void initView() {
        navHome = findViewById(R.id.navHome);
        navAlarm = findViewById(R.id.navAlarm);
        navStore = findViewById(R.id.navStore);
        navConsultation = findViewById(R.id.navConsultation);

        navConsultation.setSelected(true);
    }

    private void setListener() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ConsultationActivity.this, HomeActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(ConsultationActivity.this, ScheduleActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navStore.setOnClickListener(v -> {
            Intent intent = new Intent(ConsultationActivity.this, StoreActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navConsultation.setOnClickListener(v -> {
            // Already on the Consultation screen
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        navHome.setSelected(false);
        navAlarm.setSelected(false);
        navStore.setSelected(false);
        navConsultation.setSelected(true);
    }
}
