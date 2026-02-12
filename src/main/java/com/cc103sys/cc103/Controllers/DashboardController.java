package com.cc103sys.cc103.Controllers;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

import com.cc103sys.cc103.DB.DBUtil;
import com.cc103sys.cc103.Models.Task;
import com.cc103sys.cc103.Models.UserRank;
import com.cc103sys.cc103.Utils.Navigator;
import com.cc103sys.cc103.Utils.Session;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private TextField taskField;
    @FXML private DatePicker taskDate;
    @FXML private ListView<Task> taskList;
    @FXML private Label timerLabel;
    @FXML private ListView<UserRank> leaderboardPreview;
    private Timeline timeline;
    private int seconds;



    private ObservableList<Task> tasks = FXCollections.observableArrayList();
    @FXML
    private ComboBox<String> timerPreset;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, " + Session.getUsername());
        taskList.setItems(tasks);
        taskList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Task task, boolean empty) {
                super.updateItem(task, empty);
                if(empty || task == null){
                    setText(null);
                } else {
                        setText(
                        task.getTaskName() + " | " +
                        task.getDate() + " | " +
                        task.getStatus()
                        );
                    }
                }
            }
        );
    
        loadTasks();

        if (timerPreset != null) {
            timerPreset.getItems().addAll("1 Minute", "5 Minutes", "10 Minutes", "1 Hour");
            timerPreset.setValue("1 Minute");
        } else {
            System.err.println("timerPreset is null! FXML not injected properly.");
        }
    }

    private void loadTasks(){

        tasks.clear();

        String sql = "SELECT * FROM tasks WHERE username=?";

        try(Connection conn = DBUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, Session.getUsername());

            ResultSet rs = stmt.executeQuery();

            while(rs.next()){
                tasks.add(new Task(
                        rs.getInt("id"),
                        rs.getString("task_name"),
                        rs.getDate("task_date").toLocalDate(),
                        rs.getString("status")
                ));
            }

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddTask(){

        String taskName = taskField.getText();
        LocalDate date = taskDate.getValue();

        if(taskName.isEmpty() || date == null) return;

        String sql = "INSERT INTO tasks(username,task_name,task_date,status) VALUES (?,?,?,?)";

        try(Connection conn = DBUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setString(1, Session.getUsername());
            stmt.setString(2, taskName);
            stmt.setDate(3, Date.valueOf(date));
            stmt.setString(4, "Pending");

            stmt.executeUpdate();

            loadTasks();
            playAnimation();

            taskField.clear();
            taskDate.setValue(null);

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteTask(){

        Task selected = taskList.getSelectionModel().getSelectedItem();
        if(selected == null) return;

        String sql = "DELETE FROM tasks WHERE id=?";

        try(Connection conn = DBUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){

            stmt.setInt(1, selected.getId());
            stmt.executeUpdate();

            loadTasks();

        } catch(Exception e){
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMarkDone(){

        Task selected = taskList.getSelectionModel().getSelectedItem();
        if(selected == null) return;

        String sql = "UPDATE tasks SET status='Done' WHERE id=?";
        String updatePoints = "UPDATE users SET points = points + 10 WHERE username=?";

        try(Connection conn = DBUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)){
            stmt.setInt(1, selected.getId());
            stmt.executeUpdate();

            loadTasks();

        } catch(Exception e){
            e.printStackTrace();
        }

        try(Connection conn = DBUtil.getConnection();
            PreparedStatement pts = conn.prepareStatement(updatePoints)){
            pts.setString(1, Session.getUsername());
            pts.executeUpdate();
        } catch (Exception e){
            e.printStackTrace();
          }
    }

    private void playAnimation(){

        FadeTransition fade = new FadeTransition(Duration.seconds(1), taskList);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.seconds(1), taskList);
        slide.setFromY(-50);
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    @FXML
    private void handleLogout(){
        Session.clear();
        Navigator.switchScene("Login");
    }
    @FXML
    private void openLeaderboardScene() {
    Navigator.switchScene("Leaderboard");
    }

    @FXML
    private void handleStartTimer() {

        String selected = timerPreset.getValue();
        if (selected == null) return;

        seconds = convertToSeconds(selected);

        if (timeline != null) timeline.stop();

        timeline = new Timeline(
            new KeyFrame(
                Duration.seconds(1), e -> {
                seconds--;
                updateTimerLabel();
                if (seconds <= 0) timeline.stop();
            }
        )
    );

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
    private void updateTimerLabel() {
        int min = seconds / 60;
        int sec = seconds % 60;
        timerLabel.setText(String.format("%02d:%02d", min, sec));
    }

    private int convertToSeconds(String value) {
        if (value.contains("Hour")) return 3600;
        if (value.contains("10")) return 600;
        if (value.contains("5")) return 300;
        return 60;
    }

    private void loadLeaderboardPreview() {
        ObservableList<UserRank> data = FXCollections.observableArrayList();
        String sql = "SELECT username, points FROM users ORDER BY points DESC LIMIT 5";
        try (Connection conn = DBUtil.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)
        ) 
        {
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            data.add(
                    new UserRank(
                    rs.getString("username"),
                    rs.getInt("points")
                )
            );
        }

        leaderboardPreview.setItems(data);
        leaderboardPreview.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(UserRank item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null :
                            item.getUsername() + " - " + item.getPoints() + " pts");
                }
            }
        );
    } catch (Exception e) {
        e.printStackTrace();
    }
}
}