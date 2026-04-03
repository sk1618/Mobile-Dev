package com.example.studysmart;

public class Task {
    private int id;
    private String title;
    private String category;
    private String priority;
    private String status;

    public Task(int id, String title, String category, String priority, String status) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.priority = priority;
        this.status = status;
    }

    public Task(String title, String category, String priority, String status) {
        this.title = title;
        this.category = category;
        this.priority = priority;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }
}