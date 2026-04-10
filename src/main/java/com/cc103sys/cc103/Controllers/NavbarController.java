package com.cc103sys.cc103.Controllers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class NavbarController {

    private static NavbarController instance;

    @FXML
    private ImageView profilePictureImageView;
    @FXML
    private javafx.scene.control.Label usernameLabel;
    @FXML
    private javafx.scene.control.Label userRoleLabel;
    @FXML
    private javafx.scene.control.ProgressBar levelProgressBar;
    @FXML
    private javafx.scene.control.Label levelLabel;

    @FXML
    private Button dashboardBtn;
    @FXML
    private Button classesBtn;
    @FXML
    private Button leaderboardBtn;
    @FXML
    private Button tasksBtn;
    @FXML
    private Button settingsBtn;

    @FXML
    public void initialize() throws Exception {
        instance = this;
        setupRoleBasedAccess();
        loadUserInfo();
    }

    private void setupRoleBasedAccess() {
        // Hide Classes button for participants
        if (classesBtn != null) {
            classesBtn.setVisible(Session.isHost());
            classesBtn.setManaged(Session.isHost());
        }
    }

    public void loadUserInfo() throws Exception {
        if (usernameLabel != null) {
            usernameLabel.setText(Session.getUsername() != null ? Session.getUsername() : "Unknown User");
        }
        if (userRoleLabel != null) {
            userRoleLabel.setText(Session.getDisplayRole());
        }
        if (levelProgressBar != null) {
            levelProgressBar.setProgress(Session.getProgress());
        }
        if (levelLabel != null) {
            levelLabel.setText("Level " + Session.getLevel());
        }
        
        // Load profile picture
        loadProfilePicture();
    }

    private void loadProfilePicture() throws Exception {
        try {
            Integer userId = getCurrentUserId();
            if (userId != null) {
                try (Connection conn = DBUtil.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "SELECT profile_picture_path FROM users WHERE id = ?")) {
                    
                    stmt.setInt(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String profilePicturePath = rs.getString("profile_picture_path");
                            if (profilePicturePath != null && !profilePicturePath.isEmpty()) {
                                File imageFile = new File(profilePicturePath);
                                if (imageFile.exists() && profilePictureImageView != null) {
                                    Image image = new Image(imageFile.toURI().toString());
                                    profilePictureImageView.setImage(image);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // Fall back to default
        }
        
        // Load default profile picture
        try {
            if (profilePictureImageView != null) {
                Image defaultImage = new Image(getClass().getResourceAsStream("/images/logowhite.png"));
                profilePictureImageView.setImage(defaultImage);
            }
        } catch (Exception e) {
            // Ignore
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
            // Ignore
        }
        return null;
    }

    public static NavbarController getInstance() {
        return instance;
    }

    public void setActive(String buttonName) {
        // Reset all buttons to transparent
        String transparentStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-padding: 8 12; -fx-alignment: CENTER_LEFT;";
        String activeStyle = "-fx-background-color: #2c5d7c; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 12; -fx-alignment: CENTER_LEFT;";

        dashboardBtn.setStyle(transparentStyle);
        classesBtn.setStyle(transparentStyle);
        leaderboardBtn.setStyle(transparentStyle);
        tasksBtn.setStyle(transparentStyle);
        settingsBtn.setStyle(transparentStyle);

        switch (buttonName.toLowerCase()) {
            case "dashboard" -> dashboardBtn.setStyle(activeStyle);
            case "classes" -> classesBtn.setStyle(activeStyle);
            case "leaderboard" -> leaderboardBtn.setStyle(activeStyle);
            case "tasks" -> tasksBtn.setStyle(activeStyle);
            case "settings" -> settingsBtn.setStyle(activeStyle);
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void goDashboard() {
        if (Navigator.switchScene("Dashboard")) {
            setActive("dashboard");
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void goTasks() {
        if (Navigator.switchScene("TaskScene")) {
            setActive("tasks");
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void goLeaderboard() {
        if (Navigator.switchScene("Leaderboard")) {
            setActive("leaderboard");
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void goClasses() {
        if (!Session.isHost()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Access Denied");
            alert.setHeaderText("Restricted Access");
            alert.setContentText("Only hosts can access the Classes section.");
            alert.showAndWait();
            return;
        }
        if (Navigator.switchScene("Classes")) {
            setActive("classes");
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void goSettings() {
        if (Navigator.switchScene("Settings")) {
            setActive("settings");
        }
    }

    @FXML
    @SuppressWarnings("unused")
    private void logout(){
        Session.clear();
        Navigator.switchScene("Login");
    }
}
