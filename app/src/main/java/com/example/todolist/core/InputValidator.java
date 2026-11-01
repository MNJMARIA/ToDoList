package com.example.todolist.core;

import java.util.regex.Pattern;

/** Form validation that does not need Android classes (so it can be tested and reused everywhere). */
public final class InputValidator {

    public static final int MAX_TITLE_LENGTH = 200;
    /** Firebase Authentication rejects shorter passwords. */
    public static final int MIN_PASSWORD_LENGTH = 6;

    private static final String LABEL = "[A-Za-z0-9](?:[A-Za-z0-9\\-]{0,61}[A-Za-z0-9])?";
    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9+._%\\-]{1,64}@" + LABEL + "(?:\\." + LABEL + ")+");
    private static final Pattern WHITESPACE_RUN = Pattern.compile("[\\s\\u00A0]+");

    private InputValidator() {
    }

    public static boolean isValidEmail(String s) {
        return s != null && EMAIL.matcher(s.trim()).matches();
    }

    /** Passwords are never trimmed (spaces may be intentional) but must not be blank. */
    public static boolean isValidPassword(String s) {
        return s != null && s.length() >= MIN_PASSWORD_LENGTH && !s.trim().isEmpty();
    }

    /** Trims, collapses runs of whitespace and caps the length. Returns "" for null. */
    public static String normalizeTitle(String s) {
        if (s == null) return "";
        String t = WHITESPACE_RUN.matcher(s.trim()).replaceAll(" ").trim();
        if (t.length() > MAX_TITLE_LENGTH) {
            int end = MAX_TITLE_LENGTH;
            if (Character.isHighSurrogate(t.charAt(end - 1))) end--;
            t = t.substring(0, end);
        }
        return t;
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
