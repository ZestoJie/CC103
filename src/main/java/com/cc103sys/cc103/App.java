package com.cc103sys.cc103;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Correct path based on your resources folder
        System.out.println("Login.fxml path: " + App.class.getResource("/com/cc103sys/cc103/fxml/Login.fxml"));

        scene = new Scene(loadFXML("Login"));
        stage.setTitle("Login System");
        stage.setScene(scene);
        stage.show();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        // Adjusted path to match your resources
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/cc103sys/cc103/fxml/" + fxml + ".fxml"));
        Parent root = loader.load();
        if (root == null) {
            throw new IOException("FXML file not found: " + fxml + ".fxml");
        }
        return root;
    }

    public static void main(String[] args) {
        launch();
    }
}
