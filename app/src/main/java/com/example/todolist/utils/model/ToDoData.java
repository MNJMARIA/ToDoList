package com.example.todolist.utils.model;

public class ToDoData{
    private String taskId;
    private String task;
    private String category;
    private Priority priority;
    private String date;
    private String time;

    public ToDoData(String taskId, String task, String category, Priority priority, String date, String time) {
        this.taskId = taskId;
        this.task = task;
        this.category = category;
        this.priority = priority;
        this.date = date;
        this.time = time;
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

}