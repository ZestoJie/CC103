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
        LOGGER.info("Classes view initialized");
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
        if (name == null || name.isBlank()) return;

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
                    joinClass(newClassId);
                }
            }
            classNameField.clear();
            publicClassToggle.setSelected(true);
            generatedCodeLabel.setText("Class Code: " + code);
            generatedCodeLabel.setVisible(true);
            copyCodeButton.setVisible(true);
            loadPublicClasses();
            loadOwnedClasses();
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to create class: " + e.getMessage());
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

    private void deleteClass(int classId) {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) return;

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

        // Delete the class (cascade will handle related records)
        String deleteSql = "DELETE FROM classes WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, classId);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info(() -> "Class deleted successfully: " + classId);
                loadOwnedClasses(); // Refresh the list
            } else {
                LOGGER.warning("No class was deleted");
            }
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
                    Button viewBtn = new Button("View");
                    Button deleteBtn = new Button("Delete");
                    
                    viewBtn.setStyle("-fx-padding: 8 16 8 16; -fx-cursor: hand; -fx-background-color: #4caf50; -fx-text-fill: white;");
                    deleteBtn.setStyle("-fx-padding: 8 16 8 16; -fx-cursor: hand; -fx-background-color: #f44336; -fx-text-fill: white;");
                    
                    // Use final reference for lambda
                    final Classes classItem = item;
                    viewBtn.setOnAction(e -> {
                        Session.setCurrentClassId(classItem.getId());
                        Navigator.navigateTo("ClassDetail");
                    });
                    deleteBtn.setOnAction(e -> deleteClass(classItem.getId()));
                    
                    buttonBox.getChildren().addAll(viewBtn, deleteBtn);
                } else {
                    Button leaveBtn = new Button("Leave");
                    leaveBtn.setStyle("-fx-padding: 8 16 8 16; -fx-cursor: hand; -fx-background-color: #ff9800; -fx-text-fill: white;");
                    
                    final Classes classItem = item;
                    leaveBtn.setOnAction(e -> leaveClass(classItem.getId()));
                    
                    buttonBox.getChildren().add(leaveBtn);
                }

                setGraphic(buttonBox);
            }
        }
    }

    private class ClassListCell extends ListCell<Classes> {
        private final Button joinButton = new Button("Join");

        {
            joinButton.setStyle("-fx-padding: 8 16 8 16; -fx-cursor: hand;");
            joinButton.setOnAction(e -> {
                Classes c = getItem();
                if (c != null) {
                    joinClass(c.getId());
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
