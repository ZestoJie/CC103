/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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

