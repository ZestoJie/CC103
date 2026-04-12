package com.cc103sys.cc103.Controllers;

import java.util.logging.Logger;

import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.TimerService;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public class TimerController implements TimerService.TimerListener {
    private static final Logger LOGGER = Logger.getLogger(TimerController.class.getName());
    private static final String[] TIMER_PRESETS = {
        "1 Minute", "5 Minutes", "10 Minutes", "30 Minutes", "1 Hour"
    };

    @FXML private ComboBox<String> timeSelect;
    @FXML private Label timerLabel;

    private TimerService timerService;

    @FXML
    public void initialize() {
        timerService = TimerService.getInstance();
        timerService.addTimerListener(this);

        if (timeSelect != null) {
            timeSelect.getItems().addAll(TIMER_PRESETS);
            timeSelect.setValue(TIMER_PRESETS[0]);
        }
        
        // Sync display with current timer state
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

            int seconds = convertToSeconds(selected);
            LOGGER.info(() -> "Timer started: " + seconds + " seconds");
            
            // Use TimerService which runs in the background
            timerService.start(seconds, null, false);
        } catch (Exception e) {
            LOGGER.severe(() -> "Timer start error: " + e);
        }
    }

    private int convertToSeconds(String preset) {
        if (preset.contains("Hour")) return 3600;
        if (preset.contains("30")) return 1800;
        if (preset.contains("10")) return 600;
        if (preset.contains("5")) return 300;
        return 60;
    }

    @Override
    public void onTimerUpdated(int remainingSeconds, boolean running, boolean paused) {
        updateTimerDisplay();
    }

    @Override
    public void onTimerCompleted() {
        if (timerLabel != null) {
            timerLabel.setStyle("-fx-text-fill: #4caf50;");
        }
        LOGGER.info("Timer completed!");
    }

    private void updateTimerDisplay() {
        if (timerLabel == null) {
            return;
        }

        int remaining = timerService.getRemainingSeconds();
        boolean running = timerService.isRunning();

        if (!running) {
            timerLabel.setText("00:00");
            timerLabel.setStyle("-fx-text-fill: #7f8c8d;");
            return;
        }

        int minutes = remaining / 60;
        int seconds = remaining % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        timerLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
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

