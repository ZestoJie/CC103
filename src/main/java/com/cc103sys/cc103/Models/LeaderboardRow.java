package com.cc103sys.cc103.Models;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class LeaderboardRow {
    private final SimpleStringProperty username;
    private final SimpleIntegerProperty points;

    public LeaderboardRow(String username, int points) {
        this.username = new SimpleStringProperty(username);
        this.points = new SimpleIntegerProperty(points);
    }

    public SimpleStringProperty usernameProperty() {
        return username;
    }

    public SimpleIntegerProperty pointsProperty() {
        return points;
    }
}