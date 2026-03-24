package com.cc103sys.cc103.Controllers;

import java.util.logging.Logger;

import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.fxml.FXML;

/**
 * Controller for Navigation bar component.
 * Handles navigation between main scenes and logout.
 */
public class NavbarController {
    private static final Logger LOGGER = Logger.getLogger(NavbarController.class.getName());

    /**
     * Navigate to Dashboard scene.
     */
    @FXML
    private void goDashboard() {
        navigateToScene("Dashboard");
    }

    /**
     * Navigate to Task Management scene.
     */
    @FXML
    private void goTasks() {
        navigateToScene("Dashboard");
    }

    /**
     * Navigate to Leaderboard scene.
     */
    @FXML
    private void goLeaderboard() {
        navigateToScene("Leaderboard");
    }

    /**
     * Navigate to Classes scene.
     */
    @FXML
    private void goClasses() {
        navigateToScene("Classes");
    }

    /**
     * Logout current user and return to Login scene.
     */
    @FXML
    private void logout() {
        try {
            String username = Session.getUsername();
            Session.clear();
            Navigator.switchScene("Login");
            LOGGER.info("User logged out: " + username);
        } catch (Exception e) {
            LOGGER.severe("Logout error: " + e.getMessage());
        }
    }

    /**
     * Helper method to navigate to a scene with error handling.
     */
    private void navigateToScene(String sceneName) {
        try {
            Navigator.switchScene(sceneName);
            LOGGER.info("Navigated to: " + sceneName);
        } catch (Exception e) {
            LOGGER.severe("Navigation error to " + sceneName + ": " + e.getMessage());
        }
    }
}

