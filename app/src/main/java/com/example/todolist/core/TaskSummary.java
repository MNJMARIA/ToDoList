package com.example.todolist.core;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.OptionalLong;

/** Counters for the header of the task list: how much is due today, how much is already late. */
public final class TaskSummary {

    public final int total;
    public final int today;
    public final int overdue;

    private TaskSummary(int total, int today, int overdue) {
        this.total = total;
        this.today = today;
        this.overdue = overdue;
    }

    /** True when the deadline can be read and is before {@code nowMillis}. Unreadable deadlines are not late. */
    public static boolean isOverdue(String date, String time, long nowMillis, ZoneId zone) {
        OptionalLong deadline = TaskDateTime.deadlineMillis(date, time, zone);
        return deadline.isPresent() && deadline.getAsLong() < nowMillis;
    }

    /**
     * @param today  tasks whose date is today (finished tasks are deleted, so these are all still open)
     * @param overdue tasks whose deadline has passed, on any day
     */
    public static <T> TaskSummary of(List<T> items, TaskQuery.Accessor<T> accessor, long nowMillis, ZoneId zone) {
        String todayText = TaskDateTime.formatDate(LocalDate.now(zone));
        int todayCount = 0;
        int overdueCount = 0;
        for (T item : items) {
            String date = accessor.date(item);
            if (TaskDateTime.sameDay(date, todayText)) todayCount++;
            if (isOverdue(date, accessor.time(item), nowMillis, zone)) overdueCount++;
        }
        return new TaskSummary(items.size(), todayCount, overdueCount);
    }
}
