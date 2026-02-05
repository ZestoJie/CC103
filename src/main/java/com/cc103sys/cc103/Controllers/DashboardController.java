package com.cc103sys.cc103.Controllers;

import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {

    @FXML private Label welcomeLabel;

    @FXML
    public void initialize(){
        welcomeLabel.setText("Welcome, " + Session.getUsername());
    }

    @FXML
    private void handleLogout(){
        Session.clear();
        Navigator.switchScene("Login");
    }
}
