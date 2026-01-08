package dillauniversity.dao;

import dillauniversity.models.Teacher;
import dillauniversity.models.Student;
import dillauniversity.models.Course;
import dillauniversity.models.Assessment;
import dillauniversity.models.Report;
import dillauniversity.database.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class TeacherDAO {

    private boolean hasColumn(ResultSet rs, String columnLabel) {
        try {
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            for (int i = 1; i <= count; i++) {
                if (columnLabel.equalsIgnoreCase(meta.getColumnLabel(i))) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }

    // Get teacher by username (for login)
    public Teacher getTeacherByUsername(String username) {
        Teacher teacher = null;
        String query = "SELECT t.*, u.username, u.full_name, u.email, u.phone_number " +
                       "FROM teachers t " +
                       "JOIN users u ON t.user_id = u.user_id " +
                       "WHERE u.username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                teacher = new Teacher();
                teacher.setTeacherId(rs.getString("teacher_id"));
                teacher.setUsername(rs.getString("username"));
                teacher.setPassword(rs.getString("password"));
                teacher.setDepartment(rs.getString("department"));
                teacher.setFullName(rs.getString("full_name"));
                teacher.setEmail(rs.getString("email"));
                teacher.setPhone(rs.getString("phone_number"));
                teacher.setQualification(rs.getString("qualification"));
                teacher.setOfficeNumber(rs.getString("office_number"));
                teacher.setOfficeHours(rs.getString("office_hours"));
                teacher.setEmploymentDate(rs.getString("employment_date"));

                // Get profile photo
                if (hasColumn(rs, "profile_photo")) {
                    Blob photoBlob = rs.getBlob("profile_photo");
                    if (photoBlob != null) {
                        teacher.setProfilePhotoBytes(photoBlob.getBytes(1, (int) photoBlob.length()));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teacher;
    }

    // Get courses assigned to a teacher
    public ObservableList<Course> getTeacherCourses(String teacherId) {
        ObservableList<Course> courses = FXCollections.observableArrayList();
        String query = "SELECT DISTINCT c.course_code, c.course_name, c.ects AS credits, c.department " +
                      "FROM courses c " +
                      "INNER JOIN teacher_courses tc ON c.course_code = tc.course_code " +
                      "WHERE tc.teacher_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, teacherId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Course course = new Course(
                    rs.getString("course_code"),
                    rs.getString("course_name"),
                    rs.getInt("credits"),
                    rs.getString("department")
                );
                courses.add(course);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    // Get all courses offered at Dilla University
    public ObservableList<Course> getAllCourses() {
        ObservableList<Course> courses = FXCollections.observableArrayList();
        String query = "SELECT course_code, course_name, ects AS credits, department FROM courses ORDER BY course_code";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                Course course = new Course(
                    rs.getString("course_code"),
                    rs.getString("course_name"),
                    rs.getInt("credits"),
                    rs.getString("department")
                );
                courses.add(course);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    // Get students enrolled in a specific course
    public ObservableList<Student> getStudentsByCourse(String courseCode, String teacherId, String semester, String year) {
        ObservableList<Student> students = FXCollections.observableArrayList();
        // Updated query to use student_courses table and join with users for full name
        // Return the actual student_id from students table, not the numeric ID from student_courses
        String query = "SELECT s.student_id, u.full_name, s.department " +
                      "FROM student_courses sc " +
                      "JOIN students s ON sc.student_id = s.id " +
                      "JOIN users u ON s.user_id = u.user_id " +
                      "JOIN courses c ON sc.course_id = c.course_id " +
                      "WHERE c.course_code = ? AND sc.semester = ? AND sc.academic_year = ? " +
                      "ORDER BY s.student_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, courseCode);
            stmt.setString(2, semester);
            stmt.setString(3, year);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Student student = new Student();
                student.setStudentId(rs.getString("student_id")); // This will be the actual student_id string
                student.setFullName(rs.getString("full_name"));
                student.setDepartment(rs.getString("department"));
                students.add(student);
                
                System.out.println("DEBUG: Loaded student - ID: " + rs.getString("student_id") + 
                                  ", Name: " + rs.getString("full_name") + 
                                  ", Dept: " + rs.getString("department"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading students for course " + courseCode + ": " + e.getMessage());
            e.printStackTrace();
        }
        return students;
    }

    // Submit grade for a student
    public boolean submitGrade(String studentId, String courseCode, String teacherId,
                              String grade, double total, String semester, String year,
                              String comments) {
        String query = "INSERT INTO grades (student_id, course_code, teacher_id, " +
                      "grade, total_score, semester, academic_year, comments, submission_date) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE) " +
                      "ON DUPLICATE KEY UPDATE grade = ?, total_score = ?, " +
                      "comments = ?, submission_date = CURRENT_DATE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, studentId);
            stmt.setString(2, courseCode);
            stmt.setString(3, teacherId);
            stmt.setString(4, grade);
            stmt.setDouble(5, total);
            stmt.setString(6, semester);
            stmt.setString(7, year);
            stmt.setString(8, comments);
            stmt.setString(9, grade);
            stmt.setDouble(10, total);
            stmt.setString(11, comments);

            System.out.println("DEBUG: Submitting grade for student_id: " + studentId + 
                              ", course: " + courseCode + ", semester: " + semester + ", year: " + year);
            
            int result = stmt.executeUpdate();
            System.out.println("DEBUG: Grade submission result: " + result + " rows affected");
            
            return result > 0;
        } catch (SQLException e) {
            System.err.println("ERROR: Failed to submit grade for student " + studentId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Save individual assessment scores
    public boolean saveAssessment(Assessment assessment) {
        String query = "INSERT INTO assessments (student_id, course_code, assessment_type, " +
                      "score, max_score, semester, academic_year, teacher_id, recorded_date) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE) " +
                      "ON DUPLICATE KEY UPDATE score = ?, recorded_date = CURRENT_DATE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, assessment.getStudentId());
            stmt.setString(2, assessment.getCourseCode());
            stmt.setString(3, assessment.getAssessmentType());
            stmt.setDouble(4, assessment.getScore());
            stmt.setDouble(5, assessment.getMaxScore());
            stmt.setString(6, assessment.getSemester());
            stmt.setString(7, assessment.getAcademicYear());
            stmt.setString(8, assessment.getTeacherId());
            stmt.setDouble(9, assessment.getScore());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get existing assessments for a student
    public Map<String, Double> getExistingAssessments(String studentId, String courseCode, 
                                                     String semester, String year) {
        Map<String, Double> assessments = new HashMap<>();
        String query = "SELECT assessment_type, score FROM assessments " +
                      "WHERE student_id = ? AND course_code = ? " +
                      "AND semester = ? AND academic_year = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, studentId);
            stmt.setString(2, courseCode);
            stmt.setString(3, semester);
            stmt.setString(4, year);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                assessments.put(rs.getString("assessment_type"), rs.getDouble("score"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return assessments;
    }

    // Get existing grade for a student
    public Map<String, Object> getExistingGrade(String studentId, String courseCode, 
                                               String semester, String year) {
        Map<String, Object> gradeInfo = new HashMap<>();
        String query = "SELECT grade, total_score, comments FROM grades " +
                      "WHERE student_id = ? AND course_code = ? " +
                      "AND semester = ? AND academic_year = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, studentId);
            stmt.setString(2, courseCode);
            stmt.setString(3, semester);
            stmt.setString(4, year);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                gradeInfo.put("grade", rs.getString("grade"));
                gradeInfo.put("total", rs.getDouble("total_score"));
                gradeInfo.put("comments", rs.getString("comments"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return gradeInfo;
    }

    // Submit report to dean
    public boolean submitReport(String teacherId, String teacherName, String department,
                               String reportType, String title, String description) {
        String query = "INSERT INTO dean_reports (teacher_id, teacher_name, department, " +
                      "report_type, title, description, status, submission_date) " +
                      "VALUES (?, ?, ?, ?, ?, ?, 'Pending', CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, teacherId);
            stmt.setString(2, teacherName);
            stmt.setString(3, department);
            stmt.setString(4, reportType);
            stmt.setString(5, title);
            stmt.setString(6, description);

            int result = stmt.executeUpdate();
            System.out.println("DEBUG: Report submission result: " + result + " rows affected");
            
            if (result > 0) {
                // Verify the report was actually inserted
                String verifyQuery = "SELECT COUNT(*) FROM dean_reports WHERE teacher_id = ? AND title = ? AND submission_date >= NOW() - INTERVAL 1 MINUTE";
                try (PreparedStatement verifyStmt = conn.prepareStatement(verifyQuery)) {
                    verifyStmt.setString(1, teacherId);
                    verifyStmt.setString(2, title);
                    ResultSet rs = verifyStmt.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        System.out.println("DEBUG: Report verification successful - found in database");
                        return true;
                    } else {
                        System.out.println("DEBUG: Report verification failed - not found in database");
                        return false;
                    }
                }
            }
            return false;
        } catch (SQLException e) {
            System.err.println("ERROR: Failed to submit report: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Get teacher's previous reports
    public ObservableList<Report> getTeacherReports(String teacherId) {
        ObservableList<Report> reports = FXCollections.observableArrayList();
        String query = "SELECT DATE_FORMAT(submission_date, '%Y-%m-%d') as report_date, " +
                      "report_type, title, status, " +
                      "COALESCE(dean_response, 'No response yet') as response " +
                      "FROM dean_reports WHERE teacher_id = ? " +
                      "ORDER BY submission_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, teacherId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Report report = new Report(
                    rs.getString("report_date"),
                    rs.getString("report_type"),
                    rs.getString("title"),
                    rs.getString("status"),
                    rs.getString("response")
                );
                reports.add(report);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reports;
    }

    // Update teacher profile
    public boolean updateTeacherProfile(Teacher teacher) {
        String query = "UPDATE teachers SET first_name = ?, last_name = ?, email = ?, " +
                      "phone = ?, qualification = ?, office_number = ?, office_hours = ? " +
                      "WHERE teacher_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, teacher.getFirstName());
            stmt.setString(2, teacher.getLastName());
            stmt.setString(3, teacher.getEmail());
            stmt.setString(4, teacher.getPhone());
            stmt.setString(5, teacher.getQualification());
            stmt.setString(6, teacher.getOfficeNumber());
            stmt.setString(7, teacher.getOfficeHours());
            stmt.setString(8, teacher.getTeacherId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Update teacher profile with photo
    public boolean updateTeacherProfileWithPhoto(Teacher teacher) {
        String query = "UPDATE teachers SET first_name = ?, last_name = ?, email = ?, " +
                      "phone = ?, qualification = ?, office_number = ?, office_hours = ?, " +
                      "profile_photo = ? WHERE teacher_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, teacher.getFirstName());
            stmt.setString(2, teacher.getLastName());
            stmt.setString(3, teacher.getEmail());
            stmt.setString(4, teacher.getPhone());
            stmt.setString(5, teacher.getQualification());
            stmt.setString(6, teacher.getOfficeNumber());
            stmt.setString(7, teacher.getOfficeHours());

            // Handle profile photo
            byte[] photoBytes = teacher.getProfilePhotoBytes();
            if (photoBytes != null && photoBytes.length > 0) {
                stmt.setBytes(8, photoBytes);
            } else {
                stmt.setNull(8, java.sql.Types.BLOB);
            }

            stmt.setString(9, teacher.getTeacherId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get teacher by ID
    public Teacher getTeacherById(String teacherId) {
        Teacher teacher = null;
        String query = "SELECT t.*, u.username, u.full_name, u.email, u.phone_number " +
                       "FROM teachers t " +
                       "JOIN users u ON t.user_id = u.user_id " +
                       "WHERE t.teacher_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, teacherId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                teacher = new Teacher();
                teacher.setTeacherId(rs.getString("teacher_id"));
                teacher.setUsername(rs.getString("username"));
                teacher.setPassword(rs.getString("password"));
                teacher.setDepartment(rs.getString("department"));
                teacher.setFullName(rs.getString("full_name"));
                teacher.setEmail(rs.getString("email"));
                teacher.setPhone(rs.getString("phone_number"));
                teacher.setQualification(rs.getString("qualification"));
                teacher.setOfficeNumber(rs.getString("office_number"));
                teacher.setOfficeHours(rs.getString("office_hours"));
                teacher.setEmploymentDate(rs.getString("employment_date"));

                if (hasColumn(rs, "profile_photo")) {
                    Blob photoBlob = rs.getBlob("profile_photo");
                    if (photoBlob != null) {
                        teacher.setProfilePhotoBytes(photoBlob.getBytes(1, (int) photoBlob.length()));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teacher;
    }

    // Check if grade already exists
    public boolean gradeExists(String studentId, String courseCode, String semester, String year) {
        String query = "SELECT COUNT(*) as count FROM grades " +
                      "WHERE student_id = ? AND course_code = ? " +
                      "AND semester = ? AND academic_year = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, studentId);
            stmt.setString(2, courseCode);
            stmt.setString(3, semester);
            stmt.setString(4, year);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Get course statistics
    public Map<String, Integer> getCourseStatistics(String teacherId) {
        Map<String, Integer> stats = new HashMap<>();
        String query = "SELECT COUNT(DISTINCT course_code) as course_count " +
                      "FROM teacher_courses WHERE teacher_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, teacherId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                stats.put("courseCount", rs.getInt("course_count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats;
    }
    
    // Get student count for a course
    public int getStudentCountForCourse(String courseCode, String teacherId, String semester, String year) {
        String query = "SELECT COUNT(*) as student_count FROM student_courses " +
                      "WHERE course_code = ? AND teacher_id = ? " +
                      "AND semester = ? AND academic_year = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, courseCode);
            stmt.setString(2, teacherId);
            stmt.setString(3, semester);
            stmt.setString(4, year);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("student_count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    // Get all semesters for a teacher
    public ObservableList<String> getTeacherSemesters(String teacherId) {
        ObservableList<String> semesters = FXCollections.observableArrayList();
        String query = "SELECT DISTINCT semester FROM teacher_courses " +
                      "WHERE teacher_id = ? ORDER BY semester DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, teacherId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                semesters.add(rs.getString("semester"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return semesters;
    }
    
    // Get all academic years for a teacher
    public ObservableList<String> getTeacherAcademicYears(String teacherId) {
        ObservableList<String> years = FXCollections.observableArrayList();
        String query = "SELECT DISTINCT academic_year FROM teacher_courses " +
                      "WHERE teacher_id = ? ORDER BY academic_year DESC";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, teacherId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                years.add(rs.getString("academic_year"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return years;
    }
    
    // Verify teacher credentials
    public boolean verifyTeacherCredentials(String username, String password) {
        String query = "SELECT COUNT(*) as count " +
                      "FROM teachers t " +
                      "JOIN users u ON t.user_id = u.user_id " +
                      "WHERE u.username = ? AND t.password = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}