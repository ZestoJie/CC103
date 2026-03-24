package com.cc103sys.cc103.Controllers;

import java.sql.*;
import java.time.LocalDate;
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
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

/**
 * Controller for Dashboard scene.
 * Manages tasks, timer, and leaderboard preview.
 */
public class DashboardController {
    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final int TASK_COMPLETION_BONUS = 10;

    @FXML private ComboBox<Classes> classSelector;
    @FXML private Label welcomeLabel;
    @FXML private TextField taskField;
    @FXML private DatePicker taskDate;
    @FXML private ListView<Task> taskList;
    @FXML private Label timerLabel;
    @FXML private ListView<UserRank> leaderboardPreview;
    @FXML private ComboBox<String> timerPreset;

    private Timeline timeline;
    private int remainingSeconds;
    private ObservableList<Task> tasks = FXCollections.observableArrayList();

    /**
     * Initialize dashboard controller.
     */
    @FXML
    public void initialize() {
        try {
            setupWelcomeMessage();
            setupTaskListView();
            setupTimerPresets();
            loadTasks();
            loadUserClassesForLeaderboard();
            
            if (classSelector != null) {
                classSelector.setOnAction(e -> loadLeaderboardPreviewForClass());
            }
            LOGGER.info("Dashboard initialized successfully");
        } catch (Exception e) {
            LOGGER.severe("Dashboard initialization error: " + e.getMessage());
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
            timerPreset.setValue("1 Minute");
        }
    }

    private void loadTasks() {
        tasks.clear();
        String sql = "SELECT id, task_name, task_date, status FROM tasks WHERE username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
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
            LOGGER.info("Loaded " + tasks.size() + " tasks");
        } catch (Exception e) {
            LOGGER.severe("Failed to load tasks: " + e.getMessage());
        }
    }

    private void loadUserClassesForLeaderboard() {
        ObservableList<Classes> userClasses = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT c.id, c.class_name FROM classes c WHERE c.id IN (SELECT class_id FROM users WHERE username = ?)";

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
                if (!userClasses.isEmpty()) {
                    classSelector.setValue(userClasses.get(0));
                    loadLeaderboardPreviewForClass();
                }
            }
            LOGGER.info("Loaded " + userClasses.size() + " user classes");
        } catch (Exception e) {
            LOGGER.severe("Failed to load user classes: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddTask() {
        try {
            String taskName = taskField.getText();
            LocalDate date = taskDate.getValue();

            if (!validateTaskInput(taskName, date)) {
                return;
            }

            String sql = "INSERT INTO tasks(username, task_name, task_date, status) VALUES (?, ?, ?, ?)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, Session.getUsername());
                stmt.setString(2, taskName);
                stmt.setDate(3, Date.valueOf(date));
                stmt.setString(4, "Pending");
                stmt.executeUpdate();

                loadTasks();
                playAddTaskAnimation();
                taskField.clear();
                taskDate.setValue(null);
                LOGGER.info("Task added: " + taskName);
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to add task: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteTask() {
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
        } catch (Exception e) {
            LOGGER.severe("Failed to delete task: " + e.getMessage());
        }
    }

    @FXML
    private void handleMarkDone() {
        updateTaskStatus("Done", TASK_COMPLETION_BONUS);
    }

    @FXML
    private void handleMarkUndone() {
        updateTaskStatus("Pending", -TASK_COMPLETION_BONUS);
    }

    private void updateTaskStatus(String status, int pointsChange) {
        try {
            Task selected = taskList.getSelectionModel().getSelectedItem();
            if (selected == null) {
                LOGGER.warning("No task selected for status update");
                return;
            }

            String updateTaskSql = "UPDATE tasks SET status = ? WHERE id = ?";
            String updatePointsSql = "UPDATE users SET points = points + ? WHERE username = ?";

            try (Connection conn = DBUtil.getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement(updateTaskSql)) {
                    stmt.setString(1, status);
                    stmt.setInt(2, selected.getId());
                    stmt.executeUpdate();
                }
                
                try (PreparedStatement stmt = conn.prepareStatement(updatePointsSql)) {
                    stmt.setInt(1, pointsChange);
                    stmt.setString(2, Session.getUsername());
                    stmt.executeUpdate();
                }
                
                loadTasks();
                loadLeaderboardPreviewForClass();
                LOGGER.info("Task " + status + ", points adjusted: " + pointsChange);
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to update task status: " + e.getMessage());
        }
    }

    private void loadLeaderboardPreviewForClass() {
        try {
            Classes selectedClass = classSelector.getValue();
            if (selectedClass == null) {
                LOGGER.warning("No class selected for leaderboard");
                return;
            }

            ObservableList<UserRank> data = FXCollections.observableArrayList();
            String sql = "SELECT username, points FROM users WHERE class_id = ? ORDER BY points DESC LIMIT 5";

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
        } catch (Exception e) {
            LOGGER.severe("Failed to load leaderboard preview: " + e.getMessage());
        }
    }

    @FXML
    private void openLeaderboardScene() {
        try {
            Classes selectedClass = classSelector.getValue();
            if (selectedClass == null) {
                LOGGER.warning("No class selected for leaderboard");
                return;
            }
            Session.setSelectedClassId(selectedClass.getId());
            Navigator.switchScene("Leaderboard");
        } catch (Exception e) {
            LOGGER.severe("Failed to open leaderboard: " + e.getMessage());
        }
    }

    @FXML
    private void handleStartTimer() {
        try {
            String selected = timerPreset.getValue();
            if (selected == null || selected.isBlank()) {
                LOGGER.warning("No timer preset selected");
                return;
            }

            stopTimer();
            remainingSeconds = convertToSeconds(selected);
            startCountdown();
            LOGGER.info("Timer started: " + remainingSeconds + " seconds");
        } catch (Exception e) {
            LOGGER.severe("Failed to start timer: " + e.getMessage());
        }
    }

    private void startCountdown() {
        timeline = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> {
                remainingSeconds--;
                updateTimerLabel();
                if (remainingSeconds <= 0) {
                    stopTimer();
                }
            })
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void stopTimer() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    private void updateTimerLabel() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private int convertToSeconds(String value) {
        if (value.contains("Hour")) return 3600;
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
}
