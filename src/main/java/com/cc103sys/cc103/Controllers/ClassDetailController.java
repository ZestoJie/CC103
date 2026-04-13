package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.ClassTask;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class ClassDetailController {

    private static final Logger LOGGER = Logger.getLogger(ClassDetailController.class.getName());
    private static final int BASE_TASK_POINTS = 10;
    private static final int LATE_TASK_POINTS = 5;
    private static final int REFRESH_INTERVAL_SECONDS = 5; // Auto-refresh every 5 seconds

    @FXML private Label classTitleLabel;
    @FXML private Label classCodeLabel;
    @FXML private Label classDescriptionLabel;
    @FXML private Label classVisibilityLabel;
    @FXML private ListView<ClassTask> tasksListView;
    @FXML private ListView<String> participantsListView;
    @FXML private Label formTitleLabel;
    @FXML private TextField taskTitleField;
    @FXML private TextArea taskDescriptionArea;
    @FXML private DatePicker taskDueDatePicker;
    @FXML private Button submitTaskButton;
    @FXML private Button cancelEditButton;
    @FXML private Button editTaskButton;
    @FXML private Button deleteTaskButton;
    private int currentClassId = -1;
    private ClassTask editingTask = null;
    private final ObservableList<ClassTask> classTasks = FXCollections.observableArrayList();
    private boolean isOwner = false;
    private Timeline refreshTimeline;

    @FXML
    public void initialize() {
        setupTaskListView();
        loadClassDetails();
        loadClassTasks();
        loadParticipants();
        startAutoRefresh();

        // Set navbar active
        NavbarController.getInstance().setActive("classes");

        LOGGER.info("Class detail view initialized");
    }

    private void startAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(REFRESH_INTERVAL_SECONDS), e -> {
                loadClassTasks();
                loadParticipants();
            })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        LOGGER.info("Class detail auto-refresh timeline started (interval: " + REFRESH_INTERVAL_SECONDS + " seconds)");
    }

    private void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            LOGGER.info("Class detail auto-refresh timeline stopped");
        }
    }

    private void setupTaskListView() {
        tasksListView.setItems(classTasks);
        tasksListView.setCellFactory(lv -> new TaskListCell());
    }

    private void loadClassDetails() {
        currentClassId = Session.getCurrentClassId();
        if (currentClassId == -1) {
            LOGGER.warning("No class ID set for detail view");
            return;
        }

        String sql = "SELECT class_name, join_code, owner_id, is_public FROM classes WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentClassId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    classTitleLabel.setText(rs.getString("class_name"));
                    String joinCode = rs.getString("join_code");
                    if (classCodeLabel != null) {
                        classCodeLabel.setText(joinCode != null && !joinCode.isBlank() ? "Code: " + joinCode : "Code: Public class");
                    }
                    isOwner = rs.getInt("owner_id") == getCurrentUserId();
                    int isPublic = rs.getInt("is_public");
                    String visibility = isPublic == 1 ? "Public Class" : "Private Class";
                    if (classVisibilityLabel != null) {
                        classVisibilityLabel.setText(visibility);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to load class details: " + e.getMessage());
        }

        // Approval system moved to Dashboard

        // For now, set a default description. In a real app, you'd have a description field in classes table
        classDescriptionLabel.setText("Welcome to " + classTitleLabel.getText() + ". Here you can manage class tasks and assignments.");
        loadParticipants();
    }

    private void loadClassTasks() {
        classTasks.clear();
        if (currentClassId == -1) return;

        String sql = "SELECT id, class_id, task_name, description, due_date, owner_id, created_at FROM class_tasks WHERE class_id = ? ORDER BY due_date ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentClassId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    classTasks.add(new ClassTask(
                        rs.getInt("id"),
                        rs.getInt("class_id"),
                        rs.getString("task_name"),
                        rs.getString("description"),
                        rs.getDate("due_date").toLocalDate(),
                        rs.getInt("owner_id"),
                        rs.getString("created_at")
                    ));
                }
            }
            LOGGER.info("Loaded " + classTasks.size() + " class tasks");
        } catch (Exception e) {
            LOGGER.severe("Failed to load class tasks: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void goBack() {
        stopAutoRefresh();
        Session.setCurrentClassId(-1);
        Navigator.navigateTo("Classes");
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleSubmitTask() {
        String title = taskTitleField.getText().trim();
        if (title.isEmpty()) {
            return;
        }

        String description = taskDescriptionArea.getText().trim();
        LocalDate dueDate = taskDueDatePicker.getValue();

        if (editingTask != null) {
            // Update existing task
            updateClassTask(editingTask.getId(), title, description, dueDate);
        } else {
            // Create new task
            createClassTask(title, description, dueDate);
        }

        clearForm();
        loadClassTasks();
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleCancelEdit() {
        clearForm();
        editingTask = null;
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleEditTask() {
        ClassTask selected = tasksListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        editingTask = selected;
        formTitleLabel.setText("Edit Task");
        taskTitleField.setText(selected.getTaskName());
        taskDescriptionArea.setText(selected.getDescription());
        taskDueDatePicker.setValue(selected.getDueDate());
        submitTaskButton.setText("Update Task");
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleDeleteTask() {
        ClassTask selected = tasksListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        deleteClassTask(selected.getId());
        loadClassTasks();
    }

    private void createClassTask(String title, String description, LocalDate dueDate) {
        Integer ownerId = getCurrentUserId();
        if (ownerId == null) return;

        String sql = "INSERT INTO class_tasks (class_id, task_name, description, due_date, owner_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentClassId);
            stmt.setString(2, title);
            stmt.setString(3, description);
            stmt.setDate(4, Date.valueOf(dueDate));
            stmt.setInt(5, ownerId);
            stmt.executeUpdate();

            // Assign task to all class members
            assignTaskToClassMembers(stmt.getGeneratedKeys());

            LOGGER.info("Class task created successfully");
        } catch (Exception e) {
            LOGGER.severe("Failed to create class task: " + e.getMessage());
        }
    }

    private void updateClassTask(int taskId, String title, String description, LocalDate dueDate) {
        String sql = "UPDATE class_tasks SET task_name = ?, description = ?, due_date = ? WHERE id = ? AND class_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setString(2, description);
            stmt.setDate(3, Date.valueOf(dueDate));
            stmt.setInt(4, taskId);
            stmt.setInt(5, currentClassId);
            stmt.executeUpdate();

            LOGGER.info("Class task updated successfully");
        } catch (Exception e) {
            LOGGER.severe("Failed to update class task: " + e.getMessage());
        }
    }

    private void deleteClassTask(int taskId) {
        String sql = "DELETE FROM class_tasks WHERE id = ? AND class_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, currentClassId);
            stmt.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe("Failed to delete class task: " + e.getMessage());
        }
    }

    private void assignTaskToClassMembers(ResultSet generatedKeys) {
        try {
            if (generatedKeys.next()) {
                int classTaskId = generatedKeys.getInt(1);
                String sql = "INSERT INTO tasks (username, user_id, task_name, task_date, status, class_id, class_task_id, created_by) "
                           + "SELECT u.username, u.id, ct.task_name, ct.due_date, 'Pending', ct.class_id, ct.id, ct.owner_id "
                           + "FROM class_tasks ct "
                           + "JOIN users u ON u.id IN (SELECT user_id FROM user_classes WHERE class_id = ct.class_id) "
                           + "WHERE ct.id = ? "
                           + "AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.class_task_id = ct.id AND t.user_id = u.id)";

                try (PreparedStatement assignStmt = DBUtil.getConnection().prepareStatement(sql)) {
                    assignStmt.setInt(1, classTaskId);
                    assignStmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to assign task to class members: " + e.getMessage());
        }
    }

    private void clearForm() {
        taskTitleField.clear();
        taskDescriptionArea.clear();
        taskDueDatePicker.setValue(null);
        formTitleLabel.setText("Add New Task");
        submitTaskButton.setText("Add Task");
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
        editingTask = null;
    }

    private void loadParticipants() {
        if (currentClassId == -1 || participantsListView == null) {
            return;
        }
        participantsListView.getItems().clear();
        String sql = "SELECT u.username FROM users u "
                   + "JOIN user_classes uc ON u.id = uc.user_id "
                   + "WHERE uc.class_id = ? ORDER BY u.username";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentClassId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    participantsListView.getItems().add(rs.getString("username"));
                }
            }
            LOGGER.info("Loaded " + participantsListView.getItems().size() + " participants");
        } catch (Exception e) {
            LOGGER.severe("Failed to load class participants: " + e.getMessage());
        }
    }

    private Integer getCurrentUserId() {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to get current user ID: " + e.getMessage());
        }
        return null;
    }



    private class TaskListCell extends ListCell<ClassTask> {
        @Override
        protected void updateItem(ClassTask task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setText(null);
            } else {
                String description = task.getDescription() != null && !task.getDescription().isEmpty()
                    ? "\n" + task.getDescription()
                    : "";
                setText(task.getTaskName() + " (Due: " + task.getDueDate() + ")" + description);
            }
        }
    }
}