package com.cc103sys.cc103.Utils;

import javafx.scene.Scene;

public class Navigator {

    private static Scene scene;

    public static void setScene(Scene sc) {
        scene = sc;
    }

    public static void switchScene(String fxml) {
        try {
            scene.setRoot(ResourceLoader.loadFXML(fxml));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
