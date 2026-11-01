package com.example.todolist.core;

import java.util.regex.Pattern;

/** Makes GitHub release notes (Markdown) readable in a plain Android dialog. */
public final class ReleaseNotes {

    private static final Pattern HEADING = Pattern.compile("^\\s{0,3}#{1,6}\\s*");
    private static final Pattern BULLET = Pattern.compile("^\\s*[-*+]\\s+");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)\\]\\([^)]*\\)");
    private static final Pattern EMPHASIS = Pattern.compile("\\*\\*|__|`");
    private static final Pattern MANY_BLANK_LINES = Pattern.compile("\n{3,}");

    private ReleaseNotes() {
    }

    public static String toPlainText(String markdown, int maxChars) {
        if (markdown == null || maxChars <= 0) return "";
        String text = markdown.replace("\r\n", "\n").replace('\r', '\n');

        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            String l = HEADING.matcher(line).replaceFirst("");
            l = BULLET.matcher(l).replaceFirst("• ");
            l = LINK.matcher(l).replaceAll("$1");
            l = EMPHASIS.matcher(l).replaceAll("");
            out.append(l.trim()).append('\n');
        }
        String result = MANY_BLANK_LINES.matcher(out.toString()).replaceAll("\n\n").trim();

        if (result.length() <= maxChars) return result;
        int cut = maxChars - 1;
        if (cut > 0 && Character.isHighSurrogate(result.charAt(cut - 1))) cut--;
        return rtrim(result.substring(0, cut)) + "…";
    }

    private static String rtrim(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }
}
