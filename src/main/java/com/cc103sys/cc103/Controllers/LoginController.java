package com.cc103sys.cc103.Controllers;

import com.cc103sys.cc103.App;
import com.cc103sys.cc103.DB.DBUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please fill all fields");
            return;
        }

        String sql = "SELECT * FROM users WHERE username=? AND password=?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                App.setRoot("Dashboard"); // load Dashboard.fxml
            } else {
                messageLabel.setText("Invalid login");
            }

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Database error");
        }
    }
}
