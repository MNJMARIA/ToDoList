package com.example.todolist.core;

/**
 * Decides when to remind the user about a deadline.
 *
 * <p>Preferred reminder: three days before. If that moment has already passed (task created
 * shortly before its deadline) the fallback is one hour before. If that has passed as well there is
 * nothing useful left to say, so no reminder is planned. The old code always scheduled "deadline
 * minus three days", which for near deadlines lay in the past and fired immediately.
 */
public final class ReminderPlanner {

    public static final long ONE_HOUR_MILLIS = 60L * 60 * 1000;
    public static final long THREE_DAYS_MILLIS = 3 * 24 * ONE_HOUR_MILLIS;

    public enum Kind { THREE_DAYS_BEFORE, ONE_HOUR_BEFORE }

    public static final class Plan {
        public final long triggerAtMillis;
        public final Kind kind;

        public Plan(long triggerAtMillis, Kind kind) {
            this.triggerAtMillis = triggerAtMillis;
            this.kind = kind;
        }
    }

    private ReminderPlanner() {
    }

    /** @return the reminder to schedule, or {@code null} if no useful reminder fits before the deadline. */
    public static Plan plan(long deadlineMillis, long nowMillis) {
        if (deadlineMillis <= nowMillis) return null;
        long threeDays = deadlineMillis - THREE_DAYS_MILLIS;
        if (threeDays > nowMillis) return new Plan(threeDays, Kind.THREE_DAYS_BEFORE);
        long oneHour = deadlineMillis - ONE_HOUR_MILLIS;
        if (oneHour > nowMillis) return new Plan(oneHour, Kind.ONE_HOUR_BEFORE);
        return null;
    }

    /** Stable per-task id for PendingIntents, so each task owns exactly one alarm that can be cancelled. */
    public static int requestCode(String taskId) {
        return taskId == null ? 0 : taskId.hashCode();
    }
}
