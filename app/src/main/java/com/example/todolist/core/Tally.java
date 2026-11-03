package com.example.todolist.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Counts equal keys and lists them biggest first (equal counts keep the order in which they first appeared). */
public final class Tally {

    public static final class Entry {
        public final String key;
        public final int count;

        Entry(String key, int count) {
            this.key = key;
            this.count = count;
        }
    }

    private Tally() {
    }

    /** Null keys are skipped. */
    public static List<Entry> count(List<String> keys) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (keys != null) {
            for (String key : keys) {
                if (key == null) continue;
                Integer current = counts.get(key);
                counts.put(key, current == null ? 1 : current + 1);
            }
        }
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) entries.add(new Entry(e.getKey(), e.getValue()));
        // Stable sort: ties stay in first-seen order.
        entries.sort((a, b) -> Integer.compare(b.count, a.count));
        return entries;
    }
}
