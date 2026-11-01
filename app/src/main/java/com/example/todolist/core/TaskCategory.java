package com.example.todolist.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The built-in task categories. A task stores the language-independent {@link #key()} (for example
 * {@code "work"}), so changing the phone language translates old tasks too. Versions before 1.3.0 stored
 * the translated label ("Робота", "Work", ...); {@link #fromStored(String)} still recognises those, so
 * existing tasks need no database migration.
 */
public enum TaskCategory {
    WORK("work", "Work", "Робота", "Travail", "Arbeit", "仕事", "작업"),
    PERSONAL("personal", "Personal", "Особисті справи", "Personnel", "Persönlich", "個人", "개인"),
    STUDY("study", "Study", "Навчання", "Études", "Studium", "勉強", "공부"),
    HEALTH("health", "Health", "Здоров'я", "Santé", "Gesundheit", "健康", "건강"),
    FAMILY("family", "Family", "Сім'я", "Famille", "Familie", "家族", "가족"),
    HOBBY("hobby", "Hobby", "Хобі", "Loisir", "Hobby", "趣味", "취미");

    private final String key;
    private final String[] legacyLabels;

    TaskCategory(String key, String... legacyLabels) {
        this.key = key;
        this.legacyLabels = legacyLabels;
    }

    /** What is written to the database. */
    public String key() {
        return key;
    }

    private static final Map<String, TaskCategory> LOOKUP = new HashMap<>();

    static {
        for (TaskCategory category : values()) {
            LOOKUP.put(fold(category.key), category);
            for (String label : category.legacyLabels) LOOKUP.put(fold(label), category);
        }
    }

    /**
     * @param stored the value found in the database: a key, a label of any app language, free text or null
     * @return the matching category, or null for free text / empty values
     */
    public static TaskCategory fromStored(String stored) {
        if (stored == null) return null;
        return LOOKUP.get(fold(stored));
    }

    /** True when the value is not one of the built-in categories (and not empty), i.e. user text from an old version. */
    public static boolean isCustom(String stored) {
        return stored != null && !stored.trim().isEmpty() && fromStored(stored) == null;
    }

    /** The value to write for a stored category: the key for built-ins, the trimmed text otherwise. */
    public static String normalize(String stored) {
        if (stored == null) return "";
        TaskCategory category = fromStored(stored);
        return category != null ? category.key : stored.trim();
    }

    /** Case-insensitive and tolerant to the typographic apostrophe, which keyboards often substitute. */
    private static String fold(String text) {
        return text.trim()
                .replace('’', '\'').replace('ʼ', '\'').replace('`', '\'')
                .toLowerCase(Locale.ROOT);
    }
}
