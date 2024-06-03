package com.example.todolist.utils.model;

import android.content.Context;
import com.example.todolist.R;
import java.io.Serializable;

public enum Priority implements Serializable {
    URGENT_AND_IMPORTANT(R.string.priority_urgent_and_important),
    URGENT_BUT_NOT_IMPORTANT(R.string.priority_urgent_but_not_important),
    NOT_URGENT_BUT_IMPORTANT(R.string.priority_not_urgent_but_important),
    NOT_URGENT_AND_NOT_IMPORTANT(R.string.priority_not_urgent_and_not_important);

    private final int displayStringResId;

    Priority(int displayStringResId) {
        this.displayStringResId = displayStringResId;
    }

    public String getDisplayString(Context context) {
        return context.getString(displayStringResId);
    }

    // Метод для отримання обраного пріоритету з рядка
    public static Priority fromString(Context context, String text) {
        for (Priority priority : Priority.values()) {
            if (priority.getDisplayString(context).equals(text)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}