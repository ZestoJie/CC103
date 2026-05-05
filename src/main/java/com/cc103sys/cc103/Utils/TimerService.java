package com.cc103sys.cc103.Utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.cc103sys.cc103.Controllers.NavbarController;
import com.cc103sys.cc103.DB.DBUtil;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class TimerService {
    private static final Logger LOGGER = Logger.getLogger(TimerService.class.getName());
    private static final int BASE_TASK_POINTS = 10;
    private static final TimerService INSTANCE = new TimerService();

    private final List<TimerListener> listeners = new ArrayList<>();
    private Timeline timeline;
    private int remainingSeconds;
    private int initialSeconds;
    private boolean running;
    private boolean paused;
    private Integer selectedTaskId;
    private boolean xpActive;

    public static TimerService getInstance() {
        return INSTANCE;
    }

    public void addTimerListener(TimerListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            LOGGER.fine(() -> "Listener added. Total listeners: " + listeners.size());
        }
    }

    public void removeTimerListener(TimerListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            LOGGER.fine(() -> "Listener removed. Total listeners: " + listeners.size());
        }
    }

    public void clearAllListeners() {
        listeners.clear();
        LOGGER.fine("All listeners cleared");
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public int getInitialSeconds() {
        return initialSeconds;
    }

    public int getMultiplier() {
        if (initialSeconds <= 0) {
            return 1;
        }
        return calculateMultiplier(initialSeconds / 60);
    }

    public Integer getSelectedTaskId() {
        return selectedTaskId;
    }

    public void start(int seconds, Integer taskId, boolean xpActive) {
        stop();
        this.selectedTaskId = taskId;
        this.xpActive = xpActive;
        this.remainingSeconds = seconds;
        this.initialSeconds = seconds;
        this.running = true;
        this.paused = false;
        notifyTimerUpdated();

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        LOGGER.info(() -> "Timer started for task " + taskId + " with " + seconds + " seconds");
    }

    public void pause() {
        if (timeline != null && running && !paused) {
            timeline.pause();
            paused = true;
            notifyTimerUpdated();
            LOGGER.info("Timer paused");
        }
    }

    public void resume() {
        if (timeline != null && running && paused) {
            timeline.play();
            paused = false;
            notifyTimerUpdated();
            LOGGER.info("Timer resumed");
        }
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
        timeline = null;
        running = false;
        paused = false;
        notifyTimerUpdated();
    }

    private void tick() {
        remainingSeconds = Math.max(0, remainingSeconds - 1);
        notifyTimerUpdated();
        if (remainingSeconds <= 0) {
            complete();
        }
    }

    private void complete() {
        stop();
        updateTaskStatus();
        if (xpActive) {
            awardXp();
        }
        notifyTimerCompleted();
    }

    private void notifyTimerUpdated() {
        for (TimerListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onTimerUpdated(remainingSeconds, running, paused);
            } catch (Exception e) {
                LOGGER.severe(() -> "Timer listener update failed: " + e.getMessage());
            }
        }
    }

    private void notifyTimerCompleted() {
        for (TimerListener listener : new ArrayList<>(listeners)) {
            try {
                listener.onTimerCompleted();
            } catch (Exception e) {
                LOGGER.severe(() -> "Timer listener completion failed: " + e.getMessage());
            }
        }
    }

    private void updateTaskStatus() {
        if (selectedTaskId == null) {
            return;
        }
        String sql = "UPDATE tasks SET status = 'completed' WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selectedTaskId);
            stmt.executeUpdate();
            LOGGER.info(() -> "Timer task completed and status updated for task id " + selectedTaskId);
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to update timer task status: " + e.getMessage());
        }
    }

    private void awardXp() {
        int multiplier = calculateMultiplier(initialSeconds / 60);
        int points = BASE_TASK_POINTS * multiplier;
        String sql = "UPDATE users SET points = points + ? WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, points);
            stmt.setString(2, Session.getUsername());
            stmt.executeUpdate();
            Session.setPoints(Session.getPoints() + points);
            refreshNavbarPoints();
            LOGGER.info(() -> "Awarded " + points + " XP for timer completion");
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to award timer XP: " + e.getMessage());
        }
    }

    private int calculateMultiplier(int minutes) {
        if (minutes <= 5) return 5;
        if (minutes <= 15) return 4;
        if (minutes <= 30) return 3;
        if (minutes <= 60) return 2;
        return 1;
    }

    private void refreshNavbarPoints() {
        try {
            if (NavbarController.getInstance() != null) {
                NavbarController.getInstance().loadUserInfo();
            }
        } catch (Exception e) {
            LOGGER.warning(() -> "Unable to refresh navbar points: " + e.getMessage());
        }
    }

    public interface TimerListener {
        void onTimerUpdated(int remainingSeconds, boolean running, boolean paused);
        void onTimerCompleted();
    }
}
