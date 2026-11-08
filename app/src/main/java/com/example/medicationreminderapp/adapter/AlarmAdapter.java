package com.example.medicationreminderapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.model.AlarmWithMedicine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    private final List<AlarmWithMedicine> alarms;
    private final OnAlarmToggleListener listener; // 1. Khai báo Listener

    // 2. Tạo Interface để giao tiếp với Activity
    public interface OnAlarmToggleListener {
        void onToggle(AlarmWithMedicine item, boolean isChecked);
    }

    // 3. Cập nhật Constructor để nhận Listener từ bên ngoài
    public AlarmAdapter(List<AlarmWithMedicine> alarms, OnAlarmToggleListener listener) {
        this.alarms = alarms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmWithMedicine alarmWithMedicine = alarms.get(position);

        // Hiển thị tên thuốc
        holder.tvMedicineName.setText(alarmWithMedicine.medicine.getName());

        // Hiển thị giờ (Format hh:mm a -> 08:30 AM)
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String timeString = sdf.format(new Date(alarmWithMedicine.alarm.getTime()));
        holder.tvAlarmTime.setText(timeString);

        // Hiển thị liều lượng (nếu có, hoặc ẩn đi nếu null)
        // (Giả sử bạn muốn hiện Frequency hoặc Notes vào chỗ dosage)
        String dosage = alarmWithMedicine.medicine.getFrequency();
        if (dosage != null) {
            holder.tvMedicineDosage.setText(dosage);
        }

        // --- QUAN TRỌNG: XỬ LÝ SWITCH ---
        // Phải gỡ listener cũ ra trước khi set trạng thái để tránh lỗi vòng lặp khi cuộn danh sách
        holder.swAlarm.setOnCheckedChangeListener(null);

        // Set trạng thái ON/OFF hiện tại từ Database
        holder.swAlarm.setChecked(alarmWithMedicine.alarm.isEnabled());

        // Gắn listener mới để bắt sự kiện người dùng bấm vào
        holder.swAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Cập nhật trạng thái vào biến tạm
                alarmWithMedicine.alarm.setEnabled(isChecked);

                // Gọi Interface để báo cho ScheduleActivity biết mà xử lý (Lưu DB, đặt AlarmManager...)
                if (listener != null) {
                    listener.onToggle(alarmWithMedicine, isChecked);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    public static class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlarmTime, tvMedicineName, tvMedicineDosage;
        SwitchCompat swAlarm; // Lưu ý: Nếu XML dùng MaterialSwitch thì đổi thành MaterialSwitch

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAlarmTime = itemView.findViewById(R.id.tvAlarmTime);
            tvMedicineName = itemView.findViewById(R.id.tvMedicineName);
            tvMedicineDosage = itemView.findViewById(R.id.tvMedicineDosage);
            swAlarm = itemView.findViewById(R.id.swAlarm);
        }
    }
}