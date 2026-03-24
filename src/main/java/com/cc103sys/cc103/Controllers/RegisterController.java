package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Utils.Navigator;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for Register scene.
 * Handles user registration and navigation back to Login.
 */
public class RegisterController {
    private static final Logger LOGGER = Logger.getLogger(RegisterController.class.getName());
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 4;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    /**
     * Handle register button action.
     * Validates input and creates new user account.
     */
    @SuppressWarnings("StringConcatenationInFormatCall")
    @FXML
    public void handleRegister() {
        try {
            String username = usernameField.getText();
            String password = passwordField.getText();

            // Validate input
            if (!validateRegisterInput(username, password)) {
                return;
            }

            // Register user
            if (registerUser(username, password)) {
                displaySuccess("Registration successful! Redirecting to login...");
                redirectToLogin();
            } else {
                displayError("Registration failed. Username may already exist.");
            }
        } catch (Exception e) {
            LOGGER.severe("Registration error: " + e);
            displayError("Registration failed. Please try again.");
        }
    }

    /**
     * Handle back to login button action.
     */
    @SuppressWarnings("StringConcatenationInFormatCall")
    @FXML
    public void goLogin() {
        try {
            Navigator.switchScene("Login");
        } catch (Exception e) {
            LOGGER.severe("Navigation error: " + e);
            displayError("Failed to navigate to Login.");
        }
    }

    /**
     * Validate registration input with specific requirements.
     */
    private boolean validateRegisterInput(String username, String password) {
        if (username == null || username.isBlank()) {
            displayError("Username is required.");
            return false;
        }
        if (username.length() < MIN_USERNAME_LENGTH) {
            displayError("Username must be at least " + MIN_USERNAME_LENGTH + " characters.");
            return false;
        }
        if (password == null || password.isBlank()) {
            displayError("Password is required.");
            return false;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            displayError("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
            return false;
        }
        return true;
    }

    /**
     * Register user in database.
     */
    @SuppressWarnings("StringConcatenationInFormatCall")
    private boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES (?, ?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            
            LOGGER.info("User registered: " + username);
            return true;
        } catch (Exception e) {
            LOGGER.severe("Database error during registration: " + e);
            return false;
        }
    }

    /**
     * Redirect to login after small delay.
     */
    @SuppressWarnings("StringConcatenationInFormatCall")
    private void redirectToLogin() {
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                javafx.application.Platform.runLater(() -> {
                    try {
                        Navigator.switchScene("Login");
                    } catch (Exception e) {
                        LOGGER.severe("Navigation error: " + e);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Display error message to user.
     */
    private void displayError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #d32f2f;");
    }

    /**
     * Display success message to user.
     */
    private void displaySuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #4caf50;");
    }
}

