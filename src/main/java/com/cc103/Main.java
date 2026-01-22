package com.cc103;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        //SCENES
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/primary.fxml"));
            Scene scene = new Scene(root, 600, 600, Color.SKYBLUE);
            scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
        }
        

        /*
        texts

        Text text = new Text();
        text.setText("WOWWW!");
        text.setX(50);
        text.setY(50);
        text.setFont(Font.font("Verdana", 50));
        */

        /* 
        Line line = new Line();
        line.setStartX(100);
        line.setStartY(100);
        line.setEndX(300);
        line.setEndY(100);
        line.setStrokeWidth(5);
        line.setStroke(Color.RED);
        line.setRotate(45);
        */

        /*
        rectangles
        Rectangle rectangle = new Rectangle();
        rectangle.setX(800);
        rectangle.setY(300);
        rectangle.setWidth(100);
        rectangle.setHeight(100);
        rectangle.setFill(Color.BLUEVIOLET);
        rectangle.setStrokeWidth(5);
        rectangle.setStroke(Color.BLACK);
        */

        /*
        triangles
        Polygon triangle = new Polygon();
        triangle.getPoints().setAll(
            200.0,200.0,
            400.0,600.0,
            200.0,600.0
        );
        triangle.setFill(Color.YELLOW);
        */
        /*images
        Image lmao = new Image("wow.jpg");
        ImageView imgv = new ImageView(lmao);
        imgv.setX(400);
        imgv.setY(400);
        */

        /*root nodes
        root.getChildren().add(imgv);
        root.getChildren().add(triangle);
        root.getChildren().add(rectangle);
        root.getChildren().add(text);
        root.getChildren().add(line);
        */

        /*
        stages
        stage.getIcons().add(icon);
        stage.setTitle("Stage Demo");
        stage.setHeight(500);
        stage.setWidth(500);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("LMAO PRESS Q");
        stage.setFullScreenExitKeyCombination(KeyCombination.valueOf("Q"));
        stage.setScene(scene);
        stage.show();
        */
    }

    public static void main(String[] args) {
        launch(args);
    }

}