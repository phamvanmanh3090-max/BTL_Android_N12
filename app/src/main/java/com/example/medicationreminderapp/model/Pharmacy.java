package com.example.medicationreminderapp.model;

public class Pharmacy {
    private String name;
    private int imageResId;
    private String websiteUrl;

    public Pharmacy(String name, int imageResId, String websiteUrl) {
        this.name = name;
        this.imageResId = imageResId;
        this.websiteUrl = websiteUrl;
    }

    public String getName() { return name; }
    public int getImageResId() { return imageResId; }
    public String getWebsiteUrl() { return websiteUrl; }
}