package com.example.todolist.core;

import com.example.todolist.utils.model.Stats;

/**
 * How the per-user counters change when tasks are added, finished or deleted.
 *
 * <p>Semantics (match the README): {@code totalTasks} = every task the user still has, finished or
 * not; {@code tasksToDo} = unfinished tasks; {@code completedTasks} = finished tasks. Finishing a
 * task never changes the total; deleting an unfinished task lowers total and to-do. Counters never
 * drop below zero.
 *
 * <p>All methods mutate the given object in place so they can be used inside a Firebase transaction.
 */
public final class StatsRules {

    public static final int POINTS_PER_TASK = 25;

    private StatsRules() {
    }

    public static Stats initial(String userId) {
        return new Stats(userId, 0, 0, 0, 0);
    }

    public static void onTaskAdded(Stats s) {
        s.setTotalTasks(nonNegative(s.getTotalTasks()) + 1);
        s.setTasksToDo(nonNegative(s.getTasksToDo()) + 1);
    }

    public static void onTaskCompleted(Stats s, boolean onTime) {
        s.setCompletedTasks(nonNegative(s.getCompletedTasks()) + 1);
        s.setTasksToDo(nonNegative(s.getTasksToDo() - 1));
        if (onTime) {
            s.setTotalScores(nonNegative(s.getTotalScores()) + POINTS_PER_TASK);
        }
    }

    public static void onTaskDeleted(Stats s) {
        s.setTotalTasks(nonNegative(s.getTotalTasks() - 1));
        s.setTasksToDo(nonNegative(s.getTasksToDo() - 1));
    }

    /**
     * Repairs counters using the real number of unfinished tasks in the database. Heals data that
     * older versions damaged (negative numbers, double decrements, "total" that excluded finished tasks).
     */
    public static void reconcile(Stats s, int actualPendingTasks) {
        int pending = nonNegative(actualPendingTasks);
        int completed = nonNegative(s.getCompletedTasks());
        s.setTasksToDo(pending);
        s.setCompletedTasks(completed);
        s.setTotalTasks(pending + completed);
        s.setTotalScores(nonNegative(s.getTotalScores()));
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }
}
