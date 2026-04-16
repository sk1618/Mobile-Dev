package com.example.studysmart;

public class StudySession {
    private int id;
    private int durationMinutes;
    private String sessionDate;

    public StudySession(int id, int durationMinutes, String sessionDate) {
        this.id = id;
        this.durationMinutes = durationMinutes;
        this.sessionDate = sessionDate;
    }

    public int getId() {
        return id;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getSessionDate() {
        return sessionDate;
    }
}