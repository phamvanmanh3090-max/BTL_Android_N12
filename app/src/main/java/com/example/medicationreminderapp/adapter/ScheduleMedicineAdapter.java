package com.example.medicationreminderapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.model.Medicine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ScheduleMedicineAdapter extends RecyclerView.Adapter<ScheduleMedicineAdapter.ViewHolder> {

    private final List<Medicine> medicineList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Medicine medicine);
    }

    public ScheduleMedicineAdapter(List<Medicine> medicineList, OnItemClickListener listener) {
        this.medicineList = medicineList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule_medicine, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Medicine medicine = medicineList.get(position);

        holder.tvMedicineName.setText(medicine.getName());
        holder.tvFrequency.setText(medicine.getFrequency());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateRange = sdf.format(new Date(medicine.getStartDate())) + " - " + sdf.format(new Date(medicine.getEndDate()));
        holder.tvDateRange.setText(dateRange);

        List<String> times = medicine.getTimes();
        if (times != null && !times.isEmpty()) {
            holder.tvTimes.setText(String.join(", ", times));
        } else {
            holder.tvTimes.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(medicine);
            }
        });
    }

    @Override
    public int getItemCount() {
        return medicineList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFrequency, tvDateRange, tvMedicineName, tvTimes;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFrequency = itemView.findViewById(R.id.tvFrequency);
            tvDateRange = itemView.findViewById(R.id.tvDateRange);
            tvMedicineName = itemView.findViewById(R.id.tvMedicineName);
            tvTimes = itemView.findViewById(R.id.tvTimes);
        }
    }
}