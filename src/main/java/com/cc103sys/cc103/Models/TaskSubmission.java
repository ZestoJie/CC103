package com.cc103sys.cc103.Models;

import java.time.LocalDateTime;

public class TaskSubmission {

    private int id;
    private int classTaskId;
    private int userId;
    private String submissionStatus;
    private LocalDateTime submittedAt;

    public TaskSubmission(int id, int classTaskId, int userId, String submissionStatus, LocalDateTime submittedAt) {
        this.id = id;
        this.classTaskId = classTaskId;
        this.userId = userId;
        this.submissionStatus = submissionStatus;
        this.submittedAt = submittedAt;
    }

    public int getId() { return id; }
    public int getClassTaskId() { return classTaskId; }
    public int getUserId() { return userId; }
    public String getSubmissionStatus() { return submissionStatus; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }

    public void setSubmissionStatus(String status) {
        this.submissionStatus = status;
    }
}
