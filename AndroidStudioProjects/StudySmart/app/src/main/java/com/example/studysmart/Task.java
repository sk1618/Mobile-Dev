package com.example.studysmart;

public class Task {
    private int id;
    private String title;
    private String category;
    private String status;
    private String dueDate;

    public Task(int id, String title, String category, String status, String dueDate) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.status = status;
        this.dueDate = dueDate;
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

    public String getStatus() {
        return status;
    }

    public String getDueDate() {
        return dueDate;
    }
}