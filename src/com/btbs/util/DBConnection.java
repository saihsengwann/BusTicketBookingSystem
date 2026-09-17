package com.btbs.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database Connection Utility for Bus Ticket Booking System (BTBS).
 * Manages JDBC connection lifecycle to MySQL.
 */
public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/btbs?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // default MySQL root password, update if configured

    static {
        try {
            // Explicitly load MySQL JDBC Driver class
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: MySQL JDBC Driver not found in classpath. Ensure mysql-connector-j jar is added to lib/.");
        }
    }

    /**
     * Obtains a new database Connection.
     * 
     * @return active java.sql.Connection instance
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Quick health check to test database connectivity.
     * 
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return false;
        }
    }
}
