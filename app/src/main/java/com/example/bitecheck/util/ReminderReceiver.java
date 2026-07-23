package com.example.bitecheck.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.bitecheck.R;
import com.example.bitecheck.ui.MainActivity;
import com.example.bitecheck.ui.SettingsActivity;

/** Fires the daily "log your meals" reminder notification. */
public class ReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "reminders";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        createChannel(context);

        Intent openChat = new Intent(context, MainActivity.class);
        openChat.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, openChat,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Load the Avocado JPG as a Large Icon
        Bitmap avocado = BitmapFactory.decodeResource(context.getResources(), R.drawable.logo_bitecheck);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // Use your actual app icon
                .setLargeIcon(avocado) // Keep the big avocado logo
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_text))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }

        // Re-schedule the next exact alarm for tomorrow
        SharedPreferences prefs = context.getSharedPreferences(
                SettingsActivity.PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(SettingsActivity.KEY_REMINDER_ENABLED, false)) {
            int h = prefs.getInt(SettingsActivity.KEY_REMINDER_HOUR, 20);
            int m = prefs.getInt(SettingsActivity.KEY_REMINDER_MINUTE, 0);
            ReminderScheduler.schedule(context, h, m, true);
        }
    }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.notification_channel_desc));
            channel.enableLights(true);
            channel.enableVibration(true);
            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
