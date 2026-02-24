package com.cc103sys.cc103.Utils;

public class Session {

    private static String username;
    private static int selectedClassId; // add this

    public static void setUsername(String user) {
        username = user;
    }

    public static String getUsername() {
        return username;
    }

    public static void setSelectedClassId(int classId) {
        selectedClassId = classId;
    }

    public static int getSelectedClassId() {
        return selectedClassId;
    }

    public static void clear() {
        username = null;
        selectedClassId = 0; // reset class
    }
}