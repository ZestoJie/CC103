package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.Classes;
import com.cc103sys.cc103.Utils.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ClassesController {
    private static final Logger LOGGER = Logger.getLogger(ClassesController.class.getName());

    @FXML private ListView<Classes> classList;
    @FXML private TextField codeField;
    @FXML private TextField classNameField;

    @FXML@SuppressWarnings("unused")
    private TextField privateClassCodeField;

    @FXML private CheckBox publicClassCheckbox;

    @FXML
    public void initialize() {
        loadPublicClasses();
        loadOwnedClasses();

        // Set navbar active
        NavbarController.getInstance().setActive("classes");
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
        String sql = "SELECT c.id, c.class_name FROM classes c JOIN user_classes uc ON c.id = uc.class_id WHERE uc.user_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    owned.add(new Classes(rs.getInt("id"), rs.getString("class_name")));
                }
            }
            if (classList != null && !owned.isEmpty()) {
                classList.getItems().addAll(owned);
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load owned classes: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void createClass() {
        String name = classNameField.getText();
        if (name == null || name.isBlank()) return;

        boolean isPublic = publicClassCheckbox.isSelected();
        String code = isPublic ? null : generateJoinCode();

        String sql = "INSERT INTO classes (class_name, is_public, join_code) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setInt(2, isPublic ? 1 : 0);
            stmt.setString(3, code);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newClassId = rs.getInt(1);
                    joinClass(newClassId);
                }
            }
            classNameField.clear();
            publicClassCheckbox.setSelected(false);
            loadPublicClasses();
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

        String query = "SELECT id FROM classes WHERE join_code = ?";
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
