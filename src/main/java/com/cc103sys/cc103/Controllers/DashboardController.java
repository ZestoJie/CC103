package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.Classes;
import com.cc103sys.cc103.Models.Task;
import com.cc103sys.cc103.Models.UserRank;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;
import com.cc103sys.cc103.Utils.TimerService;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class DashboardController implements TimerService.TimerListener {
    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    // UI Elements
    @FXML private ComboBox<Classes> classSelector;
    @FXML private Label welcomeLabel;

    @FXML private TextField taskField;
    @FXML private DatePicker taskDate;
    @FXML private Button addTaskButton;
    @FXML private VBox addTaskFooter;
    @FXML private ListView<Task> taskList;
    
    @FXML private Label timerLabel;
    @FXML private Label timerMultiplierLabel; // Not used in new UI but kept for compatibility
    @FXML private ListView<UserRank> leaderboardPreview;
    
    @FXML private ComboBox<String> timerPreset;
    @FXML private Button timerStartButton;
    @FXML private Button timerPauseButton;
    @FXML private RadioButton studyModeRadio;
    @FXML private RadioButton breakModeRadio;
    @FXML private CheckBox xpActiveCheckbox;
    
    @FXML private ProgressBar dailyProgressBar;
    @FXML private Label dailyGoalSummary;
    @FXML private Label dailyGoalTip;
    @FXML private TextField customTimeField;

    // Logic & State
    private TimerService timerService;
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private Task selectedTask;

    @FXML
    public void initialize() {
        try {
            timerService = TimerService.getInstance();
            // Manage listener lifecycle to prevent duplicates
            timerService.removeTimerListener(this);
            timerService.addTimerListener(this);

            setupUIComponents();
            setupEventListeners();
            
            loadData();
            
            // Initial UI state sync
            updateTimerAvailability();
            updateTimerLabel();
            NavbarController.getInstance().setActive("dashboard");

        } catch (Exception e) {
            LOGGER.severe(() -> "Dashboard Initialization Error: " + e.getMessage());
        }
    }

    private void setupUIComponents() {
        setupWelcomeMessage();
        setupTaskListCellFactory();
        setupTimerPresets();
        setupRoleBasedUI();
        animateNode(taskList);
        animateNode(leaderboardPreview);
        
        if (xpActiveCheckbox != null) xpActiveCheckbox.setSelected(true);
    }

    private void setupEventListeners() {
        if (classSelector != null) {
            classSelector.setOnAction(e -> {
                selectedTask = null; // Reset selection on filter change
                updateTimerAvailability();
                try {
                    loadLeaderboardPreviewForClass();
                } catch (Exception ex) {
                    LOGGER.warning(() -> "Failed to load leaderboard preview: " + ex.getMessage());
                }
            });
        }

        if (taskList != null) {
            taskList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                selectedTask = newVal;
                updateTimerAvailability();
            });
        }
    }

    private void loadData() {
        try {
            loadTasks();
            checkMissedTasks();
            loadUserClassesForLeaderboard();
        } catch (Exception e) {
            LOGGER.severe(() -> "Error loading dashboard data: " + e.getMessage());
        }
    }

    // --- Visual Animations ---

    private void animateNode(Node node) {
        if (node == null) return;
        FadeTransition fade = new FadeTransition(Duration.millis(400), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(400), node);
        slide.setFromY(10);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    // --- Setup Helpers ---

    private void setupRoleBasedUI() {
        boolean isHost = Session.isHost();
        if (addTaskFooter != null) {
            addTaskFooter.setVisible(isHost);
            addTaskFooter.setManaged(isHost);
        }
    }

    private void setupWelcomeMessage() {
        String username = Session.getUsername();
        if (username != null && welcomeLabel != null) {
            welcomeLabel.setText("Welcome back, " + username + "!");
        }
    }

    private void setupTaskListCellFactory() {
        if (taskList == null) return;
        
        taskList.setItems(tasks);
        taskList.setCellFactory(param -> new ListCell<Task>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle(""); // Reset style
                } else {
                    // Determine style based on status
                    String statusColor = "#2d3748"; // Default dark
                    String bgStyle = "-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #edf2f7; -fx-border-width: 0 0 1 0; -fx-border-insets: 0 0 5 0;";
                    
                    String statusLower = task.getStatus().toLowerCase();
                    if (statusLower.contains("done") || statusLower.contains("completed")) {
                        statusColor = "#a0aec0"; // Muted gray
                        bgStyle = "-fx-background-color: #f7fafc; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #edf2f7; -fx-border-width: 0 0 1 0; -fx-border-insets: 0 0 5 0;";
                    } else if (statusLower.contains("missed")) {
                        statusColor = "#e53e3e"; // Red
                    } else if (statusLower.contains("pending_approval")) {
                        statusColor = "#d69e2e"; // Orange
                    }

                    String classLabel = (task.getClassName() != null && !task.getClassName().isEmpty()) 
                                      ? "[" + task.getClassName() + "] " : "";
                    
                    VBox content = new VBox();
                    Label nameLabel = new Label(classLabel + task.getTaskName());
                    nameLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 14; -fx-text-fill: " + statusColor + ";");
                    
                    HBox metaRow = new HBox();
                    metaRow.setSpacing(10);
                    Label dateLabel = new Label(task.getDate().toString());
                    dateLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #718096;");
                    
                    Label statusLabel = new Label(task.getStatus());
                    statusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #718096; -fx-background-radius: 4; -fx-padding: 2 6; -fx-background-color: #edf2f7;");
                    
                    metaRow.getChildren().addAll(dateLabel, statusLabel);
                    content.getChildren().addAll(nameLabel, metaRow);

                    setGraphic(content);
                    setText(null);
                    setStyle(bgStyle);
                }
            }
        });
    }

    // --- Data Loading ---

    private void loadTasks() throws Exception {
        tasks.clear();
        Integer userId = getCurrentUserId();
        if (userId == null) return;

        String sql = "SELECT t.id, t.task_name, t.task_date, t.status, t.class_id, COALESCE(c.class_name, '') as class_name "
                   + "FROM tasks t "
                   + "LEFT JOIN classes c ON t.class_id = c.id "
                   + "WHERE t.user_id = ? "
                   + "ORDER BY t.status ASC, t.task_date ASC"; // Pending first, then by date

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
        } catch (SQLException e) {
            LOGGER.severe(() -> "Failed to load tasks: " + e.getMessage());
        }
        
        updateDailyGoalProgress();
    }

    private void checkMissedTasks() {
        String sql = "UPDATE tasks SET status = 'missed' WHERE username = ? AND status NOT IN ('Done', 'completed', 'missed', 'pending_approval') AND task_date < CURDATE()";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
            int updated = stmt.executeUpdate();
            if (updated > 0) LOGGER.info(() -> "Marked " + updated + " tasks as missed");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to check missed tasks: " + e.getMessage());
        }
    }

    private void updateDailyGoalProgress() {
        int total = 0;
        int completed = 0;
        // Only counts tasks due TODAY
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
            if (dailyProgressBar != null) dailyProgressBar.setProgress(progress);
            
            if (dailyGoalSummary != null) {
                dailyGoalSummary.setText(total == 0 ? "No tasks due today" : String.format("%d/%d Completed", completed, total));
            }
            if (dailyGoalTip != null) {
                dailyGoalTip.setText(total == 0 ? "Enjoy your free time!" : (progress == 1.0 ? "Daily goal reached! 🎉" : "Keep going!"));
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to update daily goal: " + e.getMessage());
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
                if (!userClasses.isEmpty()) {
                    classSelector.setValue(userClasses.get(0));
                    loadLeaderboardPreviewForClass();
                }
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load user classes: " + e.getMessage());
        }
    }

    private void loadLeaderboardPreviewForClass() throws Exception {
        if (classSelector == null || classSelector.getValue() == null) return;

        Classes selectedClass = classSelector.getValue();
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
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            int rank = getIndex() + 1;
                            String rankIcon = (rank == 1) ? "🥇 " : (rank == 2) ? "🥈 " : (rank == 3) ? "🥉 " : rank + ". ";
                            
                            HBox container = new HBox();
                            Label rankLabel = new Label(rankIcon);
                            rankLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #718096;");
                            
                            Label nameLabel = new Label(item.getUsername());
                            nameLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 14; -fx-text-fill: #2d3748;");
                            
                            Region spacer = new Region();
                            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                            
                            Label ptsLabel = new Label(item.getPoints() + " XP");
                            ptsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #3182ce;");
                            
                            container.getChildren().addAll(rankLabel, nameLabel, spacer, ptsLabel);
                            setGraphic(container);
                            setText(null);
                            setStyle("-fx-padding: 5 0;");
                        }
                    }
                });
            }
        } catch (SQLException e) {
            LOGGER.severe(() -> "Failed to load leaderboard preview: " + e.getMessage());
        }
    }

    // --- Actions: Task Management ---

    @FXML
    @SuppressWarnings("unused")
    private void handleAddTask() {
        String taskName = taskField.getText().trim();
        LocalDate date = taskDate.getValue();

        if (!validateTaskInput(taskName, date)) return;

        try {
            // Host Logic: Class Task
            if (Session.isHost() && classSelector != null && classSelector.getValue() != null) {
                Classes selectedClass = classSelector.getValue();
                if (isCurrentUserClassOwner(selectedClass.getId())) {
                    int classTaskId = createClassTask(selectedClass.getId(), taskName, date);
                    if (classTaskId > 0) {
                        assignClassTaskToMembers(classTaskId, selectedClass.getId());
                        finishTaskAdd();
                        LOGGER.info(() -> "Class task created: " + taskName);
                        return;
                    }
                }
            }

            // Personal/Default Task Logic
            String sql = "INSERT INTO tasks (username, user_id, task_name, task_date, status, is_personal) VALUES (?, ?, ?, ?, 'Pending', 1)";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, Session.getUsername());
                stmt.setInt(2, getCurrentUserId());
                stmt.setString(3, taskName);
                stmt.setDate(4, Date.valueOf(date));
                stmt.executeUpdate();
                
                finishTaskAdd();
                LOGGER.info(() -> "Personal task created: " + taskName);
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to add task: " + e.getMessage());
        }
    }

    private void finishTaskAdd() {
        taskField.clear();
        taskDate.setValue(null);
        try {
            loadTasks();
            playAddTaskAnimation();
        } catch (Exception e) {
            LOGGER.warning("Error refreshing task list");
        }
    }

    private void playAddTaskAnimation() {
        if (taskList != null) {
            // Subtle flash effect
            FadeTransition ft = new FadeTransition(Duration.millis(300), taskList);
            ft.setFromValue(1.0);
            ft.setToValue(0.7);
            ft.setCycleCount(2);
            ft.setAutoReverse(true);
            ft.play();
        }
    }

    // --- Actions: Timer ---

    private void setupTimerPresets() {
        if (timerPreset != null) {
            timerPreset.getItems().addAll("1 Minute", "5 Minutes", "10 Minutes", "15 Minutes", "25 Minutes", "45 Minutes", "1 Hour", "Custom");
            timerPreset.setValue("25 Minutes");
            timerPreset.setOnAction(e -> handleTimerPresetChange());
        }
    }

    private void handleTimerPresetChange() {
        boolean isCustom = "Custom".equals(timerPreset.getValue());
        if (customTimeField != null) {
            customTimeField.setVisible(isCustom);
            customTimeField.setManaged(isCustom);
        }
    }

    private void updateTimerAvailability() {
        // Timer can start if:
        // 1. A task is selected
        // 2. Task is not already Done, Completed, or Missed
        boolean canStart = selectedTask != null 
                        && !selectedTask.getStatus().equalsIgnoreCase("Done")
                        && !selectedTask.getStatus().equalsIgnoreCase("completed")
                        && !selectedTask.getStatus().equalsIgnoreCase("missed");

        if (timerStartButton != null) timerStartButton.setDisable(!canStart);
        if (timerPreset != null) timerPreset.setDisable(!canStart);
        if (customTimeField != null) customTimeField.setDisable(!canStart);
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleStartTimer() {
        if (selectedTask == null || timerPreset == null) return;

        // Resume Logic
        if (timerService.isRunning() && timerService.isPaused()) {
            timerService.resume();
            updateTimerButtonState(true);
            return;
        }

        // Start Logic
        int seconds = parseTimerDuration();
        if (seconds <= 0) return;

        // Update Task Status to 'in progress'
        updateTaskStatus(selectedTask.getId(), "in progress");

        boolean xpEnabled = xpActiveCheckbox != null && xpActiveCheckbox.isSelected();
        timerService.start(seconds, selectedTask.getId(), xpEnabled);

        updateTimerButtonState(true);
        LOGGER.info(() -> "Timer started for: " + selectedTask.getTaskName());
    }

    @FXML
    @SuppressWarnings("unused")
    private void handlePauseTimer() {
        if (!timerService.isRunning()) return;

        if (timerService.isPaused()) {
            timerService.resume();
            updateTimerButtonState(true);
        } else {
            timerService.pause();
            updateTimerButtonState(false); // Paused state
        }
    }

    private int parseTimerDuration() {
        String val = timerPreset.getValue();
        if ("Custom".equals(val)) {
            try {
                int mins = Integer.parseInt(customTimeField.getText());
                if (mins > 0 && mins <= 480) return mins * 60;
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            return switch (val) {
                case "1 Hour" -> 3600;
                case "45 Minutes" -> 2700;
                case "25 Minutes" -> 1500;
                case "15 Minutes" -> 900;
                case "10 Minutes" -> 600;
                case "5 Minutes" -> 300;
                case "1 Minute" -> 60;
                default -> 1500;
            };
        }
        return 0;
    }

    private void updateTimerButtonState(boolean isRunning) {
        if (timerStartButton != null) {
            timerStartButton.setText(isRunning ? "Restart" : "Start Focus");
        }
        if (timerPauseButton != null) {
            timerPauseButton.setDisable(!isRunning);
            timerPauseButton.setText(timerService.isPaused() ? "Resume" : "Pause");
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleModeChange(ActionEvent event) {
        if (timerPreset == null) return;
        
        boolean isStudy = studyModeRadio.isSelected();
        if (isStudy) {
            timerPreset.getItems().setAll("25 Minutes", "45 Minutes", "1 Hour", "Custom");
        } else {
            timerPreset.getItems().setAll("5 Minutes", "10 Minutes", "15 Minutes", "Custom");
        }
        timerPreset.setValue(timerPreset.getItems().get(0));
        handleTimerPresetChange();
    }

    // --- Timer Listener Implementation ---

    @Override
    public void onTimerUpdated(int remainingSeconds, boolean running, boolean paused) {
        javafx.application.Platform.runLater(this::updateTimerLabel);
    }

    @Override
    public void onTimerCompleted() {
        javafx.application.Platform.runLater(() -> {
            updateTimerLabel();
            updateTimerButtonState(false);
            // Refresh task list to show status changes (e.g., if logic moved it to Done)
            try {
                loadTasks(); 
            } catch (Exception e) {
                LOGGER.warning("Failed to refresh tasks after timer");
            }
        });
    }

    private void updateTimerLabel() {
        if (timerLabel == null) return;

        if (timerService.isRunning()) {
            int remaining = timerService.getRemainingSeconds();
            int m = remaining / 60;
            int s = remaining % 60;
            timerLabel.setText(String.format("%02d:%02d", m, s));
            timerLabel.setStyle("-fx-text-fill: #ffffff;");
        } else {
            timerLabel.setText("00:00");
            timerLabel.setStyle("-fx-text-fill: #718096;");
        }
    }

    // --- Utilities & Helpers ---

    @FXML
    @SuppressWarnings("unused")
    private void openLeaderboardScene() {
        if (classSelector != null && classSelector.getValue() != null) {
            Session.setCurrentClassId(classSelector.getValue().getId());
            try {
                Navigator.switchScene("Leaderboard");
            } catch (Exception e) {
                LOGGER.severe(() -> "Navigation error: " + e.getMessage());
            }
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void goSettings() {
        try {
            Navigator.switchScene("Settings");
        } catch (Exception e) {
            LOGGER.severe(() -> "Navigation error: " + e.getMessage());
        }
    }

    private void updateTaskStatus(int taskId, String status) {
        String sql = "UPDATE tasks SET status = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, taskId);
            stmt.executeUpdate();
            if (selectedTask != null && selectedTask.getId() == taskId) {
                selectedTask.setStatus(status);
                taskList.refresh();
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to update task status: " + e.getMessage());
        }
    }

    private Integer getCurrentUserId() {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to get user ID: " + e.getMessage());
        }
        return null;
    }

    private boolean isCurrentUserClassOwner(int classId) {
        Integer ownerId = null;
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) return false;

        String sql = "SELECT owner_id FROM classes WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, classId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) ownerId = rs.getInt("owner_id");
            }
        } catch (Exception e) {
            return false;
        }
        return currentUserId.equals(ownerId);
    }

    private int createClassTask(int classId, String taskName, LocalDate date) throws Exception {
        Integer ownerId = getCurrentUserId();
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
                if (rs.next()) return rs.getInt(1);
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
            LOGGER.severe(() -> "Failed to assign class task: " + e.getMessage());
        }
    }

    private boolean validateTaskInput(String taskName, LocalDate date) {
        if (taskName == null || taskName.isBlank()) {
            LOGGER.warning("Task name empty");
            return false;
        }
        if (date == null) {
            LOGGER.warning("Task date empty");
            return false;
        }
        return true;
    }
}