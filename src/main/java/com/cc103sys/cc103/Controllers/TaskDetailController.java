package com.cc103sys.cc103.Controllers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.ClassTask;
import com.cc103sys.cc103.Models.TaskAttachment;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class TaskDetailController {

    private static final Logger LOGGER = Logger.getLogger(TaskDetailController.class.getName());
    private static final String TASK_UPLOADS_DIR = "task_submissions";
    private static final int REFRESH_INTERVAL_SECONDS = 5;

    @FXML private Label classNameLabel;
    @FXML private Label taskTitleLabel;
    @FXML private Label dueDateLabel;
    @FXML private TextArea taskDescriptionArea;
    @FXML private Button uploadFileButton;
    @FXML private Button markDoneButton;
    @FXML private Button backButton;
    @FXML private ListView<TaskAttachment> attachmentsListView;
    @FXML private Label submissionStatusLabel;

    private ClassTask currentTask;
    private Integer currentClassId;
    private Integer currentTaskId;
    private Integer currentUserId;
    private Integer currentSubmissionId;
    private ObservableList<TaskAttachment> attachments = FXCollections.observableArrayList();
    private Timeline refreshTimeline;

    @FXML
    public void initialize() {
        // Navbar setup
        NavbarController.getInstance().setActive("classes");
        setupAttachmentsListView();
        LOGGER.info("Task detail view initialized");
    }

    public void setTaskData(Integer classId, Integer taskId) {
        this.currentClassId = classId;
        this.currentTaskId = taskId;
        
        // Get user ID from database using username
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
            LOGGER.severe("Failed to get user ID: " + e.getMessage());
        }
        
        // Create uploads directory if doesn't exist
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

                    // Load class name
                    loadClassName();
                    updateUI();
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to load task details: " + e.getMessage());
        }
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
            LOGGER.severe("Failed to load class name: " + e.getMessage());
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
                    // Create new submission if doesn't exist
                    createNewSubmission();
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to load submission data: " + e.getMessage());
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
            LOGGER.severe("Failed to create submission: " + e.getMessage());
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
            LOGGER.info("Loaded " + attachments.size() + " attachments");
        } catch (Exception e) {
            LOGGER.severe("Failed to load attachments: " + e.getMessage());
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
        if (currentSubmissionId == null) {
            submissionStatusLabel.setText("Status: DRAFT");
            markDoneButton.setDisable(false);
            return;
        }
        String sql = "SELECT submission_status FROM task_submissions WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentSubmissionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("submission_status");
                    submissionStatusLabel.setText("Status: " + status);
                    
                    // Disable Mark as Done if already submitted
                    markDoneButton.setDisable("SUBMITTED".equals(status));
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to update submission status: " + e.getMessage());
        }
    }

    private void setupAttachmentsListView() {
        attachmentsListView.setItems(attachments);
        attachmentsListView.setCellFactory(lv -> new AttachmentListCell());
    }

    @FXML
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
                // Validate file size (20MB max for task submissions)
                if (selectedFile.length() > 20 * 1024 * 1024) {
                    showAlert("Error", "File Too Large", "File size must not exceed 20MB.");
                    return;
                }

                // Copy file to submissions directory
                String filename = currentUserId + "_" + System.currentTimeMillis() + 
                                 selectedFile.getName().substring(selectedFile.getName().lastIndexOf('.'));
                Path sourcePath = selectedFile.toPath();
                Path destPath = Paths.get(TASK_UPLOADS_DIR, filename);

                Files.copy(sourcePath, destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Save to database
                saveAttachmentToDatabase(destPath.toString(), selectedFile.getName());

                showAlert("Success", "File Uploaded", "File uploaded successfully!");
                loadAttachments();
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to upload file: " + e.getMessage());
            showAlert("Error", "Upload Failed", e.getMessage());
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
            LOGGER.info("Attachment saved to database: " + fileName);
        } catch (Exception e) {
            LOGGER.severe("Failed to save attachment to database: " + e.getMessage());
        }
    }

    @FXML
    private void handleRemoveAttachment() {
        TaskAttachment selected = attachmentsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "No Selection", "Please select a file to remove.");
            return;
        }

        try {
            // Delete from database
            String sql = "DELETE FROM task_attachments WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, selected.getId());
                stmt.executeUpdate();
            }

            // Delete file from disk
            File file = new File(selected.getFilePath());
            if (file.exists()) {
                file.delete();
            }

            LOGGER.info("Attachment removed: " + selected.getFileName());
            loadAttachments();
            showAlert("Success", "Removed", "File removed successfully!");
        } catch (Exception e) {
            LOGGER.severe("Failed to remove attachment: " + e.getMessage());
            showAlert("Error", "Removal Failed", e.getMessage());
        }
    }

    @FXML
    private void handleMarkDone() {
        try {
            // Update submission status to SUBMITTED
            String sql = "UPDATE task_submissions SET submission_status = 'SUBMITTED' WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, currentSubmissionId);
                stmt.executeUpdate();
            }

            // Update corresponding task status to "For Approval"
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
            showAlert("Success", "Task Submitted", "Task submitted for approval!");
        } catch (Exception e) {
            LOGGER.severe("Failed to mark task as done: " + e.getMessage());
            showAlert("Error", "Submission Failed", e.getMessage());
        }
    }

    @FXML
    private void handleGoBack() {
        stopAutoRefresh();
        Navigator.navigateTo("classes");
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

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Custom ListCell for displaying attachments
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
                removeBtn.setStyle("-fx-padding: 5; -fx-font-size: 10;");
                removeBtn.setOnAction(e -> {
                    attachmentsListView.getSelectionModel().select(item);
                    handleRemoveAttachment();
                });

                HBox.setHgrow(fileLabel, Priority.ALWAYS);
                hbox.getChildren().addAll(fileLabel, removeBtn);
                setGraphic(hbox);
            }
        }
    }
}
