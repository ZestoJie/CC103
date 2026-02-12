package com.cc103sys.cc103.Models;

import java.time.LocalDate;

public class Task {

    private int id;
    private String taskName;
    private LocalDate date;
    private String status;

    public Task(int id, String taskName, LocalDate date, String status) {
        this.id = id;
        this.taskName = taskName;
        this.date = date;
        this.status = status;
    }

    public int getId() { return id; }

    public String getTaskName() { return taskName; }

    public LocalDate getDate() { return date; }

    public String getStatus() { return status; }

    public void setStatus(String status) {
        this.status = status;
    }
}
