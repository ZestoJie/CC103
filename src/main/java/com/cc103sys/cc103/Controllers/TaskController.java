package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.Classes;
import com.cc103sys.cc103.Models.Task;
import com.cc103sys.cc103.Utils.Session;
import com.cc103sys.cc103.Utils.TimerService;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class TaskController implements TimerService.TimerListener {
    private static final Logger LOGGER = Logger.getLogger(TaskController.class.getName());
    private static final int BASE_TASK_POINTS = 10;
    private static final int LATE_TASK_POINTS = 5;
    private static final int REFRESH_INTERVAL_SECONDS = 5; // Auto-refresh every 5 seconds

    private TimerService timerService;
    private Task timerTask;

    @FXML
    private TextField taskField;
    @FXML
    private DatePicker taskDate;
    @FXML
    private ComboBox<Classes> classFilterComboBox;
    @FXML
    private ListView<Task> allTaskList;
    @FXML
    private ListView<Task> filteredTaskList;
    @FXML
    @SuppressWarnings("unused")
    private Button addTaskBtn;
    @FXML
    @SuppressWarnings("unused")
    private Button markDoneBtn;
    @FXML
    @SuppressWarnings("unused")
    private Button deleteTaskBtn;
    
    // Tab Navigation
    @FXML
    private Button tabMyTasksBtn;
    @FXML
    private Button tabApprovalsBtn;
    @FXML
    private VBox myTasksView;
    @FXML
    private VBox approvalsView;
    
    // Task Details Display
    @FXML
    private TextArea taskDescriptionArea;
    @FXML
    private TextArea taskInstructions;
    
    // Attachment Management
    @FXML
    private Button uploadAttachmentBtn;
    @FXML
    private ListView<String> attachmentsList;
    
    // Timer Controls
    @FXML
    private Button startTimerBtn;
    @FXML
    private Button saveTaskInfoBtn;
    @FXML
    private ComboBox<String> timerDuration;
    
    // Approval Management (for hosts/teachers)
    @FXML
    private ListView<Task> pendingApprovalsList;
    @FXML
    private Button approveSelectedBtn;
    @FXML
    private Button rejectSelectedBtn;
    @FXML
    private ComboBox<Classes> approvalClassFilterComboBox;

    private final ObservableList<Task> allTasks = FXCollections.observableArrayList();
    private final ObservableList<Task> filteredTasks = FXCollections.observableArrayList();
    private final ObservableList<Task> pendingApprovals = FXCollections.observableArrayList();
    private final ObservableList<Classes> userClasses = FXCollections.observableArrayList();
    private Timeline refreshTimeline;

    @FXML
    public void initialize() {
        timerService = TimerService.getInstance();
        timerService.removeTimerListener(this);
        timerService.addTimerListener(this);
        
        setupRoleBasedUI();
        setupTaskListView();
        setupTimerDropdown();
        loadUserClasses();
        
        // Initialize Tabs Logic
        switchToMyTasksView();
        
        loadAllClassTasks();
        loadFilteredTasks();
        
        if (Session.isHost()) {
            loadPendingApprovals();
            if (approvalClassFilterComboBox != null) {
                approvalClassFilterComboBox.setItems(userClasses);
                approvalClassFilterComboBox.setOnAction(e -> loadPendingApprovals());
            }
        }
        
        startAutoRefresh();
        // Set navbar active to tasks
        NavbarController.getInstance().setActive("tasks");
        LOGGER.info("Task scene initialized successfully");
    }

    private void startAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(REFRESH_INTERVAL_SECONDS), e -> {
                loadAllClassTasks();
                loadFilteredTasks();
            })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        LOGGER.info("Auto-refresh timeline started (interval: " + REFRESH_INTERVAL_SECONDS + " seconds)");
    }

    private void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            LOGGER.info("Auto-refresh timeline stopped");
        }
    }

    private void setupRoleBasedUI() {
        // Show/hide approvals for hosts only
        boolean isHost = Session.isHost();
        
        if (tabApprovalsBtn != null) {
            tabApprovalsBtn.setVisible(isHost);
            tabApprovalsBtn.setManaged(isHost);
        }
        if (approvalsView != null) {
            approvalsView.setVisible(false);
            approvalsView.setManaged(false);
        }
        
        // All users can add personal tasks from the Task page.
        // Host-only class task creation remains on the Dashboard.
    }

    private void setupTaskListView() {
        if (allTaskList != null) {
            allTaskList.setItems(allTasks);
            allTaskList.setCellFactory(param -> createTaskListCell());
        }
        if (filteredTaskList != null) {
            filteredTaskList.setItems(filteredTasks);
            filteredTaskList.setCellFactory(param -> createTaskListCell());
        }
    }

    private ListCell<Task> createTaskListCell() {
        return new javafx.scene.control.ListCell<Task>() {
            private final Label taskLabel = new Label();
            private final Button actionButton = new Button();
            private final HBox container = new HBox(12, taskLabel, actionButton);

            {
                container.setStyle("-fx-alignment: CENTER_LEFT;");
                actionButton.setOnAction(e -> {
                    Task task = getItem();
                    if (task == null) return;
                    if ("Done".equalsIgnoreCase(task.getStatus()) || "For Approval".equalsIgnoreCase(task.getStatus())) {
                        undoTaskCompletion(task);
                    } else {
                        completeTask(task);
                    }
                });
            }

            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    taskLabel.setText(formatTaskWithClass(task));
                    boolean isCompleted = "Done".equalsIgnoreCase(task.getStatus()) || "For Approval".equalsIgnoreCase(task.getStatus());
                    if (isCompleted) {
                        taskLabel.setStyle("-fx-text-fill: #6b7280; -fx-opacity: 0.7;");
                        actionButton.setText("Undo");
                        actionButton.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white;");
                        if ("For Approval".equalsIgnoreCase(task.getStatus())) {
                            taskLabel.setText(formatTaskWithClass(task) + " (For Approval)");
                        } else {
                            taskLabel.setText(formatTaskWithClass(task));
                        }
                    } else {
                        taskLabel.setStyle("-fx-text-fill: black;");
                        actionButton.setText("Mark Done");
                        actionButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
                        taskLabel.setText(formatTaskWithClass(task));
                    }
                    setText(null);
                    setGraphic(container);
                }
            }
        };
    }

    private void completeTask(Task task) {
        updateTaskStatus(task, "For Approval");
    }

    private void undoTaskCompletion(Task task) {
        updateTaskStatus(task, "Pending");
    }

    private void updateTaskStatus(Task task, String newStatus) {
        if (task == null || task.getId() <= 0) {
            return;
        }

        if ("For Approval".equalsIgnoreCase(newStatus) && "For Approval".equalsIgnoreCase(task.getStatus())) {
            return;
        }

        if ("Pending".equalsIgnoreCase(newStatus) && !"Done".equalsIgnoreCase(task.getStatus()) && !"For Approval".equalsIgnoreCase(task.getStatus())) {
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            if ("For Approval".equalsIgnoreCase(newStatus)) {
                String updateSql = "UPDATE tasks SET status = 'For Approval', points_awarded = 0 WHERE id = ? AND username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setInt(1, task.getId());
                    stmt.setString(2, Session.getUsername());
                    stmt.executeUpdate();
                }

            } else {
                int awardedPoints = getTaskPointsAwarded(task.getId(), conn);
                String updateSql = "UPDATE tasks SET status = 'Pending', completed_date = NULL, points_awarded = 0 WHERE id = ? AND username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setInt(1, task.getId());
                    stmt.setString(2, Session.getUsername());
                    stmt.executeUpdate();
                }

                if (awardedPoints > 0) {
                    try (PreparedStatement stmt = conn.prepareStatement("UPDATE users SET points = GREATEST(points - ?, 0) WHERE username = ?")) {
                        stmt.setInt(1, awardedPoints);
                        stmt.setString(2, Session.getUsername());
                        stmt.executeUpdate();
                    }
                }
            }

            Session.setPoints(fetchCurrentUserPoints(conn));
            refreshNavbarUserInfo();
            reloadTaskLists();
            LOGGER.info(() -> "Updated task status to " + newStatus + " for task: " + task.getTaskName());
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to update task status: " + e.getMessage());
        }
    }

    private int getTaskPointsAwarded(int taskId, Connection conn) {
        String sql = "SELECT points_awarded FROM tasks WHERE id = ? AND username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setString(2, Session.getUsername());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("points_awarded");
                }
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to fetch task awarded points: " + e.getMessage());
        }
        return 0;
    }

    private int fetchCurrentUserPoints(Connection conn) {
        String sql = "SELECT points FROM users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("points");
                }
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to refresh current user points: " + e.getMessage());
        }
        return Session.getPoints();
    }

    private void refreshNavbarUserInfo() {
        NavbarController navbar = NavbarController.getInstance();
        if (navbar != null) {
            navbar.loadUserInfo();
        }
    }

    private int calculateTaskCompletionPoints(LocalDate dueDate) {
        if (dueDate == null) {
            return BASE_TASK_POINTS;
        }
        long daysBefore = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        if (daysBefore < 0) {
            return LATE_TASK_POINTS;
        }
        return BASE_TASK_POINTS + (int) Math.max(0, daysBefore) * 2;
    }

    private void reloadTaskLists() {
        loadAllClassTasks();
        loadFilteredTasks();
    }

    private void loadUserClasses() {
        userClasses.clear();
        String sql = "SELECT c.id, c.class_name FROM classes c "
                   + "JOIN user_classes uc ON c.id = uc.class_id "
                   + "JOIN users u ON uc.user_id = u.id "
                   + "WHERE u.username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userClasses.add(new Classes(rs.getInt("id"), rs.getString("class_name")));
                }
            }

            if (classFilterComboBox != null) {
                classFilterComboBox.setItems(userClasses);
                classFilterComboBox.setOnAction(e -> loadFilteredTasks());
                if (!userClasses.isEmpty()) {
                    classFilterComboBox.getSelectionModel().selectFirst();
                }
            }

            LOGGER.info(() -> "Loaded " + userClasses.size() + " task classes");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load task classes: " + e.getMessage());
        }
    }

    private void loadAllClassTasks() {
        allTasks.clear();
        
        try {
            // First get the current user's ID
            Integer userId = getCurrentUserId();
            if (userId == null) {
                LOGGER.warning("Could not determine current user ID for user: " + Session.getUsername());
                return;
            }
            
            LOGGER.info("Loading all tasks for user ID: " + userId + " (" + Session.getUsername() + ")");
            
            String sql = "SELECT t.id, t.task_name, t.task_date, t.status, t.class_id, COALESCE(c.class_name, '') as class_name "
                       + "FROM tasks t "
                       + "LEFT JOIN classes c ON t.class_id = c.id "
                       + "WHERE t.user_id = ? "
                       + "ORDER BY t.task_date DESC";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                LOGGER.info("Executing SQL: " + sql.replace("?", userId.toString()));
                try (ResultSet rs = stmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) {
                        int classId = rs.getInt("class_id");
                        String className = rs.getString("class_name");
                        
                        Task task = new Task(
                            rs.getInt("id"),
                            rs.getString("task_name"),
                            rs.getDate("task_date").toLocalDate(),
                            rs.getString("status"),
                            classId > 0 ? classId : null,
                            className != null && !className.isEmpty() ? className : null
                        );
                        allTasks.add(task);
                        LOGGER.info("Added task: " + rs.getString("task_name") + " (ID: " + rs.getInt("id") + ", ClassID: " + classId + ")");
                        count++;
                    }
                    LOGGER.info("Total tasks loaded: " + count);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to load tasks: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String formatTaskWithClass(Task task) {
        String classSuffix = task.getClassName() != null ? " [" + task.getClassName() + "]" : "";
        return String.format("%s - %s%s (%s)", task.getTaskName(), task.getStatus(), classSuffix, task.getDate());
    }

    private void loadFilteredTasks() {
        filteredTasks.clear();
        Classes selected = classFilterComboBox == null ? null : classFilterComboBox.getValue();
        if (selected == null) {
            return;
        }

        try {
            Integer userId = getCurrentUserId();
            if (userId == null) {
                LOGGER.warning("Could not determine current user ID");
                return;
            }

            String sql = "SELECT t.id, t.task_name, t.task_date, t.status, t.class_id, c.class_name "
                       + "FROM tasks t "
                       + "JOIN classes c ON t.class_id = c.id "
                       + "WHERE t.user_id = ? AND t.class_id = ?";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, selected.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        filteredTasks.add(new Task(
                            rs.getInt("id"),
                            rs.getString("task_name"),
                            rs.getDate("task_date").toLocalDate(),
                            rs.getString("status"),
                            rs.getInt("class_id"),
                            rs.getString("class_name")
                        ));
                    }
                }
            }
            LOGGER.info(() -> "Loaded " + filteredTasks.size() + " filtered tasks for class " + (selected == null ? "none" : selected.getClassName()));
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load filtered tasks: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleAddTask() {
        String taskName = taskField.getText().trim();
        LocalDate date = taskDate.getValue();

        if (taskName.isEmpty() || date == null) {
            return;
        }

        String sql = "INSERT INTO tasks (username, user_id, task_name, task_date, status, is_personal) VALUES (?, ?, ?, ?, 'Pending', 1)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
            
            // Get user ID
            Integer userId = getCurrentUserId();
            if (userId != null) {
                stmt.setInt(2, userId);
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }
            
            stmt.setString(3, taskName);
            stmt.setDate(4, java.sql.Date.valueOf(date));
            stmt.executeUpdate();

            taskField.clear();
            taskDate.setValue(null);
            loadAllClassTasks();
            loadFilteredTasks();
            LOGGER.info("Personal task added successfully");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to add task: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleMarkDone() {
        Task selectedTask = getSelectedTask();
        if (selectedTask == null) {
            return;
        }
        // Tasks go to "For Approval" status instead of immediately being marked Done
        updateTaskStatus(selectedTask, "For Approval");
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleDeleteTask() {
        Task selectedTask = getSelectedTask();
        if (selectedTask == null) {
            return;
        }

        String sql = "DELETE FROM tasks WHERE id = ? AND username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selectedTask.getId());
            stmt.setString(2, Session.getUsername());
            stmt.executeUpdate();

            loadAllClassTasks();
            loadFilteredTasks();
            LOGGER.info("Task deleted");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to delete task: " + e.getMessage());
        }
    }

    private Task getSelectedTask() {
        Task selected = null;
        if (filteredTaskList != null) {
            selected = filteredTaskList.getSelectionModel().getSelectedItem();
        }
        if (selected == null && allTaskList != null) {
            selected = allTaskList.getSelectionModel().getSelectedItem();
        }
        return selected;
    }

    private Integer getCurrentUserId() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id FROM users WHERE username = ?")) {
            stmt.setString(1, Session.getUsername());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to get current user ID: " + e.getMessage());
        }
        return null;
    }

    // ===== TAB SWITCHING METHODS =====
    @FXML
    private void switchToMyTasksView() {
        if (myTasksView != null) {
            myTasksView.setVisible(true);
            myTasksView.setManaged(true);
        }
        if (approvalsView != null) {
            approvalsView.setVisible(false);
            approvalsView.setManaged(false);
        }
        updateTabStyles(true);
        loadAllClassTasks();
        loadFilteredTasks();
    }

    @FXML
    private void switchToApprovalsView() {
        if (myTasksView != null) {
            myTasksView.setVisible(false);
            myTasksView.setManaged(false);
        }
        if (approvalsView != null) {
            approvalsView.setVisible(true);
            approvalsView.setManaged(true);
        }
        updateTabStyles(false);
        loadPendingApprovals();
    }

    private void updateTabStyles(boolean isMyTasksActive) {
        String activeStyle = "-fx-background-color: #3182ce; -fx-text-fill: white; -fx-border-color: #3182ce;";
        String inactiveStyle = "-fx-background-color: white; -fx-text-fill: #cbd5e0; -fx-border-color: #e2e8f0;";
        
        if (tabMyTasksBtn != null) {
            tabMyTasksBtn.setStyle(isMyTasksActive ? activeStyle : inactiveStyle);
        }
        if (tabApprovalsBtn != null) {
            tabApprovalsBtn.setStyle(isMyTasksActive ? inactiveStyle : activeStyle);
        }
    }

    // ===== TIMER METHODS =====
    private void setupTimerDropdown() {
        if (timerDuration != null) {
            ObservableList<String> durations = FXCollections.observableArrayList(
                "5 min", "10 min", "15 min", "20 min", "25 min", "30 min", "45 min", "60 min"
            );
            timerDuration.setItems(durations);
            timerDuration.getSelectionModel().select(4); // Default to 25 min (Pomodoro)
        }
    }

    @FXML
    private void handleStartTimer() {
        Task selected = getSelectedTask();
        if (selected == null) return;
        
        String durationStr = timerDuration != null ? timerDuration.getValue() : "25 min";
        int minutes = Integer.parseInt(durationStr.replace(" min", ""));
        int totalSeconds = minutes * 60;
        
        timerTask = selected;
        LOGGER.info(() -> "Timer started for " + minutes + " minutes on task: " + selected.getTaskName());
        timerService.start(totalSeconds, selected.getId(), true);
    }

    public Button getStartTimerBtn() {
        return startTimerBtn;
    }

    public void setStartTimerBtn(Button startTimerBtn) {
        this.startTimerBtn = startTimerBtn;
    }

    public void updateTimerDisplay() {
        // Timer display will be updated by TimerService
    }

    // ===== ATTACHMENT METHODS =====
    @FXML
    private void handleUploadAttachment() {
        Task selected = getSelectedTask();
        if (selected == null) return;
        
        showAttachmentDialog(selected);
        LOGGER.info(() -> "Upload attachment handler for task: " + selected.getTaskName());
    }

    private void showAttachmentDialog(Task task) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Attachment");
        java.io.File selectedFile = fileChooser.showOpenDialog(null);
        
        if (selectedFile != null) {
            // Update attachment list display
            if (attachmentsList != null) {
                attachmentsList.getItems().clear();
                attachmentsList.getItems().add("📎 " + selectedFile.getName());
                attachmentsList.getItems().add("Path: " + selectedFile.getAbsolutePath());
            }
            
            // Update task status to pending approval
            updateTaskStatus(task, "For Approval");
            LOGGER.info(() -> "Attachment added for task: " + task.getTaskName());
        } else {
            // Allow submission without attachment
            updateTaskStatus(task, "For Approval");
        }
    }

    @FXML
    private void handleSaveTaskInfo() {
        Task selected = getSelectedTask();
        if (selected == null) return;
        
        String notes = taskInstructions != null ? taskInstructions.getText() : "";
        try (Connection conn = DBUtil.getConnection()) {
            String updateSql = "UPDATE tasks SET description = ? WHERE id = ? AND username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, notes);
                stmt.setInt(2, selected.getId());
                stmt.setString(3, Session.getUsername());
                stmt.executeUpdate();
            }
            LOGGER.info("Task notes saved for task: " + selected.getTaskName());
        } catch (Exception e) {
            LOGGER.severe("Failed to save task notes: " + e.getMessage());
        }
    }

    // ===== APPROVAL METHODS (FOR HOSTS) =====
    @FXML
    private void handleApproveSelected() {
        Task selected = pendingApprovalsList != null ? pendingApprovalsList.getSelectionModel().getSelectedItem() : null;
        if (selected == null) return;
        
        approveTask(selected);
        loadPendingApprovals();
    }

    @FXML
    private void handleRejectSelected() {
        Task selected = pendingApprovalsList != null ? pendingApprovalsList.getSelectionModel().getSelectedItem() : null;
        if (selected == null) return;
        
        updateTaskStatus(selected, "Pending");
        loadPendingApprovals();
    }

    private void approveTask(Task task) {
        updateTaskStatus(task, "Done");
    }

    private void loadPendingApprovals() {
        if (pendingApprovalsList == null) return;
        
        Integer userId = getCurrentUserId();
        if (userId == null) return;
        
        pendingApprovals.clear();
        
        String sql = "SELECT t.id, t.task_name, t.task_date, t.status, t.class_id, " +
                     "COALESCE(c.class_name, '') as class_name, t.is_personal " +
                     "FROM tasks t " +
                     "LEFT JOIN classes c ON t.class_id = c.id " +
                     "WHERE t.status = 'For Approval' AND t.user_id = ? " +
                     "ORDER BY t.task_date DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Task task = new Task(
                        rs.getInt("id"),
                        rs.getString("task_name"),
                        rs.getDate("task_date").toLocalDate(),
                        rs.getString("status"),
                        rs.getInt("class_id") > 0 ? rs.getInt("class_id") : null,
                        rs.getString("class_name"),
                        rs.getBoolean("is_personal")
                    );
                    pendingApprovals.add(task);
                }
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load pending approvals: " + e.getMessage());
        }
        
        pendingApprovalsList.setItems(pendingApprovals);
        pendingApprovalsList.setCellFactory(param -> createTaskListCell());
    }

    // ===== HELPER METHODS =====
    public void onTimerUpdated(int secondsRemaining, boolean isRunning, boolean isExpired) {
        // Update UI with timer state
    }

    public void onTimerCompleted() {
        LOGGER.info("Timer completed for task");
        loadAllClassTasks();
        loadFilteredTasks();
    }
}