package com.example.medicationreminderapp.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.model.Day;

import java.util.List;

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.DateViewHolder> {

    private final List<Day> days;
    private int selectedPosition = 0;
    private final OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(Day day, int position);
    }

    public DateAdapter(List<Day> days, int initialSelected, OnDateClickListener listener) {
        this.days = days;
        this.selectedPosition = initialSelected;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        Day day = days.get(position);
        Context context = holder.itemView.getContext();
        boolean isSelected = (position == selectedPosition);

        holder.tvDayOfWeek.setText(day.getDayOfWeek());
        holder.tvDate.setText(String.valueOf(day.getDay()));

        Context ctx = holder.itemView.getContext();
        if (isSelected) {
            holder.flDateContainer.setBackgroundResource(R.drawable.bg_date_pill);
            holder.tvDate.setTextColor(ContextCompat.getColor(ctx, R.color.green_dark));
            holder.tvDayOfWeek.setTextColor(ContextCompat.getColor(ctx, R.color.white));
        } else {
            holder.flDateContainer.setBackgroundResource(0);
            holder.tvDate.setTextColor(ContextCompat.getColor(ctx, R.color.white));
            holder.tvDayOfWeek.setTextColor(0xB3FFFFFF);
        }

        holder.itemView.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onDateClick(day, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return days != null ? days.size() : 0;
    }

    static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayOfWeek, tvDate;
        FrameLayout flDateContainer;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvDate = itemView.findViewById(R.id.tvDate);
            flDateContainer = itemView.findViewById(R.id.flDateContainer);
        }
    }
}