package com.cc103sys.cc103.Utils;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import com.cc103sys.cc103.Controllers.TaskDetailController;
import com.cc103sys.cc103.Controllers.TaskReviewController;

import javafx.animation.FadeTransition;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.util.Duration;

public class Navigator {

    private static final String FXML_BASE_PATH = "/com/cc103sys/cc103/fxml/";
    private static Scene scene;

    public static void setScene(Scene sc) {
        scene = sc;
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
            case "taskdetail" -> "TaskDetail";
            case "taskreview" -> "TaskReview";
            default -> viewName;
        };
        return switchScene(fxmlFile);
    }

    public static boolean navigateToTaskDetail(Integer classId, Integer taskId) {
        try {
            URL fxmlUrl = Objects.requireNonNull(
                ResourceLoader.class.getResource(FXML_BASE_PATH + "TaskDetail.fxml"),
                "TaskDetail.fxml resource not found"
            );
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            javafx.scene.layout.HBox root = loader.load();
            TaskDetailController controller = loader.getController();
            controller.setTaskData(classId, taskId);
            setRootWithTransition(root);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to navigate to task detail: " + e.getMessage());
            return false;
        }
    }

    public static boolean navigateToTaskReview(Integer classId, Integer taskId) {
        try {
            URL fxmlUrl = Objects.requireNonNull(
                ResourceLoader.class.getResource(FXML_BASE_PATH + "TaskReview.fxml"),
                "TaskReview.fxml resource not found"
            );
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            javafx.scene.layout.HBox root = loader.load();
            TaskReviewController controller = loader.getController();
            controller.setTaskData(classId, taskId);
            setRootWithTransition(root);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to navigate to task review: " + e.getMessage());
            return false;
        }
    }

    public static boolean switchScene(String fxml) {
        try {
            Parent root = ResourceLoader.loadFXML(fxml);
            setRootWithTransition(root);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to switch to scene: " + fxml);
            return false;
        }
    }

    private static void setRootWithTransition(Parent root) {
        if (scene == null) {
            return;
        }
        root.setOpacity(0);
        scene.setRoot(root);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(160), root);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }
}
