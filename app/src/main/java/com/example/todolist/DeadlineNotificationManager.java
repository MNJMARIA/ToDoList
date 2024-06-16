package com.example.todolist;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class DeadlineNotificationManager {
    private static final String CHANNEL_ID = "Deadline_Channel";
    private static final int NOTIFICATION_ID = 1001;

    public static void scheduleNotification(Context context, long deadlineMillis) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MODE", Context.MODE_PRIVATE);
        boolean notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", true);

        if (!notificationsEnabled) {
            return; // Вихід, якщо повідомлення вимкнені
        }
        // Визначаємо час сповіщення за 3 дні до дедлайну
        long notificationTime = deadlineMillis - (3 * 24 * 60 * 60 * 1000);

        Intent notificationIntent = new Intent(context, DeadlineNotificationReceiver.class);
        notificationIntent.putExtra("deadline", deadlineMillis);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // Плануємо сповіщення за 3 дні до дедлайну
            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent);
        }
    }

    public static void showNotification(Context context, long deadlineMillis) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("MODE", Context.MODE_PRIVATE);
        boolean notificationsEnabled = sharedPreferences.getBoolean("notificationsEnabled", true);

        if (!notificationsEnabled) {
            return; // Вихід, якщо повідомлення вимкнені
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Deadline Channel";
            String description = "Channel for Deadline Notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notifications)
                .setContentTitle("Deadline Notification")
                .setContentText("You have a task deadline in 3 days.")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
