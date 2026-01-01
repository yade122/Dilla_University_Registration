package dillauniversity.dao;

import dillauniversity.database.DatabaseConnection;
import dillauniversity.models.Dean;
import dillauniversity.models.Student;
import java.sql.*;
import java.time.LocalDate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DeanDAO {

    public Dean getDeanByUsername(String username) {
        Dean dean = new Dean();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            // Fixed query: Removed join with 'departments' table as it doesn't exist.
            // Assuming d.department stores the department name directly.
            String query = "SELECT d.*, u.full_name, u.email, u.phone_number as phone, u.date_of_birth, u.profile_photo " +
                    "FROM deans d " +
                    "JOIN users u ON d.user_id = u.user_id " +
                    "WHERE u.username = ?";

            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                dean.setDeanId(rs.getString("dean_id"));
                dean.setUsername(username);
                dean.setFullName(rs.getString("full_name"));
                dean.setEmail(rs.getString("email"));
                dean.setPhone(rs.getString("phone"));
                dean.setDepartmentCode(rs.getString("department")); // Using department column as code/name
                dean.setDepartmentName(rs.getString("department")); // Using department column as name
                dean.setAppointmentDate(rs.getDate("appointment_date").toLocalDate());
                dean.setOfficeLocation(rs.getString("office_location"));
                dean.setDeanType(rs.getString("dean_type"));

                // Store profile photo bytes if available
                byte[] photoBytes = rs.getBytes("profile_photo");
                if (photoBytes != null) {
                    dean.setProfilePhotoBytes(photoBytes);
                }

                System.out.println("✅ Dean data loaded for: " + username);
            } else {
                System.out.println("⚠️ No dean found with username: " + username);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error loading dean data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, conn);
        }
        return dean;
    }

    public DepartmentStats getDepartmentStatistics(String department) {
        DepartmentStats stats = new DepartmentStats();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            // Total Students
            String studentQuery = "SELECT COUNT(*) FROM students WHERE department = ?";
            pstmt = conn.prepareStatement(studentQuery);
            pstmt.setString(1, department);
            rs = pstmt.executeQuery();
            if (rs.next()) stats.setTotalStudents(rs.getInt(1));
            rs.close();
            pstmt.close();

            // Total Teachers
            String teacherQuery = "SELECT COUNT(*) FROM teachers WHERE department = ?";
            pstmt = conn.prepareStatement(teacherQuery);
            pstmt.setString(1, department);
            rs = pstmt.executeQuery();
            if (rs.next()) stats.setTotalTeachers(rs.getInt(1));
            rs.close();
            pstmt.close();

            // Average GPA (mock calculation from grades)
            // In a real system, you'd calculate this from student_grades
            String gpaQuery = "SELECT AVG(marks) FROM student_grades sg " +
                    "JOIN courses c ON sg.course_code = c.course_code " +
                    "WHERE c.department = ?";
            pstmt = conn.prepareStatement(gpaQuery);
            pstmt.setString(1, department);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                double avgMarks = rs.getDouble(1);
                // Convert marks to GPA roughly (just for demo)
                stats.setAverageGPA(avgMarks > 0 ? (avgMarks / 25.0) : 0.0);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, conn);
        }
        return stats;
    }

    public ObservableList<Student> getStudentsInDepartment(String department) {
        ObservableList<Student> students = FXCollections.observableArrayList();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            String query = "SELECT s.*, u.full_name, u.email, u.phone_number as phone " +
                    "FROM students s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "WHERE s.department = ?";

            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, department);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Student student = new Student();
                student.setStudentId(rs.getString("student_id"));
                student.setFullName(rs.getString("full_name"));
                student.setRegistrationNumber(rs.getString("registration_number"));
                student.setDepartment(rs.getString("department"));
                student.setAcademicYear(rs.getString("academic_year"));
                // student.setYearOfStudy(rs.getInt("year")); // Assuming year column exists or parsed from academic_year
                student.setEmail(rs.getString("email"));
                student.setPhone(rs.getString("phone"));
                students.add(student);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, conn);
        }
        return students;
    }

    public ObservableList<Student> searchStudents(String department, String searchTerm) {
        ObservableList<Student> students = FXCollections.observableArrayList();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            String query = "SELECT s.*, u.full_name, u.email, u.phone_number as phone " +
                    "FROM students s " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "WHERE s.department = ? AND (u.full_name LIKE ? OR s.student_id LIKE ? OR s.registration_number LIKE ?)";

            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, department);
            String searchPattern = "%" + searchTerm + "%";
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                Student student = new Student();
                student.setStudentId(rs.getString("student_id"));
                student.setFullName(rs.getString("full_name"));
                student.setRegistrationNumber(rs.getString("registration_number"));
                student.setDepartment(rs.getString("department"));
                students.add(student);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, conn);
        }
        return students;
    }

    public boolean removeStudentFromDepartment(String studentId, String reason, String deanId) {
        // In a real system, you might archive the student or update status
        // For now, we'll just delete from students table (cascade will handle user?)
        // Actually, better to just delete from students table, keeping user record maybe?
        // Or delete user record too.

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            // Get user_id first
            String getUserId = "SELECT user_id FROM students WHERE student_id = ?";
            pstmt = conn.prepareStatement(getUserId);
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                rs.close();
                pstmt.close();

                // Delete user (cascade should handle student record)
                String deleteUser = "DELETE FROM users WHERE user_id = ?";
                pstmt = conn.prepareStatement(deleteUser);
                pstmt.setInt(1, userId);
                return pstmt.executeUpdate() > 0;
            }
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            closeResources(null, pstmt, conn);
        }
    }

    public ObservableList<Grade> getDepartmentGrades(String department, String semester, String year) {
        ObservableList<Grade> grades = FXCollections.observableArrayList();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            String query = "SELECT sg.*, u.full_name as student_name, c.course_name, t_u.full_name as teacher_name " +
                    "FROM student_grades sg " +
                    "JOIN students s ON sg.student_id = s.student_id " +
                    "JOIN users u ON s.user_id = u.user_id " +
                    "JOIN courses c ON sg.course_code = c.course_code " +
                    "JOIN teachers t ON sg.teacher_id = t.teacher_id " +
                    "JOIN users t_u ON t.user_id = t_u.user_id " +
                    "WHERE c.department = ? AND sg.semester = ? AND sg.academic_year = ?";

            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, department);
            pstmt.setString(2, semester);
            pstmt.setString(3, year);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                grades.add(new Grade(
                        rs.getString("student_id"),
                        rs.getString("student_name"),
                        rs.getString("course_name"),
                        rs.getString("grade"),
                        rs.getDouble("marks"),
                        rs.getString("teacher_name"),
                        "Pending" // Approval status placeholder
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, conn);
        }
        return grades;
    }

    public ObservableList<Schedule> getDepartmentSchedules(String department, LocalDate start, LocalDate end) {
        // Placeholder for schedule logic
        return FXCollections.observableArrayList();
    }

    // NEW: Method to get reports for the dean's department
    public ObservableList<String[]> getDepartmentReports(String department) {
        ObservableList<String[]> reports = FXCollections.observableArrayList();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            // Teacher dashboard submits reports into dean_reports.
            // Filter by department so deans see only their department's reports.
            String query = "SELECT DATE_FORMAT(submission_date, '%Y-%m-%d') as report_date, " +
                    "report_type, title as report_title, teacher_name, status " +
                    "FROM dean_reports " +
                    "WHERE department = ? " +
                    "ORDER BY submission_date DESC";

            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, department);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String[] report = {
                        rs.getString("report_date"),
                        rs.getString("report_type"),
                        rs.getString("report_title"),
                        rs.getString("teacher_name"),
                        rs.getString("status")
                };
                reports.add(report);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error loading department reports: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(rs, pstmt, conn);
        }
        return reports;
    }

    private void closeResources(ResultSet rs, PreparedStatement pstmt, Connection conn) {
        try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Inner classes for data models
    public static class DepartmentStats {
        private int totalStudents;
        private int totalTeachers;
        private double averageGPA;
        private int pendingApprovals;

        // Getters and setters
        public int getTotalStudents() { return totalStudents; }
        public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }
        public int getTotalTeachers() { return totalTeachers; }
        public void setTotalTeachers(int totalTeachers) { this.totalTeachers = totalTeachers; }
        public double getAverageGPA() { return averageGPA; }
        public void setAverageGPA(double averageGPA) { this.averageGPA = averageGPA; }
        public int getPendingApprovals() { return pendingApprovals; }
        public void setPendingApprovals(int pendingApprovals) { this.pendingApprovals = pendingApprovals; }
    }

    public static class Grade {
        private String studentId;
        private String studentName;
        private String course;
        private String grade;
        private double totalMarks;
        private String submittedBy;
        private String approvalStatus;

        public Grade(String studentId, String studentName, String course, String grade, double totalMarks, String submittedBy, String approvalStatus) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.course = course;
            this.grade = grade;
            this.totalMarks = totalMarks;
            this.submittedBy = submittedBy;
            this.approvalStatus = approvalStatus;
        }

        // Getters for TableView
        public String getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getCourse() { return course; }
        public String getGrade() { return grade; }
        public double getTotalMarks() { return totalMarks; }
        public String getSubmittedBy() { return submittedBy; }
        public String getApprovalStatus() { return approvalStatus; }
    }

    public static class Schedule {
        // Placeholder
    }
}
