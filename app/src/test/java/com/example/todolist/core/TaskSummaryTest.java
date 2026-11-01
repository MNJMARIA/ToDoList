package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class TaskSummaryTest {

    private static final ZoneId ZONE = ZoneOffset.UTC;

    private static final TaskQuery.Accessor<String[]> ACC = new TaskQuery.Accessor<String[]>() {
        public String title(String[] t) { return t[0]; }
        public String categoryLabel(String[] t) { return ""; }
        public int priorityRank(String[] t) { return 0; }
        public String date(String[] t) { return t[1]; }
        public String time(String[] t) { return t[2]; }
    };

    private static long at(String date, String time) {
        return TaskDateTime.deadlineMillis(date, time, ZONE).getAsLong();
    }

    @Test
    public void overdueMeansTheDeadlineHasPassed() {
        long now = at("10/05/2030", "12:00");
        assertTrue(TaskSummary.isOverdue("10/05/2030", "11:59", now, ZONE));
        assertFalse(TaskSummary.isOverdue("10/05/2030", "12:00", now, ZONE));
        assertFalse(TaskSummary.isOverdue("10/05/2030", "12:01", now, ZONE));
        assertTrue(TaskSummary.isOverdue("9/5/2030", "23:59", now, ZONE));
    }

    @Test
    public void unreadableDeadlinesAreNeverOverdue() {
        long now = at("10/05/2030", "12:00");
        assertFalse(TaskSummary.isOverdue("", "", now, ZONE));
        assertFalse(TaskSummary.isOverdue(null, null, now, ZONE));
        assertFalse(TaskSummary.isOverdue("10/05/2030", "bad", now, ZONE));
    }

    @Test
    public void countsTodayOverdueAndTotal() {
        LocalDate todayDate = LocalDate.now(ZONE);
        String today = TaskDateTime.formatDate(todayDate);
        String yesterday = TaskDateTime.formatDate(todayDate.minusDays(1));
        String tomorrow = TaskDateTime.formatDate(todayDate.plusDays(1));
        long now = System.currentTimeMillis();
        List<String[]> items = new ArrayList<>(Arrays.asList(
                new String[]{"a", yesterday, "10:00"},   // overdue
                new String[]{"b", today, "23:59"},       // today (may or may not be overdue, see below)
                new String[]{"c", tomorrow, "10:00"},    // upcoming
                new String[]{"d", "", ""}));             // no deadline
        TaskSummary s = TaskSummary.of(items, ACC, now, ZONE);
        assertEquals(4, s.total);
        assertEquals(1, s.today);
        assertTrue(s.overdue >= 1 && s.overdue <= 1 + (TaskSummary.isOverdue(today, "23:59", now, ZONE) ? 1 : 0));
    }

    @Test
    public void emptyListGivesZeros() {
        TaskSummary s = TaskSummary.of(Collections.<String[]>emptyList(), ACC, 0L, ZONE);
        assertEquals(0, s.total);
        assertEquals(0, s.today);
        assertEquals(0, s.overdue);
    }
}
