package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class for creating a JDBC connection to the MySQL database on mudfoot.
 * Update DB_URL, DB_USER, and DB_PASS with your own mudfoot credentials.
 */
public class DBConnection {

    private static final String URL =
            "jdbc:mysql://mudfoot.doc.stu.mmu.ac.uk:6306/chopracs" +
            "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER     = "chopracs";
    private static final String PASSWORD = "LowdEwfos2";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found on classpath", e);
        }
    }

    /**
     * Opens and returns a new connection to the database.
     * Callers are responsible for closing it (use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
