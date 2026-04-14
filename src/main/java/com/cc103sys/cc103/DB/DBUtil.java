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

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(255) UNIQUE NOT NULL, "
                    + "password VARCHAR(255) NOT NULL, "
                    + "role VARCHAR(50) DEFAULT 'PARTICIPANT', "
                    + "points INT DEFAULT 0"
                    + ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS classes ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "class_name VARCHAR(255) NOT NULL, "
                    + "is_public TINYINT(1) DEFAULT 1, "
                    + "join_code VARCHAR(10), "
                    + "owner_id INT NULL"
                    + ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS class_tasks ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "class_id INT NOT NULL, "
                    + "task_name VARCHAR(255) NOT NULL, "
                    + "due_date DATE NOT NULL, "
                    + "owner_id INT NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS class_groups ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "class_id INT NOT NULL, "
                    + "leader_user_id INT NOT NULL, "
                    + "group_name VARCHAR(255) NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tasks ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(255) NOT NULL, "
                    + "user_id INT NULL, "
                    + "task_name VARCHAR(255) NOT NULL, "
                    + "task_date DATE NOT NULL, "
                    + "status VARCHAR(50) NOT NULL, "
                    + "class_id INT NULL, "
                    + "class_task_id INT NULL, "
                    + "group_id INT NULL, "
                    + "completed_date DATE NULL, "
                    + "points_awarded INT DEFAULT 0, "
                    + "created_by INT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS user_classes ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "user_id INT NOT NULL, "
                    + "class_id INT NOT NULL, "
                    + "UNIQUE KEY uk_user_class (user_id, class_id), "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE"
                    + ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS group_members ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "group_id INT NOT NULL, "
                    + "user_id INT NOT NULL, "
                    + "UNIQUE KEY uk_group_member (group_id, user_id), "
                    + "FOREIGN KEY (group_id) REFERENCES class_groups(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                    + ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS task_submissions ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "class_task_id INT NOT NULL, "
                    + "user_id INT NOT NULL, "
                    + "submission_status VARCHAR(50) DEFAULT 'SUBMITTED', "
                    + "submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (class_task_id) REFERENCES class_tasks(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE"
                    + ")");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS task_attachments ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "submission_id INT NOT NULL, "
                    + "file_name VARCHAR(255) NOT NULL, "
                    + "file_path VARCHAR(500) NOT NULL, "
                    + "uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (submission_id) REFERENCES task_submissions(id) ON DELETE CASCADE"
                    + ")");

            if (!columnExists(conn, "users", "role")) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN role VARCHAR(50) DEFAULT 'PARTICIPANT'");
            }

            if (!columnExists(conn, "users", "points")) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN points INT DEFAULT 0");
            }

            if (!columnExists(conn, "users", "profile_picture_path")) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN profile_picture_path VARCHAR(500) NULL");
            }

            if (!columnExists(conn, "users", "email")) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN email VARCHAR(255) UNIQUE NULL");
            }

            if (!columnExists(conn, "users", "full_name")) {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN full_name VARCHAR(255) NULL");
            }

            if (!columnExists(conn, "classes", "is_public")) {
                stmt.executeUpdate("ALTER TABLE classes ADD COLUMN is_public TINYINT(1) DEFAULT 1");
            }

            if (!columnExists(conn, "tasks", "is_personal")) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN is_personal TINYINT(1) DEFAULT 0");
            }

            if (!columnExists(conn, "classes", "join_code")) {
                stmt.executeUpdate("ALTER TABLE classes ADD COLUMN join_code VARCHAR(10) NULL");
            }

            if (!columnExists(conn, "classes", "owner_id")) {
                stmt.executeUpdate("ALTER TABLE classes ADD COLUMN owner_id INT NULL");
            }

            if (!columnExists(conn, "tasks", "class_id")) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN class_id INT NULL");
            }

            if (!columnExists(conn, "tasks", "class_task_id")) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN class_task_id INT NULL");
            }

            if (!columnExists(conn, "tasks", "group_id")) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN group_id INT NULL");
            }

            if (!columnExists(conn, "tasks", "completed_date")) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN completed_date DATE NULL");
            }

            if (!columnExists(conn, "tasks", "points_awarded")) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN points_awarded INT DEFAULT 0");
            }

            if (!columnExists(conn, "tasks", "created_by")) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN created_by INT NULL");
            }

            if (!columnExists(conn, "tasks", "user_id")) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN user_id INT NULL");
            }

            if (!columnExists(conn, "tasks", "approved_by")) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN approved_by INT NULL");
            }

            if (!columnExists(conn, "tasks", "approved_date")) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN approved_date DATE NULL");
            }

            if (!columnExists(conn, "tasks", "description")) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN description TEXT NULL");
            }

            if (!columnExists(conn, "class_tasks", "owner_id")) {
                stmt.executeUpdate("ALTER TABLE class_tasks ADD COLUMN owner_id INT NOT NULL");
            }

            if (!columnExists(conn, "class_tasks", "description")) {
                stmt.executeUpdate("ALTER TABLE class_tasks ADD COLUMN description TEXT NULL");
            }

            if (!columnExists(conn, "class_tasks", "created_at")) {
                stmt.executeUpdate("ALTER TABLE class_tasks ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            }

            if (!foreignKeyExists(conn, "classes", "fk_classes_owner_id")) {
                try {
                    stmt.executeUpdate("ALTER TABLE classes ADD CONSTRAINT fk_classes_owner_id FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL");
                } catch (SQLException e) {
                }
            }

            if (!foreignKeyExists(conn, "class_tasks", "fk_class_tasks_class_id")) {
                try {
                    stmt.executeUpdate("ALTER TABLE class_tasks ADD CONSTRAINT fk_class_tasks_class_id FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE");
                } catch (SQLException e) {
                }
            }

            if (!foreignKeyExists(conn, "class_tasks", "fk_class_tasks_owner_id")) {
                try {
                    stmt.executeUpdate("ALTER TABLE class_tasks ADD CONSTRAINT fk_class_tasks_owner_id FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL");
                } catch (SQLException e) {
                }
            }

            if (!foreignKeyExists(conn, "tasks", "fk_tasks_class_id")) {
                try {
                    stmt.executeUpdate("ALTER TABLE tasks ADD CONSTRAINT fk_tasks_class_id FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL");
                } catch (SQLException e) {
                }
            }

            if (!foreignKeyExists(conn, "tasks", "fk_tasks_class_task_id")) {
                try {
                    stmt.executeUpdate("ALTER TABLE tasks ADD CONSTRAINT fk_tasks_class_task_id FOREIGN KEY (class_task_id) REFERENCES class_tasks(id) ON DELETE SET NULL");
                } catch (SQLException e) {
                }
            }

            if (!foreignKeyExists(conn, "tasks", "fk_tasks_group_id")) {
                try {
                    stmt.executeUpdate("ALTER TABLE tasks ADD CONSTRAINT fk_tasks_group_id FOREIGN KEY (group_id) REFERENCES class_groups(id) ON DELETE SET NULL");
                } catch (SQLException e) {
                }
            }

            if (!foreignKeyExists(conn, "tasks", "fk_tasks_user_id")) {
                try {
                    stmt.executeUpdate("ALTER TABLE tasks ADD CONSTRAINT fk_tasks_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL");
                } catch (SQLException e) {
                }
            }

            if (!foreignKeyExists(conn, "tasks", "fk_tasks_created_by")) {
                try {
                    stmt.executeUpdate("ALTER TABLE tasks ADD CONSTRAINT fk_tasks_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL");
                } catch (SQLException e) {
                }
            }

            if (!foreignKeyExists(conn, "tasks", "fk_tasks_approved_by")) {
                try {
                    stmt.executeUpdate("ALTER TABLE tasks ADD CONSTRAINT fk_tasks_approved_by FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL");
                } catch (SQLException e) {
                }
            }

            if (!foreignKeyExists(conn, "group_members", "fk_group_members_group_id")) {
                try {
                    stmt.executeUpdate("ALTER TABLE group_members ADD CONSTRAINT fk_group_members_group_id FOREIGN KEY (group_id) REFERENCES class_groups(id) ON DELETE CASCADE");
                } catch (SQLException e) {
                }
            }

            if (!foreignKeyExists(conn, "group_members", "fk_group_members_user_id")) {
                try {
                    stmt.executeUpdate("ALTER TABLE group_members ADD CONSTRAINT fk_group_members_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE");
                } catch (SQLException e) {
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database schema", e);
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
