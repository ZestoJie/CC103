package com.cc103sys.cc103.Utils;

import java.io.IOException;

import javafx.scene.Scene;

public class Navigator {

    private static Scene scene;

    public static void setScene(Scene sc) {
        scene = sc;
    }

    public static boolean navigateTo(String viewName) {
        String fxmlFile;
        switch (viewName.toLowerCase()) {
            case "login":
                fxmlFile = "Login";
                break;
            case "register":
                fxmlFile = "Register";
                break;
            case "dashboard":
                fxmlFile = "Dashboard";
                break;
            case "classes":
                fxmlFile = "Classes";
                break;
            case "classdetail":
                fxmlFile = "ClassDetail";
                break;
            case "leaderboard":
                fxmlFile = "Leaderboard";
                break;
            case "task":
            case "tasks":
                fxmlFile = "TaskScene";
                break;
            default:
                fxmlFile = viewName;
        }
        return switchScene(fxmlFile);
    }

    public static boolean switchScene(String fxml) {
        try {
            scene.setRoot(ResourceLoader.loadFXML(fxml));
            return true;
        } catch (IOException e) {
            System.err.println("Failed to switch to scene: " + fxml);
            e.printStackTrace();
            return false;
        }
    }
}
