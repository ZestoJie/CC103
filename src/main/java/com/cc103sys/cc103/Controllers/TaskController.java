package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.Task;
import com.cc103sys.cc103.Utils.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class TaskController {
    private static final Logger LOGGER = Logger.getLogger(TaskController.class.getName());

    @FXML
    private TextField taskField;
    @FXML
    private DatePicker taskDate;
    @FXML
    private ListView<Task> taskList;
    @FXML
    @SuppressWarnings("unused")
    private Button addTaskBtn;
    @FXML
    @SuppressWarnings("unused")
    private Button markDoneBtn;
    @FXML
    @SuppressWarnings("unused")
    private Button deleteTaskBtn;

    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTaskListView();
        loadTasks();
        // Set navbar active to tasks
        NavbarController.getInstance().setActive("tasks");
        LOGGER.info("Task scene initialized successfully");
    }

    private void setupTaskListView() {
        taskList.setItems(tasks);
        taskList.setCellFactory(param -> new javafx.scene.control.ListCell<Task>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if (empty || task == null) {
                    setText(null);
                } else {
                    setText(task.getTaskName() + " - " + task.getDate() + " (" + task.getStatus() + ")");
                }
            }
        });
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
            LOGGER.info(() -> "Loaded " + tasks.size() + " tasks");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load tasks: " + e.getMessage());
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

        String sql = "INSERT INTO tasks (username, task_name, task_date, status) VALUES (?, ?, ?, 'pending')";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, Session.getUsername());
            stmt.setString(2, taskName);
            stmt.setDate(3, Date.valueOf(date));
            stmt.executeUpdate();

            taskField.clear();
            taskDate.setValue(null);
            loadTasks();
            LOGGER.info("Task added successfully");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to add task: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleMarkDone() {
        Task selectedTask = taskList.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            return;
        }

        String sql = "UPDATE tasks SET status = 'completed' WHERE id = ? AND username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selectedTask.getId());
            stmt.setString(2, Session.getUsername());
            stmt.executeUpdate();

            loadTasks();
            LOGGER.info("Task marked as done");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to mark task as done: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleDeleteTask() {
        Task selectedTask = taskList.getSelectionModel().getSelectedItem();
        if (selectedTask == null) {
            return;
        }

        String sql = "DELETE FROM tasks WHERE id = ? AND username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selectedTask.getId());
            stmt.setString(2, Session.getUsername());
            stmt.executeUpdate();

            loadTasks();
            LOGGER.info("Task deleted");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to delete task: " + e.getMessage());
        }
    }
}