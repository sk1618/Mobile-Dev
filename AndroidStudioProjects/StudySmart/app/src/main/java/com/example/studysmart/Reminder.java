package com.example.studysmart;

public class Reminder {
    private int id;
    private String title;
    private String date;
    private String time;
    private int isDone;

    public Reminder(int id, String title, String date, String time, int isDone) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.time = time;
        this.isDone = isDone;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public int getIsDone() {
        return isDone;
    }
}