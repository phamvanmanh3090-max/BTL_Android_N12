package com.example.medicationreminderapp.activity;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.service.AlarmService;

public class AlarmRingActivity extends AppCompatActivity {

    private TextView tvMedicineName, tvNote;
    private Button btnTaken, btnDismiss;
    private String medicineName;
    private String scheduledTime;
    private int medicineId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupWindow();
        setContentView(R.layout.activity_alarm_ring);

        initView();
        setupData();
        setupListener();
    }

    private void setupWindow() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (keyguardManager != null) {
                    keyguardManager.requestDismissKeyguard(this, null);
                }
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initView() {
        tvMedicineName = findViewById(R.id.tvMedicineName);
        tvNote = findViewById(R.id.tvNote);
        btnTaken = findViewById(R.id.btnTaken);
        btnDismiss = findViewById(R.id.btnDismiss);
    }

    private void setupData() {
        if (getIntent() != null) {
            medicineName = getIntent().getStringExtra("MEDICINE_NAME");
            String note = getIntent().getStringExtra("MEDICINE_NOTE");
            scheduledTime = getIntent().getStringExtra("SCHEDULED_TIME");
            medicineId = getIntent().getIntExtra("MEDICINE_ID", -1);

            if (tvMedicineName != null) {
                tvMedicineName.setText(medicineName != null ? medicineName : "Đến giờ uống thuốc");
            }
            if (tvNote != null) {
                tvNote.setText(note != null ? note : "");
            }
        }
    }

    private void setupListener() {
        if (btnTaken != null) {
            btnTaken.setOnClickListener(v -> {
                // Thay vì tự gửi broadcast, hãy gọi Service thực hiện ACTION_TAKEN
                // Vì Service đã có sẵn logic: Lưu DB -> Gửi Broadcast chuẩn -> Đặt báo thức ngày mai
                Intent serviceIntent = new Intent(this, AlarmService.class);
                serviceIntent.setAction(AlarmService.ACTION_TAKEN);
                serviceIntent.putExtra("MEDICINE_ID", medicineId);
                serviceIntent.putExtra("MEDICINE_NAME", medicineName);
                serviceIntent.putExtra("SCHEDULED_TIME", scheduledTime);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }

                Toast.makeText(this, "Đã xác nhận uống thuốc!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> {
                // Tương tự, gọi ACTION_DISMISS để Service tính toán báo thức lần sau
                Intent serviceIntent = new Intent(this, AlarmService.class);
                serviceIntent.setAction(AlarmService.ACTION_DISMISS);
                serviceIntent.putExtra("MEDICINE_ID", medicineId);
                serviceIntent.putExtra("SCHEDULED_TIME", scheduledTime);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                
                finish();
            });
        }
    }
}
