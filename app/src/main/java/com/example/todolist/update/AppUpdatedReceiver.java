package com.example.todolist.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.todolist.BuildConfig;
import com.example.todolist.core.UpdatePolicy;
import com.example.todolist.notify.NotificationHelper;
import com.example.todolist.reminders.ReminderScheduler;

/**
 * Runs right after this app was replaced by a newer version (installed from the in-app updater, from
 * a downloaded file, or by any other means). The new code is already running here.
 */
public class AppUpdatedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;

        // Alarms are cleared when an app is replaced; rebuild them from the local store.
        ReminderScheduler.rescheduleAll(context);
        // The installer APK is no longer needed.
        ApkDownloader.deleteDownloads(context);

        int lastSeen = new UpdatePrefs(context).lastSeenVersionCode();
        if (UpdatePolicy.shouldShowUpdatedMessage(lastSeen, BuildConfig.VERSION_CODE)) {
            NotificationHelper.showAppUpdated(context, BuildConfig.VERSION_NAME);
        }
    }
}
