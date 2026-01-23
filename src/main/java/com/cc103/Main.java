package com.cc103;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage stage) throws IOException {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/primary.fxml"));
            Scene scene = new Scene(root, 600, 600, Color.SKYBLUE);
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {}
    }
}