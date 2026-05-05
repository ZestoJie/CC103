package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
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
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;
import com.cc103sys.cc103.Utils.TimerService;
import com.cc103sys.cc103.Utils.UiDialogs;

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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

@SuppressWarnings("unused")
public class TaskController implements TimerService.TimerListener {
    private static final Logger LOGGER = Logger.getLogger(TaskController.class.getName());
    private static final int BASE_TASK_POINTS = 10;
    private static final int LATE_TASK_POINTS = 5;
    private static final int REFRESH_INTERVAL_SECONDS = 5;

    private TimerService timerService;
    @SuppressWarnings("unused")
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
    private VBox classFilterSection;
    @FXML
    private VBox filteredTasksSection;
    @FXML
    private HBox actionControls;
    @FXML
    @SuppressWarnings("unused")
    private Button addTaskBtn;
    @FXML
    @SuppressWarnings("unused")
    private Button markDoneBtn;
    @FXML
    @SuppressWarnings("unused")
    private Button deleteTaskBtn;

    @FXML
    private TextArea taskDescriptionArea;
    @FXML
    private TextArea taskInstructions;

    @FXML
    private Button uploadAttachmentBtn;
    @FXML
    private ListView<String> attachmentsList;

    @FXML
    private Button startTimerBtn;
    @FXML
    private Button saveTaskInfoBtn;
    @FXML
    private ComboBox<String> timerDuration;

    @FXML
    private ListView<Task> pendingApprovalsList;
    @FXML
    private Button approveSelectedBtn;
    @FXML
    private Button rejectSelectedBtn;
    @FXML
    private VBox approvalsView;
    @FXML
    private ComboBox<Classes> approvalClassFilterComboBox;

    @FXML
    private Button submitTaskBtn;
    @FXML
    private Button unsubmitTaskBtn;

    private final ObservableList<Task> allTasks = FXCollections.observableArrayList();
    private final ObservableList<Task> filteredTasks = FXCollections.observableArrayList();
    private final ObservableList<Task> pendingApprovals = FXCollections.observableArrayList();
    private final ObservableList<Classes> userClasses = FXCollections.observableArrayList();
    private Timeline refreshTimeline;
    private boolean disposed;

    private Window window() {
        return allTaskList != null && allTaskList.getScene() != null
            ? allTaskList.getScene().getWindow()
            : null;
    }

    @FXML
    public void initialize() throws Exception {
        timerService = TimerService.getInstance();
        timerService.removeTimerListener(this);
        timerService.addTimerListener(this);
        
        setupRoleBasedUI();
        setupTaskListView();
        setupTimerDropdown();
        loadUserClasses();
        
        loadAllClassTasks();
        loadFilteredTasks();
        
        if (Session.isHost()) {
            loadPendingApprovals();
        }
        
        startAutoRefresh();
        NavbarController.getInstance().setActive("tasks");
        setupListPlaceholders();
        registerLifecycleHooks();
        LOGGER.info("Task scene initialized successfully");
    }

