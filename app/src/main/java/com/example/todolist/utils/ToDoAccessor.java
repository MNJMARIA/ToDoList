package com.example.todolist.utils;

import android.content.Context;
import com.example.todolist.core.TaskQuery;
import com.example.todolist.utils.model.ToDoData;

/** Lets the pure search/sort/summary code in {@code core} read a {@link ToDoData}. */
public final class ToDoAccessor implements TaskQuery.Accessor<ToDoData> {

    private final Context context;

    public ToDoAccessor(Context context) {
        this.context = context;
    }

    @Override
    public String title(ToDoData t) {
        return t.getTask();
    }

    /** The category in the current language, so a search in Ukrainian finds the built-in "work" category. */
    @Override
    public String categoryLabel(ToDoData t) {
        return CategoryLabels.display(context, t.getCategory());
    }

    @Override
    public int priorityRank(ToDoData t) {
        return t.getPriority() == null ? -1 : t.getPriority().ordinal();
    }

    @Override
    public String date(ToDoData t) {
        return t.getDate();
    }

    @Override
    public String time(ToDoData t) {
        return t.getTime();
    }
}
