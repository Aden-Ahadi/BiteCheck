package com.example.bitecheck.ui;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.bitecheck.R;
import com.example.bitecheck.util.ReminderReceiver;
import com.example.bitecheck.util.ReminderScheduler;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Locale;

public class SettingsActivity extends BaseSecondaryActivity {

    public static final String PREFS = "bitecheck_settings";
    public static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    public static final String KEY_REMINDER_HOUR = "reminder_hour";
    public static final String KEY_REMINDER_MINUTE = "reminder_minute";
    private static final int REQUEST_NOTIFICATIONS = 51;

    private SharedPreferences prefs;
    private MaterialSwitch reminderSwitch;
    private TextView timeText;

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_settings;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_settings);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        reminderSwitch = findViewById(R.id.switch_reminder);
        timeText = findViewById(R.id.text_reminder_time);

        reminderSwitch.setChecked(prefs.getBoolean(KEY_REMINDER_ENABLED, false));
        updateTimeLabel();

        reminderSwitch.setOnCheckedChangeListener((button, checked) -> {
            prefs.edit().putBoolean(KEY_REMINDER_ENABLED, checked).apply();
            if (checked) {
                requestNotificationPermissionIfNeeded();
                ReminderReceiver.createChannel(this);
                ReminderScheduler.schedule(this, hour(), minute());
            } else {
                ReminderScheduler.cancel(this);
            }
        });

        timeText.setOnClickListener(v -> new TimePickerDialog(this,
                (view, pickedHour, pickedMinute) -> {
                    prefs.edit()
                            .putInt(KEY_REMINDER_HOUR, pickedHour)
                            .putInt(KEY_REMINDER_MINUTE, pickedMinute)
                            .apply();
                    updateTimeLabel();
                    if (reminderSwitch.isChecked()) {
                        ReminderScheduler.schedule(this, pickedHour, pickedMinute);
                        String msg = String.format(Locale.US, "Reminder set for %02d:%02d", pickedHour, pickedMinute);
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    }
                }, hour(), minute(), true).show());

        findViewById(R.id.btn_devices).setOnClickListener(v ->
                startActivity(new Intent(this, DevicesActivity.class)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS && grantResults.length > 0 
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.msg_sms_permission_granted, Toast.LENGTH_SHORT).show();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        }
    }

    private void updateTimeLabel() {
        timeText.setText(getString(R.string.settings_reminder_time, hour(), minute()));
    }

    private int hour() {
        return prefs.getInt(KEY_REMINDER_HOUR, 19);
    }

    private int minute() {
        return prefs.getInt(KEY_REMINDER_MINUTE, 30);
    }
}