    private void registerLifecycleHooks() {
        if (allTaskList != null) {
            allTaskList.sceneProperty().addListener((obs, oldScene, newScene) -> {
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
        if (timerService != null) {
            timerService.removeTimerListener(this);
        }
        LOGGER.info("Task scene resources cleaned up");
    }

    private void setupListPlaceholders() {
        if (allTaskList != null) {
            Label empty = new Label("No tasks yet.\nAdd a personal task above or enroll in a class.");
            empty.getStyleClass().add("empty-state");
            empty.setWrapText(true);
            allTaskList.setPlaceholder(empty);
        }
        if (filteredTaskList != null) {
            Label empty = new Label("No tasks for the selected class.");
            empty.getStyleClass().add("empty-state");
            empty.setWrapText(true);
            filteredTaskList.setPlaceholder(empty);
        }
    }

    private void startAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(REFRESH_INTERVAL_SECONDS), e -> {
                try {
                    loadAllClassTasks();
                } catch (Exception e1) {
                }
                try {
                    loadFilteredTasks();
                } catch (Exception e1) {
                }
            })
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
        LOGGER.info(() -> "Auto-refresh timeline started (interval: " + REFRESH_INTERVAL_SECONDS + " seconds)");
    }

    private void stopAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            LOGGER.info("Auto-refresh timeline stopped");
        }
    }

    private void setupRoleBasedUI() {
        boolean isHost = Session.isHost();
        
        if (approvalsView != null) {
            approvalsView.setVisible(isHost);
            approvalsView.setManaged(isHost);
        }

        if (submitTaskBtn != null) {
            submitTaskBtn.setVisible(!isHost);
            submitTaskBtn.setManaged(!isHost);
        }
        if (unsubmitTaskBtn != null) {
            unsubmitTaskBtn.setVisible(!isHost);
            unsubmitTaskBtn.setManaged(!isHost);
        }

        if (classFilterSection != null) {
            classFilterSection.setVisible(!isHost);
            classFilterSection.setManaged(!isHost);
        }
        if (filteredTasksSection != null) {
            filteredTasksSection.setVisible(!isHost);
            filteredTasksSection.setManaged(!isHost);
        }
        if (actionControls != null) {
            actionControls.setVisible(!isHost);
            actionControls.setManaged(!isHost);
        }
    }

    private void setupTaskListView() {
        if (allTaskList != null) {
            allTaskList.setItems(allTasks);
            allTaskList.setCellFactory(param -> createTaskListCell());
            allTaskList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadTaskDetails(newVal);
                }
            });
        }
        if (filteredTaskList != null) {
            filteredTaskList.setItems(filteredTasks);
            filteredTaskList.setCellFactory(param -> createTaskListCell());
            filteredTaskList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadTaskDetails(newVal);
                }
            });
        }
    }

    private void loadTaskDetails(Task task) {
        if (task == null) return;

        if (taskInstructions != null) {
            taskInstructions.setText(task.getDescription() != null ? task.getDescription() : "");
        }

        if (attachmentsList != null) {
            attachmentsList.getItems().clear();
            attachmentsList.getItems().add("No attachments uploaded yet");
        }
    }

    private ListCell<Task> createTaskListCell() {
        return new javafx.scene.control.ListCell<Task>() {
            private final Label taskLabel = new Label();
            private final Button markDoneButton = new Button();
            private final Button viewButton = new Button("View Task");
            private final HBox container = new HBox(12, taskLabel, viewButton, markDoneButton);

            {
                container.setStyle("-fx-alignment: CENTER_LEFT;");
                
                markDoneButton.setOnAction(e -> {
                    Task task = getItem();
                    if (task == null) return;
                    if ("Done".equalsIgnoreCase(task.getStatus()) || "For Approval".equalsIgnoreCase(task.getStatus())) {
                        undoTaskCompletion(task);
                    } else {
                        completeTask(task);
                    }
                });

                viewButton.setStyle("-fx-padding: 5 15; -fx-font-size: 11;");
                viewButton.setOnAction(e -> {
                    Task task = getItem();
                    if (task == null) return;
                    if (task.getClassId() == null || task.getClassTaskId() == null) {
                        LOGGER.info(() -> "Task has no class assignment, cannot open class detail: " + task.getTaskName());
                        return;
                    }
                    Navigator.navigateToTaskDetail(task.getClassId(), task.getClassTaskId());
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
                        markDoneButton.setText("Undo");
                        markDoneButton.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; -fx-padding: 5 15;");
                        if ("For Approval".equalsIgnoreCase(task.getStatus())) {
                            taskLabel.setText(formatTaskWithClass(task) + " (For Approval)");
                        } else {
                            taskLabel.setText(formatTaskWithClass(task));
                        }
                    } else {
                        taskLabel.setStyle("-fx-text-fill: black;");
                        markDoneButton.setText("Mark Done");
                        markDoneButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-padding: 5 15;");
                        taskLabel.setText(formatTaskWithClass(task));
                    }
                    viewButton.setDisable(task.getClassTaskId() == null || task.getClassId() == null);
                    viewButton.setStyle("-fx-padding: 5 15; -fx-font-size: 11;");
                    HBox.setHgrow(taskLabel, Priority.ALWAYS);
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

    private void refreshNavbarUserInfo() throws Exception {
        NavbarController navbar = NavbarController.getInstance();
        if (navbar != null) {
            navbar.loadUserInfo();
        }
    }

    @SuppressWarnings("unused")
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

    private void reloadTaskLists() throws Exception {
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
                classFilterComboBox.setOnAction(e -> {
                    try {
                        loadFilteredTasks();
                    } catch (Exception e1) {
                    }
                });
                if (!userClasses.isEmpty()) {
                    classFilterComboBox.getSelectionModel().selectFirst();
                }
            }

            LOGGER.info(() -> "Loaded " + userClasses.size() + " task classes");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load task classes: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private void loadAllClassTasks() throws Exception {
        allTasks.clear();
        
        try {
            Integer userId = getCurrentUserId();
            if (userId == null) {
                LOGGER.warning(() -> "Could not determine current user ID for user: " + Session.getUsername());
                return;
            }
            
            LOGGER.info(() -> "Loading all tasks for user ID: " + userId + " (" + Session.getUsername() + ")");
            
            String sql = "SELECT t.id, t.task_name, t.task_date, t.status, t.description, t.class_id, t.class_task_id, COALESCE(c.class_name, '') as class_name "
                       + "FROM tasks t "
                       + "LEFT JOIN classes c ON t.class_id = c.id "
                       + "WHERE (t.user_id = ? OR t.username = ?) "
                       + "AND NOT (t.created_by = ? AND t.class_task_id IS NOT NULL) "
                       + "ORDER BY t.task_date DESC";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, Session.getUsername());
                stmt.setString(3, Session.getUsername());
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
                            rs.getString("description"),
                            classId > 0 ? classId : null,
                            rs.getObject("class_task_id") != null ? rs.getInt("class_task_id") : null,
                            className != null && !className.isEmpty() ? className : null,
                            false,
                            Session.getUsername()
                        );
                        allTasks.add(task);
                        LOGGER.log(Level.INFO, "Added task: {0} (ID: {1}, ClassID: {2})", new Object[]{rs.getString("task_name"), rs.getInt("id"), classId});
                        count++;
                    }
                    LOGGER.info(String.format("Total tasks loaded: %d", count));
                }
            }
        } catch (SQLException e) {
            LOGGER.severe(() -> "Failed to load tasks: " + e.getMessage());
        }
    }

    private String formatTaskWithClass(Task task) {
        String classSuffix = task.getClassName() != null ? " [" + task.getClassName() + "]" : "";
        return String.format("%s - %s%s (%s)", task.getTaskName(), task.getStatus(), classSuffix, task.getDate());
    }

    @SuppressWarnings("unused")
    private void loadFilteredTasks() throws Exception {
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

            String sql = "SELECT t.id, t.task_name, t.task_date, t.status, t.description, t.class_id, t.class_task_id, c.class_name "
                       + "FROM tasks t "
                       + "JOIN classes c ON t.class_id = c.id "
                       + "WHERE (t.user_id = ? OR t.username = ?) AND t.class_id = ? "
                       + "AND NOT (t.created_by = ? AND t.class_task_id IS NOT NULL)";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, Session.getUsername());
                stmt.setInt(3, selected.getId());
                stmt.setString(4, Session.getUsername());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        filteredTasks.add(new Task(
                            rs.getInt("id"),
                            rs.getString("task_name"),
                            rs.getDate("task_date").toLocalDate(),
                            rs.getString("status"),
                            rs.getString("description"),
                            rs.getInt("class_id"),
                            rs.getObject("class_task_id") != null ? rs.getInt("class_task_id") : null,
                            rs.getString("class_name"),
                            false,
                            Session.getUsername()
                        ));
                    }
                }
            }
            LOGGER.info(() -> "Loaded " + filteredTasks.size() + " filtered tasks for class " + selected.getClassName());
        } catch (SQLException e) {
            LOGGER.severe(() -> "Failed to load filtered tasks: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleAddTask() {
        String taskName = taskField.getText().trim();
        LocalDate date = taskDate.getValue();
        String description = taskDescriptionArea != null ? taskDescriptionArea.getText().trim() : "";

        if (taskName.isEmpty()) {
            UiDialogs.warn(window(), "Missing name", "Please enter a task name.");
            return;
        }
        if (date == null) {
            UiDialogs.warn(window(), "Missing date", "Please choose a due date.");
            return;
        }

        String sql = "INSERT INTO tasks (username, user_id, task_name, task_date, status, is_personal, description) VALUES (?, ?, ?, ?, 'Pending', 1, ?)";

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
            stmt.setDate(4, java.sql.Date.valueOf(date));
            stmt.setString(5, description);
            stmt.executeUpdate();

            taskField.clear();
            taskDate.setValue(null);
            if (taskDescriptionArea != null) {
                taskDescriptionArea.clear();
            }
            loadAllClassTasks();
            loadFilteredTasks();
            LOGGER.info("Personal task added successfully");
            UiDialogs.info(window(), "Task added", "Your task was saved.");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to add task: " + e.getMessage());
            UiDialogs.error(window(), "Could not add task", e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleMarkDone() {
        Task selectedTask = getSelectedTask();
        if (selectedTask == null) {
            return;
        }
        updateTaskStatus(selectedTask, "For Approval");
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleDeleteTask() {
        Task selectedTask = getSelectedTask();
        if (selectedTask == null) {
            UiDialogs.warn(window(), "Nothing selected", "Select a task to delete.");
            return;
        }
        if (!UiDialogs.confirm(window(), "Delete task?",
            "Remove \"" + selectedTask.getTaskName() + "\"? This cannot be undone.")) {
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
            UiDialogs.info(window(), "Deleted", "The task was removed.");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to delete task: " + e.getMessage());
            UiDialogs.error(window(), "Delete failed", e.getMessage());
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
            LOGGER.warning(() -> "Failed to get current user ID: " + e.getMessage());
        }
        return null;
    }

    private void setupTimerDropdown() {
        if (timerDuration != null) {
            ObservableList<String> durations = FXCollections.observableArrayList(
                "5 min", "10 min", "15 min", "20 min", "25 min", "30 min", "45 min", "60 min"
            );
            timerDuration.setItems(durations);
            timerDuration.getSelectionModel().select(4);
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
    }

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
            if (attachmentsList != null) {
                attachmentsList.getItems().clear();
                attachmentsList.getItems().add("📎 " + selectedFile.getName());
                attachmentsList.getItems().add("Path: " + selectedFile.getAbsolutePath());
            }

            updateTaskStatus(task, "For Approval");
            LOGGER.info(() -> "Attachment added for task: " + task.getTaskName());
        } else {
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
            LOGGER.log(Level.INFO, "Task notes saved for task: {0}", selected.getTaskName());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save task notes: {0}", e.getMessage());
        }
    }

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

    @FXML
    private void handleSubmitTask() {
        Task selected = allTaskList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            selected = filteredTaskList.getSelectionModel().getSelectedItem();
        }
        if (selected == null) return;

        completeTask(selected);
        LOGGER.log(Level.INFO, "Task submitted for approval: {0}", selected.getTaskName());
    }

    @FXML
    private void handleUnsubmitTask() {
        Task selected = allTaskList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            selected = filteredTaskList.getSelectionModel().getSelectedItem();
        }
        if (selected == null) return;

        undoTaskCompletion(selected);
        LOGGER.log(Level.INFO, "Task un-submitted: {0}", selected.getTaskName());
    }

    private void approveTask(Task task) {
        updateTaskStatus(task, "Done");
    }

    @SuppressWarnings("unused")
    private void loadPendingApprovals() {
        if (pendingApprovalsList == null) return;
        
        Integer userId = getCurrentUserId();
        if (userId == null) return;
        
        pendingApprovals.clear();
        
        String sql = "SELECT t.id, t.task_name, t.task_date, t.status, t.class_id, " +
                     "COALESCE(c.class_name, '') as class_name, t.is_personal " +
                     "FROM tasks t " +
                     "LEFT JOIN classes c ON t.class_id = c.id " +
                     "WHERE t.status = 'For Approval' AND (t.user_id = ? OR t.username = ?) " +
                     "ORDER BY t.task_date DESC";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(2, Session.getUsername());
            
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

    @Override
    public void onTimerUpdated(int secondsRemaining, boolean running, boolean paused) {
    }

    @Override
    public void onTimerCompleted() throws Exception {
        LOGGER.info("Timer completed for task");
        loadAllClassTasks();
        loadFilteredTasks();
    }
}