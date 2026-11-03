package com.example.todolist.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ActivityStatsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    private static Map<String, Integer> days(Object... pairs) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(ActivityStats.dayKey(TODAY.minusDays((Integer) pairs[i])), (Integer) pairs[i + 1]);
        }
        return map;
    }

    @Test
    public void keysAreIsoDatesAndRoundTrip() {
        assertEquals("2026-11-01", ActivityStats.dayKey(TODAY));
        assertEquals(TODAY, ActivityStats.parseKey("2026-11-01"));
        assertNull(ActivityStats.parseKey("01/11/2026"));
        assertNull(ActivityStats.parseKey(null));
    }

    @Test
    public void lastDaysIsOldestFirstAndFillsGapsWithZero() {
        Map<String, Integer> map = days(0, 3, 2, 1, 6, 4, 9, 99);
        assertArrayEquals(new int[]{4, 0, 0, 0, 1, 0, 3}, ActivityStats.lastDays(map, TODAY, 7));
        assertArrayEquals(new int[0], ActivityStats.lastDays(map, TODAY, 0));
        assertArrayEquals(new int[]{0}, ActivityStats.lastDays(null, TODAY, 1));
    }

    @Test
    public void negativeStoredValuesCountAsZero() {
        assertEquals(0, ActivityStats.on(days(0, -5), TODAY));
    }

    @Test
    public void sumAndPeak() {
        assertEquals(8, ActivityStats.sum(new int[]{4, 0, 1, 3}));
        assertEquals(0, ActivityStats.peakIndex(new int[]{4, 0, 1, 4}));
        assertEquals(-1, ActivityStats.peakIndex(new int[]{0, 0}));
        assertEquals(-1, ActivityStats.peakIndex(new int[0]));
    }

    @Test
    public void streakCountsBackFromToday() {
        assertEquals(3, ActivityStats.currentStreak(days(0, 1, 1, 5, 2, 2, 4, 9), TODAY));
    }

    @Test
    public void streakSurvivesAnEmptyTodayIfYesterdayWasActive() {
        assertEquals(2, ActivityStats.currentStreak(days(1, 1, 2, 1), TODAY));
    }

    @Test
    public void streakIsZeroAfterTwoQuietDays() {
        assertEquals(0, ActivityStats.currentStreak(days(2, 4, 3, 1), TODAY));
        assertEquals(0, ActivityStats.currentStreak(null, TODAY));
    }

    @Test
    public void bestStreakFindsTheLongestRunAnywhere() {
        Map<String, Integer> map = days(0, 1, 1, 1, 5, 1, 6, 1, 7, 1, 8, 1, 20, 1);
        assertEquals(4, ActivityStats.bestStreak(map));
        assertEquals(0, ActivityStats.bestStreak(null));
        assertEquals(1, ActivityStats.bestStreak(days(3, 2)));
    }

    @Test
    public void bestStreakIgnoresZerosAndBadKeys() {
        Map<String, Integer> map = days(0, 1, 1, 0, 2, 1);
        map.put("garbage", 7);
        assertEquals(1, ActivityStats.bestStreak(map));
    }

    @Test
    public void completionPercentUsesFinishedAndOpen() {
        assertEquals(0, ActivityStats.completionPercent(0, 0));
        assertEquals(50, ActivityStats.completionPercent(5, 5));
        assertEquals(83, ActivityStats.completionPercent(5, 1));
        assertEquals(100, ActivityStats.completionPercent(3, 0));
        assertEquals(0, ActivityStats.completionPercent(-4, 2));
    }
}
