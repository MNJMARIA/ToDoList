package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class WidgetDataTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Kyiv");

    private static String date(int plusDays) {
        return TaskDateTime.formatDate(LocalDate.now(ZONE).plusDays(plusDays));
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    @Test
    public void emptyCopyGivesEmptyView() {
        WidgetData.View view = WidgetData.build(new ArrayList<WidgetData.Entry>(), now(), ZONE, 4);
        assertEquals(0, view.total);
        assertTrue(view.rows.isEmpty());
        assertEquals(0, WidgetData.build(null, now(), ZONE, 4).total);
    }

    @Test
    public void overdueComesFirstThenTheRestByDeadline() {
        List<WidgetData.Entry> entries = Arrays.asList(
                new WidgetData.Entry("1", "later", date(3), "10:00"),
                new WidgetData.Entry("2", "late", date(-2), "09:00"),
                new WidgetData.Entry("3", "soon", date(1), "08:00"));
        WidgetData.View view = WidgetData.build(entries, now(), ZONE, 4);
        assertEquals("late", view.rows.get(0).title);
        assertTrue(view.rows.get(0).overdue);
        assertEquals("soon", view.rows.get(1).title);
        assertEquals("later", view.rows.get(2).title);
        assertEquals(1, view.overdue);
        assertEquals(3, view.total);
    }

    @Test
    public void rowsAreLimitedButCountsAreNot() {
        List<WidgetData.Entry> entries = new ArrayList<>();
        for (int i = 0; i < 9; i++) entries.add(new WidgetData.Entry("" + i, "t" + i, date(i + 1), "10:00"));
        WidgetData.View view = WidgetData.build(entries, now(), ZONE, 3);
        assertEquals(3, view.rows.size());
        assertEquals(9, view.total);
    }

    @Test
    public void todayShowsTimeOtherDaysShowDayAndMonth() {
        String today = date(0);
        LocalDate other = LocalDate.now(ZONE).plusDays(5);
        List<WidgetData.Entry> entries = Arrays.asList(
                new WidgetData.Entry("1", "a", today, "23:59"),
                new WidgetData.Entry("2", "b", TaskDateTime.formatDate(other), "10:00"));
        WidgetData.View view = WidgetData.build(entries, 0L, ZONE, 4); // "now" at the epoch: nothing is late
        assertEquals("23:59", view.rows.get(0).when);
        assertEquals(String.format("%02d/%02d", other.getDayOfMonth(), other.getMonthValue()), view.rows.get(1).when);
        assertEquals(1, view.today);
        assertEquals(0, view.overdue);
        assertFalse(view.rows.get(0).overdue);
    }

    @Test
    public void unreadableDateShowsNoTimeAndIsNotLate() {
        WidgetData.View view = WidgetData.build(
                Arrays.asList(new WidgetData.Entry("1", "x", "00/00/0000", "00:00")), now(), ZONE, 4);
        assertEquals("", view.rows.get(0).when);
        assertEquals(0, view.overdue);
        assertEquals(0, view.today);
    }

    @Test
    public void copyRoundTripsEvenWithAwkwardText() {
        List<WidgetData.Entry> entries = Arrays.asList(
                new WidgetData.Entry("id1", "tab\there\nnew line \\ back\\slash", "01/02/2027", "10:00"),
                new WidgetData.Entry("id2", "Привіт, світе 😊", "02/02/2027", "11:30"),
                new WidgetData.Entry("id3", "", "", ""));
        List<WidgetData.Entry> back = WidgetData.decode(WidgetData.encode(entries));
        assertEquals(3, back.size());
        for (int i = 0; i < entries.size(); i++) {
            assertEquals(entries.get(i).id, back.get(i).id);
            assertEquals(entries.get(i).title, back.get(i).title);
            assertEquals(entries.get(i).date, back.get(i).date);
            assertEquals(entries.get(i).time, back.get(i).time);
        }
    }

    @Test
    public void damagedCopyIsSkippedNotCrashed() {
        assertTrue(WidgetData.decode(null).isEmpty());
        assertTrue(WidgetData.decode("").isEmpty());
        assertEquals(1, WidgetData.decode("only\ttwo\nid\ttitle\t01/01/2027\t10:00\n").size());
    }
}
