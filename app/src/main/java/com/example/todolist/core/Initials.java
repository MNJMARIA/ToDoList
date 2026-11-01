package com.example.todolist.core;

import java.util.Locale;

/** The letters shown in the round profile picture: "Mariia Stepanova" gives "MS". */
public final class Initials {

    private Initials() {
    }

    /**
     * First letters of the first and the last word of the name; if there is no name, the first letter of the e-mail;
     * "?" when there is nothing at all.
     */
    public static String of(String name, String email) {
        String fromName = fromWords(name);
        if (!fromName.isEmpty()) return fromName;
        String fromEmail = fromWords(ProfileNames.localPart(email));
        return fromEmail.isEmpty() ? "?" : fromEmail.substring(0, firstCodePointLength(fromEmail));
    }

    /** First letter of the first word and of the last word ("Anna Maria Boyko" gives "AB"). */
    private static String fromWords(String text) {
        if (text == null) return "";
        int first = -1;
        int last = -1;
        // Words are separated by spaces and the usual e-mail/name separators.
        for (String word : text.trim().split("[\\s._\\-]+")) {
            if (word.isEmpty()) continue;
            int cp = word.codePointAt(0);
            if (!Character.isLetterOrDigit(cp)) continue;
            if (first < 0) first = cp;
            last = cp;
        }
        if (first < 0) return "";
        StringBuilder sb = new StringBuilder().appendCodePoint(first);
        if (last != first || countWords(text) > 1) sb.appendCodePoint(last);
        return sb.toString().toUpperCase(Locale.ROOT);
    }

    private static int countWords(String text) {
        int n = 0;
        for (String word : text.trim().split("[\\s._\\-]+")) {
            if (!word.isEmpty() && Character.isLetterOrDigit(word.codePointAt(0))) n++;
        }
        return n;
    }

    private static int firstCodePointLength(String s) {
        return Character.charCount(s.codePointAt(0));
    }
}
