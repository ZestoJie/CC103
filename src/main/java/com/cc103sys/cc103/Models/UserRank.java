
package com.cc103sys.cc103.Models;
public class UserRank {

    private String username;
    private int points;

    public UserRank(String username, int points){
        this.username = username;
        this.points = points;
    }

    public String getUsername(){ return username; }
    public int getPoints(){ return points; }
}

