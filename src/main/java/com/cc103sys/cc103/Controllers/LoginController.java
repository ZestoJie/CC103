package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Utils.CredentialsManager;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMe;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        // Clear error message when user starts typing
        if (usernameField != null) {
            usernameField.textProperty().addListener((obs, oldText, newText) -> clearError());
        }
        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, oldText, newText) -> clearError());
        }

        // Load saved credentials if available
        loadSavedCredentials();
    }

    private void loadSavedCredentials() {
        try {
            String[] credentials = CredentialsManager.loadCredentials();
            if (credentials != null && credentials.length == 2) {
                usernameField.setText(credentials[0]);
                passwordField.setText(credentials[1]);
                if (rememberMe != null) {
                    rememberMe.setSelected(true);
                }
                LOGGER.info("Saved credentials loaded");
            }
        } catch (Exception e) {
            LOGGER.warning(() -> "Could not load saved credentials: " + e.getMessage());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void handleLogin() {
        try {
            String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
            String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

            if (!validateLoginInput(username, password)) {
                return;
            }

            boolean authenticated = authenticateUser(username, password);
            if (authenticated) {
                // Handle Remember Me
                boolean rememberEnabled = rememberMe != null && rememberMe.isSelected();
                if (rememberEnabled) {
                    CredentialsManager.saveCredentials(username, password);
                } else {
                    CredentialsManager.clearCredentials();
                }

                Session.setCurrentClassId(-1);
                Navigator.switchScene("Dashboard");
                LOGGER.info(() -> "User logged in: " + username);
            } else {
                showError("Invalid username or password");
                // Clear saved credentials on failed login
                CredentialsManager.clearCredentials();
                LOGGER.warning(() -> "Failed login attempt for user: " + username);
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Login error: " + e.getMessage());
            showError("Login failed. Please try again.");
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void goRegister() {
        try {
            Navigator.switchScene("Register");
        } catch (Exception e) {
            LOGGER.severe(() -> "Navigation error: " + e.getMessage());
        }
    }

    private boolean validateLoginInput(String username, String password) {
        return username != null && !username.isBlank() 
            && password != null && !password.isBlank();
    }

    private boolean authenticateUser(String username, String password) {
        String sql = "SELECT id, username, role, points FROM users WHERE LOWER(username) = LOWER(?) AND password = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String dbUsername = rs.getString("username");
                    String role = rs.getString("role");
                    int points = rs.getInt("points");
                    Session.setUsername(dbUsername);
                    Session.setUserRole(role);
                    Session.setPoints(points);
                    return true;
                }
                return false;
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Database error during authentication: " + e.getMessage());
            return false;
        }
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    private void clearError() {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
}

