package com.cc103sys.cc103.DB;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBUtil {

    private static final String URL = "jdbc:mysql://localhost:3306/cc103?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "Databasecc103.javafxmavenarchetype.sysproject";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Ensure mysql-connector-j is in classpath.", e);
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
