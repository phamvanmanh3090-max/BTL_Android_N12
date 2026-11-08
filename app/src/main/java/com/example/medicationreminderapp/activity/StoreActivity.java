package com.example.medicationreminderapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.adapter.PharmacyAdapter;
import com.example.medicationreminderapp.model.Pharmacy;

import java.util.ArrayList;
import java.util.List;

public class StoreActivity extends AppCompatActivity {

    private RecyclerView rvStore;
    private PharmacyAdapter adapter;
    private List<Pharmacy> mListPharmacy;
    private TextView navHome, navAlarm, navStore, navConsultation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        initView();
        setupBottomNavigation();
        setupRecyclerView();
    }

    private void initView() {
        rvStore = findViewById(R.id.rvStore);
        navHome = findViewById(R.id.navHome);
        navAlarm = findViewById(R.id.navAlarm);
        navStore = findViewById(R.id.navStore);
        navConsultation = findViewById(R.id.navConsultation);

        navStore.setSelected(true);
        navHome.setSelected(false);
        navAlarm.setSelected(false);
        navConsultation.setSelected(false);
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(StoreActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(StoreActivity.this, ScheduleActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navStore.setOnClickListener(v -> { });

        navConsultation.setOnClickListener(v -> {
            Intent intent = new Intent(StoreActivity.this, AIChatActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    private void setupRecyclerView() {
        rvStore.setLayoutManager(new LinearLayoutManager(this));

        mListPharmacy = new ArrayList<>();

        // 1. Long Châu
        mListPharmacy.add(new Pharmacy("Nhà thuốc FPT Long Châu", R.drawable.logo_longchau, "https://nhathuoclongchau.com.vn"));

        // 2. Pharmacity
        mListPharmacy.add(new Pharmacy("Nhà thuốc Pharmacity", R.drawable.logo_pharmacity, "https://www.pharmacity.vn"));

        // 3. An Khang
        mListPharmacy.add(new Pharmacy("Nhà thuốc An Khang", R.drawable.logo_ankhang, "https://www.nhathuocankhang.com"));

        // 4. Trung Sơn
        mListPharmacy.add(new Pharmacy("Trung Sơn Pharma", R.drawable.logo_trungson, "https://trungson.vn"));

        // 5. Nhà thuốc 365
        mListPharmacy.add(new Pharmacy("Nhà thuốc 365", R.drawable.logo_365, "https://nhathuoc365.vn"));

        // 6. Phương Chính
        mListPharmacy.add(new Pharmacy("Nhà thuốc Phương Chính", R.drawable.logo_phuongchinh, "https://nhathuocphuongchinh.com"));

        // 7. Medigo
        mListPharmacy.add(new Pharmacy("Ứng dụng Medigo", R.drawable.logo_medigo, "https://medigoapp.com"));

        adapter = new PharmacyAdapter(this, mListPharmacy);
        rvStore.setAdapter(adapter);
    }
}