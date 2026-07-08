package com.example.bitecheck;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

public class BiteCheckApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // The Yuka-style design is light-first (white surfaces, hardcoded
        // brand text colors), so keep one consistent look on every device.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
