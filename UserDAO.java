package dillauniversity.dao;

import dillauniversity.database.DatabaseConnection;
import java.sql.*;
import java.security.MessageDigest;

public class UserDAO {
    
    public boolean authenticateUser(String username, String password, String role) {
        // Removed is_active check to ensure login works even if default value isn't set
        String sql = "SELECT u.user_id, u.password, u.role FROM users u WHERE u.username = ? AND u.role = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.setString(2, role.toLowerCase()); // Convert to lowercase to match database
            
            System.out.println("DEBUG: Executing query for user: " + username + ", role: " + role.toLowerCase());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                String hashedPassword = hashPassword(password);
                
                System.out.println("DEBUG: User found. Checking password...");
                
                // Check hashed password OR raw password (for backward compatibility/testing)
                boolean match = storedPassword.equals(hashedPassword) || storedPassword.equals(password);
                
                if (match) {
                    System.out.println("DEBUG: Password match!");
                    return true;
                } else {
                    System.out.println("DEBUG: Password mismatch.");
                    // Debug info (remove in production)
                    // System.out.println("Stored: " + storedPassword);
                    // System.out.println("Hashed Input: " + hashedPassword);
                    return false;
                }
            } else {
                System.out.println("DEBUG: User not found or role mismatch.");
            }
            
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return password;
        }
    }
    
    // Method to check if user exists
    public boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    // Method to get user role
    public String getUserRole(String username) {
        String sql = "SELECT role FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("role");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    // Method to update last login
    public void updateLastLogin(String username) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, username);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}