package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.Classes;
import com.cc103sys.cc103.Models.UserRank;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class LeaderboardController {

    private static final Logger LOGGER = Logger.getLogger(LeaderboardController.class.getName());

    @FXML private ComboBox<Classes> classComboBox;
    @FXML private TableView<UserRank> table;
    @FXML private TableColumn<UserRank, String> usernameCol;
    @FXML private TableColumn<UserRank, Integer> pointsCol;

    @FXML
    public void initialize() {
        try {
            setupTableColumns();
            loadUserClasses();

            if (classComboBox != null) {
                classComboBox.setOnAction(e -> {
                    try {
                        loadLeaderboard();
                    } catch (Exception e1) {
                    }
                });
            }

            // Set navbar active
            NavbarController.getInstance().setActive("leaderboard");

            LOGGER.info("Leaderboard initialized");
        } catch (Exception e) {
            LOGGER.severe(() -> "Initialization error: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        if (usernameCol != null) {
            usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        }
        if (pointsCol != null) {
            pointsCol.setCellValueFactory(new PropertyValueFactory<>("points"));
        }
    }

    private void loadUserClasses() {
        ObservableList<Classes> userClasses = FXCollections.observableArrayList();

        String sql = "SELECT c.id, c.class_name FROM classes c "
                   + "JOIN user_classes uc ON c.id = uc.class_id "
                   + "JOIN users u ON uc.user_id = u.id "
                   + "WHERE u.username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, Session.getUsername());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userClasses.add(new Classes(
                            rs.getInt("id"),
                            rs.getString("class_name")
                    ));
                }
            }

            if (classComboBox != null) {
                classComboBox.setItems(userClasses);

                if (!userClasses.isEmpty()) {
                    classComboBox.getSelectionModel().selectFirst();
                    loadLeaderboard();
                }
            }

            LOGGER.info(() -> "Loaded " + userClasses.size() + " classes");

        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to load classes: " + e.getMessage());
        }
    }

    @FXML
    private void loadLeaderboard() throws Exception {
        try {
            Classes selectedClass = classComboBox.getValue();

            if (selectedClass == null) {
                LOGGER.warning("No class selected");
                return;
            }

            ObservableList<UserRank> data = FXCollections.observableArrayList();

            String sql = "SELECT u.username, u.points FROM users u "
                       + "JOIN user_classes uc ON u.id = uc.user_id "
                       + "WHERE uc.class_id = ? "
                       + "ORDER BY u.points DESC";

            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, selectedClass.getId());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        data.add(new UserRank(
                                rs.getString("username"),
                                rs.getInt("points")
                        ));
                    }
                }

                if (table != null) {
                    table.setItems(data);
                    table.setVisible(true);
                }

                LOGGER.info(() -> "Leaderboard loaded: " + data.size() + " users");

            }

        } catch (SQLException e) {
            LOGGER.severe(() -> "Failed to load leaderboard: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void goBack() {
        try {
            Navigator.switchScene("Dashboard");
        } catch (Exception e) {
            LOGGER.severe(() -> "Navigation error: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void goSettings() {
        try {
            Navigator.switchScene("Settings");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to navigate to Settings: " + e.getMessage());
        }
    }
}