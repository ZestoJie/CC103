package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.Classes;
import com.cc103sys.cc103.Models.Task;
import com.cc103sys.cc103.Utils.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class TaskController {
    private static final Logger LOGGER = Logger.getLogger(TaskController.class.getName());
    private static final int BASE_TASK_POINTS = 10;
    private static final int LATE_TASK_POINTS = 5;

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

    private final ObservableList<Task> allTasks = FXCollections.observableArrayList();
    private final ObservableList<Task> filteredTasks = FXCollections.observableArrayList();
    private final ObservableList<Classes> userClasses = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupRoleBasedUI();
        setupTaskListView();
        loadUserClasses();
        loadAllClassTasks();
        loadFilteredTasks();
        // Set navbar active to tasks
        NavbarController.getInstance().setActive("tasks");
        LOGGER.info("Task scene initialized successfully");
    }

    private void setupRoleBasedUI() {
        boolean isHost = Session.isHost();
        // Hide add task section for participants
        if (!isHost) {
            // Find the parent container of the add task elements and hide it
            // Since we can't directly access the VBox from FXML, we'll hide the individual elements
            if (taskField != null) taskField.setVisible(false);
            if (taskDate != null) taskDate.setVisible(false);
            if (addTaskBtn != null) addTaskBtn.setVisible(false);
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
                    if ("Done".equalsIgnoreCase(task.getStatus())) {
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
                    boolean isDone = "Done".equalsIgnoreCase(task.getStatus());
                    if (isDone) {
                        taskLabel.setStyle("-fx-text-fill: #6b7280; -fx-opacity: 0.7;");
                        actionButton.setText("Undo");
                        actionButton.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white;");
                    } else {
                        taskLabel.setStyle("-fx-text-fill: black;");
                        actionButton.setText("Mark Done");
                        actionButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white;");
                    }
                    setText(null);
                    setGraphic(container);
                }
            }
        };
    }

    private void completeTask(Task task) {
        updateTaskStatus(task, "Done");
    }

    private void undoTaskCompletion(Task task) {
        updateTaskStatus(task, "Pending");
    }

    private void updateTaskStatus(Task task, String newStatus) {
        if (task == null || task.getId() <= 0) {
            return;
        }

        if ("Done".equalsIgnoreCase(newStatus) && "Done".equalsIgnoreCase(task.getStatus())) {
            return;
        }

        if ("Pending".equalsIgnoreCase(newStatus) && !"Done".equalsIgnoreCase(task.getStatus())) {
            return;
        }

        try (Connection conn = DBUtil.getConnection()) {
            if ("Done".equalsIgnoreCase(newStatus)) {
                int points = calculateTaskCompletionPoints(task.getDate());
                String updateSql = "UPDATE tasks SET status = 'Done', completed_date = ?, points_awarded = ? WHERE id = ? AND username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setDate(1, Date.valueOf(LocalDate.now()));
                    stmt.setInt(2, points);
                    stmt.setInt(3, task.getId());
                    stmt.setString(4, Session.getUsername());
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = conn.prepareStatement("UPDATE users SET points = points + ? WHERE username = ?")) {
                    stmt.setInt(1, points);
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
        String sql = "SELECT t.id, t.task_name, t.task_date, t.status, t.class_id, c.class_name "
                   + "FROM tasks t "
                   + "LEFT JOIN classes c ON t.class_id = c.id "
                   + "WHERE t.username = ? "
                   + "ORDER BY t.task_date DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    allTasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("task_name"),
                        rs.getDate("task_date").toLocalDate(),
                        rs.getString("status"),
                        rs.getObject("class_id") == null ? null : rs.getInt("class_id"),
                        rs.getString("class_name")
                    ));
                }
            }
            LOGGER.info(() -> "Loaded " + allTasks.size() + " tasks");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load class tasks: " + e.getMessage());
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

        String sql = "SELECT t.id, t.task_name, t.task_date, t.status, t.class_id, c.class_name "
                   + "FROM tasks t "
                   + "JOIN classes c ON t.class_id = c.id "
                   + "WHERE t.username = ? AND t.class_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
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

        String sql = "INSERT INTO tasks (username, task_name, task_date, status) VALUES (?, ?, ?, 'Pending')";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
            stmt.setString(2, taskName);
            stmt.setDate(3, Date.valueOf(date));
            stmt.executeUpdate();

            taskField.clear();
            taskDate.setValue(null);
            loadAllClassTasks();
            loadFilteredTasks();
            LOGGER.info("Task added successfully");
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
        updateTaskStatus(selectedTask, "Done");
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
}