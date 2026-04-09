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
import com.cc103sys.cc103.Models.UserRank;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class DashboardController {
    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final int BASE_TASK_POINTS = 10;
    private static final int LATE_TASK_POINTS = 5;

    @FXML private ComboBox<Classes> classSelector;
    @FXML private Label welcomeLabel;

    // ✅ RESTORED (IMPORTANT)
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
    @FXML private TextField timerSubjectField;
    @FXML private ProgressBar dailyProgressBar;
    @FXML private Label dailyGoalSummary;
    @FXML private Label dailyGoalTip;

    private Timeline timeline;
    private int remainingSeconds;
    private boolean timerRunning;
    private boolean timerPaused;
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        try {
            setupWelcomeMessage();
            setupTaskListView();
            setupTimerPresets();
            setupRoleBasedUI();
            loadTasks();
            loadUserClassesForLeaderboard();
            
            if (classSelector != null) {
                classSelector.setOnAction(e -> {
                    try {
                        loadLeaderboardPreviewForClass();
                    } catch (Exception e1) {
                    }
                    loadTasks();
                });
            }

            if (timerPauseButton != null) {
                timerPauseButton.setDisable(true);
            }
            if (xpActiveCheckbox != null) {
                xpActiveCheckbox.setSelected(true);
            }

            NavbarController.getInstance().setActive("dashboard");
            LOGGER.info("Dashboard initialized successfully");
        } catch (Exception e) {
            LOGGER.severe("Dashboard initialization error: " + e.getMessage());
        }
    }

    private void setupRoleBasedUI() {
        boolean isHost = Session.isHost();
        if (taskField != null) taskField.setVisible(isHost);
        if (taskDate != null) taskDate.setVisible(isHost);
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
                    } else {
                        setText(String.format("%s | %s | %s", 
                            task.getTaskName(), task.getDate(), task.getStatus()));
                    }
                }
            });
        }
    }

    private void setupTimerPresets() {
        if (timerPreset != null) {
            timerPreset.getItems().addAll(
                "1 Minute", "5 Minutes", "10 Minutes", "1 Hour"
            );
            timerPreset.setValue("10 Minutes");
        }
    }

    private void loadTasks() {
        tasks.clear();
        Classes selectedClass = classSelector == null ? null : classSelector.getValue();

        String sql = "SELECT id, task_name, task_date, status FROM tasks WHERE username = ?";
        if (selectedClass != null) {
            sql += " AND class_id = ?";
        }

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
            if (selectedClass != null) {
                stmt.setInt(2, selectedClass.getId());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("task_name"),
                        rs.getDate("task_date").toLocalDate(),
                        rs.getString("status")
                    ));
                }
            }
            if (dailyGoalSummary != null) {
                updateDailyGoalProgress();
            }
            LOGGER.info("Loaded " + tasks.size() + " tasks");
        } catch (Exception e) {
            LOGGER.severe("Failed to load tasks: " + e.getMessage());
        }
    }

    private void updateDailyGoalProgress() {
        int total = 0;
        int completed = 0;
        String sql = "SELECT SUM(CASE WHEN status IN ('Done','completed') THEN 1 ELSE 0 END) AS completed, "
                   + "COUNT(*) AS total FROM tasks WHERE username = ? AND task_date = CURDATE()";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    completed = rs.getInt("completed");
                    total = rs.getInt("total");
                }
            }

            double progress = total == 0 ? 0.0 : completed / (double) total;
            if (dailyProgressBar != null) {
                dailyProgressBar.setProgress(progress);
            }
            if (dailyGoalSummary != null) {
                if (total == 0) {
                    dailyGoalSummary.setText("No tasks are due today. Add a task to start your daily goal.");
                } else {
                    dailyGoalSummary.setText(String.format("%d of %d tasks completed today", completed, total));
                }
            }
            if (dailyGoalTip != null) {
                dailyGoalTip.setText(total == 0 ? "Try adding a new task and complete it today." : "Keep going — every completed task increases your XP.");
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to update daily goal progress: " + e.getMessage());
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
                    loadTasks();
                });

                if (!userClasses.isEmpty()) {
                    classSelector.setValue(userClasses.get(0));
                    loadLeaderboardPreviewForClass();
                    loadTasks();
                } else {
                    tasks.clear();
                }
            }
            LOGGER.info("Loaded " + userClasses.size() + " user classes");
        } catch (Exception e) {
            LOGGER.severe("Failed to load user classes: " + e.getMessage());
        }

    }

    @FXML
    @SuppressWarnings("unused")
    private void handleAddTask() throws Exception {
        if (!Session.isHost()) {
            LOGGER.warning("Only Hosts can create class tasks");
            return;
        }

        String taskName = taskField.getText();
        LocalDate date = taskDate.getValue();

        if (!validateTaskInput(taskName, date)) {
            return;
        }

        Classes selectedClass = classSelector == null ? null : classSelector.getValue();
        if (selectedClass == null) {
            LOGGER.warning("No class selected for task");
            return;
        }

        if (!isCurrentUserClassOwner(selectedClass.getId())) {
            LOGGER.warning("Only class owners can create official class tasks");
            return;
        }

        try {
            int classTaskId = createClassTask(selectedClass.getId(), taskName, date);
            if (classTaskId <= 0) {
                LOGGER.severe("Failed to create class task for " + taskName);
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
                LOGGER.warning("Error playing animation: " + e.getMessage());
            }
            LOGGER.info("Class task created and assigned: " + taskName);
        } catch (SQLException e) {
            LOGGER.severe("Failed to create task: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleDeleteTask() throws Exception {
        try {
            Task selected = taskList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                LOGGER.warning("No task selected for deletion");
                return;
            }

            String sql = "DELETE FROM tasks WHERE id = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, selected.getId());
                stmt.executeUpdate();
                loadTasks();
                LOGGER.info("Task deleted: " + selected.getTaskName());
            }
        } catch (SQLException e) {
            LOGGER.severe("Failed to delete task: " + e.getMessage());
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
            LOGGER.warning("Task is already completed: " + selected.getTaskName());
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
            LOGGER.info("Task completed: " + selected.getTaskName() + " and awarded " + points + " points");
        } catch (SQLException e) {
            LOGGER.severe("Failed to update task status: " + e.getMessage());
        }
    }

    private boolean isTaskAlreadyCompleted(String status) {
        return status != null && ("Done".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status));
    }

    private int calculateTaskCompletionPoints(LocalDate dueDate) {
        if (dueDate == null) {
            return BASE_TASK_POINTS;
        }

        long daysBefore = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        if (daysBefore < 0) {
            return LATE_TASK_POINTS;
        }

        return BASE_TASK_POINTS * (int) Math.max(1, daysBefore);
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
            LOGGER.severe("Failed to check class owner: " + e.getMessage());
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
            LOGGER.severe("Failed to get current user id: " + e.getMessage());
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
            stmt.setString(3, null); // description - null for now
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

    private void assignClassTaskToMembers(int classTaskId, int classId) {
        String sql = "INSERT INTO tasks (username, user_id, task_name, task_date, status, class_id, class_task_id, created_by) "
                   + "SELECT u.username, u.id, ct.task_name, ct.due_date, 'Pending', ct.class_id, ct.id, ct.owner_id "
                   + "FROM class_tasks ct "
                   + "JOIN user_classes uc ON ct.class_id = uc.class_id "
                   + "JOIN users u ON uc.user_id = u.id "
                   + "WHERE ct.id = ? "
                   + "AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.class_task_id = ct.id AND t.user_id = u.id)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, classTaskId);
            stmt.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe("Failed to assign class task to members: " + e.getMessage());
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
            LOGGER.severe("Failed to load leaderboard preview: " + e.getMessage());
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
            LOGGER.severe("Failed to open leaderboard: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleStartTimer() {
        try {
            if (timerRunning && timerPaused) {
                resumeTimer();
                return;
            }

            String selected = timerPreset.getValue();
            if (selected == null || selected.isBlank()) {
                LOGGER.warning("No timer preset selected");
                return;
            }

            stopTimer();
            remainingSeconds = convertToSeconds(selected);
            timerRunning = true;
            timerPaused = false;
            if (timerPauseButton != null) {
                timerPauseButton.setDisable(false);
                timerPauseButton.setText("Pause");
            }
            if (timerStartButton != null) {
                timerStartButton.setText("Restart");
            }
            startCountdown();
            LOGGER.info("Timer started: " + remainingSeconds + " seconds");
        } catch (Exception e) {
            LOGGER.severe("Failed to start timer: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handlePauseTimer() {
        if (!timerRunning) {
            return;
        }
        if (timerPaused) {
            resumeTimer();
        } else {
            pauseTimer();
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
        // This is intentionally lightweight; XP activation is controlled by the checkbox state.
    }

    private void startCountdown() {
        if (timerLabel != null) {
            updateTimerLabel();
        }
        timeline = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> {
                remainingSeconds--;
                updateTimerLabel();
                if (remainingSeconds <= 0) {
                    timerCompleted();
                }
            })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void pauseTimer() {
        if (timeline != null) {
            timeline.pause();
            timerPaused = true;
            if (timerPauseButton != null) {
                timerPauseButton.setText("Resume");
            }
            LOGGER.info("Timer paused");
        }
    }

    private void resumeTimer() {
        if (timeline != null) {
            timeline.play();
            timerPaused = false;
            if (timerPauseButton != null) {
                timerPauseButton.setText("Pause");
            }
            LOGGER.info("Timer resumed");
        }
    }

    private void timerCompleted() {
        stopTimer();
        timerRunning = false;
        timerPaused = false;
        if (timerPauseButton != null) {
            timerPauseButton.setDisable(true);
            timerPauseButton.setText("Pause");
        }
        if (timerStartButton != null) {
            timerStartButton.setText("Start");
        }
        if (timerLabel != null) {
            timerLabel.setText("Done!");
        }

        if (xpActiveCheckbox != null && xpActiveCheckbox.isSelected()) {
            awardTimerXp(10);
        }
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
            LOGGER.info("Timer XP awarded: " + points);
        } catch (Exception e) {
            LOGGER.severe("Failed to award timer XP: " + e.getMessage());
        }
    }

    private void refreshNavbarPoints() {
        if (NavbarController.getInstance() != null) {
            NavbarController.getInstance().loadUserInfo();
        }
    }

    private void stopTimer() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    private void updateTimerLabel() {
        int minutes = Math.max(0, remainingSeconds) / 60;
        int seconds = Math.max(0, remainingSeconds) % 60;
        if (timerLabel != null) {
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }
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
            return false;
        }
        if (date == null) {
            LOGGER.warning("Task date is null");
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
            LOGGER.severe("Failed to navigate to Settings: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void openTimerPopup() {
        try {
            LOGGER.info("Opening timer popup");
            // Placeholder: Can be expanded to show a popup window or modal
        } catch (Exception e) {
            LOGGER.severe("Failed to open timer popup: " + e.getMessage());
        }
    }
}
