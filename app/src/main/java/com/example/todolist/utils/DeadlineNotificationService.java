package com.example.todolist.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import com.example.todolist.DeadlineNotificationReceiver;

public class DeadlineNotificationService extends Service {
    private static final long INTERVAL = 24 * 60 * 60 * 1000; // Перевірка кожні 24 години

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scheduleDeadlineCheck();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void scheduleDeadlineCheck() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return; // Перевірка на null для уникнення NullPointerException

        Intent intent = new Intent(this, DeadlineNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long triggerTime = SystemClock.elapsedRealtime() + INTERVAL;
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, INTERVAL, pendingIntent);
    }
}