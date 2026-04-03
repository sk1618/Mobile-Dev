package com.example.studysmart;

public class AiRoadmap {
    private int id;
    private String topic;
    private String roadmap;
    private String createdAt;

    public AiRoadmap(int id, String topic, String roadmap, String createdAt) {
        this.id = id;
        this.topic = topic;
        this.roadmap = roadmap;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getRoadmap() {
        return roadmap;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}