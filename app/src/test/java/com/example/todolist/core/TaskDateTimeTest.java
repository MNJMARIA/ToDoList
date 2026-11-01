package com.example.todolist.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.OptionalLong;
import org.junit.Test;

public class TaskDateTimeTest {

    // 2026-10-29T18:30:00Z
    private static final long DEADLINE_UTC = 1793298600000L;
    // 2026-10-29 18:30 in Europe/Kiev (UTC+2, DST ended on 2026-10-25)
    private static final long DEADLINE_KIEV = 1793291400000L;
    private static final ZoneId UTC = ZoneOffset.UTC;
    private static final ZoneId KIEV = ZoneId.of("Europe/Kiev");

    // ---------- parseDate ----------

    @Test
    public void parseDate_acceptsZeroPaddedAndPlainForms() {
        assertEquals(LocalDate.of(2026, 11, 5), TaskDateTime.parseDate("05/11/2026"));
        assertEquals(LocalDate.of(2026, 11, 5), TaskDateTime.parseDate("5/11/2026"));
        assertEquals(LocalDate.of(2026, 3, 5), TaskDateTime.parseDate("5/3/2026"));
        assertEquals(LocalDate.of(2026, 10, 29), TaskDateTime.parseDate("29/10/2026"));
    }

    @Test
    public void parseDate_trimsWhitespace() {
        assertEquals(LocalDate.of(2026, 3, 5), TaskDateTime.parseDate("  5/3/2026 "));
    }

    @Test
    public void parseDate_acceptsLeapDay() {
        assertEquals(LocalDate.of(2028, 2, 29), TaskDateTime.parseDate("29/02/2028"));
    }

    @Test
    public void parseDate_rejectsGarbageAndImpossibleDates() {
        assertNull(TaskDateTime.parseDate(null));
        assertNull(TaskDateTime.parseDate(""));
        assertNull(TaskDateTime.parseDate("abc"));
        assertNull(TaskDateTime.parseDate("32/01/2026"));
        assertNull(TaskDateTime.parseDate("15/13/2026"));
        assertNull(TaskDateTime.parseDate("29/02/2027"));
        assertNull(TaskDateTime.parseDate("2026-10-29"));
        assertNull(TaskDateTime.parseDate("5/11/26"));
        assertNull(TaskDateTime.parseDate("5/11"));
    }

    // ---------- parseTime ----------

    @Test
    public void parseTime_acceptsOneAndTwoDigitHours() {
        assertEquals(LocalTime.of(18, 30), TaskDateTime.parseTime("18:30"));
        assertEquals(LocalTime.of(8, 5), TaskDateTime.parseTime("8:05"));
        assertEquals(LocalTime.of(0, 0), TaskDateTime.parseTime("00:00"));
        assertEquals(LocalTime.of(23, 59), TaskDateTime.parseTime(" 23:59 "));
    }

    @Test
    public void parseTime_rejectsGarbage() {
        assertNull(TaskDateTime.parseTime(null));
        assertNull(TaskDateTime.parseTime(""));
        assertNull(TaskDateTime.parseTime("24:00"));
        assertNull(TaskDateTime.parseTime("12:60"));
        assertNull(TaskDateTime.parseTime("18-30"));
        assertNull(TaskDateTime.parseTime("18:3"));
        assertNull(TaskDateTime.parseTime("noon"));
    }

    // ---------- formatting ----------

    @Test
    public void format_isAlwaysZeroPadded() {
        assertEquals("05/11/2026", TaskDateTime.formatDate(LocalDate.of(2026, 11, 5)));
        assertEquals("29/10/2026", TaskDateTime.formatDate(LocalDate.of(2026, 10, 29)));
        assertEquals("08:05", TaskDateTime.formatTime(LocalTime.of(8, 5)));
    }

