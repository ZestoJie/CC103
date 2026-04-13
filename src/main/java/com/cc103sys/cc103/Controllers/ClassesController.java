package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Random;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.ClassTask;
import com.cc103sys.cc103.Models.Classes;
import com.cc103sys.cc103.Utils.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class ClassesController {
    private static final Logger LOGGER = Logger.getLogger(ClassesController.class.getName());

    // Navigation Buttons (Fake Tabs)
    @FXML private Button tabAllClassesBtn;
    @FXML private Button tabDetailBtn;

    // View Containers
    @FXML private VBox allClassesView;
    @FXML private VBox classDetailView;

    // Input Fields
    @FXML private TextField codeField;
    @FXML private TextField classNameField;
    @FXML private VBox createClassSection;
    @FXML private Button addClassButton;
    @FXML private CheckBox publicClassCheckbox;
    @FXML @SuppressWarnings("unused")
    private TextField privateClassCodeField;

    // List Views
    @FXML private ListView<Classes> classList;
    @FXML private ListView<Classes> myClassList;

    // Detail View Elements
    @FXML private Label detailClassTitleLabel;
    @FXML private Label detailClassDescriptionLabel;
    @FXML private ListView<ClassTask> detailTasksListView;
    @FXML private ListView<String> detailParticipantsListView;
    @FXML private Label detailFormTitleLabel;
    @FXML private TextField detailTaskTitleField;
    @FXML private TextArea detailTaskDescriptionArea;
    @FXML private DatePicker detailTaskDueDatePicker;
    @FXML private Button detailSubmitTaskButton;
    @FXML private Button detailCancelEditButton;
    @FXML private Button detailEditTaskButton;
    @FXML private Button detailDeleteTaskButton;

    private int currentDetailClassId = -1;
    private ClassTask editingDetailTask = null;
    private final ObservableList<ClassTask> detailClassTasks = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupRoleBasedUI();
        initializeClassDetailTab(); // Prepares the detail view state
        loadPublicClasses();
        loadOwnedClasses();

        // --- FIX 1: Hide Create Section by Default ---
        if (createClassSection != null) {
            createClassSection.setVisible(false);
            createClassSection.setManaged(false);
        }

        // --- FIX 2: Set Initial View to "All Classes" ---
        switchToAllClassesView();

        NavbarController.getInstance().setActive("classes");
        LOGGER.info("Classes view initialized");
    }

    // --- Navigation / Tab Logic ---

    @FXML
    private void switchToAllClassesView() {
        if (allClassesView != null) {
            allClassesView.setVisible(true);
            allClassesView.setManaged(true);
        }
        if (classDetailView != null) {
            classDetailView.setVisible(false);
            classDetailView.setManaged(false);
        }
        updateTabStyles(true);
    }

    @FXML
    private void switchToDetailView() {
        if (allClassesView != null) {
            allClassesView.setVisible(false);
            allClassesView.setManaged(false);
        }
        if (classDetailView != null) {
            classDetailView.setVisible(true);
            classDetailView.setManaged(true);
        }
        updateTabStyles(false);
    }

    private void updateTabStyles(boolean isAllClassesActive) {
        String activeStyle = "-fx-background-color: #3182ce; -fx-text-fill: white; -fx-border-color: #3182ce; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand;";
        String inactiveStyle = "-fx-background-color: white; -fx-text-fill: #718096; -fx-border-color: #e2e8f0; -fx-background-radius: 20; -fx-border-radius: 20; -fx-cursor: hand;";

        if (tabAllClassesBtn != null) {
            tabAllClassesBtn.setStyle(isAllClassesActive ? activeStyle : inactiveStyle);
        }
        if (tabDetailBtn != null) {
            tabDetailBtn.setStyle(isAllClassesActive ? inactiveStyle : activeStyle);
            // Disable detail button if no class is selected
            tabDetailBtn.setDisable(currentDetailClassId == -1 && !isAllClassesActive);
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void toggleCreateSection() {
        if (createClassSection == null) return;
        boolean visible = createClassSection.isVisible();
        createClassSection.setVisible(!visible);
        createClassSection.setManaged(!visible);
    }

    // --- Existing Logic (Adapted) ---

    private void setupRoleBasedUI() {
        boolean isHost = Session.isHost();
        if (createClassSection != null) {
            createClassSection.setVisible(isHost); // Respect role
            createClassSection.setManaged(isHost);
        }
        if (addClassButton != null) {
            addClassButton.setVisible(isHost);
            addClassButton.setManaged(isHost);
        }
        // ... other role checks ...
    }

    private void initializeClassDetailTab() {
        // Detail view starts hidden/managed=false, handled by switchToAllClassesView
        if (detailCancelEditButton != null) {
            detailCancelEditButton.setVisible(false);
            detailCancelEditButton.setManaged(false);
        }
        if (detailTasksListView != null) {
            detailTasksListView.setItems(detailClassTasks);
            detailTasksListView.setCellFactory(lv -> new DetailTaskListCell());
        }
        clearDetailForm();
    }

    // --- Loaders ---

    private void loadPublicClasses() {
        ObservableList<Classes> publicClasses = FXCollections.observableArrayList();
        Integer userId = getCurrentUserId();

        String sql = "SELECT id, class_name FROM classes WHERE is_public = 1";
        if (userId != null) {
            sql += " AND id NOT IN (SELECT class_id FROM user_classes WHERE user_id = ?)";
        }

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (userId != null) {
                stmt.setInt(1, userId);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    publicClasses.add(new Classes(rs.getInt("id"), rs.getString("class_name")));
                }
            }

            if (classList != null) {
                classList.getItems().clear();
                classList.setItems(publicClasses);
                classList.setCellFactory(lv -> new ClassListCell());
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load public classes: " + e.getMessage());
        }
    }

    private void loadOwnedClasses() {
        Integer userId = getCurrentUserId();
        if (userId == null) return;

        ObservableList<Classes> owned = FXCollections.observableArrayList();
        String sql = "SELECT c.id, c.class_name, c.owner_id FROM classes c JOIN user_classes uc ON c.id = uc.class_id WHERE uc.user_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    owned.add(new Classes(rs.getInt("id"), rs.getString("class_name"), rs.getInt("owner_id")));
                }
            }
            if (myClassList != null) {
                myClassList.getItems().clear();
                myClassList.setItems(owned);
                myClassList.setCellFactory(lv -> new OwnedClassListCell());
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load owned classes: " + e.getMessage());
        }
    }

    // --- Actions: Create/Join ---

    @FXML
    @SuppressWarnings("unused")
    private void createClass() {
        if (!Session.isHost()) {
            LOGGER.warning("Only Hosts can create classes");
            return;
        }

        String name = classNameField.getText();
        if (name == null || name.isBlank()) return;

        boolean isPublic = publicClassCheckbox.isSelected();
        String code = null;
        if (!isPublic) {
            String customCode = privateClassCodeField == null ? null : privateClassCodeField.getText();
            if (customCode != null) {
                customCode = customCode.trim().toUpperCase();
            }
            if (customCode != null && !customCode.isBlank()) {
                code = customCode;
            } else {
                code = generateJoinCode();
            }
        }
        Integer ownerId = getCurrentUserId();
        if (ownerId == null) {
            LOGGER.severe("Unable to determine class owner");
            return;
        }

        String sql = "INSERT INTO classes (class_name, is_public, join_code, owner_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setInt(2, isPublic ? 1 : 0);
            stmt.setString(3, code);
            stmt.setInt(4, ownerId);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newClassId = rs.getInt(1);
                    joinClass(newClassId);
                }
            }
            // Clear inputs and hide section
            classNameField.clear();
            publicClassCheckbox.setSelected(false);
            if (privateClassCodeField != null) privateClassCodeField.clear();
            toggleCreateSection(); // Hide after creation
            loadPublicClasses();
            loadOwnedClasses();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to create class: " + e.getMessage());
        }
    }

    private String generateJoinCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder(6);
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return code.toString();
    }

    private void joinClass(int classId) {
        Integer userId = getCurrentUserId();
        if (userId == null) return;

        String sql = "INSERT IGNORE INTO user_classes (user_id, class_id) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, classId);
            stmt.executeUpdate();
            assignPendingClassTasksToUser(classId, userId);
            loadPublicClasses();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to join class: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void joinPrivateClass() {
        String code = codeField.getText();
        if (code == null || code.isBlank()) return;

        code = code.trim().toUpperCase();
        String query = "SELECT id FROM classes WHERE UPPER(join_code) = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    joinClass(rs.getInt("id"));
                    codeField.clear();
                }
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to join private class: " + e.getMessage());
        }
    }

    // --- Actions: Class Management ---

    private void deleteClass(int classId) {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) return;

        String checkOwnerSql = "SELECT owner_id FROM classes WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkOwnerSql)) {
            checkStmt.setInt(1, classId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    Integer ownerId = rs.getInt("owner_id");
                    if (!currentUserId.equals(ownerId)) {
                        LOGGER.warning("User is not the owner of the class");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to verify class ownership: " + e.getMessage());
            return;
        }

        String deleteSql = "DELETE FROM classes WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, classId);
            stmt.executeUpdate();
            loadOwnedClasses();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to delete class: " + e.getMessage());
        }
    }

    private void leaveClass(int classId) {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) return;

        String leaveSql = "DELETE FROM user_classes WHERE user_id = ? AND class_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(leaveSql)) {
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, classId);
            stmt.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to leave class: " + e.getMessage());
        }

        String deleteTasksSql = "DELETE FROM tasks WHERE user_id = ? AND class_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteTasksSql)) {
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, classId);
            stmt.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to remove class tasks after leaving: " + e.getMessage());
        }

        loadPublicClasses();
        loadOwnedClasses();
    }

    // --- Detail View Logic ---

    private void showClassDetails(Classes selectedClass) {
        if (selectedClass == null) return;
        currentDetailClassId = selectedClass.getId();
        
        // Switch to the Detail View Tab
        switchToDetailView();
        
        loadDetailClassInfo(selectedClass.getClassName());
        loadDetailClassTasks();
        loadDetailParticipants();
        clearDetailForm();
    }

    @FXML
    @SuppressWarnings("unused")
    private void goBackToClasses() {
        switchToAllClassesView();
    }

    private void loadDetailClassInfo(String className) {
        if (detailClassTitleLabel != null) {
            detailClassTitleLabel.setText(className != null ? className : "Class Details");
        }
        if (detailClassDescriptionLabel != null) {
            detailClassDescriptionLabel.setText("Welcome to " + (className != null ? className : "this class") + ". Here you can manage tasks and participants.");
        }
    }

    private void loadDetailClassTasks() {
        detailClassTasks.clear();
        if (currentDetailClassId == -1) return;

        String sql = "SELECT id, class_id, task_name, description, due_date, owner_id, created_at FROM class_tasks WHERE class_id = ? ORDER BY due_date ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentDetailClassId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    detailClassTasks.add(new ClassTask(
                        rs.getInt("id"),
                        rs.getInt("class_id"),
                        rs.getString("task_name"),
                        rs.getString("description"),
                        rs.getDate("due_date") != null ? rs.getDate("due_date").toLocalDate() : null,
                        rs.getInt("owner_id"),
                        rs.getString("created_at")
                    ));
                }
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load class detail tasks: " + e.getMessage());
        }
    }

    private void loadDetailParticipants() {
        if (currentDetailClassId == -1 || detailParticipantsListView == null) return;
        detailParticipantsListView.getItems().clear();
        String sql = "SELECT u.username FROM users u "
                   + "JOIN user_classes uc ON u.id = uc.user_id "
                   + "WHERE uc.class_id = ? ORDER BY u.username";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentDetailClassId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    detailParticipantsListView.getItems().add(rs.getString("username"));
                }
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load class participants: " + e.getMessage());
        }
    }

    // --- Detail Task Actions ---

    @FXML
    @SuppressWarnings("unused")
    private void handleDetailSubmitTask() {
        if (currentDetailClassId == -1) return;
        String title = detailTaskTitleField.getText().trim();
        if (title.isEmpty()) return;

        String description = detailTaskDescriptionArea.getText().trim();
        LocalDate dueDate = detailTaskDueDatePicker.getValue();

        if (editingDetailTask != null) {
            updateDetailClassTask(editingDetailTask.getId(), title, description, dueDate);
        } else {
            createDetailClassTask(title, description, dueDate);
        }
        clearDetailForm();
        loadDetailClassTasks();
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleDetailCancelEdit() {
        clearDetailForm();
        editingDetailTask = null;
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleDetailEditTask() {
        ClassTask selected = detailTasksListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        editingDetailTask = selected;
        detailFormTitleLabel.setText("Edit Task");
        detailTaskTitleField.setText(selected.getTaskName());
        detailTaskDescriptionArea.setText(selected.getDescription());
        detailTaskDueDatePicker.setValue(selected.getDueDate());
        detailSubmitTaskButton.setText("Update Task");
        detailCancelEditButton.setVisible(true);
        detailCancelEditButton.setManaged(true);
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleDetailDeleteTask() {
        ClassTask selected = detailTasksListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        deleteDetailClassTask(selected.getId());
        loadDetailClassTasks();
    }

    private void createDetailClassTask(String title, String description, LocalDate dueDate) {
        Integer ownerId = getCurrentUserId();
        if (ownerId == null) return;
        String sql = "INSERT INTO class_tasks (class_id, task_name, description, due_date, owner_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, currentDetailClassId);
            stmt.setString(2, title);
            stmt.setString(3, description);
            stmt.setDate(4, dueDate != null ? Date.valueOf(dueDate) : null);
            stmt.setInt(5, ownerId);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int classTaskId = rs.getInt(1);
                    assignTaskToClassMembers(classTaskId);
                }
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to create class task: " + e.getMessage());
        }
    }

    private void updateDetailClassTask(int taskId, String title, String description, LocalDate dueDate) {
        String sql = "UPDATE class_tasks SET task_name = ?, description = ?, due_date = ? WHERE id = ? AND class_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setString(2, description);
            stmt.setDate(3, dueDate != null ? Date.valueOf(dueDate) : null);
            stmt.setInt(4, taskId);
            stmt.setInt(5, currentDetailClassId);
            stmt.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to update class task: " + e.getMessage());
        }
    }

    private void deleteDetailClassTask(int taskId) {
        String sql = "DELETE FROM class_tasks WHERE id = ? AND class_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            stmt.setInt(2, currentDetailClassId);
            stmt.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to delete class task: " + e.getMessage());
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

    private void clearDetailForm() {
        if (detailTaskTitleField != null) detailTaskTitleField.clear();
        if (detailTaskDescriptionArea != null) detailTaskDescriptionArea.clear();
        if (detailTaskDueDatePicker != null) detailTaskDueDatePicker.setValue(null);
        if (detailFormTitleLabel != null) detailFormTitleLabel.setText("Add New Task");
        if (detailSubmitTaskButton != null) detailSubmitTaskButton.setText("Add Task");
        if (detailCancelEditButton != null) {
            detailCancelEditButton.setVisible(false);
            detailCancelEditButton.setManaged(false);
        }
        editingDetailTask = null;
    }

    private void assignPendingClassTasksToUser(int classId, int userId) {
        String sql = "INSERT INTO tasks (username, user_id, task_name, task_date, status, class_id, class_task_id, created_by) "
                   + "SELECT u.username, u.id, ct.task_name, ct.due_date, 'Pending', ct.class_id, ct.id, ct.owner_id "
                   + "FROM class_tasks ct "
                   + "JOIN users u ON u.id = ? "
                   + "WHERE ct.class_id = ? "
                   + "AND ct.due_date >= CURDATE() "
                   + "AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.class_task_id = ct.id AND t.user_id = u.id)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, classId);
            stmt.executeUpdate();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to assign pending class tasks: " + e.getMessage());
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
            LOGGER.severe(() -> "Failed to get current user ID: " + e.getMessage());
        }
        return null;
    }

    // --- List Cells ---

    private class OwnedClassListCell extends ListCell<Classes> {
        private final Button viewButton = new Button("View");
        private final Button deleteButton = new Button("Delete");
        private final Button leaveButton = new Button("Leave");

        {
            viewButton.setStyle("-fx-padding: 6 12; -fx-cursor: hand; -fx-background-color: #38a169; -fx-text-fill: white; -fx-background-radius: 6;");
            viewButton.setOnAction(e -> {
                Classes c = getItem();
                if (c != null) showClassDetails(c);
            });

            deleteButton.setStyle("-fx-padding: 6 12; -fx-cursor: hand; -fx-background-color: #e53e3e; -fx-text-fill: white; -fx-background-radius: 6;");
            deleteButton.setOnAction(e -> {
                Classes c = getItem();
                if (c != null) deleteClass(c.getId());
            });

            leaveButton.setStyle("-fx-padding: 6 12; -fx-cursor: hand; -fx-background-color: #ed8936; -fx-text-fill: white; -fx-background-radius: 6;");
            leaveButton.setOnAction(e -> {
                Classes c = getItem();
                if (c != null) leaveClass(c.getId());
            });
        }

        @Override
        protected void updateItem(Classes item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                Integer currentUserId = getCurrentUserId();
                boolean isOwner = currentUserId != null && item.getOwnerId() != null && currentUserId.equals(item.getOwnerId());
                
                // Card Style for List Item
                VBox container = new VBox();
                container.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 4; -fx-border-radius: 8;");
                if(isOwner) container.setStyle(container.getStyle() + " -fx-border-color: #3182ce;");

                Label nameLabel = new Label(item.getClassName());
                nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: #2d3748;");

                HBox buttonBox = new HBox(8);
                buttonBox.setStyle("-fx-padding-top: 8;");
                
                if (isOwner) {
                    buttonBox.getChildren().addAll(viewButton, deleteButton);
                } else {
                    buttonBox.getChildren().add(leaveButton);
                }

                container.getChildren().addAll(nameLabel, buttonBox);
                setGraphic(container);
                setText(null);
            }
        }
    }

    private class ClassListCell extends ListCell<Classes> {
        private final Button joinButton = new Button("Join");

        {
            joinButton.setStyle("-fx-padding: 6 12; -fx-cursor: hand; -fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 6;");
            joinButton.setOnAction(e -> {
                Classes c = getItem();
                if (c != null) joinClass(c.getId());
            });
        }

        @Override
        protected void updateItem(Classes item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox container = new VBox();
                container.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 0 4; -fx-border-radius: 8; -fx-border-color: #38a169;");
                
                Label nameLabel = new Label(item.getClassName());
                nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: #2d3748;");
                
                HBox btnBox = new HBox(joinButton);
                btnBox.setStyle("-fx-padding-top: 8;");

                container.getChildren().addAll(nameLabel, btnBox);
                setGraphic(container);
                setText(null);
            }
        }
    }

    private class DetailTaskListCell extends ListCell<ClassTask> {
        @Override
        protected void updateItem(ClassTask task, boolean empty) {
            super.updateItem(task, empty);
            if (empty || task == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox container = new VBox();
                container.setStyle("-fx-padding: 12; -fx-background-color: #f7fafc; -fx-background-radius: 8; -fx-border-color: #edf2f7; -fx-border-radius: 8; -fx-border-width: 1;");
                
                Label titleLabel = new Label(task.getTaskName());
                titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: #2d3748;");
                
                Label metaLabel = new Label("Due: " + task.getDueDate());
                metaLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #718096;");
                
                if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                    Label descLabel = new Label(task.getDescription());
                    descLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #4a5568; -fx-wrap-text: true;");
                    container.getChildren().addAll(titleLabel, metaLabel, descLabel);
                } else {
                    container.getChildren().addAll(titleLabel, metaLabel);
                }

                setGraphic(container);
                setText(null);
            }
        }
    }
}