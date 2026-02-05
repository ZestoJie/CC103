package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Utils.Navigator;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void handleRegister(){

        String username = usernameField.getText();
        String password = passwordField.getText();

        if(username.isEmpty() || password.isEmpty()){
            messageLabel.setText("Fill all fields.");
            return;
        }

        String sql = "INSERT INTO users(username,password) VALUES (?,?)";

        try(Connection conn = DBUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, username);
            stmt.setString(2, password);

            stmt.executeUpdate();

            messageLabel.setText("Registration success!");

        } catch(Exception e){
            e.printStackTrace();
            messageLabel.setText("Registration failed.");
        }
    }

    @FXML
    private void goLogin(){
        Navigator.switchScene("Login");
    }
}
