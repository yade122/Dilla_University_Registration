package dillauniversity.database;

import dillauniversity.models.User;
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthService {
    
    // Hash password (simple SHA-256 for demo)
    public static String hashPassword(String password) {
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
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Hashing algorithm not found: " + e.getMessage());
            return password; // Fallback to plain text
        }
    }
    
    // Authenticate user with database
    public static User authenticate(String username, String password, String role) {
        // For production, use hashed passwords:
        // String hashedPassword = hashPassword(password);
        
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND role = ? AND is_active = TRUE";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password); // In production: use hashedPassword
            pstmt.setString(3, role.toLowerCase());
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                User user = new User();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setRole(rs.getString("role"));
                user.setActive(rs.getBoolean("is_active"));
                
                System.out.println("Authentication successful for: " + username);
                return user;
            }
            
            System.out.println("Authentication failed for: " + username);
            return null;
            
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
            return null;
        }
    }
    
    // Check if user exists in database
    public static boolean userExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            return rs.next() && rs.getInt(1) > 0;
            
        } catch (SQLException e) {
            System.err.println("Error checking user existence: " + e.getMessage());
            return false;
        }
    }
    
    // Get user ID by username
    public static int getUserId(String username) {
        String sql = "SELECT user_id FROM users WHERE username = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("user_id");
            }
            return -1;
            
        } catch (SQLException e) {
            System.err.println("Error getting user ID: " + e.getMessage());
            return -1;
        }
    }
    
    // Get student details for dashboard
    public static String getStudentDetailsForDashboard(int userId) {
        String sql = "SELECT s.full_name, s.registration_number, s.department, s.year, s.semester, " +
                     "COUNT(DISTINCT e.course_code) as total_courses " +
                     "FROM students s " +
                     "LEFT JOIN enrollment e ON s.student_id = e.student_id " +
                     "WHERE s.user_id = ? " +
                     "GROUP BY s.student_id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return String.format("""
                                     Welcome, %s!
                                     
                                     \ud83d\udccb Student Information:
                                        \u2022 Registration No: %s
                                        \u2022 Department: %s
                                        \u2022 Year: %d, Semester: %d
                                        \u2022 Enrolled Courses: %d""",
                    rs.getString("full_name"),
                    rs.getString("registration_number"),
                    rs.getString("department"),
                    rs.getInt("year"),
                    rs.getInt("semester"),
                    rs.getInt("total_courses")
                );
            }
            return "Student information not found";
            
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }
    
    // Get teacher details for dashboard
    public static String getTeacherDetailsForDashboard(int userId) {
        String sql = "SELECT t.full_name, t.employee_id, t.department, t.qualification, " +
                     "COUNT(DISTINCT ac.course_code) as assigned_courses " +
                     "FROM teachers t " +
                     "LEFT JOIN assigned_courses ac ON t.teacher_id = ac.teacher_id " +
                     "WHERE t.user_id = ? " +
                     "GROUP BY t.teacher_id";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return String.format(
                    "Welcome, %s!\n\n" +
                    "📋 Teacher Information:\n" +
                    "   • Employee ID: %s\n" +
                    "   • Department: %s\n" +
                    "   • Qualification: %s\n" +
                    "   • Assigned Courses: %d",
                    rs.getString("full_name"),
                    rs.getString("employee_id"),
                    rs.getString("department"),
                    rs.getString("qualification"),
                    rs.getInt("assigned_courses")
                );
            }
            return "Teacher information not found";
            
        } catch (SQLException e) {
            return "Error: " + e.getMessage();
        }
    }
}