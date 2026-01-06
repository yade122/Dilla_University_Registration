package dillauniversity;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import dillauniversity.database.DatabaseConnection;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Test database connection first
            testDatabaseConnection();

            // Set up database tables
            setupDatabase();

            // Seed database with courses and assign to teacher
            seedDatabase();

            // Load login FXML - Use absolute path from classpath root
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/dillauniversity/resources/login.fxml"));

            // Check if loader found the resource
            if (loader.getLocation() == null) {
                System.err.println("FXML file not found at: /dillauniversity/resources/login.fxml");
                System.err.println("Available resources:");
                try {
                    java.util.Enumeration<java.net.URL> resources = Main.class.getClassLoader().getResources("");
                    while (resources.hasMoreElements()) {
                        System.err.println("  " + resources.nextElement());
                    }
                } catch (Exception e) {
                    System.err.println("Could not list resources: " + e.getMessage());
                }
                throw new IOException("Could not find FXML file: /dillauniversity/resources/login.fxml");
            }

            Parent root = loader.load();

            // Create scene
            Scene scene = new Scene(root, 1000, 700);

            // Setup stage
            primaryStage.setTitle("Dilla University - Registration System");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMaximized(true); // Start maximized
            primaryStage.show();

        } catch (IOException e) {
            showError("Application Error", "Failed to start application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void testDatabaseConnection() {
        try {
            DatabaseConnection.getConnection();
            System.out.println("Database connection established successfully!");
        } catch (SQLException e) {
            showError("Database Error",
                    """
                    Cannot connect to database!
                    
                    Please ensure:
                    1. MySQL Server is running
                    2. Database 'dilla_university' exists
                    3. Correct credentials in DatabaseConnection.java
                    
                    Error: """ + e.getMessage());
            System.exit(1);
        }
    }

    private void setupDatabase() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("Checking database schema...");

            // Fix 'users' table role column if it's an ENUM that doesn't include 'dean'
            try {
                // This will convert it to VARCHAR(20) to be safe and flexible
                stmt.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL");
                System.out.println("Updated 'users' table role column to VARCHAR(20).");
            } catch (SQLException e) {
                System.out.println("Could not modify users role column (might already be correct): " + e.getMessage());
            }

            // 1. Fix 'students' table if 'id' column is missing
            boolean hasId = false;
            try (ResultSet rs = conn.getMetaData().getColumns(null, null, "students", "id")) {
                if (rs.next()) hasId = true;
            }

            if (!hasId) {
                System.out.println("Adding 'id' column to students table...");
                try {
                    // Try adding as Primary Key
                    stmt.execute("ALTER TABLE students ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST");
                    System.out.println("Added 'id' as PRIMARY KEY to students table.");
                } catch (SQLException e) {
                    // If PK exists, add as unique column
                    System.out.println("Could not add as PK (maybe one exists), trying as unique column: " + e.getMessage());
                    try {
                        stmt.execute("ALTER TABLE students ADD COLUMN id INT AUTO_INCREMENT UNIQUE FIRST");
                        System.out.println("Added 'id' as UNIQUE column to students table.");
                    } catch (SQLException ex) {
                        System.err.println("Failed to add 'id' column: " + ex.getMessage());
                    }
                }
            }

            // 2. Create 'courses' table if not exists
            String createCoursesTable = "CREATE TABLE IF NOT EXISTS courses (" +
                    "  course_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "  course_code VARCHAR(20) UNIQUE NOT NULL," +
                    "  course_name VARCHAR(100) NOT NULL," +
                    "  ects INT," +
                    "  credit_hours INT," +
                    "  lecture_hours INT," +
                    "  lab_hours INT," +
                    "  tutorial_hours INT," +
                    "  department VARCHAR(100)," +
                    "  year INT," +
                    "  semester VARCHAR(50)," +
                    "  prerequisite VARCHAR(50)" +
                    ");";
            stmt.execute(createCoursesTable);
            System.out.println("Table 'courses' checked/created.");

            // 3. Create 'student_courses' table
            String createStudentCoursesTable = "CREATE TABLE IF NOT EXISTS student_courses (" +
                    "  id INT AUTO_INCREMENT PRIMARY KEY," +
                    "  student_id INT NOT NULL," +
                    "  course_id INT NOT NULL," +
                    "  semester VARCHAR(50) NOT NULL," +
                    "  academic_year VARCHAR(20) NOT NULL," +
                    "  grade VARCHAR(5)," +
                    "  marks DOUBLE," +
                    "  registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  UNIQUE KEY unique_registration (student_id, course_id, academic_year, semester)," +
                    "  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE," +
                    "  FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createStudentCoursesTable);
            System.out.println("Table 'student_courses' checked/created.");

            // 4. Create 'teacher_courses' table if not exists
            String createTeacherCoursesTable = "CREATE TABLE IF NOT EXISTS teacher_courses (" +
                    "  id INT AUTO_INCREMENT PRIMARY KEY," +
                    "  teacher_id VARCHAR(20) NOT NULL," +
                    "  course_code VARCHAR(20) NOT NULL," +
                    "  semester VARCHAR(50) NOT NULL," +
                    "  academic_year VARCHAR(20) NOT NULL," +
                    "  UNIQUE KEY unique_assignment (teacher_id, course_code, academic_year, semester)," +
                    "  FOREIGN KEY (course_code) REFERENCES courses(course_code) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createTeacherCoursesTable);
            System.out.println("Table 'teacher_courses' checked/created.");

            // 5. Create 'student_grades' table if not exists
            // Added mid_exam, project, final_exam columns
            String createStudentGradesTable = "CREATE TABLE IF NOT EXISTS student_grades (" +
                    "  id INT AUTO_INCREMENT PRIMARY KEY," +
                    "  student_id VARCHAR(20) NOT NULL," +
                    "  course_code VARCHAR(20) NOT NULL," +
                    "  teacher_id VARCHAR(20) NOT NULL," +
                    "  grade VARCHAR(5)," +
                    "  marks DOUBLE," +
                    "  mid_exam DOUBLE DEFAULT 0," +
                    "  project DOUBLE DEFAULT 0," +
                    "  final_exam DOUBLE DEFAULT 0," +
                    "  semester VARCHAR(50)," +
                    "  academic_year VARCHAR(20)," +
                    "  comments TEXT," +
                    "  grade_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  UNIQUE KEY unique_grade (student_id, course_code, academic_year, semester)" +
                    ");";
            stmt.execute(createStudentGradesTable);

            // Add columns if they don't exist (for existing tables)
            try {
                stmt.execute("ALTER TABLE student_grades ADD COLUMN mid_exam DOUBLE DEFAULT 0");
            } catch (SQLException e) { /* Column might exist */ }
            try {
                stmt.execute("ALTER TABLE student_grades ADD COLUMN project DOUBLE DEFAULT 0");
            } catch (SQLException e) { /* Column might exist */ }
            try {
                stmt.execute("ALTER TABLE student_grades ADD COLUMN final_exam DOUBLE DEFAULT 0");
            } catch (SQLException e) { /* Column might exist */ }

            System.out.println("Table 'student_grades' checked/created.");

            // 6. Create 'teacher_reports' table if not exists
            String createTeacherReportsTable = "CREATE TABLE IF NOT EXISTS teacher_reports (" +
                    "  report_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "  teacher_id VARCHAR(20) NOT NULL," +
                    "  report_type VARCHAR(50) NOT NULL," +
                    "  report_title VARCHAR(100) NOT NULL," +
                    "  report_description TEXT," +
                    "  status VARCHAR(20) DEFAULT 'Pending'," +
                    "  admin_response TEXT," +
                    "  report_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");";
            stmt.execute(createTeacherReportsTable);
            System.out.println("Table 'teacher_reports' checked/created.");

            // 7. Create 'deans' table if not exists (NEW)
            String createDeansTable = "CREATE TABLE IF NOT EXISTS deans (" +
                    "  id INT AUTO_INCREMENT PRIMARY KEY," +
                    "  user_id INT NOT NULL," +
                    "  dean_id VARCHAR(20) UNIQUE NOT NULL," +
                    "  department VARCHAR(100) NOT NULL," +
                    "  appointment_date DATE," +
                    "  office_location VARCHAR(100)," +
                    "  dean_type VARCHAR(50)," +
                    "  password VARCHAR(255)," +
                    "  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createDeansTable);
            System.out.println("Table 'deans' checked/created.");

        } catch (SQLException e) {
            showError("Database Setup Error", "Failed to setup database schema: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void seedDatabase() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 0. Ensure users exist (Teacher 'dani', Dean 'dean', Admin 'admin')
            String hashedPassword = hashPassword("1234");

            // Insert/Update Teacher 'dani'
            String upsertUserSQL = "INSERT INTO users (username, password, email, full_name, role, date_of_birth, gender, phone_number, region, zone, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, '1980-01-01', 'Male', '+251911000000', 'Addis Ababa', 'Bole', NOW()) " +
                    "ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role)";
            PreparedStatement userStmt = conn.prepareStatement(upsertUserSQL, Statement.RETURN_GENERATED_KEYS);

            // Teacher
            userStmt.setString(1, "dani");
            userStmt.setString(2, hashedPassword);
            userStmt.setString(3, "dani@dilla.edu.et");
            userStmt.setString(4, "Daniel Teacher");
            userStmt.setString(5, "teacher");
            userStmt.executeUpdate();

            // Get user_id for dani
            int daniUserId = getUserId(conn, "dani");
            if (daniUserId > 0) {
                String upsertTeacherSQL = "INSERT INTO teachers (user_id, teacher_id, department, employee_id, qualification, password, employment_date) VALUES (?, ?, ?, ?, ?, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE department = VALUES(department)";
                PreparedStatement teacherStmt = conn.prepareStatement(upsertTeacherSQL);
                teacherStmt.setInt(1, daniUserId);
                teacherStmt.setString(2, "T-001");
                teacherStmt.setString(3, "Computer Science");
                teacherStmt.setString(4, "EMP-001");
                teacherStmt.setString(5, "MSc in Computer Science");
                teacherStmt.setString(6, hashedPassword);
                teacherStmt.executeUpdate();
            }

            // Dean
            userStmt.setString(1, "dean");
            userStmt.setString(2, hashedPassword);
            userStmt.setString(3, "dean@dilla.edu.et");
            userStmt.setString(4, "Dean Smith");
            userStmt.setString(5, "dean");
            userStmt.executeUpdate();

            // Admin
            userStmt.setString(1, "admin");
            userStmt.setString(2, hashedPassword);
            userStmt.setString(3, "admin@dilla.edu.et");
            userStmt.setString(4, "System Admin");
            userStmt.setString(5, "admin");
            userStmt.executeUpdate();

            // --- SEED STUDENTS ---
            // Create 5 sample students
            for (int i = 1; i <= 5; i++) {
                String username = "student" + i;
                userStmt.setString(1, username);
                userStmt.setString(2, hashedPassword);
                userStmt.setString(3, username + "@dilla.edu.et");
                userStmt.setString(4, "Student " + i);
                userStmt.setString(5, "student");
                userStmt.executeUpdate();

                int studentUserId = getUserId(conn, username);
                if (studentUserId > 0) {
                    String upsertStudentSQL = "INSERT INTO students (user_id, student_id, registration_number, department, academic_year, registration_date, password) " +
                            "VALUES (?, ?, ?, ?, ?, NOW(), ?) " +
                            "ON DUPLICATE KEY UPDATE department = VALUES(department)";
                    PreparedStatement studentStmt = conn.prepareStatement(upsertStudentSQL);
                    studentStmt.setInt(1, studentUserId);
                    studentStmt.setString(2, "DU-2024-" + String.format("%03d", i)); // student_id (string)
                    studentStmt.setString(3, "REG-" + String.format("%03d", i));
                    studentStmt.setString(4, "Computer Science");
                    studentStmt.setString(5, "2024");
                    studentStmt.setString(6, hashedPassword);
                    studentStmt.executeUpdate();
                }
            }

            // 1. Insert Courses
            String insertCourseSQL = "INSERT INTO courses (course_code, course_name, ects, credit_hours, lecture_hours, lab_hours, tutorial_hours, department, year, semester, prerequisite) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE course_name = VALUES(course_name), ects = VALUES(ects), credit_hours = VALUES(credit_hours), " +
                    "lecture_hours = VALUES(lecture_hours), lab_hours = VALUES(lab_hours), tutorial_hours = VALUES(tutorial_hours), " +
                    "department = VALUES(department), year = VALUES(year), semester = VALUES(semester), prerequisite = VALUES(prerequisite)";
            PreparedStatement pstmt = conn.prepareStatement(insertCourseSQL);

            // List of courses to insert
            Object[][] courses = {
                    {"EmTe1012", "Introduction to Emerging Technologies", 5, 3, 2, 3, 0, "Computer Science", 1, "Semester 1", "None"},
                    {"CoSc2064", "Computer Programming", 5, 3, 2, 3, 0, "Computer Science", 1, "Semester 2", "None"},
                    {"CoSc2011", "Computer Programming II", 5, 3, 2, 3, 2, "Computer Science", 2, "Semester 1", "CoSc2064"},
                    {"CoSc2022", "Computer Organization and Architecture", 5, 3, 2, 3, 2, "Computer Science", 2, "Semester 2", "CoSc2021"},
                    {"CoSc3025", "Microprocessor and Assembly Language Programming", 5, 3, 2, 3, 1, "Computer Science", 3, "Semester 1", "CoSc2011"},
                    {"CoSc3023", "Operating Systems", 5, 3, 2, 3, 2, "Computer Science", 3, "Semester 1", "CoSc2022"},
                    {"CoSc4021", "Real Time and Embedded Systems", 5, 3, 2, 3, 2, "Computer Science", 4, "Semester 1", "CoSc2022"},
                    {"CoSc2034", "Data Communication and Computer Networks", 5, 3, 2, 3, 2, "Computer Science", 2, "Semester 2", "None"},
                    {"CoSc4036", "Network and System Administration", 5, 3, 2, 3, 1, "Computer Science", 4, "Semester 2", "CoSc2034"},
                    {"CoSc3034", "Wireless Communication and Mobile Computing", 5, 3, 2, 3, 1, "Computer Science", 3, "Semester 2", "CoSc2034"},
                    {"CoSc4031", "Computer Security", 5, 3, 2, 3, 1, "Computer Science", 4, "Semester 1", "CoSc2034"},
                    {"CoSc4038", "Introduction to Distributed Systems", 5, 3, 2, 3, 2, "Computer Science", 4, "Semester 2", "CoSc2034"},
                    {"CoSc2041", "Fundamentals of Database Systems", 5, 3, 2, 3, 2, "Computer Science", 2, "Semester 1", "None"},
                    {"CoSc3045", "Advanced Database Systems", 5, 3, 2, 3, 2, "Computer Science", 3, "Semester 1", "CoSc2041"},
                    {"CoSc2052", "Object Oriented Programming", 5, 3, 2, 3, 2, "Computer Science", 2, "Semester 2", "CoSc2011"},
                    {"CoSc3053", "Java Programming", 5, 3, 2, 3, 2, "Computer Science", 3, "Semester 1", "CoSc2052"},
                    {"CoSc3061", "Software Engineering", 5, 3, 3, 0, 2, "Computer Science", 3, "Semester 1", "None"},
                    {"CoSc3072", "Computer Graphics", 5, 3, 2, 3, 1, "Computer Science", 3, "Semester 2", "CoSc2011"},
                    {"CoSc4113", "Computer Vision and Image Processing", 5, 3, 2, 3, 2, "Computer Science", 4, "Semester 1", "CoSc3072"},
                    {"CoSc3086", "Web Programming", 7, 4, 3, 3, 1, "Computer Science", 3, "Semester 2", "None"},
                    {"CoSc2092", "Data Structures and Algorithms", 5, 3, 2, 3, 2, "Computer Science", 2, "Semester 2", "CoSc2011"},
                    {"CoSc3094", "Design and Analysis of Algorithms", 5, 3, 3, 0, 0, "Computer Science", 3, "Semester 2", "CoSc2092"},
                    {"CoSc3101", "Automata and Complexity Theory", 5, 3, 3, 0, 2, "Computer Science", 3, "Semester 1", "None"},
                    {"CoSc4104", "Compiler Design", 5, 3, 2, 3, 2, "Computer Science", 4, "Semester 2", "CoSc3101"},
                    {"CoSc3112", "Introduction to Artificial Intelligence", 5, 3, 2, 3, 2, "Computer Science", 3, "Semester 2", "CoSc2092"},
                    {"CoSc3122", "Industrial Practice", 3, 2, 0, 0, 0, "Computer Science", 3, "Semester 2", "None"},
                    {"CoSc3128", "Research Methods in Computer Science", 3, 2, 2, 0, 0, "Computer Science", 3, "Semester 2", "None"},
                    {"CoSc4125", "Final Year Project I", 5, 3, 0, 0, 0, "Computer Science", 4, "Semester 1", "CoSc3061"},
                    {"CoSc4126", "Final Year Project II", 5, 3, 0, 0, 0, "Computer Science", 4, "Semester 2", "CoSc4125"},
                    {"CoSc4132", "Selected Topics in Computer Science", 5, 3, 3, 0, 0, "Computer Science", 4, "Semester 2", "None"}
            };

            for (Object[] course : courses) {
                pstmt.setString(1, (String) course[0]);
                pstmt.setString(2, (String) course[1]);
                pstmt.setInt(3, (Integer) course[2]);
                pstmt.setInt(4, (Integer) course[3]);
                pstmt.setInt(5, (Integer) course[4]);
                pstmt.setInt(6, (Integer) course[5]);
                pstmt.setInt(7, (Integer) course[6]);
                pstmt.setString(8, (String) course[7]);
                pstmt.setInt(9, (Integer) course[8]);
                pstmt.setString(10, (String) course[9]);
                pstmt.setString(11, (String) course[10]);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            System.out.println("Courses seeded successfully.");

            // 2. Assign courses to teacher 'dani'
            String getTeacherIdSQL = "SELECT teacher_id FROM teachers t JOIN users u ON t.user_id = u.user_id WHERE u.username = 'dani'";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(getTeacherIdSQL);

            if (rs.next()) {
                String teacherId = rs.getString("teacher_id");
                String assignCourseSQL = "INSERT INTO teacher_courses (teacher_id, course_code, semester, academic_year) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE semester = VALUES(semester), academic_year = VALUES(academic_year)";
                PreparedStatement assignStmt = conn.prepareStatement(assignCourseSQL);

                for (Object[] course : courses) {
                    assignStmt.setString(1, teacherId);
                    assignStmt.setString(2, (String) course[0]);
                    assignStmt.setString(3, (String) course[9]); // Semester
                    assignStmt.setString(4, "2024");
                    assignStmt.addBatch();
                }
                assignStmt.executeBatch();
                System.out.println("Courses assigned to teacher 'dani'.");

                // --- REGISTER STUDENTS FOR COURSES ---
                // Register the 5 sample students for the first 5 courses
                String getStudentIdSQL = "SELECT id, student_id FROM students";
                ResultSet studentRs = stmt.executeQuery(getStudentIdSQL);

                String registerStudentSQL = "INSERT INTO student_courses (student_id, course_id, semester, academic_year) " +
                        "SELECT ?, course_id, ?, ? FROM courses WHERE course_code = ? " +
                        "ON DUPLICATE KEY UPDATE semester = VALUES(semester)";
                PreparedStatement regStmt = conn.prepareStatement(registerStudentSQL);

                while (studentRs.next()) {
                    int studentPk = studentRs.getInt("id");
                    // Register for first 5 courses
                    for (int i = 0; i < 5; i++) {
                        regStmt.setInt(1, studentPk); // Use integer PK for student_courses foreign key
                        regStmt.setString(2, (String) courses[i][9]); // Semester
                        regStmt.setString(3, "2024");
                        regStmt.setString(4, (String) courses[i][0]); // Course Code
                        regStmt.addBatch();
                    }
                }
                regStmt.executeBatch();
                System.out.println("Students registered for courses.");

            } else {
                System.out.println("Teacher 'dani' not found. Skipping course assignment.");
            }

        } catch (SQLException e) {
            System.err.println("Error seeding database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getUserId(Connection conn, String username) throws SQLException {
        String sql = "SELECT user_id FROM users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("user_id");
            }
        }
        return -1;
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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

