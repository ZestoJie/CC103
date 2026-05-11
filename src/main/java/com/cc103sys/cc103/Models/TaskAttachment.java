package com.cc103sys.cc103.Models;

import java.time.LocalDateTime;

public class TaskAttachment {

    private final int id;
    private final int submissionId;
    private final String fileName;
    private final String filePath;
    private final LocalDateTime uploadedAt;

    public TaskAttachment(int id, int submissionId, String fileName, String filePath, LocalDateTime uploadedAt) {
        this.id = id;
        this.submissionId = submissionId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.uploadedAt = uploadedAt;
    }

    public int getId() { return id; }
    public int getSubmissionId() { return submissionId; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