    @Test
    public void normalizeDate_convertsLegacyFormsToCanonical() {
        assertEquals("05/03/2026", TaskDateTime.normalizeDate("5/3/2026"));
        assertEquals("05/03/2026", TaskDateTime.normalizeDate("05/03/2026"));
        assertEquals("29/10/2026", TaskDateTime.normalizeDate(" 29/10/2026 "));
        assertNull(TaskDateTime.normalizeDate("nonsense"));
        assertNull(TaskDateTime.normalizeDate(null));
    }

    // ---------- deadlineMillis ----------

    @Test
    public void deadlineMillis_utc() {
        OptionalLong r = TaskDateTime.deadlineMillis("29/10/2026", "18:30", UTC);
        assertTrue(r.isPresent());
        assertEquals(DEADLINE_UTC, r.getAsLong());
    }

    @Test
    public void deadlineMillis_respectsZone() {
        assertEquals(DEADLINE_KIEV, TaskDateTime.deadlineMillis("29/10/2026", "18:30", KIEV).getAsLong());
    }

    @Test
    public void deadlineMillis_sameForPaddedAndUnpaddedDate() {
        assertEquals(
                TaskDateTime.deadlineMillis("05/11/2026", "09:00", UTC).getAsLong(),
                TaskDateTime.deadlineMillis("5/11/2026", "9:00", UTC).getAsLong());
    }

    /** Regression: the old MonthFragment parsed with "yyyy-MM-dd" and returned 0 for every real task. */
    @Test
    public void deadlineMillis_neverReturnsEpochZeroForValidInput() {
        assertTrue(TaskDateTime.deadlineMillis("29/10/2026", "18:30", UTC).getAsLong() > 0);
    }

    @Test
    public void deadlineMillis_emptyForInvalidInput() {
        assertFalse(TaskDateTime.deadlineMillis(null, "18:30", UTC).isPresent());
        assertFalse(TaskDateTime.deadlineMillis("29/10/2026", null, UTC).isPresent());
        assertFalse(TaskDateTime.deadlineMillis("31/02/2026", "18:30", UTC).isPresent());
        assertFalse(TaskDateTime.deadlineMillis("29/10/2026", "25:00", UTC).isPresent());
        assertFalse(TaskDateTime.deadlineMillis("", "", UTC).isPresent());
    }

    // ---------- sameDay ----------

    /** Regression: Month view compared raw strings, so "05/11/2026" never matched "5/11/2026". */
    @Test
    public void sameDay_ignoresZeroPadding() {
        assertTrue(TaskDateTime.sameDay("05/11/2026", "5/11/2026"));
        assertTrue(TaskDateTime.sameDay("5/3/2026", "05/03/2026"));
        assertTrue(TaskDateTime.sameDay("29/10/2026", "29/10/2026"));
    }

    @Test
    public void sameDay_falseForDifferentOrInvalid() {
        assertFalse(TaskDateTime.sameDay("5/11/2026", "6/11/2026"));
        assertFalse(TaskDateTime.sameDay("5/11/2026", "5/12/2026"));
        assertFalse(TaskDateTime.sameDay("5/11/2026", "5/11/2027"));
        assertFalse(TaskDateTime.sameDay(null, "5/11/2026"));
        assertFalse(TaskDateTime.sameDay("5/11/2026", "junk"));
        assertFalse(TaskDateTime.sameDay(null, null));
    }

    // ---------- isOnTime ----------

    @Test
    public void isOnTime_beforeAtAndAfterDeadline() {
        assertTrue(TaskDateTime.isOnTime("29/10/2026", "18:30", DEADLINE_UTC - 1, UTC));
        assertTrue(TaskDateTime.isOnTime("29/10/2026", "18:30", DEADLINE_UTC, UTC));
        assertFalse(TaskDateTime.isOnTime("29/10/2026", "18:30", DEADLINE_UTC + 1, UTC));
    }

    @Test
    public void isOnTime_unknownDeadlineGetsBenefitOfTheDoubt() {
        assertTrue(TaskDateTime.isOnTime("junk", "18:30", DEADLINE_UTC, UTC));
        assertTrue(TaskDateTime.isOnTime(null, null, DEADLINE_UTC, UTC));
    }

    // ---------- checkDeadline ----------

