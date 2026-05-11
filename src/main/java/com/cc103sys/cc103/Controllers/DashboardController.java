package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.Classes;
import com.cc103sys.cc103.Models.Task;
import com.cc103sys.cc103.Models.UserRank;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;
import com.cc103sys.cc103.Utils.TimerService;
import com.cc103sys.cc103.Utils.UiDialogs;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;

public class DashboardController implements TimerService.TimerListener {
    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final String POMODORO_STUDY_PRESET = "25 Minutes";
    private static final String POMODORO_BREAK_PRESET = "5 Minutes";

    @FXML private ComboBox<Classes> classSelector;
    @FXML private Label welcomeLabel;

    @FXML private TextField taskField;
    @FXML private DatePicker taskDate;
    @FXML private Label noTaskLabel;

    @FXML private ListView<Task> taskList;
    @FXML private Label timerLabel;
    @FXML private ListView<UserRank> leaderboardPreview;
    @FXML private ComboBox<String> timerPreset;
    @FXML private Button timerStartButton;
    @FXML private Button timerPauseButton;
    @FXML private RadioButton studyModeRadio;
    @FXML private RadioButton breakModeRadio;
    @FXML private CheckBox xpActiveCheckbox;
    @FXML@SuppressWarnings("unused")
    private TextField timerSubjectField;
    @FXML private Label totalTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label pendingTasksLabel;
    @FXML private VBox approvalsSection;
    @FXML private VBox leaderboardCard;
    @FXML private Label leaderboardCardTitle;
    @FXML private Label leaderboardCardSubtitle;
    @FXML private ListView<Task> pendingApprovalsListView;
    @FXML private Label approvalsCountLabel;
    @FXML private VBox approvalsCard;
    @FXML private VBox quickStatsSection;
    @FXML private VBox timerSection;
    @FXML private VBox myTasksSection;
    @FXML private Label tasksSectionLabel;
    @FXML private Label tasksSectionSubtitle;

    private Timeline refreshTimeline;
    private boolean disposed;
    private boolean pomodoroRunning;
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final ObservableList<Task> pendingApprovals = FXCollections.observableArrayList();
    private static final int REFRESH_INTERVAL_SECONDS = 5;

    private Window window() {
        return welcomeLabel != null && welcomeLabel.getScene() != null
            ? welcomeLabel.getScene().getWindow()
            : null;
    }

    @FXML
    public void initialize() {
        try {
            setupWelcomeMessage();
            setupTaskListView();
            setupTimerPresets();
            setupRoleBasedUI();
            loadTasks();
            loadUserClassesForLeaderboard();
            startAutoRefresh();

            TimerService.getInstance().addTimerListener(this);
            updateTimerDisplay();
            
            if (Session.isHost()) {
                setupPendingApprovalsView();
                loadPendingApprovals();
            }
            
            if (classSelector != null) {
                classSelector.setOnAction(e -> {
                    try {
                        loadLeaderboardPreviewForClass();
                    } catch (Exception e1) {
                    }
                });
            }

            if (timerPauseButton != null) {
                timerPauseButton.setDisable(true);
            }
            if (xpActiveCheckbox != null) {
                xpActiveCheckbox.setSelected(true);
            }

            NavbarController.getInstance().setActive("dashboard");
            setupListPlaceholders();
            registerLifecycleHooks();
            LOGGER.info("Dashboard initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Dashboard initialization error: {0}", e.getMessage());
        }
    }

