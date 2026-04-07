package com.cc103sys.cc103.Controllers;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class NavbarController {

    private static NavbarController instance;

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
    public void initialize() {
        instance = this;
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
        Navigator.switchScene("Dashboard");
        setActive("dashboard");
    }

    @FXML
    @SuppressWarnings("unused")
    private void goTasks() {
        Navigator.switchScene("TaskScene");
        setActive("tasks");
    }

    @FXML
    @SuppressWarnings("unused")
    private void goLeaderboard() {
        Navigator.switchScene("Leaderboard");
        setActive("leaderboard");
    }

    @FXML
    @SuppressWarnings("unused")
    private void goClasses() {
        Navigator.switchScene("Classes");
        setActive("classes");
    }

    @FXML
    @SuppressWarnings("unused")
    private void goSettings() {
        Navigator.switchScene("Settings");
        setActive("settings");
    }

    @FXML
    @SuppressWarnings("unused")
    private void logout(){
        Session.clear();
        Navigator.switchScene("Login");
    }
}
