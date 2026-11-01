package com.example.todolist.reminders;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.example.todolist.core.ReminderPlanner;
import com.example.todolist.core.ReminderRecord;
import com.example.todolist.core.TaskDateTime;
import java.util.OptionalLong;

/**
 * Schedules one deadline reminder per task.
 *
 * <p>Every task owns exactly one alarm (its request code and intent data come from the task id), so
 * adding a task no longer overwrites the alarm of another one, and finishing, deleting or editing a
 * task can cancel or replace precisely its own alarm.
 */
public final class ReminderScheduler {

    static final String EXTRA_TASK_ID = "taskId";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_DEADLINE = "deadline";
    static final String EXTRA_KIND = "kind";

    private ReminderScheduler() {
    }

    /** The "notifications" switch from the settings screen (shared with the old implementation). */
    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences("MODE", Context.MODE_PRIVATE)
                .getBoolean("notificationsEnabled", true);
    }

    /** Remembers the reminder for a task and, if notifications are on, (re)sets its alarm. */
    public static void schedule(Context context, String taskId, String title, String date, String time) {
        if (taskId == null) return;
        Context app = context.getApplicationContext();
        OptionalLong deadline = TaskDateTime.deadlineMillis(date, time);
        if (!deadline.isPresent()) {
            cancel(app, taskId);
            return;
        }
        ReminderRecord record = new ReminderRecord(taskId, title, deadline.getAsLong());
        new ReminderStore(app).put(record);
        arm(app, record);
    }

    /** Removes the alarm and the stored record of a task (finished, deleted, or no usable deadline). */
    public static void cancel(Context context, String taskId) {
        if (taskId == null) return;
        Context app = context.getApplicationContext();
        cancelAlarm(app, taskId);
        new ReminderStore(app).remove(taskId);
    }

    /** Recreates alarms from the local store (after reboot, app update or turning notifications on). */
    public static void rescheduleAll(Context context) {
        Context app = context.getApplicationContext();
        ReminderStore store = new ReminderStore(app);
        for (ReminderRecord record : store.all()) {
            arm(app, record);
        }
    }

    /** Notifications switched off: stop all alarms but keep the records so switching back on restores them. */
    public static void cancelAllAlarms(Context context) {
        Context app = context.getApplicationContext();
        for (ReminderRecord record : new ReminderStore(app).all()) {
            cancelAlarm(app, record.taskId);
        }
    }

    /** Sign-out: another account must not inherit this account's reminders. */
    public static void clearAll(Context context) {
        Context app = context.getApplicationContext();
        ReminderStore store = new ReminderStore(app);
        for (ReminderRecord record : store.all()) {
            cancelAlarm(app, record.taskId);
        }
        store.clear();
    }

    /** Called by the receiver after a reminder was shown: chain the one-hour reminder or forget the task. */
    static void onFired(Context app, String taskId, ReminderPlanner.Kind kind) {
        ReminderStore store = new ReminderStore(app);
        if (kind == ReminderPlanner.Kind.THREE_DAYS_BEFORE) {
            ReminderRecord record = store.get(taskId);
            if (record != null) arm(app, record);
        } else {
            store.remove(taskId);
        }
    }

    private static void arm(Context app, ReminderRecord record) {
        cancelAlarm(app, record.taskId);
        ReminderPlanner.Plan plan = ReminderPlanner.plan(record.deadlineMillis, System.currentTimeMillis());
        if (plan == null) {
            // Deadline passed or too close for a useful reminder: nothing left to remember.
            new ReminderStore(app).remove(record.taskId);
            return;
        }
        if (!isEnabled(app)) return;

        AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        Intent intent = baseIntent(app, record.taskId)
                .putExtra(EXTRA_TASK_ID, record.taskId)
                .putExtra(EXTRA_TITLE, record.title)
                .putExtra(EXTRA_DEADLINE, record.deadlineMillis)
                .putExtra(EXTRA_KIND, plan.kind.name());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                app, ReminderPlanner.requestCode(record.taskId), intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        // Inexact and allowed while idle: needs no special permission and is accurate enough for "3 days before".
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.triggerAtMillis, pendingIntent);
    }

    private static void cancelAlarm(Context app, String taskId) {
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        PendingIntent existing = PendingIntent.getBroadcast(
                app, ReminderPlanner.requestCode(taskId), baseIntent(app, taskId),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE);
        if (existing != null) {
            alarmManager.cancel(existing);
            existing.cancel();
        }
    }

    /** Component + data identify the PendingIntent; extras do not take part in matching. */
    private static Intent baseIntent(Context app, String taskId) {
        Intent intent = new Intent(app, ReminderReceiver.class);
        intent.setData(Uri.parse("todolist://reminder/" + Uri.encode(taskId)));
        return intent;
    }
}
