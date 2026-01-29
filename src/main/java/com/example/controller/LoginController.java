package com.example.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    private TextField usernameField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private VBox loginContainer;
    
    private static final String API_URL = "http://localhost:8080/api/auth/login";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();
    
    @FXML
    public void initialize() {
        errorLabel.setText("");
        loginButton.setDisable(false);
    }
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        loginButton.setDisable(true);
        loginButton.setText("Logging in...");
        
        new Thread(() -> {
            try {
                JsonObject loginRequest = new JsonObject();
                loginRequest.addProperty("username", username);
                loginRequest.addProperty("password", password);
                
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginRequest.toString()))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
                    if (responseBody.get("success").getAsBoolean()) {
                        javafx.application.Platform.runLater(() -> {
                            showSuccess("Login successful!");
                            loadPrimaryView();
                        });
                    } else {
                        javafx.application.Platform.runLater(() -> 
                            showError(responseBody.get("message").getAsString())
                        );
                    }
                } else {
                    JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
                    javafx.application.Platform.runLater(() -> 
                        showError(responseBody.get("message").getAsString())
                    );
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> 
                    showError("Connection error: " + e.getMessage())
                );
            } finally {
                javafx.application.Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Login");
                });
            }
        }).start();
    }
    
    @FXML
    private void switchToRegister() {
        showError("Register functionality coming soon!");
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #d32f2f;");
    }
    
    private void showSuccess(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #388e3c;");
    }
    
    private void loadPrimaryView() {
        try {
            Stage stage = (Stage) loginContainer.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("primary.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setScene(scene);
            stage.setTitle("CC103 Application");
        } catch (IOException e) {
            showError("Error loading main view: " + e.getMessage());
        }
    }
}
