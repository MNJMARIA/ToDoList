package com.example.todolist.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the answer of the Realtime Database REST endpoint ({@code <database url>/.json}) to explain why the
 * app cannot connect. The wording of the server's messages is matched loosely and the raw answer is always
 * shown to the user too, so a wrong guess never hides the real text.
 */
public final class DbProbe {

    public enum Verdict {
        /** HTTP 200: reachable, and the rules let anyone read (a security problem worth telling about). */
        OPEN_ACCESS,
        /** HTTP 401/403 "Permission denied": the database exists and the rules are active. */
        RULES_DENY,
        /** The address points to no database (never created, or deleted). */
        NO_DATABASE,
        /** The database exists in another region than the one in google-services.json. */
        WRONG_REGION,
        /** The instance exists but was switched off ("has been deactivated"); it must be re-enabled. */
        DEACTIVATED,
        /** No answer at all (no internet, DNS, firewall, VPN, timeout). */
        UNREACHABLE,
        UNKNOWN
    }

    private static final Pattern DB_URL =
            Pattern.compile("https://[A-Za-z0-9._-]+\\.(?:firebaseio\\.com|firebasedatabase\\.app)");

    private DbProbe() {
    }

    /**
     * @param status HTTP status, or a negative number when there was no HTTP answer
     * @param body   response text, may be null
     */
    public static Verdict diagnose(int status, String body) {
        if (status < 0) return Verdict.UNREACHABLE;
        String text = body == null ? "" : body.toLowerCase(Locale.ROOT);
        if (text.contains("has been deactivated")) return Verdict.DEACTIVATED;
        if (text.contains("different region")) return Verdict.WRONG_REGION;
        if (text.contains("ensure that you have the url") || text.contains("instance configured correctly")
                || status == 404) {
            return Verdict.NO_DATABASE;
        }
        if (status == 200) return Verdict.OPEN_ACCESS;
        if (status == 401 || status == 403 || text.contains("permission denied")) return Verdict.RULES_DENY;
        return Verdict.UNKNOWN;
    }

    /** The database address the server says is the right one ("change your database URL to ..."), or null. */
    public static String suggestedUrl(String body) {
        if (body == null) return null;
        Matcher m = DB_URL.matcher(body);
        return m.find() ? m.group() : null;
    }

    /** A short single-paragraph version of a server answer for display. */
    public static String excerpt(String body, int maxChars) {
        if (body == null) return "";
        String flat = body.replaceAll("\\s+", " ").trim();
        if (flat.length() <= maxChars) return flat;
        return flat.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    /** {@code https://x.firebasedatabase.app/} or {@code https://x.firebasedatabase.app} to the probe address. */
    public static String probeUrl(String databaseUrl) {
        if (databaseUrl == null) return null;
        String base = databaseUrl.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base.isEmpty() ? null : base + "/.json?shallow=true";
    }
}
