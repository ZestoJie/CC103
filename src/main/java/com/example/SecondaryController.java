package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SecondaryController {

    @FXML
    private Label secondaryLabel;

    @FXML
    public void initialize() {
        secondaryLabel.setText("Secondary View - Content Coming Soon");
    }
}