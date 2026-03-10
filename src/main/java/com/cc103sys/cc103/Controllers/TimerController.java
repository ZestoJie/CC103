package com.cc103sys.cc103.Controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class TimerController {

    @FXML private ComboBox<String> timeSelect;
    @FXML private Label timerLabel;

    private Timeline timeline;
    private int seconds;

    @FXML
    public void initialize(){

        timeSelect.getItems().addAll(
                "1 Minute",
                "5 Minutes",
                "10 Minutes",
                "30 Minutes",
                "1 Hour"
        );
    }

    @FXML
    private void startTimer(){

        String selected = timeSelect.getValue();
        if(selected == null) return;

        seconds = convertToSeconds(selected);

        timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {

                    seconds--;
                    updateTimerLabel();

                    if(seconds <= 0){
                        timeline.stop();
                    }
                })
        );

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void updateTimerLabel(){

        int min = seconds / 60;
        int sec = seconds % 60;

        timerLabel.setText(String.format("%02d:%02d", min, sec));
    }

    private int convertToSeconds(String value){

        if(value.contains("Hour")) return 3600;
        if(value.contains("30")) return 1800;
        if(value.contains("10")) return 600;
        if(value.contains("5")) return 300;

        return 60;
    }
}

