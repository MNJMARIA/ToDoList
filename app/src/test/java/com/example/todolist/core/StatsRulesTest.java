package com.example.todolist.core;

import static org.junit.Assert.assertEquals;

import com.example.todolist.utils.model.Stats;
import org.junit.Test;

public class StatsRulesTest {

    private static void assertStats(Stats s, int total, int completed, int toDo, int scores) {
        assertEquals("totalTasks", total, s.getTotalTasks());
        assertEquals("completedTasks", completed, s.getCompletedTasks());
        assertEquals("tasksToDo", toDo, s.getTasksToDo());
        assertEquals("totalScores", scores, s.getTotalScores());
    }

    @Test
    public void initial_isAllZeroAndKeepsId() {
        Stats s = StatsRules.initial("user-1");
        assertEquals("user-1", s.getStatsId());
        assertStats(s, 0, 0, 0, 0);
    }

    @Test
    public void pointsPerTask_matchesReadme() {
        assertEquals(25, StatsRules.POINTS_PER_TASK);
    }

    @Test
    public void taskAdded_increasesTotalAndToDo() {
        Stats s = StatsRules.initial("u");
        StatsRules.onTaskAdded(s);
        StatsRules.onTaskAdded(s);
        assertStats(s, 2, 0, 2, 0);
    }

    @Test
    public void taskCompletedOnTime_awardsPointsAndKeepsTotal() {
        Stats s = new Stats("u", 3, 0, 3, 0);
        StatsRules.onTaskCompleted(s, true);
        assertStats(s, 3, 1, 2, 25);
    }

    @Test
    public void taskCompletedLate_awardsNoPoints() {
        Stats s = new Stats("u", 3, 0, 3, 100);
        StatsRules.onTaskCompleted(s, false);
        assertStats(s, 3, 1, 2, 100);
    }

    /** Regression: completing a task used to shrink "total tasks" through the delayed delete call. */
    @Test
    public void completingDoesNotChangeTotal() {
        Stats s = new Stats("u", 5, 0, 5, 0);
        StatsRules.onTaskCompleted(s, true);
        assertEquals(5, s.getTotalTasks());
    }

    @Test
    public void taskDeleted_decreasesTotalAndToDoOnly() {
        Stats s = new Stats("u", 4, 1, 3, 25);
        StatsRules.onTaskDeleted(s);
        assertStats(s, 3, 1, 2, 25);
    }

    @Test
    public void countersNeverGoNegative() {
        Stats s = new Stats("u", 0, 0, 0, 0);
        StatsRules.onTaskDeleted(s);
        StatsRules.onTaskCompleted(s, false);
        assertEquals(0, s.getTotalTasks());
        assertEquals(0, s.getTasksToDo());
    }

    @Test
    public void realisticSequence() {
        Stats s = StatsRules.initial("u");
        StatsRules.onTaskAdded(s);                // 1 task
        StatsRules.onTaskAdded(s);                // 2 tasks
        StatsRules.onTaskAdded(s);                // 3 tasks
        StatsRules.onTaskCompleted(s, true);      // one done in time
        StatsRules.onTaskCompleted(s, false);     // one done late
        StatsRules.onTaskDeleted(s);              // last one removed without finishing
        assertStats(s, 2, 2, 0, 25);
    }

    // ---------- reconcile (self-healing of data damaged by old bugs) ----------

    @Test
    public void reconcile_trustsActualTaskCountOverStoredCounters() {
        Stats s = new Stats("u", 7, 3, 9, 150);
        StatsRules.reconcile(s, 4);
        assertStats(s, 7, 3, 4, 150);
    }

    @Test
    public void reconcile_upgradesLegacyTotalThatExcludedCompleted() {
        // legacy semantics: total == pending, completed tracked separately
        Stats s = new Stats("u", 2, 5, 2, 125);
        StatsRules.reconcile(s, 2);
        assertStats(s, 7, 5, 2, 125);
    }

    @Test
    public void reconcile_clampsNegativeValuesLeftByOldBugs() {
        Stats s = new Stats("u", -3, -1, -3, -25);
        StatsRules.reconcile(s, 0);
        assertStats(s, 0, 0, 0, 0);
    }

    @Test
    public void reconcile_ignoresNegativeActualCount() {
        Stats s = new Stats("u", 1, 0, 1, 0);
        StatsRules.reconcile(s, -5);
        assertStats(s, 0, 0, 0, 0);
    }
}
