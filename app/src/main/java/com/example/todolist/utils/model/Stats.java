package com.example.todolist.utils.model;

public class Stats {
    private String statsId;
    private int totalTasks;
    private int completedTasks;
    private int tasksToDo;
    private int totalScores;

    public Stats() {
    }

    public Stats(String statsId, int totalTasks, int completedTasks, int tasksToDo, int totalScores) {
        this.statsId = statsId;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.tasksToDo = tasksToDo;
        this.totalScores = totalScores;
    }

    public String getStatsId() {
        return statsId;
    }
    public void setStatsId(String statsId) {
        this.statsId = statsId;
    }
    public int getTotalTasks() {
        return totalTasks;
    }
    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }
    public int getCompletedTasks() {
        return completedTasks;
    }
    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }
    public int getTasksToDo() {
        return tasksToDo;
    }
    public void setTasksToDo(int tasksToDo) {
        this.tasksToDo = tasksToDo;
    }
    public int getTotalScores() {
        return totalScores;
    }
    public void setTotalScores(int totalScores) {
        this.totalScores = totalScores;
    }
}
