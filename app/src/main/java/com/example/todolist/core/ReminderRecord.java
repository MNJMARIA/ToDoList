package com.example.todolist.core;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

/**
 * What the app remembers locally about a scheduled reminder, so alarms can be restored after a
 * reboot or an app update without waiting for the network.
 */
public final class ReminderRecord {

    private static final Gson GSON = new Gson();

    @SerializedName("taskId")
    public final String taskId;
    @SerializedName("title")
    public final String title;
    @SerializedName("deadlineMillis")
    public final long deadlineMillis;

    public ReminderRecord(String taskId, String title, long deadlineMillis) {
        this.taskId = taskId;
        this.title = title == null ? "" : title;
        this.deadlineMillis = deadlineMillis;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    /** @return the record, or {@code null} if the text is empty, malformed or has no task id. */
    public static ReminderRecord fromJson(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            ReminderRecord r = GSON.fromJson(json, ReminderRecord.class);
            if (r == null || r.taskId == null || r.taskId.isEmpty()) return null;
            return new ReminderRecord(r.taskId, r.title, r.deadlineMillis);
        } catch (JsonParseException e) {
            return null;
        }
    }
}
