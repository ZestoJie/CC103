package com.cc103sys.cc103.Models;

import java.time.LocalDate;

public class Task {

    private int id;
    private String taskName;
    private LocalDate date;
    private String status;

    private Integer classId;
    private String className;
    private boolean isPersonal;

    public Task(int id, String taskName, LocalDate date, String status) {
        this(id, taskName, date, status, null, null, false);
    }

    public Task(int id, String taskName, LocalDate date, String status, Integer classId, String className) {
        this(id, taskName, date, status, classId, className, false);
    }

    public Task(int id, String taskName, LocalDate date, String status, Integer classId, String className, boolean isPersonal) {
        this.id = id;
        this.taskName = taskName;
        this.date = date;
        this.status = status;
        this.classId = classId;
        this.className = className;
        this.isPersonal = isPersonal;
    }

    public int getId() { return id; }

    public String getTaskName() { return taskName; }

    public LocalDate getDate() { return date; }

    public String getStatus() { return status; }

    public Integer getClassId() { return classId; }

    public String getClassName() { return className; }

    public boolean isPersonal() { return isPersonal; }

    public void setStatus(String status) {
        this.status = status;
    }
}
