package com.example.todolist.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AchievementRulesTest {

    @Test
    public void thresholds_matchReadmeAndAreStrictlyAscending() {
        int[] t = AchievementRules.thresholds();
        assertEquals(12, t.length);
        assertEquals(500, t[0]);
        assertEquals(100000, t[11]);
        for (int i = 1; i < t.length; i++) {
            assertTrue("thresholds must ascend at index " + i, t[i] > t[i - 1]);
        }
    }

    @Test
    public void thresholds_returnsDefensiveCopy() {
        int[] t = AchievementRules.thresholds();
        t[0] = -1;
        assertEquals(500, AchievementRules.thresholds()[0]);
    }

    @Test
    public void titles_areThresholdsAsStrings() {
        assertArrayEquals(
                new String[]{"500", "1000", "2000", "5000", "7500", "10000", "15000", "25000", "30000", "50000", "70000", "100000"},
                AchievementRules.titles());
    }

    @Test
    public void unlockedCount() {
        assertEquals(0, AchievementRules.unlockedCount(0));
        assertEquals(0, AchievementRules.unlockedCount(499));
        assertEquals(1, AchievementRules.unlockedCount(500));
        assertEquals(1, AchievementRules.unlockedCount(999));
        assertEquals(2, AchievementRules.unlockedCount(1000));
        assertEquals(12, AchievementRules.unlockedCount(100000));
        assertEquals(12, AchievementRules.unlockedCount(5_000_000));
        assertEquals(0, AchievementRules.unlockedCount(-10));
    }

    @Test
    public void isUnlocked_respectsIndexBounds() {
        assertTrue(AchievementRules.isUnlocked(0, 500));
        assertFalse(AchievementRules.isUnlocked(1, 500));
        assertFalse(AchievementRules.isUnlocked(-1, 500));
        assertFalse(AchievementRules.isUnlocked(12, 1_000_000));
    }

    @Test
    public void levelIndex_isHighestUnlockedButNeverBelowZero() {
        assertEquals(0, AchievementRules.levelIndex(0));
        assertEquals(0, AchievementRules.levelIndex(499));
        assertEquals(0, AchievementRules.levelIndex(500));
        assertEquals(0, AchievementRules.levelIndex(999));
        assertEquals(1, AchievementRules.levelIndex(1000));
        assertEquals(2, AchievementRules.levelIndex(2000));
        assertEquals(9, AchievementRules.levelIndex(50000));
        assertEquals(11, AchievementRules.levelIndex(100000));
        assertEquals(11, AchievementRules.levelIndex(150000));
    }

    @Test
    public void newlyUnlocked_whenCrossingAThreshold() {
        assertEquals(0, AchievementRules.newlyUnlockedIndex(475, 500));
        assertEquals(1, AchievementRules.newlyUnlockedIndex(990, 1010));
        assertEquals(11, AchievementRules.newlyUnlockedIndex(99975, 100000));
    }

    /** Regression: the old check used `score == threshold`, so a late task at exactly 500 re-showed the dialog. */
    @Test
    public void newlyUnlocked_noneWhenScoreDidNotMove() {
        assertEquals(-1, AchievementRules.newlyUnlockedIndex(500, 500));
        assertEquals(-1, AchievementRules.newlyUnlockedIndex(1000, 1000));
        assertEquals(-1, AchievementRules.newlyUnlockedIndex(0, 0));
    }

    @Test
    public void newlyUnlocked_noneWhenAlreadyPastThreshold() {
        assertEquals(-1, AchievementRules.newlyUnlockedIndex(500, 525));
        assertEquals(-1, AchievementRules.newlyUnlockedIndex(100000, 100025));
    }

    @Test
    public void newlyUnlocked_noneWhenScoreDecreases() {
        assertEquals(-1, AchievementRules.newlyUnlockedIndex(525, 500));
        assertEquals(-1, AchievementRules.newlyUnlockedIndex(500, 475));
    }

    @Test
    public void newlyUnlocked_reportsHighestWhenSeveralCrossedAtOnce() {
        // e.g. stats repaired from a big jump
        assertEquals(9, AchievementRules.newlyUnlockedIndex(0, 50000));
        assertEquals(2, AchievementRules.newlyUnlockedIndex(400, 2100));
    }

    @Test
    public void newlyUnlocked_belowFirstThresholdIsNone() {
        assertEquals(-1, AchievementRules.newlyUnlockedIndex(0, 475));
        assertEquals(-1, AchievementRules.newlyUnlockedIndex(475, 499));
    }

    @Test
    public void nextThresholdIsTheFirstOneNotYetReached() {
        assertEquals(500, AchievementRules.nextThreshold(0));
        assertEquals(1000, AchievementRules.nextThreshold(500));
        assertEquals(100000, AchievementRules.nextThreshold(70000));
        assertEquals(-1, AchievementRules.nextThreshold(100000));
        assertEquals(-1, AchievementRules.nextThreshold(250000));
    }

    @Test
    public void progressToNextRunsFromLastThresholdToNext() {
        assertEquals(0, AchievementRules.progressToNextPercent(0));
        assertEquals(50, AchievementRules.progressToNextPercent(250));
        assertEquals(0, AchievementRules.progressToNextPercent(500));
        assertEquals(50, AchievementRules.progressToNextPercent(750));
        assertEquals(100, AchievementRules.progressToNextPercent(100000));
    }
}
