package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Utils.Navigator;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleRegister() {

        String sql = "INSERT INTO users(username, password) VALUES (?,?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usernameField.getText());
            stmt.setString(2, passwordField.getText());

            stmt.executeUpdate();
            Navigator.switchScene("Login");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goLogin() {
        Navigator.switchScene("Login");
    }
}