package com.example.todolist.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * "Insertable hints" for the e-mail field: once the user has typed the part before the "@", the common
 * providers are offered so the rest does not have to be typed by hand.
 */
public final class EmailSuggestions {

    /** Most common providers first; the order is the order of the suggestions. */
    static final String[] DOMAINS = {
            "gmail.com", "ukr.net", "i.ua", "meta.ua", "icloud.com", "outlook.com", "yahoo.com", "hotmail.com"
    };

    private static final int MAX_SUGGESTIONS = 5;

    private EmailSuggestions() {
    }

    /**
     * @param input what is currently typed in the field
     * @return complete addresses to offer, e.g. {@code "anna@"} gives {@code anna@gmail.com, anna@ukr.net, ...};
     * empty when nothing sensible can be offered (no "@", empty name, or the domain is already complete)
     */
    public static List<String> suggest(String input) {
        if (input == null) return Collections.emptyList();
        String text = input.trim();
        int at = text.indexOf('@');
        // Offer nothing before the "@", for a second "@", or for an empty name.
        if (at <= 0 || at != text.lastIndexOf('@')) return Collections.emptyList();
        String name = text.substring(0, at);
        if (name.indexOf(' ') >= 0) return Collections.emptyList();
        String typedDomain = text.substring(at + 1).toLowerCase(Locale.ROOT);

        List<String> result = new ArrayList<>();
        for (String domain : DOMAINS) {
            if (!domain.startsWith(typedDomain)) continue;
            // The user already typed the whole domain: nothing left to complete.
            if (domain.equals(typedDomain)) return Collections.emptyList();
            result.add(name + "@" + domain);
            if (result.size() == MAX_SUGGESTIONS) break;
        }
        return result;
    }
}
