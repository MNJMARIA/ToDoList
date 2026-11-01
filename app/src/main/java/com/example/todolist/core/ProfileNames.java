package com.example.todolist.core;

/**
 * Picks the display name and the username for a profile that is created automatically (sign-in with
 * Google, or a missing database record) instead of being typed in the sign-up form.
 */
public final class ProfileNames {

    /** Same limits as the sign-up form. */
    public static final int MAX_NAME = 50;
    public static final int MAX_USERNAME = 30;

    private ProfileNames() {
    }

    /** The part of the address before "@"; the whole text when there is no "@". */
    public static String localPart(String email) {
        if (email == null) return "";
        String trimmed = email.trim();
        int at = trimmed.indexOf('@');
        return at > 0 ? trimmed.substring(0, at) : trimmed;
    }

    /** The account's own name if it has one, otherwise the part of the e-mail before "@". */
    public static String displayName(String accountName, String email) {
        String name = InputValidator.normalizeTitle(accountName);
        if (name.isEmpty()) name = localPart(email);
        return cut(name, MAX_NAME);
    }

    /** Short handle derived from the e-mail; never empty as long as the e-mail is not. */
    public static String username(String email) {
        return cut(localPart(email), MAX_USERNAME);
    }

    private static String cut(String s, int max) {
        if (s.length() <= max) return s;
        int end = max;
        if (Character.isHighSurrogate(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }
}
