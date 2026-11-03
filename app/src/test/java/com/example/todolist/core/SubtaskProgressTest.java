package com.example.todolist.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class SubtaskProgressTest {

    @Test
    public void noSubtasksIsEmptyAndNotComplete() {
        SubtaskProgress p = SubtaskProgress.of(Collections.<Boolean>emptyList());
        assertTrue(p.isEmpty());
        assertFalse(p.isComplete());
        assertEquals(0, p.percent());
        assertTrue(SubtaskProgress.of(null).isEmpty());
    }

    @Test
    public void countsTickedOnes() {
        SubtaskProgress p = SubtaskProgress.of(Arrays.asList(true, false, true, null));
        assertEquals(2, p.done);
        assertEquals(4, p.total);
        assertEquals(50, p.percent());
        assertFalse(p.isComplete());
    }

    @Test
    public void allTickedIsComplete() {
        SubtaskProgress p = SubtaskProgress.of(Arrays.asList(true, true, true));
        assertTrue(p.isComplete());
        assertEquals(100, p.percent());
    }

    @Test
    public void percentRoundsDown() {
        assertEquals(33, SubtaskProgress.of(Arrays.asList(true, false, false)).percent());
        assertEquals(66, SubtaskProgress.of(Arrays.asList(true, true, false)).percent());
    }
}
