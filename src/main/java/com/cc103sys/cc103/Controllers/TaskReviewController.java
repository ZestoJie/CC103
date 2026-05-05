package com.cc103sys.cc103.Controllers;

import java.awt.Desktop;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.ClassTask;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;
import com.cc103sys.cc103.Utils.UiDialogs;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class TaskReviewController {

    private static final Logger LOGGER = Logger.getLogger(TaskReviewController.class.getName());
    private static final int REFRESH_INTERVAL_SECONDS = 5;
    private static final int BASE_TASK_POINTS = 10;
    private static final int LATE_TASK_POINTS = 5;

    @FXML private Label breadcrumbLabel;
    @FXML private Label classNameLabel;
    @FXML private Label taskTitleLabel;
    @FXML private Label dueDateLabel;
    @FXML private TextArea taskDescriptionArea;
    @FXML private ListView<SubmissionRecord> submissionsListView;
    @FXML private Button backButton;
    @FXML private Label submissionCountLabel;

    private ClassTask currentTask;
    private Integer currentClassId;
    private Integer currentTaskId;
    private ObservableList<SubmissionRecord> submissions = FXCollections.observableArrayList();
    private Timeline refreshTimeline;
    private boolean disposed;

    private Window window() {
        return backButton != null && backButton.getScene() != null
            ? backButton.getScene().getWindow()
            : null;
    }

    @FXML
    public void initialize() {
        NavbarController.getInstance().setActive("classes");
        setupSubmissionsListView();
        if (submissionsListView != null) {
            Label empty = new Label("No submissions yet.\nStudents will appear here after they submit this task.");
            empty.getStyleClass().add("empty-state");
            empty.setWrapText(true);
            submissionsListView.setPlaceholder(empty);
        }
        registerLifecycleHooks();
        LOGGER.info("Task review view initialized");
    }

    private void registerLifecycleHooks() {
        if (taskTitleLabel != null) {
            taskTitleLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (oldScene != null && newScene == null) {
                    cleanupResources();
                }
            });
        }
    }

    private void cleanupResources() {
        if (disposed) {
            return;
        }
        disposed = true;
        stopAutoRefresh();
        LOGGER.info("Task review resources cleaned up");
    }

    public void setTaskData(Integer classId, Integer taskId) {
        this.currentClassId = classId;
        this.currentTaskId = taskId;

        loadTaskDetails();
        loadAllSubmissions();
        startAutoRefresh();
    }

    private void loadTaskDetails() {
        String sql = "SELECT id, class_id, task_name, description, due_date, owner_id, created_at FROM class_tasks WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentTaskId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    currentTask = new ClassTask(
                        rs.getInt("id"),
                        rs.getInt("class_id"),
                        rs.getString("task_name"),
                        rs.getString("description"),
                        rs.getDate("due_date").toLocalDate(),
                        rs.getInt("owner_id"),
                        rs.getString("created_at")
                    );

                    loadClassName();
                    updateBreadcrumb();
                    updateUI();
                }
            }
        } catch (Exception e) {
            LOGGER.severe(String.format("Failed to load task details: %s", e.getMessage()));
        }
    }

    private void updateBreadcrumb() {
        if (breadcrumbLabel == null) {
            return;
        }
        String cls = classNameLabel != null ? classNameLabel.getText() : "Class";
        String task = currentTask != null ? currentTask.getTaskName() : "Task";
        breadcrumbLabel.setText("Dashboard › Classes › " + cls + " › Review: " + task);
    }

    private void loadClassName() {
        String sql = "SELECT class_name FROM classes WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentClassId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    classNameLabel.setText(rs.getString("class_name"));
                }
            }
        } catch (Exception e) {
            LOGGER.severe(String.format("Failed to load class name: %s", e.getMessage()));
        }
    }

    private void loadAllSubmissions() {
        submissions.clear();
        String sql = "SELECT ts.id, u.username, ts.submission_status, ts.submitted_at, COUNT(ta.id) as file_count " +
                     "FROM task_submissions ts " +
                     "JOIN users u ON ts.user_id = u.id " +
                     "LEFT JOIN task_attachments ta ON ts.id = ta.submission_id " +
                     "WHERE ts.class_task_id = ? " +
                     "GROUP BY ts.id, u.username, ts.submission_status, ts.submitted_at " +
                     "ORDER BY ts.submitted_at DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentTaskId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    submissions.add(new SubmissionRecord(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("submission_status"),
                        rs.getTimestamp("submitted_at").toLocalDateTime(),
                        rs.getInt("file_count")
                    ));
                }
            }
            submissionCountLabel.setText("Total Submissions: " + submissions.size());
            LOGGER.info(String.format("Loaded %d submissions for task", submissions.size()));
        } catch (Exception e) {
            LOGGER.severe(String.format("Failed to load submissions: %s", e.getMessage()));
        }
    }

    private void updateUI() {
        if (currentTask != null) {
            taskTitleLabel.setText(currentTask.getTaskName());
            taskDescriptionArea.setText(currentTask.getDescription());
            taskDescriptionArea.setWrapText(true);
            taskDescriptionArea.setEditable(false);
            
            if (currentTask.getDueDate() != null) {
                dueDateLabel.setText("Due: " + currentTask.getDueDate());
            }
        }
    }

    private void setupSubmissionsListView() {
        submissionsListView.setItems(submissions);
        submissionsListView.setCellFactory(lv -> new SubmissionListCell());
    }

    private void approveSubmission(SubmissionRecord submission) {
        if (!UiDialogs.confirm(window(), "Approve this submission?",
            "Approve work from " + submission.username + "? Points will be awarded and the task will be marked done.")) {
            return;
        }
        try {
            Integer currentUserId = getCurrentUserId();

            String sql = "UPDATE task_submissions SET submission_status = 'APPROVED' WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, submission.submissionId);
                stmt.executeUpdate();
            }

            sql = "SELECT user_id FROM task_submissions WHERE id = ?";
            Integer userId = null;
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, submission.submissionId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getInt("user_id");
                    }
                }
            }

            if (userId != null) {
                int points = calculateClassTaskApprovalPoints();
                sql = "UPDATE users SET points = points + ? WHERE id = ?";
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, points);
                    stmt.setInt(2, userId);
                    stmt.executeUpdate();
                }

                sql = "UPDATE tasks SET status = 'Done', points_awarded = ?, approved_by = ?, approved_date = CURDATE() WHERE user_id = ? AND class_task_id = ?";
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, points);
                    stmt.setInt(2, currentUserId);
                    stmt.setInt(3, userId);
                    stmt.setInt(4, currentTaskId);
                    stmt.executeUpdate();
                }

                LOGGER.info(String.format("Approved submission from %s and awarded %d points", submission.username, points));
                UiDialogs.info(window(), "Approved", "Submission approved and " + points + " points awarded.");
            } else {
                UiDialogs.info(window(), "Approved", "Submission approved.");
            }

            LOGGER.info(String.format("Submission from %s approved", submission.username));
            loadAllSubmissions();
        } catch (Exception e) {
            LOGGER.severe(String.format("Failed to approve submission: %s", e.getMessage()));
            UiDialogs.error(window(), "Approval failed", e.getMessage());
        }
    }

    private int calculateClassTaskApprovalPoints() {
        if (currentTask == null || currentTask.getDueDate() == null) {
            return BASE_TASK_POINTS;
        }

        long daysBeforeDeadline = ChronoUnit.DAYS.between(LocalDate.now(), currentTask.getDueDate());
        if (daysBeforeDeadline < 0) {
            return LATE_TASK_POINTS;
        }

        int multiplier = (int) Math.max(1, daysBeforeDeadline);
        return BASE_TASK_POINTS * multiplier;
    }

    private void rejectSubmission(SubmissionRecord submission) {
        if (!UiDialogs.confirm(window(), "Reject this submission?",
            "Reject work from " + submission.username + "? They can update files and submit again.")) {
            return;
        }
        try {
            String sql = "UPDATE task_submissions SET submission_status = 'REJECTED' WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, submission.submissionId);
                stmt.executeUpdate();
            }

            LOGGER.info(String.format("Submission from %s rejected", submission.username));
            loadAllSubmissions();
            UiDialogs.info(window(), "Rejected", "Submission rejected — the participant can resubmit.");
        } catch (Exception e) {
            LOGGER.severe(String.format("Failed to reject submission: %s", e.getMessage()));
            UiDialogs.error(window(), "Rejection failed", e.getMessage());
        }
    }

    private static String formatSubmissionStatusLabel(String raw) {
        if (raw == null) {
            return "Pending";
        }
        return switch (raw.toUpperCase()) {
            case "DRAFT" -> "Pending";
            case "SUBMITTED" -> "For approval";
            case "APPROVED" -> "Approved";
            case "REJECTED" -> "Rejected";
            default -> raw;
        };
    }

    private static String submissionStatusStyle(String raw) {
        if (raw == null) {
            return "status-badge-pending";
        }
        return switch (raw.toUpperCase()) {
            case "SUBMITTED" -> "status-badge-review";
            case "APPROVED" -> "status-badge-approved";
            case "REJECTED" -> "status-badge-rejected";
            default -> "status-badge-pending";
        };
    }

    private void viewSubmissionFiles(SubmissionRecord submission) {
        if (submission.fileCount == 0) {
            UiDialogs.info(window(), "No files", "This submission has no attached files.");
            return;
        }

        // Query the database for all files in this submission
        String sql = "SELECT id, file_name, file_path, uploaded_at FROM task_attachments WHERE submission_id = ? ORDER BY uploaded_at DESC";
        ObservableList<FileRecord> files = FXCollections.observableArrayList();

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, submission.submissionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    files.add(new FileRecord(
                        rs.getInt("id"),
                        rs.getString("file_name"),
                        rs.getString("file_path"),
                        rs.getTimestamp("uploaded_at").toLocalDateTime()
                    ));
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to load submission files: " + e.getMessage());
            UiDialogs.error(window(), "Error", "Failed to load files: " + e.getMessage());
            return;
        }

        // Create a file viewer dialog
        Stage fileViewerStage = new Stage();
        fileViewerStage.setTitle("Submission Files - " + submission.username);
        fileViewerStage.setWidth(600);
        fileViewerStage.setHeight(400);

        ListView<FileRecord> fileListView = new ListView<>(files);
        fileListView.setCellFactory(lv -> new FileListCell());

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.getChildren().add(new Label("Files in submission:"));
        container.getChildren().add(fileListView);
        VBox.setVgrow(fileListView, Priority.ALWAYS);

        Scene scene = new Scene(container);
        fileViewerStage.setScene(scene);
        fileViewerStage.show();
    }

    private static class FileRecord {
        public int id;
        public String fileName;
        public String filePath;
        public java.time.LocalDateTime uploadedAt;

        public FileRecord(int id, String fileName, String filePath, java.time.LocalDateTime uploadedAt) {
            this.id = id;
            this.fileName = fileName;
            this.filePath = filePath;
            this.uploadedAt = uploadedAt;
        }
    }

    private class FileListCell extends ListCell<FileRecord> {
        @Override
        protected void updateItem(FileRecord file, boolean empty) {
            super.updateItem(file, empty);
            if (empty || file == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox container = new HBox(10);
                container.setPadding(new Insets(8));
                container.setStyle("-fx-border-color: #e2e8f0; -fx-border-radius: 4; -fx-background-color: #f9fafb;");

                VBox fileInfo = new VBox(4);
                Label fileNameLabel = new Label(file.fileName);
                fileNameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
                Label uploadTimeLabel = new Label("Uploaded: " + file.uploadedAt);
                uploadTimeLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 10;");
                Label filePathLabel = new Label("Path: " + file.filePath);
                filePathLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 9;");
                fileInfo.getChildren().addAll(fileNameLabel, uploadTimeLabel, filePathLabel);

                Button openButton = new Button("Open File");
                openButton.getStyleClass().addAll("button", "button-secondary");
                openButton.setOnAction(e -> openSubmissionFile(file));

                Button openFolderButton = new Button("Show in Folder");
                openFolderButton.getStyleClass().addAll("button", "button-secondary");
                openFolderButton.setOnAction(e -> openFileLocation(file));

                HBox buttonsBox = new HBox(8);
                buttonsBox.getChildren().addAll(openButton, openFolderButton);

                container.getChildren().addAll(fileInfo, buttonsBox);
                HBox.setHgrow(fileInfo, Priority.ALWAYS);
                setGraphic(container);
                setText(null);
            }
        }
    }

    private void openSubmissionFile(FileRecord file) {
        File targetFile = new File(file.filePath);
        if (!targetFile.exists()) {
            UiDialogs.error(window(), "File not found", "The file no longer exists: " + file.filePath);
            LOGGER.warning("File not found: " + file.filePath);
            return;
        }

        try {
            Desktop.getDesktop().open(targetFile);
            LOGGER.info("Opened file: " + file.filePath);
        } catch (Exception e) {
            LOGGER.severe("Failed to open file: " + e.getMessage());
            UiDialogs.error(window(), "Error", "Could not open file: " + e.getMessage());
        }
    }

    private void openFileLocation(FileRecord file) {
        File targetFile = new File(file.filePath);
        File parentDir = targetFile.getParentFile();

        if (parentDir == null || !parentDir.exists()) {
            UiDialogs.error(window(), "Folder not found", "The folder no longer exists.");
            LOGGER.warning("Parent directory not found: " + file.filePath);
            return;
        }

        try {
            Desktop.getDesktop().open(parentDir);
            LOGGER.info("Opened folder: " + parentDir.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.severe("Failed to open folder: " + e.getMessage());
            UiDialogs.error(window(), "Error", "Could not open folder: " + e.getMessage());
        }
    }

    private Integer getCurrentUserId() {
        String username = Session.getUsername();
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (Exception e) {
            LOGGER.severe(String.format("Failed to get current user ID: %s", e.getMessage()));
        }
        return null;
    }

    @FXML
    private void handleGoBack() {
        stopAutoRefresh();
        if (currentClassId != null && currentClassId > 0) {
            Session.setCurrentClassId(currentClassId);
            Navigator.navigateTo("ClassDetail");
        } else {
            Navigator.navigateTo("classes");
        }
    }

    private void startAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(REFRESH_INTERVAL_SECONDS), e -> {
                loadAllSubmissions();
            })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        LOGGER.info("Task review auto-refresh started");
    }

    private void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            LOGGER.info("Task review auto-refresh stopped");
        }
    }

    public static class SubmissionRecord {
        public int submissionId;
        public String username;
        public String status;
        public java.time.LocalDateTime submittedAt;
        public int fileCount;

        public SubmissionRecord(int submissionId, String username, String status, java.time.LocalDateTime submittedAt, int fileCount) {
            this.submissionId = submissionId;
            this.username = username;
            this.status = status;
            this.submittedAt = submittedAt;
            this.fileCount = fileCount;
        }
    }

    private class SubmissionListCell extends ListCell<SubmissionRecord> {
        @Override
        protected void updateItem(SubmissionRecord submission, boolean empty) {
            super.updateItem(submission, empty);
            if (empty || submission == null) {
                setGraphic(null);
                setText(null);
            } else {
                VBox container = new VBox(8);
                container.setPadding(new Insets(10));
                container.setStyle("-fx-border-color: #e2e8f0; -fx-border-radius: 4; -fx-background-color: #f9fafb;");

                HBox headerBox = new HBox(12);
                Label userLabel = new Label(submission.username);
                userLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
                
                Label statusLabel = new Label(formatSubmissionStatusLabel(submission.status));
                statusLabel.getStyleClass().addAll("status-badge", submissionStatusStyle(submission.status));
                
                Label dateLabel = new Label("Submitted: " + submission.submittedAt);
                dateLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 10;");
                
                headerBox.getChildren().addAll(userLabel, statusLabel, dateLabel);
                HBox.setHgrow(dateLabel, Priority.ALWAYS);

                Label filesLabel = new Label("Files: " + submission.fileCount);
                filesLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 11;");

                HBox buttonsBox = new HBox(8);
                buttonsBox.setPadding(new Insets(8, 0, 0, 0));

                Button viewButton = new Button("View Files");
                viewButton.getStyleClass().addAll("button", "button-secondary");
                viewButton.setOnAction(e -> viewSubmissionFiles(submission));

                if (!"APPROVED".equals(submission.status) && !"REJECTED".equals(submission.status)) {
                    Button approveButton = new Button("Approve");
                    approveButton.getStyleClass().addAll("button", "button-success");
                    approveButton.setOnAction(e -> approveSubmission(submission));

                    Button rejectButton = new Button("Reject");
                    rejectButton.getStyleClass().addAll("button", "button-danger");
                    rejectButton.setOnAction(e -> rejectSubmission(submission));

                    buttonsBox.getChildren().addAll(viewButton, approveButton, rejectButton);
                } else {
                    viewButton.setDisable(false);
                    buttonsBox.getChildren().add(viewButton);
                }

                container.getChildren().addAll(headerBox, filesLabel, buttonsBox);
                setGraphic(container);
                setText(null);
            }
        }
    }
}
