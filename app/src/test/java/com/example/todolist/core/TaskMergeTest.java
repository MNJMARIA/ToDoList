package com.example.todolist.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class TaskMergeTest {

    private static final TaskMerge.IdOf<String> IDS = new TaskMerge.IdOf<String>() {
        public String idOf(String item) {
            return item == null ? null : item.split(":")[0];
        }
    };

    private static List<String> merge(List<String> server, List<String> pending) {
        return TaskMerge.withPending(server, pending, IDS);
    }

    @Test
    public void noPendingKeepsTheServerList() {
        assertEquals(Arrays.asList("1:a", "2:b"), merge(Arrays.asList("1:a", "2:b"), Collections.<String>emptyList()));
    }

    @Test
    public void pendingItemsAreAppendedWhenTheServerDoesNotKnowThem() {
        assertEquals(Arrays.asList("1:a", "3:new"), merge(Arrays.asList("1:a"), Arrays.asList("3:new")));
        assertEquals(Arrays.asList("3:new"), merge(Collections.<String>emptyList(), Arrays.asList("3:new")));
    }

    @Test
    public void confirmedItemsAreNotDuplicated() {
        assertEquals(Arrays.asList("1:server"), merge(Arrays.asList("1:server"), Arrays.asList("1:local")));
    }

    @Test
    public void duplicatePendingAndNullIdsAreSkipped() {
        assertEquals(Arrays.asList("1:a"), merge(Collections.<String>emptyList(), Arrays.asList("1:a", "1:b")));
        assertEquals(Collections.<String>emptyList(), merge(Collections.<String>emptyList(), Arrays.asList((String) null)));
    }

    @Test
    public void inputsAreNotModified() {
        List<String> server = Arrays.asList("1:a");
        List<String> pending = Arrays.asList("2:b");
        merge(server, pending);
        assertEquals(Arrays.asList("1:a"), server);
        assertEquals(Arrays.asList("2:b"), pending);
    }
}
