package com.cc103sys.cc103.Models;

import java.time.LocalDate;

public class ClassTask {

    private int id;
    private int classId;
    private String taskName;
    private String description;
    private LocalDate dueDate;
    private int ownerId;
    private String createdAt;

    public ClassTask(int id, int classId, String taskName, String description, LocalDate dueDate, int ownerId) {
        this.id = id;
        this.classId = classId;
        this.taskName = taskName;
        this.description = description;
        this.dueDate = dueDate;
        this.ownerId = ownerId;
    }

    public ClassTask(int id, int classId, String taskName, String description, LocalDate dueDate, int ownerId, String createdAt) {
        this(id, classId, taskName, description, dueDate, ownerId);
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public int getClassId() { return classId; }
    public String getTaskName() { return taskName; }
    public String getDescription() { return description; }
    public LocalDate getDueDate() { return dueDate; }
    public int getOwnerId() { return ownerId; }
    public String getCreatedAt() { return createdAt; }

    public void setDescription(String description) {
        this.description = description;
    }
}