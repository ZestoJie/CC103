package com.cc103sys.cc103.Controllers;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.UserRank;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LeaderboardController {

    @FXML private TableView<UserRank> table;
    @FXML private TableColumn<UserRank, String> usernameCol;
    @FXML private TableColumn<UserRank, Integer> pointsCol;

    @FXML
    public void initialize() {

        usernameCol.setCellValueFactory(
                new PropertyValueFactory<>("username")
        );
        pointsCol.setCellValueFactory(
                new PropertyValueFactory<>("points")
        );

        ObservableList<UserRank> data = FXCollections.observableArrayList();

        String sql = """
            SELECT username, points
            FROM users
            WHERE class_section = (
                SELECT class_section FROM users WHERE username = ?
            )
            ORDER BY points DESC
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, Session.getUsername());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                data.add(new UserRank(
                        rs.getString("username"),
                        rs.getInt("points")
                ));
            }

            table.setItems(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        Navigator.switchScene("Dashboard");
    }
}
