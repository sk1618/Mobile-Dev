package com.example.studysmart;

public class AiStudyPlan {
    private int id;
    private String subject;
    private String examDate;
    private String plan;
    private String createdAt;

    public AiStudyPlan(int id, String subject, String examDate, String plan, String createdAt) {
        this.id = id;
        this.subject = subject;
        this.examDate = examDate;
        this.plan = plan;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getExamDate() {
        return examDate;
    }

    public String getPlan() {
        return plan;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
