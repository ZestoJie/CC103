package com.cc103sys.cc103.Models;

import java.time.LocalDateTime;

public class TaskSubmission {

    private final int id;
    private final int classTaskId;
    private final int userId;
    private String submissionStatus;
    private final LocalDateTime submittedAt;

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
