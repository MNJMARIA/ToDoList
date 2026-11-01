package com.example.todolist.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.OptionalLong;

/**
 * Single place that knows how task dates and times are written, parsed and compared.
 *
 * <p>Tasks are stored in Firebase as two strings: a date ({@code dd/MM/yyyy}; older data may be
 * {@code d/M/yyyy} because the date picker did not pad) and a time ({@code HH:mm}). Before this
 * class existed, every screen parsed these strings on its own with a different pattern, which broke
 * reminders and the month view. Everything here is pure Java so it can be unit-tested on the JVM.
 */
public final class TaskDateTime {

    /** Deadlines that are at most this far in the past are still accepted (the dialog defaults to "now"). */
    public static final long PAST_GRACE_MILLIS = 5L * 60 * 1000;

    public enum DeadlineCheck { OK, INVALID_FORMAT, IN_PAST }

    private static final DateTimeFormatter DATE_PARSER =
            DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT);
    private static final DateTimeFormatter TIME_PARSER =
            DateTimeFormatter.ofPattern("H:mm", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private TaskDateTime() {
    }

    /** Parses {@code d/M/yyyy} or {@code dd/MM/yyyy}; returns {@code null} for anything else. */
    public static LocalDate parseDate(String raw) {
        if (raw == null) return null;
        try {
            return LocalDate.parse(raw.trim(), DATE_PARSER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Parses {@code H:mm} or {@code HH:mm}; returns {@code null} for anything else. */
    public static LocalTime parseTime(String raw) {
        if (raw == null) return null;
        try {
            return LocalTime.parse(raw.trim(), TIME_PARSER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String formatDate(LocalDate date) {
        return DATE_FORMATTER.format(date);
    }

    public static String formatTime(LocalTime time) {
        return TIME_FORMATTER.format(time);
    }

    /** Converts any accepted date spelling to the canonical {@code dd/MM/yyyy}, or {@code null} if invalid. */
    public static String normalizeDate(String raw) {
        LocalDate d = parseDate(raw);
        return d == null ? null : formatDate(d);
    }

    public static OptionalLong deadlineMillis(String date, String time, ZoneId zone) {
        LocalDate d = parseDate(date);
        LocalTime t = parseTime(time);
        if (d == null || t == null) return OptionalLong.empty();
        return OptionalLong.of(LocalDateTime.of(d, t).atZone(zone).toInstant().toEpochMilli());
    }

    public static OptionalLong deadlineMillis(String date, String time) {
        return deadlineMillis(date, time, ZoneId.systemDefault());
    }

    /** True when both strings are valid dates and denote the same calendar day (padding is irrelevant). */
    public static boolean sameDay(String a, String b) {
        LocalDate x = parseDate(a);
        LocalDate y = parseDate(b);
        return x != null && x.equals(y);
    }

    /**
     * A task is on time if it is finished at or before its deadline. A task whose deadline cannot be
     * read gets the benefit of the doubt.
     */
    public static boolean isOnTime(String date, String time, long nowMillis, ZoneId zone) {
        OptionalLong deadline = deadlineMillis(date, time, zone);
        return !deadline.isPresent() || nowMillis <= deadline.getAsLong();
    }

    public static boolean isOnTime(String date, String time, long nowMillis) {
        return isOnTime(date, time, nowMillis, ZoneId.systemDefault());
    }

    public static DeadlineCheck checkDeadline(String date, String time, long nowMillis, ZoneId zone) {
        OptionalLong deadline = deadlineMillis(date, time, zone);
        if (!deadline.isPresent()) return DeadlineCheck.INVALID_FORMAT;
        return deadline.getAsLong() < nowMillis - PAST_GRACE_MILLIS ? DeadlineCheck.IN_PAST : DeadlineCheck.OK;
    }

    public static DeadlineCheck checkDeadline(String date, String time, long nowMillis) {
        return checkDeadline(date, time, nowMillis, ZoneId.systemDefault());
    }

    /** Compares calendar days, not instants: today is never "in the past". */
    public static boolean isDateInPast(LocalDate picked, LocalDate today) {
        return picked.isBefore(today);
    }

    /**
     * Orders tasks by deadline, earliest first. Tasks whose deadline cannot be read sort last.
     * Usable directly as the body of a {@link java.util.Comparator}.
     */
    public static int compareDeadlines(String dateA, String timeA, String dateB, String timeB) {
        OptionalLong a = deadlineMillis(dateA, timeA, ZoneOffset.UTC);
        OptionalLong b = deadlineMillis(dateB, timeB, ZoneOffset.UTC);
        if (a.isPresent() && b.isPresent()) return Long.compare(a.getAsLong(), b.getAsLong());
        if (a.isPresent()) return -1;
        if (b.isPresent()) return 1;
        return 0;
    }

    /** Returns {day, month, year} exactly as written, or three empty strings if the date is invalid. */
    public static String[] splitForDisplay(String date) {
        if (parseDate(date) == null) return new String[]{"", "", ""};
        String[] parts = date.trim().split("/");
        return new String[]{parts[0], parts[1], parts[2]};
    }
}
