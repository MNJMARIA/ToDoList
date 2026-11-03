package com.example.todolist.core;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;

/**
 * What the home-screen widget shows. The app keeps a small copy of the open tasks on the phone (the widget
 * cannot wait for the database), and this class turns that copy into the few lines of the widget.
 */
public final class WidgetData {

    public static final class Entry {
        public final String id;
        public final String title;
        public final String date;
        public final String time;

        public Entry(String id, String title, String date, String time) {
            this.id = id == null ? "" : id;
            this.title = title == null ? "" : title;
            this.date = date == null ? "" : date;
            this.time = time == null ? "" : time;
        }
    }

    public static final class Row {
        public final String title;
        /** "18:30" for a task of today, "25/06" for another day, empty when the date is unreadable. */
        public final String when;
        public final boolean overdue;

        Row(String title, String when, boolean overdue) {
            this.title = title;
            this.when = when;
            this.overdue = overdue;
        }
    }

    public static final class View {
        public final int today;
        public final int overdue;
        public final int total;
        public final List<Row> rows;

        View(int today, int overdue, int total, List<Row> rows) {
            this.today = today;
            this.overdue = overdue;
            this.total = total;
            this.rows = rows;
        }
    }

    private WidgetData() {
    }

    /** Overdue first, then by deadline; tasks without a readable deadline last. At most {@code maxRows} rows. */
    public static View build(List<Entry> entries, long nowMillis, ZoneId zone, int maxRows) {
        List<Entry> sorted = new ArrayList<>(entries == null ? Collections.<Entry>emptyList() : entries);
        sorted.sort((a, b) -> TaskDateTime.compareDeadlines(a.date, a.time, b.date, b.time));
        String todayText = TaskDateTime.formatDate(LocalDate.now(zone));
        int today = 0;
        int overdue = 0;
        List<Row> rows = new ArrayList<>();
        for (Entry entry : sorted) {
            boolean late = TaskSummary.isOverdue(entry.date, entry.time, nowMillis, zone);
            boolean isToday = TaskDateTime.sameDay(entry.date, todayText);
            if (late) overdue++;
            if (isToday) today++;
            if (rows.size() < maxRows) rows.add(new Row(entry.title, whenText(entry, isToday), late));
        }
        return new View(today, overdue, sorted.size(), rows);
    }

    private static String whenText(Entry entry, boolean isToday) {
        OptionalLong deadline = TaskDateTime.deadlineMillis(entry.date, entry.time, ZoneId.of("UTC"));
        if (!deadline.isPresent()) return "";
        if (isToday) {
            java.time.LocalTime time = TaskDateTime.parseTime(entry.time);
            return time == null ? "" : TaskDateTime.formatTime(time);
        }
        LocalDate date = TaskDateTime.parseDate(entry.date);
        if (date == null) return "";
        return String.format(java.util.Locale.ROOT, "%02d/%02d", date.getDayOfMonth(), date.getMonthValue());
    }

    // ------------------------------------------------------------ storing the copy as text

    /** One task per line, fields separated by tabs; tabs, line breaks and backslashes inside a field are escaped. */
    public static String encode(List<Entry> entries) {
        StringBuilder out = new StringBuilder();
        for (Entry entry : entries) {
            out.append(escape(entry.id)).append('\t').append(escape(entry.title)).append('\t')
                    .append(escape(entry.date)).append('\t').append(escape(entry.time)).append('\n');
        }
        return out.toString();
    }

    public static List<Entry> decode(String text) {
        List<Entry> entries = new ArrayList<>();
        if (text == null || text.isEmpty()) return entries;
        for (String line : text.split("\n", -1)) {
            if (line.isEmpty()) continue;
            String[] parts = line.split("\t", -1);
            if (parts.length != 4) continue;
            entries.add(new Entry(unescape(parts[0]), unescape(parts[1]), unescape(parts[2]), unescape(parts[3])));
        }
        return entries;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                switch (next) {
                    case 't':
                        out.append('\t');
                        break;
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    default:
                        out.append(next);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
