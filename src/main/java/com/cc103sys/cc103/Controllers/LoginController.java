package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for Login scene.
 * Handles user authentication and navigation to Dashboard or Register.
 */
public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    /**
     * Handle login button action.
     * Validates input, authenticates user, and switches scene on success.
     */
    @FXML
    private void handleLogin() {
        try {
            String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
            String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

            // Validate input
            if (!validateLoginInput(username, password)) {
                displayError("Username and password are required.");
                return;
            }

            // Authenticate user
            boolean authenticated = authenticateUser(username, password);
            if (authenticated) {
                Session.setUsername(username);
                Navigator.switchScene("Dashboard");
                LOGGER.info("User logged in: " + username);
            } else {
                displayError("Invalid username or password. Check credentials and try again.");
                LOGGER.warning("Authentication failed for username: " + username);
            }
        } catch (Exception e) {
            LOGGER.severe("Login error: " + e.getMessage());
            displayError("Login failed due to technical issue. Contact admin.");
        }
    }

    /**
     * Handle register button action.
     * Navigates to Register scene.
     */
    @FXML
    private void goRegister() {
        try {
            Navigator.switchScene("Register");
        } catch (Exception e) {
            LOGGER.severe("Navigation error: " + e.getMessage());
            displayError("Failed to navigate to Register.");
        }
    }

    /**
     * Validate login input fields.
     */
    private boolean validateLoginInput(String username, String password) {
        return username != null && !username.isBlank() 
            && password != null && !password.isBlank();
    }

    /**
     * Authenticate user against database.
     */
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

    /**
     * Display error message to user.
     */
    private void displayError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #d32f2f;");
    }
}