    @Test
    public void checkDeadline_futureIsOk() {
        assertEquals(TaskDateTime.DeadlineCheck.OK,
                TaskDateTime.checkDeadline("29/10/2026", "18:30", DEADLINE_UTC - 3_600_000L, UTC));
    }

    @Test
    public void checkDeadline_allowsSmallGraceForDefaultTimeOfNow() {
        long fourMinutesLate = DEADLINE_UTC + 4 * 60_000L;
        long sixMinutesLate = DEADLINE_UTC + 6 * 60_000L;
        assertEquals(TaskDateTime.DeadlineCheck.OK,
                TaskDateTime.checkDeadline("29/10/2026", "18:30", fourMinutesLate, UTC));
        assertEquals(TaskDateTime.DeadlineCheck.IN_PAST,
                TaskDateTime.checkDeadline("29/10/2026", "18:30", sixMinutesLate, UTC));
    }

    @Test
    public void checkDeadline_invalidFormat() {
        assertEquals(TaskDateTime.DeadlineCheck.INVALID_FORMAT,
                TaskDateTime.checkDeadline("junk", "18:30", DEADLINE_UTC, UTC));
        assertEquals(TaskDateTime.DeadlineCheck.INVALID_FORMAT,
                TaskDateTime.checkDeadline("29/10/2026", "", DEADLINE_UTC, UTC));
    }

    // ---------- isDateInPast ----------

    /** Regression: the picker compared instants, so choosing "today" could be rejected. */
    @Test
    public void isDateInPast_comparesDaysNotInstants() {
        LocalDate today = LocalDate.of(2026, 10, 29);
        assertFalse(TaskDateTime.isDateInPast(today, today));
        assertFalse(TaskDateTime.isDateInPast(today.plusDays(1), today));
        assertTrue(TaskDateTime.isDateInPast(today.minusDays(1), today));
    }

    // ---------- splitForDisplay ----------

    @Test
    public void splitForDisplay_validDate() {
        assertArrayEquals(new String[]{"5", "3", "2026"}, TaskDateTime.splitForDisplay("5/3/2026"));
        assertArrayEquals(new String[]{"05", "11", "2026"}, TaskDateTime.splitForDisplay("05/11/2026"));
    }

    /** Regression: TaskAdapter checked parts.length >= 2 but read parts[2]; null dates threw NPE. */
    @Test
    public void splitForDisplay_neverThrows() {
        assertArrayEquals(new String[]{"", "", ""}, TaskDateTime.splitForDisplay(null));
        assertArrayEquals(new String[]{"", "", ""}, TaskDateTime.splitForDisplay(""));
        assertArrayEquals(new String[]{"", "", ""}, TaskDateTime.splitForDisplay("5/3"));
        assertArrayEquals(new String[]{"", "", ""}, TaskDateTime.splitForDisplay("a/b/c"));
    }

    // ---------- compareDeadlines ----------

    @Test
    public void compareDeadlines_earlierDeadlineSortsFirst() {
        assertTrue(TaskDateTime.compareDeadlines("1/11/2026", "10:00", "2/11/2026", "09:00") < 0);
        assertTrue(TaskDateTime.compareDeadlines("2/11/2026", "09:00", "1/11/2026", "10:00") > 0);
        assertTrue(TaskDateTime.compareDeadlines("1/11/2026", "09:00", "1/11/2026", "10:00") < 0);
    }

    @Test
    public void compareDeadlines_sameMomentIgnoresPadding() {
        assertEquals(0, TaskDateTime.compareDeadlines("05/11/2026", "09:00", "5/11/2026", "9:00"));
    }

    @Test
    public void compareDeadlines_unreadableDeadlinesGoLast() {
        assertTrue(TaskDateTime.compareDeadlines("1/11/2026", "10:00", "junk", "10:00") < 0);
        assertTrue(TaskDateTime.compareDeadlines(null, null, "1/11/2026", "10:00") > 0);
        assertEquals(0, TaskDateTime.compareDeadlines("junk", "x", null, null));
    }
}
