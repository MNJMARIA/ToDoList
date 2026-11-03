package com.example.todolist.widget;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.example.todolist.core.WidgetData;
import com.example.todolist.utils.model.ToDoData;
import java.util.ArrayList;
import java.util.List;

/**
 * A small copy of the signed-in user's open tasks, kept on the phone for the home-screen widget. The widget
 * cannot wait for the database, so the task list screens refresh this copy whenever their list changes.
 * Only titles, dates and times are kept (no notes, no subtasks).
 */
public final class WidgetCache {

    private static final String PREFS = "widget_cache";
    private static final String KEY_UID = "uid";
    private static final String KEY_DATA = "data";
    private static final int MAX_TASKS = 100;

    private WidgetCache() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Stores the list and redraws the widgets, but only when something changed. */
    public static void save(@NonNull Context context, @NonNull String uid, @NonNull List<ToDoData> tasks) {
        List<WidgetData.Entry> entries = new ArrayList<>();
        for (ToDoData task : tasks) {
            if (entries.size() >= MAX_TASKS) break;
            entries.add(new WidgetData.Entry(task.getTaskId(), task.getTask(), task.getDate(), task.getTime()));
        }
        String encoded = WidgetData.encode(entries);
        SharedPreferences prefs = prefs(context);
        if (uid.equals(prefs.getString(KEY_UID, null)) && encoded.equals(prefs.getString(KEY_DATA, null))) return;
        prefs.edit().putString(KEY_UID, uid).putString(KEY_DATA, encoded).apply();
        TaskWidgetProvider.refreshAll(context);
    }

    /** Forgets everything (sign-out) so the widget stops showing the previous account's tasks. */
    public static void clear(@NonNull Context context) {
        prefs(context).edit().clear().apply();
        TaskWidgetProvider.refreshAll(context);
    }

    public static boolean hasUser(@NonNull Context context) {
        return prefs(context).getString(KEY_UID, null) != null;
    }

    @NonNull
    public static List<WidgetData.Entry> read(@NonNull Context context) {
        return WidgetData.decode(prefs(context).getString(KEY_DATA, ""));
    }
}
