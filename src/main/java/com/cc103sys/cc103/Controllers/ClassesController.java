package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.Classes;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;
import com.cc103sys.cc103.Utils.UiDialogs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public class ClassesController {
    private static final Logger LOGGER = Logger.getLogger(ClassesController.class.getName());

    @FXML private ListView<Classes> classList;
    @FXML private ListView<Classes> myClassList;
    @FXML private TextField codeField;
    @FXML private TextField classNameField;
    @FXML private VBox createClassSection;
    @FXML private Button addClassButton;

    @FXML@SuppressWarnings("unused")
    private TextField privateClassCodeField;

    @FXML private ToggleButton publicClassToggle;
    @FXML private Label generatedCodeLabel;
    @FXML private Button copyCodeButton;

    private Window window() {
        return classList != null && classList.getScene() != null
            ? classList.getScene().getWindow()
            : null;
    }

    @FXML
    public void initialize() {
        setupRoleBasedUI();
        if (publicClassToggle != null) {
            publicClassToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
                publicClassToggle.setText(newVal ? "Public" : "Private");
            });
            publicClassToggle.setSelected(true); // default to public
        }
        loadPublicClasses();
        loadOwnedClasses();

        NavbarController.getInstance().setActive("classes");
        setupListPlaceholders();
        LOGGER.info("Classes view initialized");
    }

    private void setupListPlaceholders() {
        if (classList != null) {
            Label empty = new Label("You're enrolled in every public class available right now,\nor there are no public classes yet.");
            empty.getStyleClass().add("empty-state");
            empty.setWrapText(true);
            classList.setPlaceholder(empty);
        }
        if (myClassList != null) {
            Label empty = new Label("No classes yet.\nCreate one as a host or join with a code.");
            empty.getStyleClass().add("empty-state");
            empty.setWrapText(true);
            myClassList.setPlaceholder(empty);
        }
    }

    private void setupRoleBasedUI() {
        boolean isHost = Session.isHost();
        if (createClassSection != null) {
            createClassSection.setVisible(isHost);
            createClassSection.setManaged(isHost);
        }
        if (addClassButton != null) {
            addClassButton.setVisible(isHost);
            addClassButton.setManaged(isHost);
        }
        if (classNameField != null) classNameField.setVisible(isHost);
        if (publicClassToggle != null) publicClassToggle.setVisible(isHost);
        if (privateClassCodeField != null) privateClassCodeField.setVisible(isHost);
    }

    @FXML
    @SuppressWarnings("unused")
    private void toggleCreateSection() {
        if (createClassSection == null) return;
        boolean visible = createClassSection.isVisible();
        createClassSection.setVisible(!visible);
        createClassSection.setManaged(!visible);
    }

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

    @FXML
    @SuppressWarnings("unused")
    private void createClass() {
        if (!com.cc103sys.cc103.Utils.Session.isHost()) {
            LOGGER.warning("Only Hosts can create classes");
            return;
        }

        String name = classNameField.getText();
        if (name == null || name.isBlank()) {
            UiDialogs.warn(window(), "Missing class name", "Enter a name for your class.");
            return;
        }

        boolean isPublic = publicClassToggle.isSelected();
        String code = generateJoinCode();
        Integer ownerId = getCurrentUserId();
        String ownerUsername = Session.getUsername();
        if (ownerId == null) {
            LOGGER.severe("Unable to determine class owner");
            return;
        }

        String sql = "INSERT INTO classes (class_name, president_username, is_public, join_code, owner_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, ownerUsername);
            stmt.setInt(3, isPublic ? 1 : 0);
            stmt.setString(4, code);
            stmt.setInt(5, ownerId);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newClassId = rs.getInt(1);
                    joinClass(newClassId, false);
                }
            }
            classNameField.clear();
            publicClassToggle.setSelected(true);
            generatedCodeLabel.setText("Class Code: " + code);
            generatedCodeLabel.setVisible(true);
            copyCodeButton.setVisible(true);
            loadPublicClasses();
            loadOwnedClasses();
            UiDialogs.info(window(), "Class created", "Your class is ready. Share the join code with participants.");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to create class: " + e.getMessage());
            UiDialogs.error(window(), "Could not create class", e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void copyCode() {
        String code = generatedCodeLabel.getText().replace("Class Code: ", "");
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(code);
        clipboard.setContent(content);
        LOGGER.info("Copied class code to clipboard: " + code);
        UiDialogs.info(window(), "Copied", "Join code copied to clipboard.");
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

    private void joinClass(int classId, boolean showSuccessDialog) {
        Integer userId = getCurrentUserId();
        if (userId == null) {
            return;
        }

        String sql = "INSERT IGNORE INTO user_classes (user_id, class_id) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, classId);
            stmt.executeUpdate();
            assignPendingClassTasksToUser(classId, userId);
            loadPublicClasses();
            loadOwnedClasses();
            if (showSuccessDialog) {
                UiDialogs.info(window(), "Joined class", "You have joined the class.");
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to join class: " + e.getMessage());
            UiDialogs.error(window(), "Join failed", e.getMessage());
        }
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

    @FXML
    @SuppressWarnings("unused")
    private void joinPrivateClass() {
        String code = codeField.getText();
        if (code == null || code.isBlank()) {
            UiDialogs.warn(window(), "Missing code", "Enter the class join code.");
            return;
        }

        code = code.trim().toUpperCase();
        String query = "SELECT id FROM classes WHERE UPPER(join_code) = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    joinClass(rs.getInt("id"), true);
                    codeField.clear();
                } else {
                    UiDialogs.warn(window(), "Code not found", "No class matches that code. Check with your instructor and try again.");
                }
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to join private class: " + e.getMessage());
            UiDialogs.error(window(), "Join failed", e.getMessage());
        }
    }

    private void deleteClass(int classId) {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return;
        }

        // Verify the current user is the owner
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
                } else {
                    LOGGER.warning("Class not found");
                    return;
                }
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to verify class ownership: " + e.getMessage());
            return;
        }

        if (!UiDialogs.confirm(window(), "Delete this class?",
            "This removes the class and related data for everyone. This cannot be undone.")) {
            return;
        }

        // Delete the class (cascade will handle related records)
        String deleteSql = "DELETE FROM classes WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, classId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info(() -> "Class deleted successfully: " + classId);
                loadOwnedClasses();
                loadPublicClasses();
                UiDialogs.info(window(), "Class deleted", "The class has been removed.");
            } else {
                LOGGER.warning("No class was deleted");
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to delete class: " + e.getMessage());
        }
    }

    private void leaveClass(int classId) {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            return;
        }
        if (!UiDialogs.confirm(window(), "Leave this class?",
            "You will lose access to class tasks until you join again.")) {
            return;
        }

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
        UiDialogs.info(window(), "Left class", "You are no longer enrolled in that class.");
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
            LOGGER.severe(() -> "Failed to get current user ID: " + e.getMessage());
        }
        return null;
    }

    private class OwnedClassListCell extends ListCell<Classes> {
        @Override
        protected void updateItem(Classes item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                Integer currentUserId = getCurrentUserId();
                boolean isOwner = currentUserId != null && item.getOwnerId() != null && currentUserId.equals(item.getOwnerId());

                setText(item.getClassName());
                javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(5);
                
                if (isOwner) {
                    // Create fresh button instances for each cell
                    Button viewBtn = new Button("Open Class");
                    Button deleteBtn = new Button("Delete");
                    
                    viewBtn.getStyleClass().addAll("button", "button-success");
                    deleteBtn.getStyleClass().addAll("button", "button-danger");
                    
                    // Use final reference for lambda
                    final Classes classItem = item;
                    viewBtn.setOnAction(e -> {
                        Session.setCurrentClassId(classItem.getId());
                        Navigator.navigateTo("ClassDetail");
                    });
                    deleteBtn.setOnAction(e -> deleteClass(classItem.getId()));
                    
                    buttonBox.getChildren().addAll(viewBtn, deleteBtn);
                } else {
                    Button leaveBtn = new Button("Leave Class");
                    leaveBtn.getStyleClass().addAll("button", "button-secondary");
                    
                    final Classes classItem = item;
                    leaveBtn.setOnAction(e -> leaveClass(classItem.getId()));
                    
                    buttonBox.getChildren().add(leaveBtn);
                }

                setGraphic(buttonBox);
            }
        }
    }

    private class ClassListCell extends ListCell<Classes> {
        private final Button joinButton = new Button("Join Class");

        {
            joinButton.getStyleClass().addAll("button", "button-success");
            joinButton.setOnAction(e -> {
                Classes c = getItem();
                if (c != null) {
                    joinClass(c.getId(), true);
                }
            });
        }

        @Override
        protected void updateItem(Classes item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.getClassName());
                setGraphic(joinButton);
            }
        }
    }
}
