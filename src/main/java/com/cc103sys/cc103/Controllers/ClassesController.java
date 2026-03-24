package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.Classes;
import com.cc103sys.cc103.Utils.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * Controller for Classes scene.
 * Manages class enrollment and viewing available classes.
 */
public class ClassesController {
    private static final Logger LOGGER = Logger.getLogger(ClassesController.class.getName());

    @FXML private ListView<Classes> classList;
    @FXML private TextField codeField;

    /**
     * Initialize classes controller.
     */
    @FXML
    public void initialize() {
        try {
            loadPublicClasses();
            LOGGER.info("Classes controller initialized");
        } catch (Exception e) {
            LOGGER.severe("Classes initialization error: " + e.getMessage());
        }
    }

    /**
     * Load public classes not yet enrolled in.
     */
    private void loadPublicClasses() {
        ObservableList<Classes> publicClasses = FXCollections.observableArrayList();
        String sql = "SELECT id, class_name FROM classes WHERE is_public = 1 AND id NOT IN (SELECT class_id FROM users WHERE username = ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, Session.getUsername());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    publicClasses.add(new Classes(rs.getInt("id"), rs.getString("class_name")));
                }
            }

            if (classList != null) {
                classList.setItems(publicClasses);
                classList.setCellFactory(lv -> new ClassListCell());
            }
            LOGGER.info("Loaded " + publicClasses.size() + " public classes");
        } catch (Exception e) {
            LOGGER.severe("Failed to load public classes: " + e.getMessage());
        }
    }

    /**
     * Join class by ID and reload class list.
     */
    private void joinClass(int classId) {
        String sql = "UPDATE users SET class_id = ? WHERE username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, classId);
            stmt.setString(2, Session.getUsername());
            stmt.executeUpdate();

            loadPublicClasses();
            LOGGER.info("User joined class ID: " + classId);
        } catch (Exception e) {
            LOGGER.severe("Failed to join class: " + e.getMessage());
        }
    }

    /**
     * Join private class by code.
     */
    @FXML
    private void joinPrivateClass() {
        try {
            String code = codeField.getText();
            if (code == null || code.isBlank()) {
                LOGGER.warning("Class code is empty");
                return;
            }

            String getClassSql = "SELECT id FROM classes WHERE join_code = ?";
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(getClassSql)) {

                stmt.setString(1, code);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int classId = rs.getInt("id");
                        joinClass(classId);
                        codeField.clear();
                        LOGGER.info("Joined private class with code: " + code);
                    } else {
                        LOGGER.warning("Invalid class code: " + code);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to join private class: " + e.getMessage());
        }
    }

    /**
     * Custom ListCell for displaying classes with join button.
     */
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
