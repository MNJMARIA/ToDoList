package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ReminderRecordTest {

    @Test
    public void roundTrip_simple() {
        ReminderRecord in = new ReminderRecord("-Nabc", "Buy milk", 1793298600000L);
        ReminderRecord out = ReminderRecord.fromJson(in.toJson());
        assertNotNull(out);
        assertEquals("-Nabc", out.taskId);
        assertEquals("Buy milk", out.title);
        assertEquals(1793298600000L, out.deadlineMillis);
    }

    @Test
    public void roundTrip_specialCharactersAndUnicode() {
        String title = "Купити \"молоко\" | хліб\nі 🍞 \\ слеш";
        ReminderRecord out = ReminderRecord.fromJson(new ReminderRecord("id", title, 1L).toJson());
        assertNotNull(out);
        assertEquals(title, out.title);
    }

    @Test
    public void nullTitleBecomesEmpty() {
        ReminderRecord r = new ReminderRecord("id", null, 5L);
        assertEquals("", r.title);
        assertEquals("", ReminderRecord.fromJson(r.toJson()).title);
    }

    @Test
    public void fromJson_returnsNullForBrokenInput() {
        assertNull(ReminderRecord.fromJson(null));
        assertNull(ReminderRecord.fromJson(""));
        assertNull(ReminderRecord.fromJson("not json"));
        assertNull(ReminderRecord.fromJson("[1,2,3]"));
        assertNull(ReminderRecord.fromJson("{\"title\":\"no id\",\"deadlineMillis\":1}"));
    }
}
