package com.cc103sys.cc103.Controllers;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.Classes;
import com.cc103sys.cc103.Utils.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ClassesController {

    @FXML private ListView<Classes> classList;
    @FXML private TextField codeField;

    @FXML
    public void initialize() {
        loadPublicClasses();
    }

    private void loadPublicClasses() {
        ObservableList<Classes> publicClasses = FXCollections.observableArrayList();

        String sql = """
            SELECT * 
            FROM classes 
            WHERE is_public = 1 
              AND id NOT IN (SELECT class_id FROM users WHERE username = ?)
        """;

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, Session.getUsername());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                publicClasses.add(new Classes(
                        rs.getInt("id"),
                        rs.getString("class_name")
                ));
            }

            classList.setItems(publicClasses);

            classList.setCellFactory(lv -> new ListCell<>() {
                private final Button joinButton = new Button("Join");

                {
                    joinButton.setOnAction(e -> {
                        Classes c = getItem();
                        if (c != null) joinClass(c.getId());
                    });
                }

                @Override
                protected void updateItem(Classes item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.getClassName());
                        setGraphic(joinButton);
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void joinClass(int classId) {
        String sql = "UPDATE users SET class_id = ? WHERE username = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, classId);
            stmt.setString(2, Session.getUsername());
            stmt.executeUpdate();

            loadPublicClasses();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void joinPrivateClass() {
        String code = codeField.getText();
        if (code.isEmpty()) return;

        String getClassSql = "SELECT id FROM classes WHERE join_code = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(getClassSql)) {

            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int classId = rs.getInt("id");
                joinClass(classId);
                codeField.clear();
            } else {
                System.out.println("Invalid class code");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}