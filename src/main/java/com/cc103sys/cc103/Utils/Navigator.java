package com.cc103sys.cc103.Utils;

import java.io.IOException;

import javafx.scene.Scene;

public class Navigator {

    private static Scene scene;
    @SuppressWarnings("unused")
    private static TimerService timerService;

    public static void setScene(Scene sc) {
        scene = sc;
        timerService = TimerService.getInstance();
    }

    public static boolean navigateTo(String viewName) {
        String fxmlFile;
        fxmlFile = switch (viewName.toLowerCase()) {
            case "login" -> "Login";
            case "register" -> "Register";
            case "dashboard" -> "Dashboard";
            case "classes" -> "Classes";
            case "classdetail" -> "ClassDetail";
            case "leaderboard" -> "Leaderboard";
            case "task", "tasks" -> "TaskScene";
            default -> viewName;
        };
        return switchScene(fxmlFile);
    }

    public static boolean switchScene(String fxml) {
        try {
            scene.setRoot(ResourceLoader.loadFXML(fxml));
            return true;
        } catch (IOException e) {
            System.err.println("Failed to switch to scene: " + fxml);
            return false;
        }
    }
}
