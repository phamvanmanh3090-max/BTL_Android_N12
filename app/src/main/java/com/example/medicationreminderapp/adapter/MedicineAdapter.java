package com.example.medicationreminderapp.adapter;

import android.content.Context;
import androidx.core.content.ContextCompat;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.model.MedicineDose;

import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {

    private List<MedicineDose> doseList;
    private OnUongClickListener listener;

    public interface OnUongClickListener {
        void onUongClick(MedicineDose dose, int position);
    }

    public MedicineAdapter(List<MedicineDose> doseList, OnUongClickListener listener) {
        this.doseList = doseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicineDose dose = doseList.get(position);

        holder.tvName.setText(dose.medicine.getName());
        holder.tvTime.setText(dose.scheduledTime);

        if (dose.medicine.getNotes() != null && !dose.medicine.getNotes().isEmpty()) {
            holder.tvDosage.setText(dose.medicine.getNotes());
        } else {
            holder.tvDosage.setText(dose.medicine.getFrequency());
        }

        Context ctx = holder.itemView.getContext();
        if (dose.isTaken) {
            holder.btnTaken.setText(dose.timeTaken != null ? "✓ " + dose.timeTaken : "✓");
            holder.btnTaken.setTextColor(ContextCompat.getColor(ctx, R.color.white));
            holder.btnTaken.setBackgroundResource(R.drawable.bg_button_completed);
            holder.itemView.setAlpha(0.7f);
        } else {
            holder.btnTaken.setText("Uống");
            holder.btnTaken.setTextColor(ContextCompat.getColor(ctx, R.color.green_primary));
            holder.btnTaken.setBackgroundResource(R.drawable.bg_button_pending);
            holder.itemView.setAlpha(1f);
        }

        holder.btnTaken.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUongClick(dose, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return doseList != null ? doseList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDosage, tvTime;
        Button btnTaken;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvMedicineName);
            tvDosage = itemView.findViewById(R.id.tvMedicineDosage);
            tvTime = itemView.findViewById(R.id.tvScheduledTime);
            btnTaken = itemView.findViewById(R.id.btnTaken);
        }
    }
}