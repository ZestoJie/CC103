package com.cc103sys.cc103.Controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.ClassTask;
import com.cc103sys.cc103.Models.TaskAttachment;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;
import com.cc103sys.cc103.Utils.UiDialogs;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public class TaskDetailController {

    private static final Logger LOGGER = Logger.getLogger(TaskDetailController.class.getName());
    private static final String TASK_UPLOADS_DIR = "task_submissions";
    private static final int REFRESH_INTERVAL_SECONDS = 5;

    @FXML private Label breadcrumbLabel;
    @FXML private Label classNameLabel;
    @FXML private Label taskTitleLabel;
    @FXML private Label dueDateLabel;
    @FXML private TextArea taskDescriptionArea;
    @FXML private Button uploadFileButton;
    @FXML private Button markDoneButton;
    @FXML@SuppressWarnings("unused")
    private Button backButton;
    @FXML private ListView<TaskAttachment> attachmentsListView;
    @FXML private Label submissionStatusLabel;

    private ClassTask currentTask;
    private Integer currentClassId;
    private Integer currentTaskId;
    private Integer currentUserId;
    private Integer currentSubmissionId;
    private final ObservableList<TaskAttachment> attachments = FXCollections.observableArrayList();
    private Timeline refreshTimeline;
    private boolean disposed;

    private Window window() {
        return uploadFileButton != null && uploadFileButton.getScene() != null
            ? uploadFileButton.getScene().getWindow()
            : null;
    }

    @FXML
    public void initialize() {
        NavbarController.getInstance().setActive("classes");
        setupAttachmentsListView();
        if (attachmentsListView != null) {
            Label empty = new Label("No files attached yet.\nUse Upload File to add your work.");
            empty.getStyleClass().add("empty-state");
            empty.setWrapText(true);
            attachmentsListView.setPlaceholder(empty);
        }
        registerLifecycleHooks();
        LOGGER.info("Task detail view initialized");
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
        LOGGER.info("Task detail resources cleaned up");
    }

    public void setTaskData(Integer classId, Integer taskId) {
        this.currentClassId = classId;
        this.currentTaskId = taskId;

        String username = Session.getUsername();
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    this.currentUserId = rs.getInt("id");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get user ID: {0}", e.getMessage());
        }

        File uploadsDir = new File(TASK_UPLOADS_DIR);
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
        }

        loadTaskDetails();
        loadSubmissionData();
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
            LOGGER.log(Level.SEVERE, "Failed to load task details: {0}", e.getMessage());
        }
    }

    private void updateBreadcrumb() {
        if (breadcrumbLabel == null) {
            return;
        }
        String cls = classNameLabel != null ? classNameLabel.getText() : "Class";
        String task = currentTask != null ? currentTask.getTaskName() : "Task";
        breadcrumbLabel.setText("Dashboard › Classes › " + cls + " › " + task);
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
            LOGGER.log(Level.SEVERE, "Failed to load class name: {0}", e.getMessage());
        }
    }

    private void loadSubmissionData() {
        String sql = "SELECT id FROM task_submissions WHERE class_task_id = ? AND user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentTaskId);
            stmt.setInt(2, currentUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    currentSubmissionId = rs.getInt("id");
                    loadAttachments();
                    updateSubmissionStatus();
                } else {
                    createNewSubmission();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load submission data: {0}", e.getMessage());
        }
    }

    private void createNewSubmission() {
        String sql = "INSERT INTO task_submissions (class_task_id, user_id, submission_status) VALUES (?, ?, 'DRAFT')";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, currentTaskId);
            stmt.setInt(2, currentUserId);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    currentSubmissionId = rs.getInt(1);
                    loadAttachments();
                    updateSubmissionStatus();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create submission: {0}", e.getMessage());
        }
    }

    private void loadAttachments() {
        attachments.clear();
        String sql = "SELECT id, submission_id, file_name, file_path, uploaded_at FROM task_attachments WHERE submission_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentSubmissionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime uploadedAt = rs.getTimestamp("uploaded_at").toLocalDateTime();
                    attachments.add(new TaskAttachment(
                        rs.getInt("id"),
                        rs.getInt("submission_id"),
                        rs.getString("file_name"),
                        rs.getString("file_path"),
                        uploadedAt
                    ));
                }
            }
            LOGGER.log(Level.INFO, "Loaded {0} attachments", attachments.size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load attachments: {0}", e.getMessage());
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
        updateSubmissionStatus();
    }

    private void updateSubmissionStatus() {
        if (submissionStatusLabel != null) {
            submissionStatusLabel.getStyleClass().removeAll(
                "status-badge", "status-badge-pending", "status-badge-review", "status-badge-approved", "status-badge-rejected"
            );
            submissionStatusLabel.getStyleClass().addAll("status-badge", "status-badge-pending");
        }
        if (currentSubmissionId == null) {
            if (submissionStatusLabel != null) {
                submissionStatusLabel.setText("Submission: Draft — add files if needed, then submit.");
            }
            if (markDoneButton != null) {
                markDoneButton.setDisable(false);
            }
            if (uploadFileButton != null) {
                uploadFileButton.setDisable(false);
            }
            return;
        }
        String sql = "SELECT submission_status FROM task_submissions WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentSubmissionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("submission_status");
                    String label = formatSubmissionStatus(status);
                    if (submissionStatusLabel != null) {
                        submissionStatusLabel.setText(label);
                        submissionStatusLabel.getStyleClass().removeAll(
                            "status-badge", "status-badge-pending", "status-badge-review", "status-badge-approved", "status-badge-rejected"
                        );
                        submissionStatusLabel.getStyleClass().addAll("status-badge", statusStyleClass(status));
                    }

                    boolean locked = "SUBMITTED".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status);
                    if (markDoneButton != null) {
                        markDoneButton.setDisable(locked);
                    }
                    if (uploadFileButton != null) {
                        uploadFileButton.setDisable(locked);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update submission status: {0}", e.getMessage());
        }
    }

    private static String formatSubmissionStatus(String raw) {
        if (raw == null) {
            return "Submission: Draft";
        }
        return switch (raw.toUpperCase()) {
            case "DRAFT" -> "Submission: Draft — not sent for approval yet.";
            case "SUBMITTED" -> "Submission: Submitted — waiting for instructor review.";
            case "APPROVED" -> "Submission: Approved.";
            case "REJECTED" -> "Submission: Rejected — you can update files and submit again.";
            default -> "Submission: " + raw;
        };
    }

    private static String statusStyleClass(String raw) {
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

    private void setupAttachmentsListView() {
        attachmentsListView.setItems(attachments);
        attachmentsListView.setCellFactory(lv -> new AttachmentListCell());
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleUploadFile() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File to Upload");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Files", "*.*"),
                    new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                    new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"),
                    new FileChooser.ExtensionFilter("Documents", "*.doc", "*.docx", "*.txt")
            );

            Stage stage = (Stage) uploadFileButton.getScene().getWindow();
            File selectedFile = fileChooser.showOpenDialog(stage);

            if (selectedFile != null) {
                if (selectedFile.length() > 20 * 1024 * 1024) {
                    UiDialogs.error(window(), "File too large", "File size must not exceed 20MB.");
                    return;
                }

                String baseName = selectedFile.getName();
                int dot = baseName.lastIndexOf('.');
                String ext = dot >= 0 ? baseName.substring(dot) : "";
                String filename = currentUserId + "_" + System.currentTimeMillis() + ext;
                Path sourcePath = selectedFile.toPath();
                Path destPath = Paths.get(TASK_UPLOADS_DIR, filename);

                Files.copy(sourcePath, destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                saveAttachmentToDatabase(destPath.toString(), selectedFile.getName());

                UiDialogs.info(window(), "File uploaded", "Your file was added to this submission.");
                loadAttachments();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to upload file: {0}", e.getMessage());
            UiDialogs.error(window(), "Upload failed", e.getMessage());
        }
    }

    private void saveAttachmentToDatabase(String filePath, String fileName) {
        String sql = "INSERT INTO task_attachments (submission_id, file_name, file_path) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentSubmissionId);
            stmt.setString(2, fileName);
            stmt.setString(3, filePath);
            stmt.executeUpdate();
            LOGGER.log(Level.INFO, "Attachment saved to database: {0}", fileName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save attachment to database: {0}", e.getMessage());
        }
    }

    @FXML
    private void handleRemoveAttachment() throws Exception {
        TaskAttachment selected = attachmentsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiDialogs.warn(window(), "Nothing selected", "Select a file to remove.");
            return;
        }

        try {
            String sql = "DELETE FROM task_attachments WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, selected.getId());
                stmt.executeUpdate();
            }

            File file = new File(selected.getFilePath());
            if (file.exists()) {
                file.delete();
            }

            LOGGER.log(Level.INFO, "Attachment removed: {0}", selected.getFileName());
            loadAttachments();
            UiDialogs.info(window(), "Removed", "The file was removed from your submission.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to remove attachment: {0}", e.getMessage());
            UiDialogs.error(window(), "Removal failed", e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleMarkDone() throws Exception {
        if (!UiDialogs.confirm(window(), "Submit this task?",
            "Submit for approval? While your submission is still a draft you can add or remove files. After you submit, your instructor will review it and you cannot change files until they act on it.")) {
            return;
        }
        try {
            String sql = "UPDATE task_submissions SET submission_status = 'SUBMITTED' WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentSubmissionId);
                stmt.executeUpdate();
            }

            sql = "UPDATE tasks SET status = 'For Approval', class_task_id = ? WHERE user_id = ? AND class_task_id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentTaskId);
                stmt.setInt(2, currentUserId);
                stmt.setInt(3, currentTaskId);
                stmt.executeUpdate();
            }

            LOGGER.info("Task marked as done - status changed to For Approval");
            updateSubmissionStatus();
            UiDialogs.info(window(), "Task submitted", "Your task was submitted successfully and is waiting for approval.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to mark task as done: {0}", e.getMessage());
            UiDialogs.error(window(), "Submission failed", e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
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
                loadSubmissionData();
                updateUI();
            })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        LOGGER.info("Task detail auto-refresh started");
    }

    private void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            LOGGER.info("Task detail auto-refresh stopped");
        }
    }

    private class AttachmentListCell extends ListCell<TaskAttachment> {
        @Override
        protected void updateItem(TaskAttachment item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox hbox = new HBox(10);
                Label fileLabel = new Label(item.getFileName());
                fileLabel.setStyle("-fx-font-size: 12;");
                
                Button removeBtn = new Button("Remove");
                removeBtn.getStyleClass().addAll("button", "button-danger");
                removeBtn.setOnAction(e -> {
                    attachmentsListView.getSelectionModel().select(item);
                    try {
                        handleRemoveAttachment();
                    } catch (Exception e1) {
                    }
                });

                HBox.setHgrow(fileLabel, Priority.ALWAYS);
                hbox.getChildren().addAll(fileLabel, removeBtn);
                setGraphic(hbox);
            }
        }
    }
}
