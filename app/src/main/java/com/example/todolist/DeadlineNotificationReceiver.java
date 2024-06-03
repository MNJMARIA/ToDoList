package com.example.todolist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DeadlineNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long deadlineMillis = intent.getLongExtra("deadline", 0);
        DeadlineNotificationManager.showNotification(context, deadlineMillis);
    }
}