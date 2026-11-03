package com.example.todolist.core;

/** The 12 achievement levels and everything derived from the user's score. */
public final class AchievementRules {

    private static final int[] THRESHOLDS = {
            500, 1000, 2000, 5000, 7500, 10000, 15000, 25000, 30000, 50000, 70000, 100000
    };

    private AchievementRules() {
    }

    public static int[] thresholds() {
        return THRESHOLDS.clone();
    }

    public static String[] titles() {
        String[] titles = new String[THRESHOLDS.length];
        for (int i = 0; i < THRESHOLDS.length; i++) {
            titles[i] = String.valueOf(THRESHOLDS[i]);
        }
        return titles;
    }

    public static int unlockedCount(int score) {
        int count = 0;
        for (int threshold : THRESHOLDS) {
            if (score >= threshold) count++;
        }
        return count;
    }

    public static boolean isUnlocked(int index, int score) {
        return index >= 0 && index < THRESHOLDS.length && score >= THRESHOLDS[index];
    }

    /** Index of the avatar/level to show: the highest unlocked one, or the first level before any is unlocked. */
    public static int levelIndex(int score) {
        return Math.max(0, unlockedCount(score) - 1);
    }

    /**
     * The achievement unlocked by moving from {@code previousScore} to {@code newScore}, or -1 if
     * none. When several thresholds are crossed at once the highest one is reported.
     */
    public static int newlyUnlockedIndex(int previousScore, int newScore) {
        if (newScore <= previousScore) return -1;
        for (int i = THRESHOLDS.length - 1; i >= 0; i--) {
            if (previousScore < THRESHOLDS[i] && THRESHOLDS[i] <= newScore) return i;
        }
        return -1;
    }

    /** The next score to reach, or -1 when the highest level is already unlocked. */
    public static int nextThreshold(int score) {
        for (int threshold : THRESHOLDS) {
            if (score < threshold) return threshold;
        }
        return -1;
    }

    /** Progress from the last unlocked threshold to the next one, 0..100; 100 once everything is unlocked. */
    public static int progressToNextPercent(int score) {
        int next = nextThreshold(score);
        if (next < 0) return 100;
        int previous = 0;
        for (int threshold : THRESHOLDS) {
            if (threshold <= score) previous = threshold;
        }
        int span = next - previous;
        return (int) ((100L * Math.max(0, score - previous)) / span);
    }
}
