import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBTest {
    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = com.cc103sys.cc103.DB.DBUtil.getConnection();

            System.out.println("Database connected successfully!");

            // Test specific user
            String testUsername = "Darwin Dela Trinidad";
            String testPassword = "1234";

            String sql = "SELECT id, role FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, testUsername);
            stmt.setString(2, testPassword);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("Authentication successful for: " + testUsername);
                System.out.println("Role: " + rs.getString("role"));
            } else {
                System.out.println("Authentication failed for: " + testUsername + " with password: " + testPassword);
            }

            // Also test case sensitivity
            sql = "SELECT username, password FROM users WHERE username = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, testUsername);
            rs = stmt.executeQuery();
            if (rs.next()) {
                String dbUsername = rs.getString("username");
                String dbPassword = rs.getString("password");
                System.out.println("DB Username: '" + dbUsername + "'");
                System.out.println("DB Password: '" + dbPassword + "'");
                System.out.println("Input matches DB: " + testPassword.equals(dbPassword));
            }

            // Check the tasks table schema
            System.out.println("\nTasks table columns:");
            rs = conn.createStatement().executeQuery(
                "SELECT COLUMN_NAME, DATA_TYPE FROM information_schema.columns " +
                "WHERE table_schema = 'cc103' AND table_name = 'tasks'"
            );
            while (rs.next()) {
                System.out.println("- " + rs.getString("COLUMN_NAME") + " (" + rs.getString("DATA_TYPE") + ")");
            }

            // Check the classes table schema
            System.out.println("\nClasses table columns:");
            rs = conn.createStatement().executeQuery(
                "SELECT COLUMN_NAME, DATA_TYPE FROM information_schema.columns " +
                "WHERE table_schema = 'cc103' AND table_name = 'classes'"
            );
            while (rs.next()) {
                System.out.println("- " + rs.getString("COLUMN_NAME") + " (" + rs.getString("DATA_TYPE") + ")");
            }

            conn.close();
        } catch (Exception e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}