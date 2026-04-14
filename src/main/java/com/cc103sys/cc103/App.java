package com.cc103sys.cc103;

import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.ResourceLoader;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        Scene scene = new Scene(ResourceLoader.loadFXML("Login"), 1440, 720);

        Navigator.setScene(scene);

        stage.setTitle("CC103 Task Manager");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
