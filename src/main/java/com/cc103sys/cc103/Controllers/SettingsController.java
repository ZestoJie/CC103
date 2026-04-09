package com.cc103sys.cc103.Controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class SettingsController {
    private static final Logger LOGGER = Logger.getLogger(SettingsController.class.getName());
    private static final String PROFILE_PICTURES_DIR = "profile_pictures";

    @FXML private ImageView profilePictureImageView;
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox enableMusicToggle;
    @FXML private CheckBox enableSFXToggle;
    @FXML private Button editFullNameButton;
    @FXML private Button editUsernameButton;
    @FXML private Button editEmailButton;
    @FXML private Button changePasswordButton;
    @FXML private Button uploadPictureButton;

    private Integer currentUserId;
    private String currentUsername;
    private boolean isEditingFullName = false;
    private boolean isEditingUsername = false;
    private boolean isEditingEmail = false;

    @FXML
    public void initialize() {
        try {
            // Create profile pictures directory if it doesn't exist
            Files.createDirectories(Paths.get(PROFILE_PICTURES_DIR));

            currentUsername = Session.getUsername();
            currentUserId = getCurrentUserId();

            loadUserSettings();
            setupFieldStates();
            
            NavbarController.getInstance().setActive("settings");
            LOGGER.info("Settings initialized successfully");
        } catch (Exception e) {
            LOGGER.severe("Error initializing settings: " + e.getMessage());
            showAlert("Error", "Failed to initialize settings", e.getMessage());
        }
    }

    private void loadUserSettings() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT full_name, email, profile_picture_path FROM users WHERE id = ?")) {
            
            stmt.setInt(1, currentUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String fullName = rs.getString("full_name");
                    String email = rs.getString("email");
                    String profilePicturePath = rs.getString("profile_picture_path");

                    if (fullNameField != null) {
                        fullNameField.setText(fullName != null ? fullName : "");
                    }
                    if (usernameField != null) {
                        usernameField.setText(currentUsername);
                    }
                    if (emailField != null) {
                        emailField.setText(email != null ? email : "");
                    }
                    if (passwordField != null) {
                        passwordField.setText("••••••••");
                    }

                    // Load profile picture
                    if (profilePicturePath != null && !profilePicturePath.isEmpty()) {
                        loadProfilePicture(profilePicturePath);
                    } else {
                        loadDefaultProfilePicture();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Error loading user settings: " + e.getMessage());
        }
    }

    private void loadProfilePicture(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                if (profilePictureImageView != null) {
                    profilePictureImageView.setImage(image);
                    profilePictureImageView.setStyle("-fx-border-radius: 50; -fx-background-radius: 50; -fx-border-color: #ddd; -fx-border-width: 2;");
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Error loading profile picture: " + e.getMessage());
            loadDefaultProfilePicture();
        }
    }

    private void loadDefaultProfilePicture() {
        try {
            if (profilePictureImageView != null) {
                Image defaultImage = new Image(getClass().getResourceAsStream("/images/logowhite.png"));
                profilePictureImageView.setImage(defaultImage);
                profilePictureImageView.setStyle("-fx-border-radius: 50; -fx-background-radius: 50; -fx-border-color: #ddd; -fx-border-width: 2;");
            }
        } catch (Exception e) {
            LOGGER.warning("Error loading default profile picture: " + e.getMessage());
        }
    }

    private void setupFieldStates() {
        if (usernameField != null) {
            usernameField.setEditable(false);
        }
        if (fullNameField != null) {
            fullNameField.setEditable(false);
        }
        if (emailField != null) {
            emailField.setEditable(false);
        }
    }

    @FXML
    private void handleEditFullName() {
        if (!isEditingFullName) {
            if (fullNameField != null) {
                fullNameField.setEditable(true);
                fullNameField.requestFocus();
                editFullNameButton.setText("Save");
                isEditingFullName = true;
            }
        } else {
            // Save the changes
            String newFullName = fullNameField.getText();
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE users SET full_name = ? WHERE id = ?")) {
                
                stmt.setString(1, newFullName);
                stmt.setInt(2, currentUserId);
                stmt.executeUpdate();

                showAlert("Success", "Full Name Updated", "Your full name has been updated successfully.");
                fullNameField.setEditable(false);
                editFullNameButton.setText("Edit");
                isEditingFullName = false;

                // Update navbar if needed
                if (NavbarController.getInstance() != null) {
                    NavbarController.getInstance().loadUserInfo();
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to Update", e.getMessage());
            }
        }
    }

    @FXML
    private void handleEditUsername() {
        if (!isEditingUsername) {
            if (usernameField != null) {
                usernameField.setEditable(true);
                usernameField.requestFocus();
                editUsernameButton.setText("Save");
                isEditingUsername = true;
            }
        } else {
            // Save the changes
            String newUsername = usernameField.getText();
            if (newUsername.isEmpty()) {
                showAlert("Error", "Invalid Input", "Username cannot be empty.");
                return;
            }
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE users SET username = ? WHERE id = ?")) {
                
                stmt.setString(1, newUsername);
                stmt.setInt(2, currentUserId);
                int result = stmt.executeUpdate();

                if (result > 0) {
                    showAlert("Success", "Username Updated", "Your username has been updated successfully.");
                    Session.setUsername(newUsername);
                    currentUsername = newUsername;
                    usernameField.setEditable(false);
                    editUsernameButton.setText("Edit");
                    isEditingUsername = false;

                    // Update navbar
                    if (NavbarController.getInstance() != null) {
                        NavbarController.getInstance().loadUserInfo();
                    }
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to Update", e.getMessage());
            }
        }
    }

    @FXML
    private void handleEditEmail() {
        if (!isEditingEmail) {
            if (emailField != null) {
                emailField.setEditable(true);
                emailField.requestFocus();
                editEmailButton.setText("Save");
                isEditingEmail = true;
            }
        } else {
            // Save the changes
            String newEmail = emailField.getText();
            if (newEmail.isEmpty()) {
                showAlert("Error", "Invalid Input", "Email cannot be empty.");
                return;
            }
            try (Connection conn = DBUtil.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE users SET email = ? WHERE id = ?")) {
                
                stmt.setString(1, newEmail);
                stmt.setInt(2, currentUserId);
                stmt.executeUpdate();

                showAlert("Success", "Email Updated", "Your email has been updated successfully.");
                emailField.setEditable(false);
                editEmailButton.setText("Edit");
                isEditingEmail = false;
            } catch (Exception e) {
                showAlert("Error", "Failed to Update", e.getMessage());
            }
        }
    }

    @FXML
    private void handleChangePassword() {
        try {
            // Create a dialog for password change
            javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Change Password");
            dialog.setHeaderText("Enter your new password");

            javafx.scene.layout.VBox content = new javafx.scene.layout.VBox();
            content.setSpacing(10);
            content.setPadding(new javafx.geometry.Insets(20));

            javafx.scene.control.Label oldPasswordLabel = new javafx.scene.control.Label("Current Password:");
            PasswordField oldPasswordField = new PasswordField();
            oldPasswordField.setPromptText("Enter current password");

            javafx.scene.control.Label newPasswordLabel = new javafx.scene.control.Label("New Password:");
            PasswordField newPasswordField = new PasswordField();
            newPasswordField.setPromptText("Enter new password");

            javafx.scene.control.Label confirmPasswordLabel = new javafx.scene.control.Label("Confirm Password:");
            PasswordField confirmPasswordField = new PasswordField();
            confirmPasswordField.setPromptText("Confirm new password");

            content.getChildren().addAll(
                    oldPasswordLabel, oldPasswordField,
                    newPasswordLabel, newPasswordField,
                    confirmPasswordLabel, confirmPasswordField
            );

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(
                    javafx.scene.control.ButtonType.OK,
                    javafx.scene.control.ButtonType.CANCEL
            );

            if (dialog.showAndWait().isPresent() && dialog.getResult() != null) {
                String oldPassword = oldPasswordField.getText();
                String newPassword = newPasswordField.getText();
                String confirmPassword = confirmPasswordField.getText();

                if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    showAlert("Error", "Invalid Input", "All fields are required.");
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    showAlert("Error", "Password Mismatch", "New passwords do not match.");
                    return;
                }

                if (newPassword.length() < 6) {
                    showAlert("Error", "Weak Password", "Password must be at least 6 characters.");
                    return;
                }

                // Verify old password and update
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement verify = conn.prepareStatement(
                             "SELECT password FROM users WHERE id = ? AND username = ?")) {
                    
                    verify.setInt(1, currentUserId);
                    verify.setString(2, currentUsername);
                    try (ResultSet rs = verify.executeQuery()) {
                        if (rs.next()) {
                            String storedPassword = rs.getString("password");
                            // In a real application, you'd hash the password for comparison
                            if (!storedPassword.equals(oldPassword)) {
                                showAlert("Error", "Invalid Password", "Current password is incorrect.");
                                return;
                            }

                            // Update password
                            try (PreparedStatement update = conn.prepareStatement(
                                    "UPDATE users SET password = ? WHERE id = ?")) {
                                update.setString(1, newPassword);
                                update.setInt(2, currentUserId);
                                update.executeUpdate();

                                showAlert("Success", "Password Changed", "Your password has been updated successfully.");
                            }
                        }
                    }
                } catch (Exception e) {
                    showAlert("Error", "Failed to Change Password", e.getMessage());
                }
            }
        } catch (Exception e) {
            showAlert("Error", "Error", e.getMessage());
        }
    }

    @FXML
    private void handleUploadPicture() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Profile Picture");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png"),
                    new FileChooser.ExtensionFilter("JPG", "*.jpg", "*.jpeg"),
                    new FileChooser.ExtensionFilter("PNG", "*.png"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            Stage stage = (Stage) uploadPictureButton.getScene().getWindow();
            File selectedFile = fileChooser.showOpenDialog(stage);

            if (selectedFile != null) {
                // Validate file size (5MB max)
                if (selectedFile.length() > 5 * 1024 * 1024) {
                    showAlert("Error", "File Too Large", "File size must not exceed 5MB.");
                    return;
                }

                // Copy file to profile_pictures directory
                String filename = currentUserId + "_" + System.currentTimeMillis() + 
                                 selectedFile.getName().substring(selectedFile.getName().lastIndexOf('.'));
                Path sourcePath = selectedFile.toPath();
                Path destPath = Paths.get(PROFILE_PICTURES_DIR, filename);

                Files.copy(sourcePath, destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Update database with new profile picture path
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "UPDATE users SET profile_picture_path = ? WHERE id = ?")) {
                    
                    stmt.setString(1, destPath.toString());
                    stmt.setInt(2, currentUserId);
                    stmt.executeUpdate();

                    // Load the new picture
                    loadProfilePicture(destPath.toString());
                    showAlert("Success", "Profile Picture Updated", "Your profile picture has been updated successfully.");

                    // Update navbar
                    if (NavbarController.getInstance() != null) {
                        NavbarController.getInstance().loadUserInfo();
                    }
                } catch (Exception e) {
                    showAlert("Error", "Failed to Update Picture", e.getMessage());
                }
            }
        } catch (Exception e) {
            showAlert("Error", "Error Selecting File", e.getMessage());
        }
    }

    private Integer getCurrentUserId() {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id FROM users WHERE username = ?")) {
            
            stmt.setString(1, Session.getUsername());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Error getting user ID: " + e.getMessage());
        }
        return null;
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