    private void registerLifecycleHooks() {
        if (welcomeLabel != null) {
            welcomeLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
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
        pomodoroRunning = false;
        stopAutoRefresh();
        TimerService.getInstance().removeTimerListener(this);
        LOGGER.info("Dashboard resources cleaned up");
    }

    private void setupListPlaceholders() {
        if (taskList != null) {
            Label empty = new Label("No tasks yet.\nAdd a task below or join a class to receive assignments.");
            empty.getStyleClass().add("empty-state");
            empty.setWrapText(true);
            taskList.setPlaceholder(empty);
        }
        if (leaderboardPreview != null) {
            Label empty = new Label("Pick a class above to preview top learners.");
            empty.getStyleClass().add("empty-state");
            empty.setWrapText(true);
            leaderboardPreview.setPlaceholder(empty);
        }
        if (pendingApprovalsListView != null) {
            Label empty = new Label("Nothing waiting for approval.\nSubmissions will appear here automatically.");
            empty.getStyleClass().add("empty-state");
            empty.setWrapText(true);
            pendingApprovalsListView.setPlaceholder(empty);
        }
    }

    private void startAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
        }
        
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(REFRESH_INTERVAL_SECONDS), e -> {
                try {
                    loadTasks();
                    if (Session.isHost()) {
                        loadPendingApprovals();
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Error during dashboard refresh", ex);
                }
            })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.setOnFinished(e -> refreshTimeline = null);
        refreshTimeline.play();
        LOGGER.log(Level.INFO, "Dashboard auto-refresh timeline started (interval: {0} seconds)", REFRESH_INTERVAL_SECONDS);
    }

    private void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
            LOGGER.info("Dashboard auto-refresh timeline stopped");
        }
    }

    private void setupRoleBasedUI() {
        boolean isHost = Session.isHost();
        if (taskField != null) taskField.setVisible(isHost);
        if (taskDate != null) taskDate.setVisible(isHost);
        
        // For hosts: replace "My Tasks" with "Pending Approvals" as the main view
        if (myTasksSection != null) {
            myTasksSection.setVisible(!isHost);
            myTasksSection.setManaged(!isHost);
        }
        
        if (approvalsSection != null) {
            approvalsSection.setVisible(isHost);
            approvalsSection.setManaged(isHost);
        }
        
        if (leaderboardCard != null) {
            leaderboardCard.setVisible(true);
            leaderboardCard.setManaged(true);
        }
        if (leaderboardCardTitle != null) {
            leaderboardCardTitle.setText(isHost ? "Pending Approvals" : "Mini Leaderboard");
        }
        if (leaderboardCardSubtitle != null) {
            leaderboardCardSubtitle.setText(isHost ? "Review tasks submitted by participants" : "Preview top learners from your classes");
        }
        if (classSelector != null) {
            classSelector.setVisible(!isHost);
            classSelector.setManaged(!isHost);
        }
        if (leaderboardPreview != null) {
            leaderboardPreview.setVisible(true);
            leaderboardPreview.setManaged(true);
        }
        if (approvalsCard != null) {
            approvalsCard.setVisible(isHost);
            approvalsCard.setManaged(isHost);
        }
        if (pendingApprovalsListView != null) {
            pendingApprovalsListView.setVisible(true);
            pendingApprovalsListView.setManaged(true);
        }
        if (approvalsCountLabel != null) {
            approvalsCountLabel.setVisible(isHost);
            approvalsCountLabel.setManaged(isHost);
        }
        if (quickStatsSection != null) {
            quickStatsSection.setVisible(!isHost);
            quickStatsSection.setManaged(!isHost);
        }
        if (timerSection != null) {
            timerSection.setVisible(!isHost);
            timerSection.setManaged(!isHost);
        }
        
        // Update labels for clarity
        if (isHost && tasksSectionLabel != null) {
            tasksSectionLabel.setText("Pending Approvals");
        }
        if (isHost && tasksSectionSubtitle != null) {
            String subtitle = "Review tasks submitted by participants";
            tasksSectionSubtitle.setText(subtitle);
        }
    }
    
    private void setupPendingApprovalsView() {
        if (pendingApprovalsListView != null) {
            pendingApprovalsListView.setItems(pendingApprovals);
            pendingApprovalsListView.setCellFactory(param -> new ApprovalListCell());
        }
    }

    private void setupWelcomeMessage() {
        String username = Session.getUsername();
        if (username != null && welcomeLabel != null) {
            welcomeLabel.setText("Welcome, " + username + "!");
        }
    }

    private void setupTaskListView() {
        if (taskList != null) {
            taskList.setItems(tasks);
            taskList.setCellFactory(param -> new ListCell<Task>() {
                @Override
                protected void updateItem(Task task, boolean empty) {
                    super.updateItem(task, empty);
                    if (empty || task == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        String classLabel = task.getClassName() != null ? "[" + task.getClassName() + "] " : "";
                        Label line = new Label(String.format("%s%s · Due %s",
                            classLabel,
                            task.getTaskName(),
                            task.getDate()));
                        line.setWrapText(true);
                        line.setMaxWidth(Double.MAX_VALUE);
                        Label badge = new Label(formatTaskStatusLabel(task.getStatus()));
                        badge.getStyleClass().addAll("status-badge", taskStatusStyle(task.getStatus()));
                        HBox row = new HBox(12, line, badge);
                        row.setStyle("-fx-alignment: CENTER_LEFT;");
                        HBox.setHgrow(line, javafx.scene.layout.Priority.ALWAYS);
                        setText(null);
                        setGraphic(row);
                    }
                }
            });
        }
    }

    private static String formatTaskStatusLabel(String status) {
        if (status == null) {
            return "Pending";
        }
        String s = status.trim();
        if ("Done".equalsIgnoreCase(s) || "completed".equalsIgnoreCase(s)) {
            return "Approved";
        }
        if ("For Approval".equalsIgnoreCase(s)) {
            return "For approval";
        }
        if ("Rejected".equalsIgnoreCase(s)) {
            return "Rejected";
        }
        if ("Pending".equalsIgnoreCase(s)) {
            return "Pending";
        }
        return s;
    }

    private static String taskStatusStyle(String status) {
        if (status == null) {
            return "status-badge-pending";
        }
        String s = status.trim();
        if ("Done".equalsIgnoreCase(s) || "completed".equalsIgnoreCase(s)) {
            return "status-badge-approved";
        }
        if ("For Approval".equalsIgnoreCase(s)) {
            return "status-badge-review";
        }
        if ("Rejected".equalsIgnoreCase(s)) {
            return "status-badge-rejected";
        }
        return "status-badge-pending";
    }

    private void setupTimerPresets() {
        if (timerPreset != null) {
            timerPreset.getItems().setAll("25 Minutes", "45 Minutes", "1 Hour");
            timerPreset.setValue(POMODORO_STUDY_PRESET);
        }
        if (studyModeRadio != null) {
            studyModeRadio.setSelected(true);
        }
    }

    private void loadTasks() throws Exception {
        tasks.clear();

        try {
            Integer userId = getCurrentUserId();
            if (userId == null) {
                LOGGER.warning("Could not determine current user ID");
                return;
            }

            String sql = "SELECT t.id, t.task_name, t.task_date, t.status, t.class_id, COALESCE(c.class_name, '') as class_name "
                       + "FROM tasks t "
                       + "LEFT JOIN classes c ON t.class_id = c.id "
                       + "WHERE t.user_id = ? "
                       + "ORDER BY t.task_date DESC";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int classId = rs.getInt("class_id");
                        String className = rs.getString("class_name");
                        
                        tasks.add(new Task(
                            rs.getInt("id"),
                            rs.getString("task_name"),
                            rs.getDate("task_date").toLocalDate(),
                            rs.getString("status"),
                            classId > 0 ? classId : null,
                            className != null && !className.isEmpty() ? className : null
                        ));
                    }
                }
            }
            if (totalTasksLabel != null) {
                updateQuickStats();
            }
            LOGGER.log(Level.INFO, "Loaded {0} tasks", tasks.size());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load tasks: {0}", e.getMessage());
        }
        if (noTaskLabel != null) {
            boolean empty = tasks.isEmpty();
            noTaskLabel.setVisible(empty);
            noTaskLabel.setManaged(empty);
        }
    }

    private void updateQuickStats() {
        int total = 0;
        int completed = 0;
        String sql = "SELECT SUM(CASE WHEN status IN ('Done','completed') THEN 1 ELSE 0 END) AS completed, "
                   + "COUNT(*) AS total FROM tasks WHERE username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    completed = rs.getInt("completed");
                    total = rs.getInt("total");
                }
            }

            int pending = Math.max(0, total - completed);
            if (totalTasksLabel != null) {
                totalTasksLabel.setText("Total Tasks: " + total);
            }
            if (completedTasksLabel != null) {
                completedTasksLabel.setText("Completed Tasks: " + completed);
            }
            if (pendingTasksLabel != null) {
                pendingTasksLabel.setText("Pending Tasks: " + pending);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to update quick stats: {0}", e.getMessage());
        }
    }

    private void loadUserClassesForLeaderboard() {
        ObservableList<Classes> userClasses = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT c.id, c.class_name FROM classes c "
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

            if (classSelector != null) {
                classSelector.setItems(userClasses);
                classSelector.setOnAction(e -> {
                    try {
                        loadLeaderboardPreviewForClass();
                    } catch (Exception e1) {
                    }
                });

                if (!userClasses.isEmpty()) {
                    classSelector.setValue(userClasses.get(0));
                    loadLeaderboardPreviewForClass();
                }
            }
            LOGGER.info(String.format("Loaded %d user classes", userClasses.size()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load user classes: {0}", e.getMessage());
        }

    }

    @FXML
    @SuppressWarnings("unused")
    private void handleAddTask() throws Exception {
        String taskName = taskField.getText().trim();
        LocalDate date = taskDate.getValue();

        if (!validateTaskInput(taskName, date)) {
            return;
        }

        if (Session.isHost()) {
            Classes selectedClass = classSelector == null ? null : classSelector.getValue();
            if (selectedClass != null && !isCurrentUserClassOwner(selectedClass.getId())) {
                UiDialogs.warn(window(), "Cannot post to this class",
                    "You can only create class tasks for classes you own. This task will be saved as a personal task instead.");
            }
            if (selectedClass != null && isCurrentUserClassOwner(selectedClass.getId())) {
                try {
                    int classTaskId = createClassTask(selectedClass.getId(), taskName, date);
                    if (classTaskId <= 0) {
                        LOGGER.log(Level.SEVERE, "Failed to create class task for {0}", taskName);
                        return;
                    }

                    assignClassTaskToMembers(classTaskId, selectedClass.getId());
                    taskField.clear();
                    taskDate.setValue(null);
                    loadTasks();
                    loadLeaderboardPreviewForClass();
                    try {
                        playAddTaskAnimation();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error playing animation: {0}", e.getMessage());
                    }
                    LOGGER.log(Level.INFO, "Class task created and assigned: {0}", taskName);
                    return;
                } catch (SQLException e) {
                    LOGGER.log(Level.SEVERE, "Failed to create task: {0}", e.getMessage());
                    return;
                }
            }
        }

        String sql = "INSERT INTO tasks (username, user_id, task_name, task_date, status, is_personal) VALUES (?, ?, ?, ?, 'Pending', 1)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());

            Integer userId = getCurrentUserId();
            if (userId != null) {
                stmt.setInt(2, userId);
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }
            
            stmt.setString(3, taskName);
            stmt.setDate(4, Date.valueOf(date));
            stmt.executeUpdate();

            taskField.clear();
            taskDate.setValue(null);
            loadTasks();
            try {
                playAddTaskAnimation();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error playing animation: {0}", e.getMessage());
            }
            LOGGER.log(Level.INFO, "Personal task created: {0}", taskName);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to create personal task: {0}", e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleDeleteTask() throws Exception {
        try {
            Task selected = taskList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                UiDialogs.warn(window(), "Nothing selected", "Select a task to delete.");
                return;
            }
            if (!UiDialogs.confirm(window(), "Delete task?",
                "Permanently remove \"" + selected.getTaskName() + "\"? This cannot be undone.")) {
                return;
            }

            String sql = "DELETE FROM tasks WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, selected.getId());
                stmt.executeUpdate();
                loadTasks();
                LOGGER.log(Level.INFO, "Task deleted: {0}", selected.getTaskName());
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete task: {0}", e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleMarkDone() throws Exception {
        updateTaskStatus("Done");
    }

    private void updateTaskStatus(String status) throws Exception {
        Task selected = taskList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            LOGGER.warning("No task selected for status update");
            return;
        }

        if (isTaskAlreadyCompleted(selected.getStatus())) {
            LOGGER.log(Level.WARNING, "Task is already completed: {0}", selected.getTaskName());
            return;
        }

        int points = calculateTaskCompletionPoints(selected.getDate());
        String updateTaskSql = "UPDATE tasks SET status = ?, completed_date = ?, points_awarded = ? WHERE id = ?";
        String updatePointsSql = "UPDATE users SET points = points + ? WHERE username = ?";

        try (Connection conn = DBUtil.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(updateTaskSql)) {
                stmt.setString(1, status);
                stmt.setDate(2, Date.valueOf(LocalDate.now()));
                stmt.setInt(3, points);
                stmt.setInt(4, selected.getId());
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(updatePointsSql)) {
                stmt.setInt(1, points);
                stmt.setString(2, Session.getUsername());
                stmt.executeUpdate();
            }

            loadTasks();
            loadLeaderboardPreviewForClass();
            LOGGER.log(Level.INFO, "Task completed: {0} and awarded {1} points", new Object[]{selected.getTaskName(), points});
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update task status: {0}", e.getMessage());
        }
    }

    private boolean isTaskAlreadyCompleted(String status) {
        return status != null && ("Done".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status));
    }
    
    private void loadPendingApprovals() {
        pendingApprovals.clear();
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) return;

        String sql = "SELECT t.id, t.task_name, t.task_date, t.status, t.class_id, t.class_task_id, t.username, c.class_name " +
                     "FROM tasks t " +
                     "LEFT JOIN classes c ON t.class_id = c.id " +
                     "WHERE t.status = 'For Approval' " +
                     "AND t.class_id IN (SELECT id FROM classes WHERE owner_id = ?) " +
                     "ORDER BY t.task_date DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Integer classId = rs.getObject("class_id", Integer.class);
                    Integer classTaskId = rs.getObject("class_task_id", Integer.class);
                    pendingApprovals.add(new Task(
                        rs.getInt("id"),
                        rs.getString("task_name"),
                        rs.getDate("task_date").toLocalDate(),
                        rs.getString("status"),
                        classId,
                        classTaskId,
                        rs.getString("class_name"),
                        false,
                        rs.getString("username")
                    ));
                }
            }
            if (approvalsCountLabel != null) {
                approvalsCountLabel.setText("Pending: " + pendingApprovals.size());
            }
            LOGGER.log(Level.INFO, "Loaded {0} pending approvals", pendingApprovals.size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load pending approvals: {0}", e.getMessage());
        }
    }
    
    private void approveTask(Task task) {
        if (task == null || task.getId() <= 0) {
            return;
        }
        String who = task.getUsername() != null ? task.getUsername() : "this participant";
        if (!UiDialogs.confirm(window(), "Approve task?",
            "Approve \"" + task.getTaskName() + "\" submitted by " + who + "?")) {
            return;
        }
        
        int points = calculateClassTaskApprovalPoints(task.getDate());
        String updateSql = "UPDATE tasks SET status = 'Done', completed_date = ?, points_awarded = ?, approved_by = ?, approved_date = ? WHERE id = ?";
        String updatePointsSql = "UPDATE users SET points = points + ? WHERE username = ?";
        
        try (Connection conn = DBUtil.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                Integer userId = getCurrentUserId();
                stmt.setDate(1, Date.valueOf(LocalDate.now()));
                stmt.setInt(2, points);
                stmt.setInt(3, userId != null ? userId : 0);
                stmt.setDate(4, Date.valueOf(LocalDate.now()));
                stmt.setInt(5, task.getId());
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(updatePointsSql)) {
                stmt.setInt(1, points);
                stmt.setString(2, task.getUsername());
                stmt.executeUpdate();
            }
            
            loadPendingApprovals();
            loadTasks();
            LOGGER.log(Level.INFO, "Task approved and {0} points awarded to {1}", new Object[]{points, task.getUsername()});
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to approve task: {0}", e.getMessage());
        }
    }
    
    private void rejectTask(Task task) {
        if (task == null || task.getId() <= 0) {
            return;
        }
        String who = task.getUsername() != null ? task.getUsername() : "this participant";
        if (!UiDialogs.confirm(window(), "Reject task?",
            "Reject \"" + task.getTaskName() + "\" from " + who + "? They can submit again after updating their work.")) {
            return;
        }
        
        String updateSql = "UPDATE tasks SET status = 'Rejected', approved_by = ?, approved_date = ? WHERE id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, getCurrentUserId());
            stmt.setDate(2, Date.valueOf(LocalDate.now()));
            stmt.setInt(3, task.getId());
            stmt.executeUpdate();
            
            loadPendingApprovals();
            loadTasks();
            LOGGER.log(Level.INFO, "Task rejected: {0}", task.getTaskName());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to reject task: {0}", e.getMessage());
        }
    }

    private int calculateClassTaskApprovalPoints(LocalDate taskDate) {
        if (taskDate == null) {
            return 10;
        }

        long daysBeforeDeadline = ChronoUnit.DAYS.between(LocalDate.now(), taskDate);
        if (daysBeforeDeadline < 0) {
            return 5;
        }

        int multiplier = (int) Math.max(1, daysBeforeDeadline);
        return 10 * multiplier;
    }
    
    private class ApprovalListCell extends ListCell<Task> {
        @Override
        protected void updateItem(Task task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setText(null);
                setGraphic(null);
            } else {
                String classLabel = task.getClassName() != null ? "[" + task.getClassName() + "] " : "";
                String userLabel = task.getUsername() != null ? " by " + task.getUsername() : "";
                String taskText = classLabel + task.getTaskName() + userLabel + " (" + task.getDate() + ")";
                
                Button viewBtn = new Button("View Task");
                Button approveBtn = new Button("Approve");
                Button rejectBtn = new Button("Reject");
                
                viewBtn.getStyleClass().addAll("button", "button-secondary");
                approveBtn.getStyleClass().addAll("button", "button-success");
                rejectBtn.getStyleClass().addAll("button", "button-danger");
                
                final Task approvalTask = task;
                viewBtn.setOnAction(e -> Navigator.navigateToTaskReview(approvalTask.getClassId(), approvalTask.getClassTaskId()));
                approveBtn.setOnAction(e -> approveTask(approvalTask));
                rejectBtn.setOnAction(e -> rejectTask(approvalTask));
                
                HBox buttonBox = new HBox(5);
                buttonBox.getChildren().addAll(viewBtn, approveBtn, rejectBtn);
                
                VBox cellBox = new VBox(3);
                cellBox.getChildren().addAll(
                    new Label(taskText),
                    buttonBox
                );
                
                setText(null);
                setGraphic(cellBox);
            }
        }
    }

    private boolean isCurrentUserClassOwner(int classId) {
        Integer ownerId = null;
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return false;
        }

        String sql = "SELECT owner_id FROM classes WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ownerId = rs.getInt("owner_id");
                    if (rs.wasNull()) {
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check class owner: {0}", e.getMessage());
            return false;
        }

        return currentUserId.equals(ownerId);
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
            LOGGER.log(Level.SEVERE, "Failed to get current user id: {0}", e.getMessage());
        }
        return null;
    }

    private int createClassTask(int classId, String taskName, LocalDate date) throws Exception {
        Integer ownerId = getCurrentUserId();
        if (ownerId == null) {
            throw new SQLException("Unable to determine current user.");
        }

        String sql = "INSERT INTO class_tasks (class_id, task_name, description, due_date, owner_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, classId);
            stmt.setString(2, taskName);
            stmt.setString(3, null);
            stmt.setDate(4, Date.valueOf(date));
            stmt.setInt(5, ownerId);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    private void assignClassTaskToMembers(int classTaskId, @SuppressWarnings("unused") int classId) {
        String sql = "INSERT INTO tasks (username, user_id, task_name, task_date, status, class_id, class_task_id, created_by) "
                   + "SELECT u.username, u.id, ct.task_name, ct.due_date, 'Pending', ct.class_id, ct.id, ct.owner_id "
                   + "FROM class_tasks ct "
                   + "JOIN user_classes uc ON ct.class_id = uc.class_id "
                   + "JOIN users u ON uc.user_id = u.id "
                   + "WHERE ct.id = ? "
                   + "AND u.id != ct.owner_id "
                   + "AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.class_task_id = ct.id AND t.user_id = u.id)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, classTaskId);
            stmt.executeUpdate();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to assign class task to members: {0}", e.getMessage());
        }
    }

    private void loadLeaderboardPreviewForClass() throws Exception {
        try {
            Classes selectedClass = classSelector.getValue();
            if (selectedClass == null) {
                LOGGER.warning("No class selected for leaderboard");
                return;
            }

            ObservableList<UserRank> data = FXCollections.observableArrayList();
            String sql = "SELECT u.username, u.points FROM users u "
                       + "JOIN user_classes uc ON u.id = uc.user_id "
                       + "WHERE uc.class_id = ? ORDER BY u.points DESC LIMIT 5";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, selectedClass.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        data.add(new UserRank(rs.getString("username"), rs.getInt("points")));
                    }
                }

                if (leaderboardPreview != null) {
                    leaderboardPreview.setItems(data);
                    leaderboardPreview.setCellFactory(param -> new ListCell<UserRank>() {
                        @Override
                        protected void updateItem(UserRank item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? null : 
                                item.getUsername() + " - " + item.getPoints() + " pts");
                        }
                    });
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load leaderboard preview: {0}", e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void openLeaderboardScene() {
        try {
            Classes selectedClass = classSelector.getValue();
            if (selectedClass == null) {
                LOGGER.warning("No class selected for leaderboard");
                return;
            }
            Session.setCurrentClassId(selectedClass.getId());
            Navigator.switchScene("Leaderboard");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to open leaderboard: {0}", e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleStartTimer() {
        try {
            String selected = timerPreset.getValue();
            if (selected == null || selected.isBlank()) {
                selected = studyModeRadio != null && studyModeRadio.isSelected()
                    ? POMODORO_STUDY_PRESET
                    : POMODORO_BREAK_PRESET;
                if (timerPreset != null) {
                    timerPreset.setValue(selected);
                }
            }

            int seconds = convertToSeconds(selected);
            boolean xpActive = xpActiveCheckbox != null && xpActiveCheckbox.isSelected();
            pomodoroRunning = true;
            
            TimerService.getInstance().start(seconds, null, xpActive);
            updateTimerDisplay();
            
            if (timerStartButton != null) {
                timerStartButton.setText("Restart");
            }
            LOGGER.log(Level.INFO, "Timer started: {0} seconds", seconds);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start timer: {0}", e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handlePauseTimer() {
        TimerService timer = TimerService.getInstance();
        if (timer.isRunning()) {
            if (timer.isPaused()) {
                timer.resume();
            } else {
                timer.pause();
            }
            updateTimerDisplay();
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleModeChange(ActionEvent event) {
        if (studyModeRadio != null && studyModeRadio.isSelected()) {
            timerPreset.getItems().setAll("25 Minutes", "45 Minutes", "1 Hour");
        } else if (breakModeRadio != null && breakModeRadio.isSelected()) {
            timerPreset.getItems().setAll("5 Minutes", "10 Minutes", "15 Minutes");
        }
        if (timerPreset != null && !timerPreset.getItems().isEmpty()) {
            timerPreset.setValue(timerPreset.getItems().get(0));
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void updateXpStatus() {
    }

    private int convertToSeconds(String value) {
        if (value.contains("Hour")) return 3600;
        if (value.contains("45")) return 2700;
        if (value.contains("25")) return 1500;
        if (value.contains("15")) return 900;
        if (value.contains("10")) return 600;
        if (value.contains("5")) return 300;
        return 60;
    }

    private void playAddTaskAnimation() {
        if (taskList != null) {
            FadeTransition fade = new FadeTransition(Duration.seconds(0.5), taskList);
            fade.setFromValue(0.7);
            fade.setToValue(1);
            fade.play();
        }
    }

    private boolean validateTaskInput(String taskName, LocalDate date) {
        if (taskName == null || taskName.isBlank()) {
            LOGGER.warning("Task name is empty");
            UiDialogs.warn(window(), "Missing task name", "Please enter what you need to do.");
            return false;
        }
        if (date == null) {
            LOGGER.warning("Task date is null");
            UiDialogs.warn(window(), "Missing date", "Please choose a due date.");
            return false;
        }
        return true;
    }

    @FXML
    @SuppressWarnings("unused")
    private void goSettings() {
        try {
            Navigator.switchScene("Settings");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to navigate to Settings: {0}", e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void openTimerPopup() {
        try {
            LOGGER.info("Opening timer popup");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to open timer popup: {0}", e.getMessage());
        }
    }

    private int calculateTaskCompletionPoints(LocalDate taskDate) {
        LocalDate today = LocalDate.now();
        long daysLate = ChronoUnit.DAYS.between(taskDate, today);

        if (daysLate <= 0) {
            return 10;
        } else {
            return 5;
        }
    }

    @Override
    public void onTimerUpdated(int secondsRemaining, boolean running, boolean paused) {
        updateTimerDisplay();
    }

    @Override
    public void onTimerCompleted() {
        updateTimerDisplay();
        timerCompleted();
    }

    private void timerCompleted() {
        if (timerLabel != null) {
            timerLabel.setText("Done!");
        }

        if (xpActiveCheckbox != null && xpActiveCheckbox.isSelected()) {
            awardTimerXp(10);
        }
        if (pomodoroRunning) {
            startNextPomodoroSession();
        }
    }

    private void startNextPomodoroSession() {
        boolean moveToBreak = studyModeRadio != null && studyModeRadio.isSelected();
        if (moveToBreak) {
            if (breakModeRadio != null) {
                breakModeRadio.setSelected(true);
            }
            handleModeChange(null);
            if (timerPreset != null) {
                timerPreset.setValue(POMODORO_BREAK_PRESET);
            }
        } else {
            if (studyModeRadio != null) {
                studyModeRadio.setSelected(true);
            }
            handleModeChange(null);
            if (timerPreset != null) {
                timerPreset.setValue(POMODORO_STUDY_PRESET);
            }
        }
        handleStartTimer();
    }

    private void awardTimerXp(int points) {
        String sql = "UPDATE users SET points = points + ? WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, points);
            stmt.setString(2, Session.getUsername());
            stmt.executeUpdate();
            Session.setPoints(Session.getPoints() + points);
            refreshNavbarPoints();
            LOGGER.log(Level.INFO, "Timer XP awarded: {0}", points);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to award timer XP: {0}", e.getMessage());
        }
    }

    private void refreshNavbarPoints() throws Exception {
        if (NavbarController.getInstance() != null) {
            NavbarController.getInstance().loadUserInfo();
        }
    }

    private void updateTimerDisplay() {
        TimerService timer = TimerService.getInstance();
        
        if (timerLabel != null) {
            if (timer.isRunning()) {
                int remainingSeconds = timer.getRemainingSeconds();
                int minutes = remainingSeconds / 60;
                int seconds = remainingSeconds % 60;
                timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
            } else {
                timerLabel.setText("00:00");
            }
        }
        
        if (timerPauseButton != null) {
            timerPauseButton.setDisable(!timer.isRunning());
            timerPauseButton.setText(timer.isPaused() ? "Resume" : "Pause");
        }
        
        if (timerStartButton != null) {
            timerStartButton.setText(timer.isRunning() ? "Restart" : "Start");
        }
    }
}
