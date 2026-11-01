package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ReminderPlannerTest {

    private static final long HOUR = 60L * 60 * 1000;
    private static final long DAY = 24 * HOUR;
    private static final long NOW = 1_800_000_000_000L;

    @Test
    public void farDeadline_remindsThreeDaysBefore() {
        long deadline = NOW + 10 * DAY;
        ReminderPlanner.Plan p = ReminderPlanner.plan(deadline, NOW);
        assertNotNull(p);
        assertEquals(deadline - 3 * DAY, p.triggerAtMillis);
        assertEquals(ReminderPlanner.Kind.THREE_DAYS_BEFORE, p.kind);
    }

    @Test
    public void deadlineJustOverThreeDays_stillUsesThreeDayReminder() {
        long deadline = NOW + 3 * DAY + 1;
        ReminderPlanner.Plan p = ReminderPlanner.plan(deadline, NOW);
        assertNotNull(p);
        assertEquals(ReminderPlanner.Kind.THREE_DAYS_BEFORE, p.kind);
        assertEquals(NOW + 1, p.triggerAtMillis);
    }

    /** Regression: the old code scheduled a "3 days" alarm in the past, so it fired immediately. */
    @Test
    public void deadlineWithinThreeDays_fallsBackToOneHourBefore() {
        long deadline = NOW + DAY;
        ReminderPlanner.Plan p = ReminderPlanner.plan(deadline, NOW);
        assertNotNull(p);
        assertEquals(ReminderPlanner.Kind.ONE_HOUR_BEFORE, p.kind);
        assertEquals(deadline - HOUR, p.triggerAtMillis);
    }

    @Test
    public void deadlineExactlyThreeDaysAway_usesOneHourReminder() {
        ReminderPlanner.Plan p = ReminderPlanner.plan(NOW + 3 * DAY, NOW);
        assertNotNull(p);
        assertEquals(ReminderPlanner.Kind.ONE_HOUR_BEFORE, p.kind);
    }

    @Test
    public void deadlineLessThanAnHourAway_hasNoReminder() {
        assertNull(ReminderPlanner.plan(NOW + 30 * 60 * 1000L, NOW));
        assertNull(ReminderPlanner.plan(NOW + HOUR, NOW));
    }

    @Test
    public void pastOrCurrentDeadline_hasNoReminder() {
        assertNull(ReminderPlanner.plan(NOW, NOW));
        assertNull(ReminderPlanner.plan(NOW - 1, NOW));
        assertNull(ReminderPlanner.plan(0, NOW));
    }

    @Test
    public void requestCode_isStablePerTaskAndDiffersBetweenTasks() {
        assertEquals(ReminderPlanner.requestCode("-Nabc123"), ReminderPlanner.requestCode("-Nabc123"));
        assertNotEquals(ReminderPlanner.requestCode("-Nabc123"), ReminderPlanner.requestCode("-Nabc124"));
        assertEquals("abc".hashCode(), ReminderPlanner.requestCode("abc"));
    }

    /** Regression: every task used request code 0, so a new reminder replaced the previous one. */
    @Test
    public void requestCode_isNotAConstant() {
        assertNotEquals(ReminderPlanner.requestCode("task-1"), ReminderPlanner.requestCode("task-2"));
    }

    @Test
    public void requestCode_nullIsZero() {
        assertEquals(0, ReminderPlanner.requestCode(null));
    }
}
