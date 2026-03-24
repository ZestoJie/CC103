package com.cc103sys.cc103.Controllers;

import java.util.logging.Logger;

import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.fxml.FXML;

public class NavbarController {
    private static final Logger LOGGER = Logger.getLogger(NavbarController.class.getName());

    @FXML
    private void goDashboard() {
        navigateToScene("Dashboard");
    }

    @FXML
    private void goTasks() {
        navigateToScene("Dashboard");
    }

    @FXML
    private void goLeaderboard() {
        navigateToScene("Leaderboard");
    }

    @FXML
    private void goClasses() {
        navigateToScene("Classes");
    }

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

    private void navigateToScene(String sceneName) {
        try {
            Navigator.switchScene(sceneName);
            LOGGER.info("Navigated to: " + sceneName);
        } catch (Exception e) {
            LOGGER.severe("Navigation error to " + sceneName + ": " + e.getMessage());
        }
    }
}

