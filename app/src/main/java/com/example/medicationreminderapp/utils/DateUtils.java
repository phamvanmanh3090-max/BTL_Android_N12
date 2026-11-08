package com.example.medicationreminderapp.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.example.medicationreminderapp.model.Day;

public class DateUtils {
    public static List<Day> getNext7Days() {
        List<Day> days = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            days.add(new Day(
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.YEAR),
                    dayFormat.format(calendar.getTime()),
                    i == 0
            ));
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return days;
    }
}
