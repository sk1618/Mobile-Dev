package com.example.studysmart;

public class Exam {
    private int id;
    private String title;
    private String examDate;

    public Exam(int id, String title, String examDate) {
        this.id = id;
        this.title = title;
        this.examDate = examDate;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getExamDate() {
        return examDate;
    }
}