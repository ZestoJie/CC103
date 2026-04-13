package com.cc103sys.cc103.Utils;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class ResourceLoader {

    private static final String FXML_PATH = "/com/cc103sys/cc103/fxml/";

    public static Parent loadFXML(String name) throws IOException {
        FXMLLoader loader = new FXMLLoader(ResourceLoader.class.getResource(FXML_PATH + name + ".fxml"));
        Parent root = loader.load();

        String cssPath = "/css/style.css"; // default
        if ("Dashboard".equals(name)) {
            cssPath = "/css/style.css";
        }
        if ("TaskScene".equals(name)) {
            cssPath = "/css/task.css";
        }

        root.getStylesheets().add(ResourceLoader.class.getResource(cssPath).toExternalForm());

        return root;
    }
}
