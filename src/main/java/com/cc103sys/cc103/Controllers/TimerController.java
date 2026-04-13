package com.cc103sys.cc103.Controllers;

import java.util.logging.Logger;

import com.cc103sys.cc103.Utils.Navigator;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class TimerController {
    private static final Logger LOGGER = Logger.getLogger(TimerController.class.getName());
    private static final String[] TIMER_PRESETS = {
        "1 Minute", "5 Minutes", "10 Minutes", "30 Minutes", "1 Hour"
    };

    @FXML private ComboBox<String> timeSelect;
    @FXML private Label timerLabel;

    private Timeline timeline;
    private int remainingSeconds;

    @FXML
    public void initialize() {
        if (timeSelect != null) {
            timeSelect.getItems().addAll(TIMER_PRESETS);
            timeSelect.setValue(TIMER_PRESETS[0]);
        }
        updateTimerDisplay();

        // Set navbar active
        NavbarController.getInstance().setActive("tasks");
    }

    @SuppressWarnings("StringConcatenationInFormatCall")
    @FXML
    public void startTimer() {
        try {
            String selected = timeSelect.getValue();
            if (selected == null || selected.isBlank()) {
                LOGGER.warning("No timer preset selected");
                return;
            }

            stopTimer();

            remainingSeconds = convertToSeconds(selected);
            startCountdown();
            LOGGER.info(() -> "Timer started: " + remainingSeconds + " seconds");
        } catch (Exception e) {
            LOGGER.severe(() -> "Timer start error: " + e);
        }
    }

    private void startCountdown() {
        timeline = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> {
                remainingSeconds--;
                updateTimerDisplay();

                if (remainingSeconds <= 0) {
                    stopTimer();
                    onTimerComplete();
                }
            })
        );

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void stopTimer() {
        if (timeline != null) {
            timeline.stop();
        }
    }

    private void updateTimerDisplay() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private int convertToSeconds(String preset) {
        if (preset.contains("Hour")) return 3600;
        if (preset.contains("30")) return 1800;
        if (preset.contains("10")) return 600;
        if (preset.contains("5")) return 300;
        return 60;
    }

    private void onTimerComplete() {
        LOGGER.info("Timer completed!");
        timerLabel.setStyle("-fx-text-fill: #4caf50;");
    }

    @FXML
    @SuppressWarnings("unused")
    private void goSettings() {
        try {
            Navigator.switchScene("Settings");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to navigate to Settings: " + e.getMessage());
        }
    }
}

