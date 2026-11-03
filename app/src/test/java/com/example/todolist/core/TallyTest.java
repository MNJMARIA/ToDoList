package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class TallyTest {

    @Test
    public void biggestFirstTiesKeepFirstSeenOrder() {
        List<Tally.Entry> entries = Tally.count(Arrays.asList("b", "a", "b", "c", "a", "b", "d"));
        assertEquals("b", entries.get(0).key);
        assertEquals(3, entries.get(0).count);
        assertEquals("a", entries.get(1).key);
        assertEquals("c", entries.get(2).key);
        assertEquals("d", entries.get(3).key);
        assertEquals(4, entries.size());
    }

    @Test
    public void nullKeysAreSkippedAndEmptyInputIsEmpty() {
        assertEquals(1, Tally.count(Arrays.asList(null, "x", null)).size());
        assertTrue(Tally.count(null).isEmpty());
    }
}
