package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        try {
            String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
            String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

            if (!validateLoginInput(username, password)) {
                return;
            }

            boolean authenticated = authenticateUser(username, password);
            if (authenticated) {
                Session.setUsername(username);

               

                Navigator.switchScene("Dashboard");
                LOGGER.info("User logged in: " + username);
            } else {
                
            }
        } catch (Exception e) {
            LOGGER.severe("Login error: " + e.getMessage());
           
        }
    }

    @FXML
    private void goRegister() {
        try {
            Navigator.switchScene("Register");
        } catch (Exception e) {
            LOGGER.severe("Navigation error: " + e.getMessage());
        }
    }

    private boolean validateLoginInput(String username, String password) {
        return username != null && !username.isBlank() 
            && password != null && !password.isBlank();
    }

    private boolean authenticateUser(String username, String password) {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            LOGGER.severe("Database error during authentication: " + e.getMessage());
            return false;
        }
    }
}

