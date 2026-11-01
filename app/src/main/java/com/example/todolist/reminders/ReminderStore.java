package com.example.todolist.reminders;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.todolist.core.ReminderRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Local copy of "which tasks have a reminder". AlarmManager forgets its alarms on reboot and when the
 * app is replaced, so they are rebuilt from here, offline and without a Firebase round trip.
 */
final class ReminderStore {

    private static final String PREFS = "reminders_store";

    private final SharedPreferences prefs;

    ReminderStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    void put(ReminderRecord record) {
        prefs.edit().putString(record.taskId, record.toJson()).apply();
    }

    void remove(String taskId) {
        prefs.edit().remove(taskId).apply();
    }

    void clear() {
        prefs.edit().clear().apply();
    }

    ReminderRecord get(String taskId) {
        String json = prefs.getString(taskId, null);
        return json == null ? null : ReminderRecord.fromJson(json);
    }

    /** All readable records; unreadable entries are dropped. */
    List<ReminderRecord> all() {
        List<ReminderRecord> result = new ArrayList<>();
        SharedPreferences.Editor cleanup = null;
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            Object value = entry.getValue();
            ReminderRecord record = value instanceof String ? ReminderRecord.fromJson((String) value) : null;
            if (record == null) {
                if (cleanup == null) cleanup = prefs.edit();
                cleanup.remove(entry.getKey());
            } else {
                result.add(record);
            }
        }
        if (cleanup != null) cleanup.apply();
        return result;
    }
}
