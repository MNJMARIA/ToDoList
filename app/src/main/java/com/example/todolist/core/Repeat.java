package com.example.todolist.core;

import java.time.LocalDate;
import java.util.Locale;

/**
 * How often a task comes back. When a repeating task is finished, the next one is created for the next
 * date of the series. Stored as a short key; an unknown or missing value means "does not repeat".
 */
public enum Repeat {
    NONE("none"),
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly");

    private final String key;

    Repeat(String key) {
        this.key = key;
    }

    /** The language-independent value saved in the database. */
    public String key() {
        return key;
    }

    public boolean repeats() {
        return this != NONE;
    }

    /** Reads a stored value; null, empty and anything unknown give {@link #NONE}. */
    public static Repeat fromStored(String raw) {
        if (raw == null) return NONE;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        for (Repeat repeat : values()) {
            if (repeat.key.equals(value)) return repeat;
        }
        return NONE;
    }

    /**
     * The date of the next task of the series: the first date of the series that is later than both the
     * finished task's own date and {@code today}. A task finished early gets its normal next date; one finished
     * late skips the days that are already gone, so finishing a week-old daily task does not leave six overdue
     * copies. Monthly steps are counted from the original date, so 31 Jan gives 28 Feb and then 31 Mar again.
     *
     * @param date  the finished task's date, or null when it has none (the series then starts from today)
     * @return null for {@link #NONE}
     */
    public LocalDate next(LocalDate date, LocalDate today) {
        if (this == NONE) return null;
        LocalDate base = date == null ? today : date;
        LocalDate bound = base.isAfter(today) ? base : today;
        for (int step = 1; step <= 40000; step++) {
            LocalDate candidate = stepFrom(base, step);
            if (candidate.isAfter(bound)) return candidate;
        }
        return stepFrom(base, 1);
    }

    private LocalDate stepFrom(LocalDate base, int steps) {
        switch (this) {
            case DAILY:
                return base.plusDays(steps);
            case WEEKLY:
                return base.plusWeeks(steps);
            case MONTHLY:
                return base.plusMonths(steps);
            case NONE:
            default:
                return base;
        }
    }
}
