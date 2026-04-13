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

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
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
import javafx.util.Duration;

public class TaskController implements TimerService.TimerListener {
    private static final Logger LOGGER = Logger.getLogger(TaskController.class.getName());
    private static final int BASE_TASK_POINTS = 10;
    private static final int LATE_TASK_POINTS = 5;
    private static final int TIMER_PENALTY_POINTS = 15;

    private Task timerTask;
    private TimerService timerService;

    // UI Elements
    @FXML private TextField taskField;
    @FXML private DatePicker taskDate;
    @FXML private ComboBox<Classes> classFilterComboBox;
    @FXML private ComboBox<Classes> classSelectionComboBox;
    @FXML private ListView<Task> allTaskList;
    @FXML private ListView<Task> filteredTaskList;
    
    @FXML private Button addTaskBtn;
    @FXML private Button markDoneBtn;
    @FXML private Button deleteTaskBtn;
    @FXML private Button uploadAttachmentBtn;
    @FXML private Button saveTaskInfoBtn;
    @FXML private Button startTimerBtn;
    @FXML private Label timerDisplayLabel;

    // Custom Tab Elements
    @FXML private Button tabMyTasksBtn;
    @FXML private Button tabApprovalsBtn;
    @FXML private VBox myTasksView;
    @FXML private VBox approvalsView;

    // Approval Elements
    @FXML private ComboBox<Classes> approvalClassFilterComboBox;
    @FXML private ListView<Task> pendingApprovalsList;
    @FXML private Button approveSelectedBtn;
    @FXML private Button rejectSelectedBtn;

    @FXML private TextArea taskInstructions;
    @FXML private ListView<String> attachmentsList;
    @FXML private ComboBox<String> timerDuration;

    private final ObservableList<Task> allTasks = FXCollections.observableArrayList();
    private final ObservableList<Task> filteredTasks = FXCollections.observableArrayList();
    private final ObservableList<Task> pendingApprovals = FXCollections.observableArrayList();
    private final ObservableList<Classes> userClasses = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        timerService = TimerService.getInstance();
        
        timerService.removeTimerListener(this);
        timerService.addTimerListener(this);

        updateTimerDisplay();

        setupRoleBasedUI();
        setupTaskListView();
        setupTimerDropdown();
        loadUserClasses();
        
        // Initialize Tabs Logic
        switchToMyTasksView();

        try {
            loadAllClassTasks();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load all class tasks: " + e.getMessage());
        }
        try {
            loadFilteredTasks();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load filtered tasks: " + e.getMessage());
        }

        if (Session.isHost()) {
            loadPendingApprovals();
            if (approvalClassFilterComboBox != null) {
                approvalClassFilterComboBox.setItems(userClasses);
                approvalClassFilterComboBox.setOnAction(e -> loadPendingApprovals());
            }
        }
        
