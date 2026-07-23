package com.example.bitecheck.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

/** Schedules the daily meal-logging reminder via AlarmManager. */
public final class ReminderScheduler {

    private static final int REQUEST_CODE = 2001;

    private ReminderScheduler() {
    }

    public static void schedule(Context context, int hour, int minute) {
        schedule(context, hour, minute, false);
    }

    public static void schedule(Context context, int hour, int minute, boolean forceTomorrow) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, hour);
        time.set(Calendar.MINUTE, minute);
        time.set(Calendar.SECOND, 0);
        time.set(Calendar.MILLISECOND, 0);

        Calendar now = Calendar.getInstance();
        
        if (forceTomorrow) {
            time.add(Calendar.DAY_OF_YEAR, 1);
        } else {
            // If the time is in the past (by more than 30 seconds), move to tomorrow.
            // This allows a small window to fire 'now' for demos.
            if (time.getTimeInMillis() < now.getTimeInMillis() - 30000) {
                time.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        PendingIntent pi = pendingIntent(context);
        
        // Use setExact for demo precision. 
        // This won't show the 'Alarm Clock' icon on most devices, unlike setAlarmClock.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    time.getTimeInMillis(), pi);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), pi);
        }
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
