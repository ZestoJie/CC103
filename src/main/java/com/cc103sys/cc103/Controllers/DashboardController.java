package com.cc103sys.cc103.Controllers;

import com.cc103sys.cc103.App;
import javafx.fxml.FXML;

public class DashboardController {

    @FXML
    private void handleLogout() {
        try {
            App.setRoot("Login"); // return to Login.fxml
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
