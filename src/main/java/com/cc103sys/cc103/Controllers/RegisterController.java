package com.cc103sys.cc103.Controllers;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Utils.Navigator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(RegisterController.class.getName());
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 4;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label messageLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObservableList<String> roles = FXCollections.observableArrayList("HOST", "PARTICIPANT");
        if (roleComboBox != null) {
            roleComboBox.setItems(roles);
            roleComboBox.setPromptText("Select Role");
            roleComboBox.setPrefWidth(250);
            roleComboBox.setMinWidth(250);
            roleComboBox.setMaxWidth(250);
        }
    }

    @SuppressWarnings({"StringConcatenationInFormatCall", "unused"})
    @FXML
    private void handleRegister(){
        try {
            String username = usernameField.getText();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField != null ? confirmPasswordField.getText() : null;
            String role = roleComboBox != null && roleComboBox.getValue() != null ? roleComboBox.getValue().trim() : "PARTICIPANT";

            if (!validateRegisterInput(username, password, confirmPassword, role)) {
                return;
            }

            if (registerUser(username, password, role)) {
                displaySuccess("Registration successful! Redirecting to login...");
                redirectToLogin();
            } else {
                displayError("Registration failed. Username may already exist.");
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Registration error: " + e);
            displayError("Registration failed. Please try again.");
        }
    }

    @SuppressWarnings("StringConcatenationInFormatCall")
    @FXML
    public void goLogin() {
        try {
            Navigator.switchScene("Login");
        } catch (Exception e) {
            LOGGER.severe(() -> "Navigation error: " + e);
            displayError("Failed to navigate to Login.");
        }
    }

    private boolean validateRegisterInput(String username, String password, String confirmPassword, String role) {
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
        if (confirmPassword == null || confirmPassword.isBlank()) {
            displayError("Confirm Password is required.");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            displayError("Passwords do not match.");
            return false;
        }
        if (role == null || role.isBlank()) {
            displayError("Please select a role.");
            return false;
        }
        return true;
    }

    @SuppressWarnings("StringConcatenationInFormatCall")
    private boolean registerUser(String username, String password, String role) {
        String sql = "INSERT INTO users(username, password, role) VALUES (?, ?, ?)";

        try(Connection conn = DBUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role != null ? role : "PARTICIPANT");
            stmt.executeUpdate();
            
            LOGGER.info(() -> "User registered: " + username + " as " + role);
            return true;
        } catch (Exception e) {
            LOGGER.severe(() -> "Database error during registration: " + e);
            return false;
        }
    }

    @SuppressWarnings("StringConcatenationInFormatCall")
    private void redirectToLogin() {
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                javafx.application.Platform.runLater(() -> {
                    try {
                        Navigator.switchScene("Login");
                    } catch (Exception e) {
                        LOGGER.severe(() -> "Navigation error: " + e);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private void displayError(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #d32f2f;");
    }

    private void displaySuccess(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: #4caf50;");
    }
}

