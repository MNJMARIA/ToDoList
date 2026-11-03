package com.example.todolist.core;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.TreeSet;

/**
 * Numbers behind the statistics charts, calculated from "tasks finished per day".
 *
 * <p>The per-day counts come from the database as {@code yyyy-MM-dd -> count}. Days with no entry count as zero.
 */
public final class ActivityStats {

    private ActivityStats() {
    }

    /** The key under which a day is stored. ISO format sorts the same as the calendar. */
    public static String dayKey(LocalDate day) {
        return day.toString();
    }

    /** @return the date for a stored key, or null when the key is not a date */
    public static LocalDate parseKey(String key) {
        if (key == null) return null;
        try {
            return LocalDate.parse(key);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Count for one day (0 when unknown or negative). */
    public static int on(Map<String, Integer> perDay, LocalDate day) {
        if (perDay == null) return 0;
        Integer value = perDay.get(dayKey(day));
        return value == null ? 0 : Math.max(0, value);
    }

    /** The last {@code days} days up to and including {@code today}, oldest first. */
    public static int[] lastDays(Map<String, Integer> perDay, LocalDate today, int days) {
        int[] result = new int[Math.max(0, days)];
        for (int i = 0; i < result.length; i++) {
            result[i] = on(perDay, today.minusDays(result.length - 1 - i));
        }
        return result;
    }

    public static int sum(int[] values) {
        int total = 0;
        for (int value : values) total += value;
        return total;
    }

    /** Index of the biggest value (the first one when tied), or -1 when everything is zero or the array is empty. */
    public static int peakIndex(int[] values) {
        int best = -1;
        int bestValue = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > bestValue) {
                bestValue = values[i];
                best = i;
            }
        }
        return best;
    }

    /**
     * Days in a row with at least one finished task. A streak is still alive when today has nothing yet but
     * yesterday had something: the day is not over, so opening the app in the morning does not show 0.
     */
    public static int currentStreak(Map<String, Integer> perDay, LocalDate today) {
        LocalDate cursor = on(perDay, today) > 0 ? today : today.minusDays(1);
        int streak = 0;
        while (on(perDay, cursor) > 0) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /** The longest run of consecutive active days in the whole history. */
    public static int bestStreak(Map<String, Integer> perDay) {
        if (perDay == null) return 0;
        TreeSet<LocalDate> active = new TreeSet<>();
        for (Map.Entry<String, Integer> e : perDay.entrySet()) {
            LocalDate day = parseKey(e.getKey());
            if (day != null && e.getValue() != null && e.getValue() > 0) active.add(day);
        }
        int best = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate day : active) {
            run = previous != null && previous.plusDays(1).equals(day) ? run + 1 : 1;
            best = Math.max(best, run);
            previous = day;
        }
        return best;
    }

    /** Whole-number percentage of finished tasks among all tasks the user has (finished and open). */
    public static int completionPercent(int completed, int open) {
        long all = (long) Math.max(0, completed) + Math.max(0, open);
        if (all == 0) return 0;
        return (int) ((100L * Math.max(0, completed)) / all);
    }
}