        // Navbar Active State
        NavbarController.getInstance().setActive("tasks");
        LOGGER.info("Task scene initialized successfully");
    }

    // --- Custom Tab Switching Logic ---

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
    }

    private void updateTabStyles(boolean isMyTasksActive) {
        String activeStyle = "-fx-background-color: #3182ce; -fx-text-fill: white; -fx-border-color: #3182ce; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: white; -fx-text-fill: #718096; -fx-border-color: #e2e8f0; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand;";

        if (tabMyTasksBtn != null) {
            tabMyTasksBtn.setStyle(isMyTasksActive ? activeStyle : inactiveStyle);
        }
        if (tabApprovalsBtn != null) {
            tabApprovalsBtn.setStyle(isMyTasksActive ? inactiveStyle : activeStyle);
        }
    }

    // --- Existing Logic (Kept intact) ---

    private void setupTimerDropdown() {
        if (timerDuration != null) {
            ObservableList<String> durations = FXCollections.observableArrayList("5 min", "15 min", "30 min", "45 min", "60 min");
            timerDuration.setItems(durations);
            timerDuration.getSelectionModel().selectFirst();
        }
    }

    private void setupRoleBasedUI() {
        boolean isHost = Session.isHost();
        
        if (taskField != null) {
            taskField.setVisible(isHost);
            taskField.setManaged(isHost);
        }
        if (taskDate != null) {
            taskDate.setVisible(isHost);
            taskDate.setManaged(isHost);
        }
        if (classSelectionComboBox != null) {
            classSelectionComboBox.setVisible(isHost);
            classSelectionComboBox.setManaged(isHost);
            if (isHost) {
                classSelectionComboBox.setItems(userClasses);
            }
        }
        if (addTaskBtn != null) {
            addTaskBtn.setVisible(isHost);
            addTaskBtn.setManaged(isHost);
        }
        if (deleteTaskBtn != null) {
            deleteTaskBtn.setVisible(isHost);
            deleteTaskBtn.setManaged(isHost);
        }
        
        // Tab Visibility
        if (tabApprovalsBtn != null) {
            tabApprovalsBtn.setVisible(isHost);
            tabApprovalsBtn.setManaged(isHost);
            if (!isHost) {
                // Force switch to My Tasks if not host
                switchToMyTasksView();
            }
        }
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
        if (pendingApprovalsList != null) {
            pendingApprovalsList.setItems(pendingApprovals);
            pendingApprovalsList.setCellFactory(param -> createPendingApprovalListCell());
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
                    String status = task.getStatus().toLowerCase();
                    if ("done".equals(status)) {
                        undoTaskCompletion(task);
                    } else if ("pending_approval".equals(status) && Session.isHost()) {
                        approveTask(task);
                    } else if ("pending".equals(status)) {
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
                    String status = task.getStatus().toLowerCase();
                    if (null == status) {
                        taskLabel.setStyle("-fx-text-fill: black;");
                        actionButton.setText("Mark Done");
                        actionButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
                        actionButton.setVisible(true);
                    } else switch (status) {
                        case "done" -> {
                            taskLabel.setStyle("-fx-text-fill: #6b7280; -fx-opacity: 0.7;");
                            actionButton.setText("Undo");
                            actionButton.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white;");
                            actionButton.setVisible(true);
                        }
                        case "pending_approval" -> {
                            if (Session.isHost()) {
                                taskLabel.setStyle("-fx-text-fill: #f59e0b;");
                                actionButton.setText("Approve");
                                actionButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white;");
                                actionButton.setVisible(true);
                            } else {
                                taskLabel.setStyle("-fx-text-fill: #f59e0b; -fx-opacity: 0.8;");
                                actionButton.setVisible(false);
                            }
                        }
                        default -> {
                            taskLabel.setStyle("-fx-text-fill: black;");
                            actionButton.setText("Mark Done");
                            actionButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
                            actionButton.setVisible(true);
                        }
                    }
                    setText(null);
                    setGraphic(container);
                }
            }
        };
    }

    private ListCell<Task> createPendingApprovalListCell() {
        return new ListCell<Task>() {
            private final Label taskLabel = new Label();
            private final Label userLabel = new Label();
            private final Label pointsLabel = new Label();
            private final VBox container = new VBox(5, taskLabel, userLabel, pointsLabel);

            {
                container.setStyle("-fx-padding: 10; -fx-background-color: #fef3c7; -fx-border-color: #f59e0b; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
            }

            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    taskLabel.setText(task.getTaskName());
                    taskLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #92400e;");
                    
                    userLabel.setText("Submitted by: " + task.getUsername());
                    userLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #78350f;");
                    
                    pointsLabel.setText("Points: " + task.getPendingPoints());
                    pointsLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #92400e; -fx-font-weight: bold;");
                    
                    setText(null);
                    setGraphic(container);
                }
            }
        };
    }

    private void completeTask(Task task) {
        if (Session.isHost()) {
            updateTaskStatus(task, "Done");
        } else {
            updateTaskStatus(task, "pending_approval");
        }
    }

    private void undoTaskCompletion(Task task) {
        updateTaskStatus(task, "Pending");
    }

    private void approveTask(Task task) {
        updateTaskStatus(task, "Done");
    }

    private void updateTaskStatus(Task task, String newStatus) {
        if (task == null || task.getId() <= 0) {
            return;
        }

        if ("Done".equalsIgnoreCase(newStatus) && "Done".equalsIgnoreCase(task.getStatus())) {
            return;
        }

        if ("Pending".equalsIgnoreCase(newStatus) && !"Done".equalsIgnoreCase(task.getStatus()) && !"pending_approval".equalsIgnoreCase(task.getStatus())) {
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            if ("Done".equalsIgnoreCase(newStatus)) {
                int points;
                if ("pending_approval".equalsIgnoreCase(task.getStatus()) && task.getPendingPoints() > 0) {
                    points = task.getPendingPoints();
                } else {
                    points = calculateTaskCompletionPoints(task.getDate());
                }
                
                String updateSql = "UPDATE tasks SET status = 'Done', completed_date = ?, points_awarded = points_awarded + ?, pending_points = 0, approved_by = ?, approved_date = ? WHERE id = ? AND username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setDate(1, Date.valueOf(LocalDate.now()));
                    stmt.setInt(2, points);
                    stmt.setInt(3, getCurrentUserId());
                    stmt.setDate(4, Date.valueOf(LocalDate.now()));
                    stmt.setInt(5, task.getId());
                    stmt.setString(6, task.getUsername());
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = conn.prepareStatement("UPDATE users SET points = points + ? WHERE username = ?")) {
                    stmt.setInt(1, points);
                    stmt.setString(2, task.getUsername());
                    stmt.executeUpdate();
                }

            } else if ("pending_approval".equalsIgnoreCase(newStatus)) {
                int points = calculateTaskCompletionPoints(task.getDate());
                String updateSql = "UPDATE tasks SET status = 'pending_approval', completed_date = ?, pending_points = ? WHERE id = ? AND username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setDate(1, Date.valueOf(LocalDate.now()));
                    stmt.setInt(2, points);
                    stmt.setInt(3, task.getId());
                    stmt.setString(4, Session.getUsername());
                    stmt.executeUpdate();
                }

            } else if ("Pending".equalsIgnoreCase(newStatus)) {
                String updateSql = "UPDATE tasks SET status = 'Pending', completed_date = NULL, pending_points = 0 WHERE id = ? AND username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setInt(1, task.getId());
                    stmt.setString(2, Session.getUsername());
                    stmt.executeUpdate();
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

    private int calculateTaskCompletionPoints(LocalDate dueDate) {
        int basePoints;
        if (dueDate == null) {
            basePoints = BASE_TASK_POINTS;
        } else {
            long daysBefore = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
            if (daysBefore < 0) {
                basePoints = LATE_TASK_POINTS;
            } else {
                basePoints = BASE_TASK_POINTS + (int) Math.max(0, daysBefore) * 2;
            }
        }

        return basePoints;
    }

    private void reloadTaskLists() {
        try {
            loadAllClassTasks();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to reload all class tasks: " + e.getMessage());
        }
        try {
            loadFilteredTasks();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to reload filtered tasks: " + e.getMessage());
        }
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
                        // ignore
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

    private void loadAllClassTasks() throws Exception {
        allTasks.clear();
        Integer userId = getCurrentUserId();
        if (userId == null) {
            LOGGER.warning(() -> "Could not determine current user ID for user: " + Session.getUsername());
            return;
        }
        
        String sql = "SELECT t.id, t.username, t.task_name, t.task_date, t.status, t.class_id, COALESCE(c.class_name, '') as class_name, t.is_personal "
                   + "FROM tasks t "
                   + "LEFT JOIN classes c ON t.class_id = c.id "
                   + "LEFT JOIN user_classes uc ON c.id = uc.class_id AND uc.user_id = ? "
                   + "WHERE (t.user_id = ? OR (t.class_id IS NOT NULL AND uc.user_id IS NOT NULL)) "
                   + "ORDER BY t.task_date DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int classId = rs.getInt("class_id");
                    String className = rs.getString("class_name");
                    boolean isPersonal = rs.getBoolean("is_personal");
                    
                    allTasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("task_name"),
                        rs.getDate("task_date").toLocalDate(),
                        rs.getString("status"),
                        classId > 0 ? classId : null,
                        className != null && !className.isEmpty() ? className : null,
                        isPersonal
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.severe(() -> "Failed to load tasks: " + e.getMessage());
        }
    }

    private String formatTaskWithClass(Task task) {
        String status = task.getStatus();
        if ("pending_approval".equalsIgnoreCase(status)) {
            status = "Pending Approval";
        }
        String classSuffix = task.getClassName() != null ? " [" + task.getClassName() + "]" : "";
        return String.format("%s - %s%s (%s)", task.getTaskName(), status, classSuffix, task.getDate());
    }
    
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

            String sql = "SELECT t.id, t.username, t.task_name, t.task_date, t.status, t.class_id, c.class_name "
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
                            rs.getString("username"),
                            rs.getString("task_name"),
                            rs.getDate("task_date").toLocalDate(),
                            rs.getString("status"),
                            rs.getInt("class_id"),
                            rs.getString("class_name"),
                            false
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.severe(() -> "Failed to load filtered tasks: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleAddTask() {
        String taskName = taskField.getText().trim();
        LocalDate date = taskDate.getValue();
        Classes selectedClass = classSelectionComboBox != null ? classSelectionComboBox.getValue() : null;

        if (taskName.isEmpty() || date == null) {
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            if (selectedClass != null) {
                Integer ownerId = getCurrentUserId();
                if (ownerId == null) return;

                String classTaskSql = "INSERT INTO class_tasks (class_id, task_name, due_date, owner_id) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(classTaskSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, selectedClass.getId());
                    stmt.setString(2, taskName);
                    stmt.setDate(3, Date.valueOf(date));
                    stmt.setInt(4, ownerId);
                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int classTaskId = rs.getInt(1);
                            assignTaskToClassMembers(classTaskId);
                        }
                    }
                }
                LOGGER.info("Class task created and assigned to members successfully");
            } else {
                String sql = "INSERT INTO tasks (username, user_id, task_name, task_date, status, is_personal) VALUES (?, ?, ?, ?, 'Pending', 1)";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, Session.getUsername());
                    Integer userId = getCurrentUserId();
                    if (userId != null) {
                        stmt.setInt(2, userId);
                    } else {
                        stmt.setNull(2, java.sql.Types.INTEGER);
                    }
                    stmt.setString(3, taskName);
                    stmt.setDate(4, java.sql.Date.valueOf(date));
                    stmt.executeUpdate();
                }
                LOGGER.info("Personal task added successfully");
            }

            taskField.clear();
            taskDate.setValue(null);
            if (classSelectionComboBox != null) {
                classSelectionComboBox.getSelectionModel().clearSelection();
            }
            loadAllClassTasks();
            loadFilteredTasks();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to add task: " + e.getMessage());
        }
    }

    private void assignTaskToClassMembers(int classTaskId) {
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
            LOGGER.severe(() -> "Failed to assign task to class members: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleMarkDone() {
        Task selectedTask = getSelectedTask();
        if (selectedTask == null) {
            return;
        }
        updateTaskStatus(selectedTask, "Done");
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleDeleteTask() {
        if (!Session.isHost()) {
            LOGGER.warning("Participants cannot delete tasks");
            return;
        }

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

    @FXML
    @SuppressWarnings("unused")
    private void handleStartTimer() {
        Task selected = getSelectedTask();
        if (selected == null || timerDuration == null || timerDuration.getValue() == null) {
            LOGGER.warning("No task selected or timer duration not set");
            return;
        }

        String durationStr = timerDuration.getValue();
        int minutes = Integer.parseInt(durationStr.replace(" min", ""));
        int totalSeconds = minutes * 60;

        timerTask = selected;

        LOGGER.info(() -> "Timer started for " + minutes + " minutes on task: " + selected.getTaskName());

        timerService.start(totalSeconds, selected.getId(), true);
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleUploadAttachment() {
        Task selected = getSelectedTask();
        if (selected == null) {
            return;
        }
        LOGGER.info(() -> "Upload attachment handler for task: " + selected.getTaskName());
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleSaveTaskInfo() {
        Task selected = getSelectedTask();
        if (selected == null) {
            return;
        }
        if (taskInstructions != null) {
            String instructions = taskInstructions.getText();
            LOGGER.info(() -> "Saved task info for task: " + selected.getTaskName());
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

    @Override
    public void onTimerUpdated(int remainingSeconds, boolean running, boolean paused) {
        updateTimerDisplay();
    }

    @Override
    public void onTimerCompleted() {
        handleTimerCompleted();
    }

    private void updateTimerDisplay() {
        if (timerDisplayLabel == null) {
            return;
        }

        int remaining = timerService.getRemainingSeconds();
        boolean running = timerService.isRunning();

        if (!running) {
            timerDisplayLabel.setText("00:00");
            timerDisplayLabel.setStyle("-fx-text-fill: #7f8c8d;");
            return;
        }

        int minutes = remaining / 60;
        int seconds = remaining % 60;
        timerDisplayLabel.setText(String.format("%02d:%02d", minutes, seconds));
        timerDisplayLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
    }

    private void handleTimerCompleted() {
        if (timerTask == null) return;

        boolean taskCompleted = isTaskCompleted(timerTask);

        if (!taskCompleted) {
            int currentPoints = Session.getPoints();
            int newPoints = Math.max(0, currentPoints - TIMER_PENALTY_POINTS);
            Session.setPoints(newPoints);

            LOGGER.info(() -> "Timer finished - task not completed. Applied " + TIMER_PENALTY_POINTS +
                          " point penalty. Points: " + currentPoints + " -> " + newPoints);
        } else {
            LOGGER.info("Timer finished - task was completed during timer period");
        }

        timerTask = null;
        updateTimerDisplay();
    }
    
    private boolean isTaskCompleted(Task task) {
        Task refreshed = fetchTaskById(task.getId());
        if (refreshed == null) return false;

        String status = refreshed.getStatus().toLowerCase();
        return "done".equals(status) || "pending_approval".equals(status);
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

    private void loadPendingApprovals() {
        pendingApprovals.clear();
        
        String sql = "SELECT t.id, t.username, t.task_name, t.task_date, t.status, t.class_id, t.pending_points, " +
                    "COALESCE(c.class_name, '') as class_name " +
                    "FROM tasks t " +
                    "LEFT JOIN classes c ON t.class_id = c.id " +
                    "WHERE t.status = 'pending_approval' AND c.owner_id = ? " +
                    "ORDER BY t.task_date DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            Integer userId = getCurrentUserId();
            if (userId != null) {
                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Task task = new Task(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("task_name"),
                            rs.getDate("task_date").toLocalDate(),
                            rs.getString("status"),
                            rs.getInt("class_id"),
                            rs.getString("class_name"),
                            false
                        );
                        task.setPendingPoints(rs.getInt("pending_points"));
                        pendingApprovals.add(task);
                    }
                }
                LOGGER.info(() -> "Loaded " + pendingApprovals.size() + " pending approvals");
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load pending approvals: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleApproveSelected() {
        Task selected = pendingApprovalsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            approveTask(selected);
            loadPendingApprovals();
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleRejectSelected() {
        Task selected = pendingApprovalsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            rejectTask(selected);
            loadPendingApprovals();
        }
    }

    private void rejectTask(Task task) {
        updateTaskStatus(task, "Pending");
    }

    private Task fetchTaskById(int taskId) {
        String sql = "SELECT * FROM tasks WHERE id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, taskId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Task(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("task_name"),
                            rs.getDate("task_date").toLocalDate(),
                            rs.getString("status"),
                            rs.getInt("class_id"),
                            null,
                            rs.getBoolean("is_personal")
                    );
                }
            }

        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to fetch task by ID: " + e.getMessage());
        }

        return null;
    }
}