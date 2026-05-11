package com.cc103sys.cc103.Utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

public final class UiDialogs {

    private UiDialogs() {
    }

    public static boolean confirm(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public static void info(Window owner, String title, String message) {
        show(Alert.AlertType.INFORMATION, owner, title, message);
    }

    public static void error(Window owner, String title, String message) {
        show(Alert.AlertType.ERROR, owner, title, message);
    }

    public static void warn(Window owner, String title, String message) {
        show(Alert.AlertType.WARNING, owner, title, message);
    }

    private static void show(Alert.AlertType type, Window owner, String title, String message) {
        Alert alert = new Alert(type);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
