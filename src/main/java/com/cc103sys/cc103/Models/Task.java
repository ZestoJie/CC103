package com.cc103sys.cc103.Models;

import java.time.LocalDate;

public class Task {

    private int id;
    private String username;
    private String taskName;
    private LocalDate date;
    private String status;

    private Integer classId;
    private String className;
    private boolean isPersonal;

    private int pointsAwarded;
    private int pendingPoints;
    private Integer approvedBy;
    private LocalDate approvedDate;
    private Integer createdBy;
    private LocalDate completedDate;

    public Task(int id, String taskName, LocalDate date, String status) {
        this(id, taskName, date, status, null, null, false);
    }

    public Task(int id, String taskName, LocalDate date, String status, Integer classId, String className) {
        this(id, taskName, date, status, classId, className, false);
    }

    public Task(int id, String taskName, LocalDate date, String status, Integer classId, String className, boolean isPersonal) {
        this(id, null, taskName, date, status, classId, className, isPersonal);
    }

    public Task(int id, String username, String taskName, LocalDate date, String status, Integer classId, String className, boolean isPersonal) {
        this.id = id;
        this.username = username;
        this.taskName = taskName;
        this.date = date;
        this.status = status;
        this.classId = classId;
        this.className = className;
        this.isPersonal = isPersonal;
        this.pointsAwarded = 0;
        this.pendingPoints = 0;
        this.createdBy = null;
        this.completedDate = null;
        this.approvedBy = null;
        this.approvedDate = null;
    }

    public int getId() { return id; }

    public String getUsername() { return username; }

    public String getTaskName() { return taskName; }

    public LocalDate getDate() { return date; }

    public String getStatus() { return status; }

    public Integer getClassId() { return classId; }

    public String getClassName() { return className; }

    public boolean isPersonal() { return isPersonal; }

    public int getPointsAwarded() { return pointsAwarded; }

    public int getPendingPoints() { return pendingPoints; }

    public Integer getApprovedBy() { return approvedBy; }

    public LocalDate getApprovedDate() { return approvedDate; }

    public Integer getCreatedBy() { return createdBy; }

    public LocalDate getCompletedDate() { return completedDate; }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPointsAwarded(int pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    public void setPendingPoints(int pendingPoints) {
        this.pendingPoints = pendingPoints;
    }

    public void setApprovedBy(Integer approvedBy) {
        this.approvedBy = approvedBy;
    }

    public void setApprovedDate(LocalDate approvedDate) {
        this.approvedDate = approvedDate;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public void setCompletedDate(LocalDate completedDate) {
        this.completedDate = completedDate;
    }
}
