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
import com.cc103sys.cc103.Utils.UiDialogs;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;

public class ClassDetailController {

    private static final Logger LOGGER = Logger.getLogger(ClassDetailController.class.getName());
    private static final int BASE_TASK_POINTS = 10;
    private static final int LATE_TASK_POINTS = 5;
    private static final int REFRESH_INTERVAL_SECONDS = 5; // Auto-refresh every 5 seconds

    @FXML private Label breadcrumbLabel;
    @FXML private Label classTitleLabel;
    @FXML private Label classCodeLabel;
    @FXML private Label classDescriptionLabel;
    @FXML private Label classVisibilityLabel;
    @FXML private Label classMetricsLabel;
    @FXML private ListView<ClassTask> tasksListView;
    @FXML private ListView<String> participantsListView;
    @FXML private Label formTitleLabel;
    @FXML private Label participantTaskHint;
    @FXML private VBox ownerTaskFormCard;
    @FXML private HBox ownerTaskActionsRow;
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
    private boolean disposed;

    @FXML
    public void initialize() {
        setupTaskListView();
        setupEmptyPlaceholders();
        loadClassDetails();
        loadClassTasks();
        loadParticipants();
        startAutoRefresh();
        registerLifecycleHooks();

        // Set navbar active
        NavbarController.getInstance().setActive("classes");

        LOGGER.info("Class detail view initialized");
    }

    private void registerLifecycleHooks() {
        if (classTitleLabel != null) {
            classTitleLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
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
        LOGGER.info("Class detail resources cleaned up");
    }

    private void setupEmptyPlaceholders() {
        Label noTasks = new Label("No class tasks yet.\nThe host can add tasks on the right, or check back later.");
        noTasks.getStyleClass().add("empty-state");
        noTasks.setWrapText(true);
        if (tasksListView != null) {
            tasksListView.setPlaceholder(noTasks);
        }
        Label noPeople = new Label("No participants yet.");
        noPeople.getStyleClass().add("empty-state");
        noPeople.setWrapText(true);
        if (participantsListView != null) {
            participantsListView.setPlaceholder(noPeople);
        }
    }

    private Window window() {
        return classTitleLabel != null && classTitleLabel.getScene() != null
            ? classTitleLabel.getScene().getWindow()
            : null;
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
                        classCodeLabel.setText(joinCode != null && !joinCode.isBlank() ? "Code: " + joinCode : "Code: — (public listing)");
                    }
                    Integer uid = getCurrentUserId();
                    isOwner = uid != null && rs.getInt("owner_id") == uid;
                    int isPublic = rs.getInt("is_public");
                    String visibility = isPublic == 1 ? "Public" : "Private";
                    if (classVisibilityLabel != null) {
                        classVisibilityLabel.setText(visibility);
                        classVisibilityLabel.getStyleClass().removeAll(
                            "status-badge", "status-badge-public", "status-badge-private", "status-badge-neutral"
                        );
                        classVisibilityLabel.getStyleClass().addAll(
                            "status-badge",
                            isPublic == 1 ? "status-badge-public" : "status-badge-private"
                        );
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to load class details: " + e.getMessage());
        }

        if (breadcrumbLabel != null && classTitleLabel != null) {
            breadcrumbLabel.setText("Dashboard › Classes › " + classTitleLabel.getText());
        }

        // For now, set a default description. In a real app, you'd have a description field in classes table
        classDescriptionLabel.setText("Welcome to " + classTitleLabel.getText() + ". Here you can manage class tasks and assignments.");
        loadParticipants();
        applyOwnerParticipantUi();
    }

    private void applyOwnerParticipantUi() {
        boolean owner = isOwner;
        if (ownerTaskFormCard != null) {
            ownerTaskFormCard.setVisible(owner);
            ownerTaskFormCard.setManaged(owner);
        }
        if (formTitleLabel != null) {
            formTitleLabel.setVisible(owner);
            formTitleLabel.setManaged(owner);
        }
        if (participantTaskHint != null) {
            participantTaskHint.setVisible(!owner);
            participantTaskHint.setManaged(!owner);
        }
        if (ownerTaskActionsRow != null) {
            ownerTaskActionsRow.setVisible(owner);
            ownerTaskActionsRow.setManaged(owner);
        }
        if (submitTaskButton != null && owner) {
            submitTaskButton.setText(editingTask != null ? "Update Task" : "Add Task");
        }
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
        refreshClassMetrics();
    }

