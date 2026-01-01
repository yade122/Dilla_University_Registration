package dillauniversity.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/dilla_university";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "@yadetan"; 
    
    private static Connection connection = null;
    
    static {
        try {
           
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
            e.printStackTrace();
        }
    }
    
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            establishConnection();
        } else {
            try {
                // Check if connection is valid (2 seconds timeout)
                if (!connection.isValid(2)) {
                    System.out.println("Connection is invalid, reconnecting...");
                    connection = null;
                    establishConnection();
                }
            } catch (SQLException e) {
                System.out.println("Error checking connection validity, reconnecting...");
                connection = null;
                establishConnection();
            }
        }
        return connection;
    }
    
    private static void establishConnection() throws SQLException {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Database connection established.");
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            throw e;
        }
    }
    
    public static Connection getNewConnection() throws SQLException {
        try {
            Connection newConn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("New database connection established.");
            return newConn;
        } catch (SQLException e) {
            System.err.println("Failed to create new database connection: " + e.getMessage());
            throw e;
        }
    }
    
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static boolean testConnection() {
        try {
            getConnection();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}