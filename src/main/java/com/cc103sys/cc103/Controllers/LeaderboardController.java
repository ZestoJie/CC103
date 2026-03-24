package com.cc103sys.cc103.Controllers;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.Classes;
import com.cc103sys.cc103.Models.UserRank;
import com.cc103sys.cc103.Utils.Session;
import com.cc103sys.cc103.Utils.Navigator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * Controller for Leaderboard scene.
 * Displays class rankings and user points.
 */
public class LeaderboardController {
    private static final Logger LOGGER = Logger.getLogger(LeaderboardController.class.getName());

    @FXML private ComboBox<Classes> classComboBox;
    @FXML private TableView<UserRank> table;
    @FXML private TableColumn<UserRank, String> usernameCol;
    @FXML private TableColumn<UserRank, Integer> pointsCol;

    /**
     * Initialize leaderboard controller.
     */
    @FXML
    public void initialize() {
        try {
            setupTableColumns();
            loadUserClasses();
            if (classComboBox != null) {
                classComboBox.setOnAction(e -> loadLeaderboard());
            }
            LOGGER.info("Leaderboard controller initialized");
        } catch (Exception e) {
            LOGGER.severe("Leaderboard initialization error: " + e.getMessage());
        }
    }

    /**
     * Setup table columns with property value factories.
     */
    private void setupTableColumns() {
        if (usernameCol != null) {
            usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        }
        if (pointsCol != null) {
            pointsCol.setCellValueFactory(new PropertyValueFactory<>("points"));
        }
    }

    /**
     * Load user classes for selection.
     */
    private void loadUserClasses() {
        ObservableList<Classes> userClasses = FXCollections.observableArrayList();
        String sql = "SELECT DISTINCT c.id, c.class_name FROM classes c WHERE c.id IN (SELECT class_id FROM users WHERE username = ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, Session.getUsername());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userClasses.add(new Classes(rs.getInt("id"), rs.getString("class_name")));
                }
            }

            if (classComboBox != null) {
                classComboBox.setItems(userClasses);
                if (!userClasses.isEmpty()) {
                    classComboBox.getSelectionModel().selectFirst();
                    loadLeaderboard();
                }
            }
            LOGGER.info("Loaded " + userClasses.size() + " user classes");
        } catch (Exception e) {
            LOGGER.severe("Failed to load user classes: " + e.getMessage());
        }
    }

    /**
     * Load leaderboard data for selected class.
     */
    @FXML
    private void loadLeaderboard() {
        try {
            Classes selectedClass = classComboBox.getValue();
            if (selectedClass == null) {
                LOGGER.warning("No class selected");
                return;
            }

            ObservableList<UserRank> data = FXCollections.observableArrayList();
            String sql = "SELECT username, points FROM users WHERE class_id = ? ORDER BY points DESC";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, selectedClass.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        data.add(new UserRank(rs.getString("username"), rs.getInt("points")));
                    }
                }

                if (table != null) {
                    table.setItems(data);
                    table.setVisible(true);
                }
                LOGGER.info("Loaded " + data.size() + " users for leaderboard");
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to load leaderboard: " + e.getMessage());
        }
    }

    /**
     * Navigate back to Dashboard.
     */
    @FXML
    private void goBack() {
        try {
            Navigator.switchScene("Dashboard");
        } catch (Exception e) {
            LOGGER.severe("Navigation error: " + e.getMessage());
        }
    }
}
