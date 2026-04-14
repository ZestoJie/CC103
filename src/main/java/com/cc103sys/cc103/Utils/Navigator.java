package com.cc103sys.cc103.Utils;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import com.cc103sys.cc103.Controllers.TaskDetailController;
import com.cc103sys.cc103.Controllers.TaskReviewController;

import javafx.scene.Scene;

public class Navigator {

    private static final String FXML_BASE_PATH = "/com/cc103sys/cc103/fxml/";
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
            case "taskdetail":
                fxmlFile = "TaskDetail";
                break;
            case "taskreview":
                fxmlFile = "TaskReview";
                break;
            default:
                fxmlFile = viewName;
        }
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
            scene.setRoot(root);
            return true;
        } catch (Exception e) {
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
            scene.setRoot(root);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to navigate to task review: " + e.getMessage());
            return false;
        }
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
