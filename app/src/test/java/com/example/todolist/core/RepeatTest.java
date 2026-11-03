package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import org.junit.Test;

public class RepeatTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    @Test
    public void storedValuesAreReadLeniently() {
        assertEquals(Repeat.DAILY, Repeat.fromStored("daily"));
        assertEquals(Repeat.WEEKLY, Repeat.fromStored(" Weekly "));
        assertEquals(Repeat.MONTHLY, Repeat.fromStored("MONTHLY"));
        assertEquals(Repeat.NONE, Repeat.fromStored(null));
        assertEquals(Repeat.NONE, Repeat.fromStored(""));
        assertEquals(Repeat.NONE, Repeat.fromStored("every other tuesday"));
        assertFalse(Repeat.NONE.repeats());
        assertTrue(Repeat.DAILY.repeats());
    }

    @Test
    public void keysRoundTrip() {
        for (Repeat repeat : Repeat.values()) assertEquals(repeat, Repeat.fromStored(repeat.key()));
    }

    @Test
    public void noRepeatHasNoNextDate() {
        assertNull(Repeat.NONE.next(TODAY, TODAY));
    }

    @Test
    public void finishedOnTimeGivesTheNormalNextDate() {
        assertEquals(LocalDate.of(2026, 11, 2), Repeat.DAILY.next(TODAY, TODAY));
        assertEquals(LocalDate.of(2026, 11, 8), Repeat.WEEKLY.next(TODAY, TODAY));
        assertEquals(LocalDate.of(2026, 12, 1), Repeat.MONTHLY.next(TODAY, TODAY));
    }

    @Test
    public void finishedEarlyDoesNotSkipAnything() {
        LocalDate due = LocalDate.of(2026, 11, 10);
        assertEquals(LocalDate.of(2026, 11, 11), Repeat.DAILY.next(due, TODAY));
        assertEquals(LocalDate.of(2026, 11, 17), Repeat.WEEKLY.next(due, TODAY));
    }

    @Test
    public void finishedLateSkipsDaysThatAreAlreadyGone() {
        LocalDate due = LocalDate.of(2026, 10, 25);
        assertEquals(LocalDate.of(2026, 11, 2), Repeat.DAILY.next(due, TODAY));
        // Weekly from Sunday 25 Oct: 1 Nov is not after today, so the next one is 8 Nov.
        assertEquals(LocalDate.of(2026, 11, 8), Repeat.WEEKLY.next(due, TODAY));
    }

    @Test
    public void monthlyKeepsTheOriginalDayOfMonth() {
        LocalDate jan31 = LocalDate.of(2027, 1, 31);
        assertEquals(LocalDate.of(2027, 2, 28), Repeat.MONTHLY.next(jan31, jan31));
        // Finished late in March: the series is Jan 31, Feb 28, Mar 31, Apr 30 ... not "28th of every month".
        assertEquals(LocalDate.of(2027, 3, 31), Repeat.MONTHLY.next(jan31, LocalDate.of(2027, 3, 1)));
        assertEquals(LocalDate.of(2027, 4, 30), Repeat.MONTHLY.next(jan31, LocalDate.of(2027, 3, 31)));
    }

    @Test
    public void taskWithoutADateStartsFromToday() {
        assertEquals(LocalDate.of(2026, 11, 2), Repeat.DAILY.next(null, TODAY));
        assertEquals(LocalDate.of(2026, 11, 8), Repeat.WEEKLY.next(null, TODAY));
    }

    @Test
    public void leapDayRepeatsMonthlyWithoutDrifting() {
        LocalDate leap = LocalDate.of(2028, 2, 29);
        assertEquals(LocalDate.of(2028, 3, 29), Repeat.MONTHLY.next(leap, leap));
    }
}
