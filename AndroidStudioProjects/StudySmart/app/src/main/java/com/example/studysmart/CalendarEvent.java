package com.example.studysmart;

public class CalendarEvent {
    private String date;   // yyyy-MM-dd
    private String type;   // exam or task
    private String status; // Pending / Completed / In Progress / exam
    private String title;

    public CalendarEvent(String date, String type, String status, String title) {
        this.date = date;
        this.type = type;
        this.status = status;
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }
}

