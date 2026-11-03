package com.example.todolist.utils.model;

import com.example.todolist.core.Repeat;
import com.example.todolist.core.SubtaskProgress;
import java.util.ArrayList;
import java.util.List;

/**
 * A task as stored in {@code Tasks/<uid>/<taskId>}. Firebase writes it through the {@code get...} methods, so
 * helpers that are not stored must not start with "get" or "is" (hence {@code repeatMode()}, {@code progress()}).
 */
public class ToDoData{
    private String taskId;
    private String task;
    private String category;
    private Priority priority;
    private String date;
    private String time;
    /** Free text, may be null. */
    private String notes;
    private List<Subtask> subtasks;
    /** {@link Repeat#key()}, or null for a task that does not repeat. */
    private String repeat;

    public ToDoData(String taskId, String task, String category, Priority priority, String date, String time) {
        this(taskId, task, category, priority, date, time, null, null, null);
    }

    public ToDoData(String taskId, String task, String category, Priority priority, String date, String time,
                    String notes, List<Subtask> subtasks, String repeat) {
        this.taskId = taskId;
        this.task = task;
        this.category = category;
        this.priority = priority;
        this.date = date;
        this.time = time;
        this.notes = notes;
        this.subtasks = subtasks;
        this.repeat = repeat;
    }

    public String getTaskId() {
        return taskId;
    }
    public String getTask() {
        return task;
    }
    public void setTask(String task) {
        this.task = task;
    }
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }
    public Priority getPriority() {
        return priority;
    }
    public void setPriority(Priority priority) {
        this.priority = priority;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public String getTime() {
        return time;
    }
    public void setTime(String time) {
        this.time = time;
    }
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    public List<Subtask> getSubtasks() {
        return subtasks;
    }
    public void setSubtasks(List<Subtask> subtasks) {
        this.subtasks = subtasks;
    }
    public String getRepeat() {
        return repeat;
    }
    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }

    // ---- helpers that are NOT stored (names deliberately without get/is)

    public Repeat repeatMode() {
        return Repeat.fromStored(repeat);
    }

    public boolean hasNotes() {
        return notes != null && !notes.trim().isEmpty();
    }

    public SubtaskProgress progress() {
        List<Boolean> flags = new ArrayList<>();
        if (subtasks != null) {
            for (Subtask subtask : subtasks) flags.add(subtask != null && subtask.isDone());
        }
        return SubtaskProgress.of(flags);
    }

    /** The same task under another id (used when a draft is saved for the first time). */
    public ToDoData withId(String newId) {
        return new ToDoData(newId, task, category, priority, date, time, notes, copyOfSubtasks(false), repeat);
    }

    /** The same task for its next turn: a new id, a new date, and every subtask unticked again. */
    public ToDoData nextOccurrence(String newId, String newDate) {
        return new ToDoData(newId, task, category, priority, newDate, time, notes, copyOfSubtasks(true), repeat);
    }

    private List<Subtask> copyOfSubtasks(boolean untick) {
        if (subtasks == null) return null;
        List<Subtask> copy = new ArrayList<>();
        for (Subtask subtask : subtasks) {
            if (subtask != null) copy.add(new Subtask(subtask.getTitle(), !untick && subtask.isDone()));
        }
        return copy;
    }
}
