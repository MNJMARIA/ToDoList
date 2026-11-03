package com.example.todolist.utils.model;

/** One step of a task. Written to the database through its getters, so keep the property names stable. */
public class Subtask {
    private String title;
    private boolean done;

    public Subtask() {
    }

    public Subtask(String title, boolean done) {
        this.title = title;
        this.done = done;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
