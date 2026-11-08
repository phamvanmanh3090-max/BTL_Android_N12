package com.example.medicationreminderapp.model;

public class Day {
    private int day;
    private int month;
    private int year;
    private String dayOfWeek;
    private boolean selected;

    public Day(int day, int month, int year, String dayOfWeek, boolean selected) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.dayOfWeek = dayOfWeek;
        this.selected = selected;
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
