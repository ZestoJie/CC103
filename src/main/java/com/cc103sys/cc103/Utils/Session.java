package com.cc103sys.cc103.Utils;

public class Session {

    private static String username;
    private static String userRole;
    private static int points;
    private static int currentClassId = -1;

    public static void setUsername(String user) {
        username = user;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUserRole(String role) {
        userRole = role;
    }

    public static String getUserRole() {
        return userRole;
    }

    public static boolean isHost() {
        return "HOST".equalsIgnoreCase(userRole);
    }

    public static boolean isParticipant() {
        return "PARTICIPANT".equalsIgnoreCase(userRole) || "STUDENT".equalsIgnoreCase(userRole);
    }

    public static String getDisplayRole() {
        if (isHost()) {
            return "Host";
        }
        if (isParticipant()) {
            return "Participant";
        }
        return "Participant";
    }

    public static void setPoints(int userPoints) {
        points = userPoints;
    }

    public static int getPoints() {
        return points;
    }

    public static int getLevel() {
        return (points / 100) + 1;
    }

    public static double getProgress() {
        return Math.min(1.0, (points % 100) / 100.0);
    }

    public static void setCurrentClassId(int classId) {
        currentClassId = classId;
    }

    public static int getCurrentClassId() {
        return currentClassId;
    }

    public static void clear() {
        username = null;
        userRole = null;
        points = 0;
        currentClassId = -1;
    }
}