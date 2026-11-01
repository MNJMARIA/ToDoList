package com.example.todolist.reminders;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.todolist.core.ReminderPlanner;
import com.example.todolist.notify.NotificationHelper;

/** Shows the reminder notification for one task when its alarm fires. */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_ID);
        if (taskId == null) return;

        String title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE);
        long deadline = intent.getLongExtra(ReminderScheduler.EXTRA_DEADLINE, 0);
        ReminderPlanner.Kind kind = parseKind(intent.getStringExtra(ReminderScheduler.EXTRA_KIND));

        if (ReminderScheduler.isEnabled(context)) {
            NotificationHelper.showReminder(context, taskId, title, kind, deadline);
        }
        ReminderScheduler.onFired(context.getApplicationContext(), taskId, kind);
    }

    private static ReminderPlanner.Kind parseKind(String name) {
        try {
            return ReminderPlanner.Kind.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return ReminderPlanner.Kind.ONE_HOUR_BEFORE;
        }
    }
}
