package com.example.todolist.core;

import java.util.List;

/** How far the subtasks of one task are: "2 of 5". Takes plain booleans so it needs no model class. */
public final class SubtaskProgress {

    public final int done;
    public final int total;

    private SubtaskProgress(int done, int total) {
        this.done = done;
        this.total = total;
    }

    /** @param doneFlags one entry per subtask, true when it is ticked; null counts as no subtasks */
    public static SubtaskProgress of(List<Boolean> doneFlags) {
        if (doneFlags == null) return new SubtaskProgress(0, 0);
        int done = 0;
        for (Boolean flag : doneFlags) {
            if (flag != null && flag) done++;
        }
        return new SubtaskProgress(done, doneFlags.size());
    }

    public boolean isEmpty() {
        return total == 0;
    }

    public boolean isComplete() {
        return total > 0 && done == total;
    }

    /** 0..100, rounded down; 0 when there are no subtasks. */
    public int percent() {
        return total == 0 ? 0 : (int) ((100L * done) / total);
    }
}
