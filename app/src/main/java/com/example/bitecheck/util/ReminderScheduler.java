package com.example.bitecheck.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/** Schedules the daily meal-logging reminder via AlarmManager. */
public final class ReminderScheduler {

    private static final int REQUEST_CODE = 2001;

    private ReminderScheduler() {
    }

    public static void schedule(Context context, int hour, int minute) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, hour);
        time.set(Calendar.MINUTE, minute);
        time.set(Calendar.SECOND, 0);
        if (time.getTimeInMillis() <= System.currentTimeMillis()) {
            time.add(Calendar.DAY_OF_YEAR, 1);
        }
        // Inexact repeating: battery-friendly and needs no exact-alarm permission.
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                time.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent(context));
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent(context));
        }
    }

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
