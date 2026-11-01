package com.example.todolist.notify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.todolist.MainActivity;
import com.example.todolist.R;
import com.example.todolist.core.ReminderPlanner;
import java.text.DateFormat;
import java.util.Date;

/** All system notifications of the app: deadline reminders and "app updated". */
public final class NotificationHelper {

    public static final String CHANNEL_REMINDERS = "deadline_reminders";
    public static final String CHANNEL_UPDATES = "app_updates";
    private static final int NOTIFICATION_ID_UPDATED = 2001;

    private NotificationHelper() {
    }

    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel reminders = new NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        reminders.setDescription(context.getString(R.string.reminder_channel_description));
        manager.createNotificationChannel(reminders);

        NotificationChannel updates = new NotificationChannel(
                CHANNEL_UPDATES,
                context.getString(R.string.updates_channel_name),
                NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel(updates);
    }

    /** False when the user (or Android 13+ permission) blocks notifications for the app. */
    public static boolean canNotify(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void showReminder(Context context, String taskId, String title,
                                    ReminderPlanner.Kind kind, long deadlineMillis) {
        if (!canNotify(context)) return;
        ensureChannels(context);

        int textRes = kind == ReminderPlanner.Kind.THREE_DAYS_BEFORE
                ? R.string.reminder_in_3_days
                : R.string.reminder_in_1_hour;
        String when = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date(deadlineMillis));
        int notificationId = ReminderPlanner.requestCode(taskId);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentTitle(title == null || title.isEmpty() ? context.getString(R.string.app_name) : title)
                .setContentText(context.getString(textRes) + " · " + when)
                .setContentIntent(openAppIntent(context, notificationId))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        notify(context, notificationId, builder);
    }

    public static void showAppUpdated(Context context, String versionName) {
        if (!canNotify(context)) return;
        ensureChannels(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_UPDATES)
                .setSmallIcon(R.drawable.ic_system_update_24)
                .setContentTitle(context.getString(R.string.app_updated_title))
                .setContentText(context.getString(R.string.app_updated_text, versionName))
                .setContentIntent(openAppIntent(context, NOTIFICATION_ID_UPDATED))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);
        notify(context, NOTIFICATION_ID_UPDATED, builder);
    }

    private static void notify(Context context, int id, NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS was revoked between the check and the call; nothing sensible to do.
        }
    }

    private static PendingIntent openAppIntent(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
