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

public class LeaderboardController {

    @FXML private ComboBox<Classes> classComboBox;
    @FXML private TableView<UserRank> table;
    @FXML private TableColumn<UserRank, String> usernameCol;
    @FXML private TableColumn<UserRank, Integer> pointsCol;

    @FXML
    public void initialize() {
        // Setup TableView columns
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        pointsCol.setCellValueFactory(new PropertyValueFactory<>("points"));

        loadUserClasses();

        // Auto-load leaderboard when a class is selected
        classComboBox.setOnAction(e -> loadLeaderboard());
    }

    private void loadUserClasses() {
        ObservableList<Classes> userClasses = FXCollections.observableArrayList();

        String sql = """
            SELECT c.id, c.class_name
            FROM classes c
            JOIN users u ON c.id = u.class_id
            WHERE u.username = ?
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, Session.getUsername());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                userClasses.add(new Classes(
                        rs.getInt("id"),
                        rs.getString("class_name")
                ));
            }

            classComboBox.setItems(userClasses);

            // Optional: select the first class automatically
            if (!userClasses.isEmpty()) {
                classComboBox.getSelectionModel().selectFirst();
                loadLeaderboard();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void loadLeaderboard() {
        Classes selectedClass = classComboBox.getValue();
        if (selectedClass == null) return;

        ObservableList<UserRank> data = FXCollections.observableArrayList();

        String sql = """
            SELECT u.username, u.points
            FROM users u
            WHERE u.class_id = ?
            ORDER BY u.points DESC
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, selectedClass.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                data.add(new UserRank(
                        rs.getString("username"),
                        rs.getInt("points")
                ));
            }

            table.setItems(data);
            table.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        Navigator.switchScene("Dashboard");
    }
}