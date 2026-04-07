package com.cc103sys.cc103.DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS user_classes ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "user_id INT NOT NULL, "
                    + "class_id INT NOT NULL, "
                    + "UNIQUE KEY uk_user_class (user_id, class_id), "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE"
                    + ")");

            if (!columnExists(conn, "tasks", "class_id")) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN class_id INT NULL");
            }

            if (!foreignKeyExists(conn, "tasks", "fk_tasks_class_id")) {
                try {
                    stmt.executeUpdate("ALTER TABLE tasks ADD CONSTRAINT fk_tasks_class_id FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL");
                } catch (SQLException e) {
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure user_classes table exists", e);
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws java.sql.SQLException {
        String query = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private static boolean foreignKeyExists(Connection conn, String tableName, String fkName) throws java.sql.SQLException {
        String query = "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = ? AND constraint_name = ? AND constraint_type = 'FOREIGN KEY'";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, tableName);
            ps.setString(2, fkName);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
