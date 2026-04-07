package com.cc103sys.cc103.Controllers;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.fxml.FXML;

public class NavbarController {

    @FXML
    private void goDashboard() {
        Navigator.switchScene("Dashboard");
    }

    @FXML
    private void goTasks() {
        Navigator.switchScene("TaskScene");
    }

    @FXML
    private void goLeaderboard() {
        Navigator.switchScene("Leaderboard");
    }

    @FXML
    private void goClasses() {
        Navigator.switchScene("Classes");
    }

    @FXML
    private void goSettings() {
        Navigator.switchScene("Settings");
    }

    @FXML
    private void logout(){
        Session.clear();
        Navigator.switchScene("Login");
    }
}
