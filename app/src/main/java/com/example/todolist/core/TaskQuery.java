package com.example.todolist.core;

import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Search and ordering of a task list, kept free of Android types so it is unit-tested on the JVM.
 * The caller describes how to read a task through {@link Accessor}.
 */
public final class TaskQuery {

    public enum Sort {
        /** As the database returns them (creation order). */
        DEFAULT,
        /** Earliest deadline first; tasks without a readable deadline last. */
        DEADLINE,
        /** Urgent and important first; ties by deadline. */
        PRIORITY,
        /** Alphabetical by title. */
        TITLE
    }

    public interface Accessor<T> {
        String title(T item);

        /** The category as the user sees it (translated), so searching "робота" finds a category "work". */
        String categoryLabel(T item);

        /** 0 = most important; any negative or large value sorts last. */
        int priorityRank(T item);

        String date(T item);

        String time(T item);
    }

    private TaskQuery() {
    }

    /**
     * Keeps the items whose title or category contains EVERY word of {@code query} (any order, ignoring case
     * and accents), then orders them. The input list is not modified.
     */
    public static <T> List<T> apply(List<T> items, String query, Sort sort, Accessor<T> accessor, Locale locale) {
        List<String> words = words(query);
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (matches(item, words, accessor)) result.add(item);
        }
        Comparator<T> order = comparator(sort == null ? Sort.DEFAULT : sort, accessor, locale);
        if (order != null) Collections.sort(result, order); // stable: equal items keep their relative order
        return result;
    }

    private static <T> boolean matches(T item, List<String> words, Accessor<T> accessor) {
        if (words.isEmpty()) return true;
        String haystack = fold(nullToEmpty(accessor.title(item)) + " " + nullToEmpty(accessor.categoryLabel(item)));
        for (String word : words) {
            if (!haystack.contains(word)) return false;
        }
        return true;
    }

    private static <T> Comparator<T> comparator(Sort sort, final Accessor<T> a, Locale locale) {
        final Comparator<T> byDeadline = new Comparator<T>() {
            @Override
            public int compare(T x, T y) {
                return TaskDateTime.compareDeadlines(a.date(x), a.time(x), a.date(y), a.time(y));
            }
        };
        switch (sort) {
            case DEADLINE:
                return byDeadline;
            case PRIORITY:
                return new Comparator<T>() {
                    @Override
                    public int compare(T x, T y) {
                        int c = Integer.compare(rank(a.priorityRank(x)), rank(a.priorityRank(y)));
                        return c != 0 ? c : byDeadline.compare(x, y);
                    }
                };
            case TITLE:
                final Collator collator = Collator.getInstance(locale == null ? Locale.getDefault() : locale);
                collator.setStrength(Collator.SECONDARY); // case-insensitive, accent-sensitive
                return new Comparator<T>() {
                    @Override
                    public int compare(T x, T y) {
                        return collator.compare(nullToEmpty(a.title(x)).trim(), nullToEmpty(a.title(y)).trim());
                    }
                };
            default:
                return null;
        }
    }

    private static int rank(int value) {
        return value < 0 ? Integer.MAX_VALUE : value;
    }

    static List<String> words(String query) {
        List<String> result = new ArrayList<>();
        if (query == null) return result;
        for (String part : fold(query).split("\\s+")) {
            if (!part.isEmpty()) result.add(part);
        }
        return result;
    }

    /**
     * Lower case, and accents removed from Latin letters only: "Études" matches "etudes". Other scripts keep
     * their marks (Ukrainian "й" is not "и", Japanese "が" is not "か").
     */
    static String fold(String text) {
        String decomposed = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        StringBuilder sb = new StringBuilder(decomposed.length());
        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            boolean accentOnLatin = Character.getType(c) == Character.NON_SPACING_MARK
                    && sb.length() > 0
                    && Character.UnicodeScript.of(sb.charAt(sb.length() - 1)) == Character.UnicodeScript.LATIN;
            if (!accentOnLatin) sb.append(c);
        }
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