    private void refreshClassMetrics() {
        if (classMetricsLabel == null) {
            return;
        }
        int taskCount = classTasks.size();
        int participantCount = participantsListView != null ? participantsListView.getItems().size() : 0;
        classMetricsLabel.setText(participantCount + " participant(s) · " + taskCount + " task(s)");
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
        if (!isOwner) {
            UiDialogs.warn(window(), "Not allowed", "Only the class host can add or edit class tasks here.");
            return;
        }
        String title = taskTitleField.getText().trim();
        if (title.isEmpty()) {
            UiDialogs.warn(window(), "Missing title", "Please enter a task title.");
            return;
        }

        String description = taskDescriptionArea.getText().trim();
        LocalDate dueDate = taskDueDatePicker.getValue();
        if (dueDate == null) {
            UiDialogs.warn(window(), "Missing due date", "Please choose a due date.");
            return;
        }

        if (editingTask != null) {
            updateClassTask(editingTask.getId(), title, description, dueDate);
            UiDialogs.info(window(), "Saved", "Task updated successfully.");
        } else {
            createClassTask(title, description, dueDate);
            UiDialogs.info(window(), "Created", "Task added and assigned to class members.");
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
        if (!isOwner) {
            return;
        }
        ClassTask selected = tasksListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiDialogs.warn(window(), "Nothing selected", "Select a task to edit.");
            return;
        }

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
        if (!isOwner) {
            return;
        }
        ClassTask selected = tasksListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiDialogs.warn(window(), "Nothing selected", "Select a task to delete.");
            return;
        }
        if (!UiDialogs.confirm(window(), "Delete task?",
            "Remove \"" + selected.getTaskName() + "\" from this class? This cannot be undone.")) {
            return;
        }

        deleteClassTask(selected.getId());
        loadClassTasks();
        UiDialogs.info(window(), "Deleted", "The class task was removed.");
    }

    private void createClassTask(String title, String description, LocalDate dueDate) {
        Integer ownerId = getCurrentUserId();
        if (ownerId == null) {
            return;
        }

        String sql = "INSERT INTO class_tasks (class_id, task_name, description, due_date, owner_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, currentClassId);
            stmt.setString(2, title);
            stmt.setString(3, description.isEmpty() ? null : description);
            stmt.setDate(4, Date.valueOf(dueDate));
            stmt.setInt(5, ownerId);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                assignTaskToClassMembers(conn, keys);
            }

            LOGGER.info("Class task created successfully");
        } catch (Exception e) {
            LOGGER.severe("Failed to create class task: " + e.getMessage());
            UiDialogs.error(window(), "Could not create task", e.getMessage());
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

    private void assignTaskToClassMembers(Connection conn, ResultSet generatedKeys) throws java.sql.SQLException {
        if (generatedKeys == null || !generatedKeys.next()) {
            return;
        }
        int classTaskId = generatedKeys.getInt(1);
        String sql = "INSERT INTO tasks (username, user_id, task_name, task_date, status, class_id, class_task_id, created_by) "
                   + "SELECT u.username, u.id, ct.task_name, ct.due_date, 'Pending', ct.class_id, ct.id, ct.owner_id "
                   + "FROM class_tasks ct "
                   + "JOIN users u ON u.id IN (SELECT user_id FROM user_classes WHERE class_id = ct.class_id) "
                   + "WHERE ct.id = ? "
                   + "AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.class_task_id = ct.id AND t.user_id = u.id)";

        try (PreparedStatement assignStmt = conn.prepareStatement(sql)) {
            assignStmt.setInt(1, classTaskId);
            assignStmt.executeUpdate();
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
        applyOwnerParticipantUi();
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
        refreshClassMetrics();
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
                setGraphic(null);
            } else {
                // Create button for View Task action
                javafx.scene.control.Button viewButton = new javafx.scene.control.Button("View Task");
                viewButton.getStyleClass().addAll("button", "button-secondary");
                boolean hostOfClass = ClassDetailController.this.isOwner;
                viewButton.setText(hostOfClass ? "Review Submissions" : "View Task");
                viewButton.setOnAction(e -> {
                    if (hostOfClass) {
                        Navigator.navigateToTaskReview(currentClassId, task.getId());
                    } else {
                        Navigator.navigateToTaskDetail(currentClassId, task.getId());
                    }
                });

                // Create label with task info
                javafx.scene.control.Label taskLabel = new javafx.scene.control.Label();
                String description = task.getDescription() != null && !task.getDescription().isEmpty()
                    ? "\n" + task.getDescription()
                    : "";
                taskLabel.setText(task.getTaskName() + " (Due: " + task.getDueDate() + ")" + description);
                taskLabel.setWrapText(true);

                // Create container with label and button
                javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(12, taskLabel, viewButton);
                container.setStyle("-fx-alignment: CENTER_LEFT;");
                javafx.scene.layout.HBox.setHgrow(taskLabel, javafx.scene.layout.Priority.ALWAYS);

                setText(null);
                setGraphic(container);
            }
        }
    }
}