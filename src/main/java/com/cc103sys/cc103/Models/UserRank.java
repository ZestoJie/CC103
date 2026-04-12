
package com.cc103sys.cc103.Models;
public class UserRank {

    private String username;
    private int points;
    private int level;

    public UserRank(String username, int points) {
        this(username, points, (points / 100) + 1);
    }

    public UserRank(String username, int points, int level) {
        this.username = username;
        this.points = points;
        this.level = level;
    }

    public String getUsername() { return username; }
    public int getPoints() { return points; }
    public int getLevel() { return level; }
}

