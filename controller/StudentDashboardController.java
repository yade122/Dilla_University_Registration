package dillauniversity.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.scene.layout.VBox;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.cell.CheckBoxTableCell;

import java.io.ByteArrayInputStream;
import java.sql.*;
import dillauniversity.database.DatabaseConnection;

public class StudentDashboardController {
    
    // Navigation Buttons
    @FXML private Button registrationButton;
    @FXML private Button gradeViewButton;
    @FXML private Button profileButton;
    @FXML private Button logoutButton;
    
    // Welcome Labels
    @FXML private Label welcomeLabel;
    @FXML private Label fullNameLabel;
    @FXML private Label idLabel;
    @FXML private Label departmentLabel;
    @FXML private Label yearLabel;
    @FXML private ImageView profileImageView;
    
    // Sections
    @FXML private VBox welcomeSection;
    @FXML private VBox registrationSection;
    @FXML private VBox gradesSection;
    @FXML private VBox profileSection;
    
    // Registration Section Components
    @FXML private ComboBox<String> deptComboBox;
    @FXML private ComboBox<String> academicYearComboBox;
    @FXML private ComboBox<String> semesterComboBox;
    @FXML private TableView<Course> coursesTable;
    @FXML private Button registerCoursesButton;
    @FXML private VBox registrationMessageBox;
    @FXML private Label registrationMessageLabel;
    @FXML private Label registrationMessageIcon;
    @FXML private CheckBox selectAllCheckBox;
    @FXML private Label totalCreditsLabel;
    
    // Grades Section Components
    @FXML private TableView<Grade> gradesTable;
    @FXML private ComboBox<String> gradeSemesterFilter;
    
    // Profile Section Components
    @FXML private Label profileFullName;
    @FXML private Label profileStudentId;
    @FXML private Label profileEmail;
    @FXML private Label profilePhone;
    @FXML private Label profileDob;
    @FXML private Label profileDepartment;
    @FXML private Label profileYear;
    @FXML private Label profileSemester;
    @FXML private Label profileRegDate;
    @FXML private Label profileCGPA;
    
    private String username;
    private int studentId; // Integer primary key from 'students' table
    private int studentYear; // Store as integer (1-5)
    private String currentDepartment;
    private String currentYear;
    private int registeredCredits = 0;
    
    // Course data
    private ObservableList<Course> coursesList = FXCollections.observableArrayList();
    private ObservableList<Grade> gradesList = FXCollections.observableArrayList();
    
    // Maximum allowed credit hours
    private static final int MAX_CREDIT_HOURS = 21;
    
    public void setUsername(String username) {
        this.username = username;
        loadStudentDataFromDatabase();
    }
    
    @FXML
    public void initialize() {
        System.out.println("Initializing StudentDashboardController...");

        // Set up navigation buttons (guard against null injection so initialize can't crash)
        if (registrationButton != null) {
            registrationButton.setOnAction(e -> showRegistrationSection());
        } else {
            System.err.println("registrationButton is null - check fx:id wiring in StudentDashboard.fxml");
        }
        if (gradeViewButton != null) {
            gradeViewButton.setOnAction(e -> showGradesSection());
        } else {
            System.err.println("gradeViewButton is null - check fx:id wiring in StudentDashboard.fxml");
        }
        if (profileButton != null) {
            profileButton.setOnAction(e -> showProfileSection());
        } else {
            System.err.println("profileButton is null - check fx:id wiring in StudentDashboard.fxml");
        }
        if (logoutButton != null) {
            logoutButton.setOnAction(e -> handleLogout());
        } else {
            System.err.println("logoutButton is null - check fx:id wiring in StudentDashboard.fxml");
        }

        populateComboBoxes();

        if (registerCoursesButton != null) {
            registerCoursesButton.setOnAction(e -> registerCoursesToDatabase());
        } else {
            System.err.println("registerCoursesButton is null - check fx:id wiring in StudentDashboard.fxml");
        }
        if (selectAllCheckBox != null) {
            selectAllCheckBox.setOnAction(e -> handleSelectAll());
        } else {
            System.err.println("selectAllCheckBox is null - check fx:id wiring in StudentDashboard.fxml");
        }

        // Set up combo box listeners to reload courses
        if (academicYearComboBox != null) {
            academicYearComboBox.valueProperty().addListener((obs, oldVal, newVal) -> loadCoursesFromDatabase());
        }
        if (semesterComboBox != null) {
            semesterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> loadCoursesFromDatabase());
        }

        setupCoursesTable();
        setupGradesTable();

        hideAllSections();
        if (welcomeSection != null) {
            welcomeSection.setVisible(true);
            welcomeSection.setManaged(true);
        }

        if (registrationMessageBox != null) {
            registrationMessageBox.setVisible(false);
            registrationMessageBox.setManaged(false);
        }

        if (totalCreditsLabel != null) {
            totalCreditsLabel.setText("Total Credits: 0 / " + MAX_CREDIT_HOURS);
        }
    }
    
    private void loadStudentDataFromDatabase() {
        // Use a JOIN to get all data in one query, querying by username
        // Note: 'full_name' is in 'users' table, not 'students' table.
        // But if the column name is ambiguous or not found, we should be careful.
        // The error says "Column 'full_name' not found".
        // In the query: SELECT s.*, u.full_name, ... FROM students s JOIN users u ...
        // If 'full_name' is not in 'users' table, that's the issue.
        // Let's check RegistrationController. It inserts into 'users' table with 'full_name'.
        // So 'full_name' should be in 'users'.
        
        // However, maybe the ResultSet is confused because of s.* if s also has full_name?
        // Or maybe the column alias is needed.
        
        String query = "SELECT s.id, s.student_id, s.registration_number, s.department, s.academic_year, s.registration_date, " +
                       "u.full_name, u.phone_number, u.date_of_birth, u.profile_photo " +
                       "FROM students s " +
                       "JOIN users u ON s.user_id = u.user_id " +
                       "WHERE u.username = ?";

        try (Connection conn = DatabaseConnection.getNewConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // --- Data Fetching ---
                this.studentId = rs.getInt("id");
                String studentIdString = rs.getString("student_id");
                String fullName = rs.getString("full_name");
                String phone = rs.getString("phone_number");
                Date dob = rs.getDate("date_of_birth");
                byte[] photoBytes = rs.getBytes("profile_photo");
                String regNo = rs.getString("registration_number");
                this.currentDepartment = rs.getString("department");
                this.currentYear = rs.getString("academic_year");
                Date enrollmentDate = rs.getDate("registration_date");
                this.studentYear = getYearNumber(this.currentYear);

                // --- Update UI ---
                if (welcomeLabel != null) welcomeLabel.setText("Welcome, " + fullName);
                if (fullNameLabel != null) fullNameLabel.setText(fullName);
                if (idLabel != null) idLabel.setText(studentIdString);
                if (departmentLabel != null) departmentLabel.setText(currentDepartment);
                if (yearLabel != null) yearLabel.setText(this.currentYear);

                // Update profile photo
                if (photoBytes != null && profileImageView != null) {
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(photoBytes)) {
                        Image image = new Image(bis);
                        if (!image.isError()) {
                            profileImageView.setImage(image);
                        }
                    } catch (IOException e) {
                        System.err.println("IOException while loading profile image.");
                    }
                }

                // Update profile section
                if (profileFullName != null) profileFullName.setText(fullName);
                if (profileStudentId != null) profileStudentId.setText(studentIdString);
                if (profileEmail != null) profileEmail.setText(username + "@dilla.edu.et");
                if (profilePhone != null) profilePhone.setText(phone);
                if (profileDob != null) profileDob.setText(dob != null ? dob.toString() : "N/A");
                if (profileDepartment != null) profileDepartment.setText(currentDepartment);
                if (profileYear != null) profileYear.setText(this.currentYear);
                if (profileRegDate != null) profileRegDate.setText(enrollmentDate != null ? enrollmentDate.toString() : "N/A");

                // --- Set Defaults and Load Data ---
                calculateAndDisplayCGPA();
                loadRegisteredCredits();
                loadGradesFromDatabase();

                // Set defaults for registration form
                if (deptComboBox != null) {
                    deptComboBox.setValue(currentDepartment);
                    deptComboBox.setDisable(true); // Disable department selection
                }
                if (academicYearComboBox != null) academicYearComboBox.setValue(currentYear);
                if (semesterComboBox != null) semesterComboBox.setValue("Semester 1"); // Default to first semester

                loadCoursesFromDatabase();

            } else {
                showAlert("Student Not Found", "Student profile not found. Please complete your registration.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Unable to load student data: " + e.getMessage());
        }
    }
    
    private void calculateAndDisplayCGPA() {
        if (profileCGPA == null) return;
        
        String query = "SELECT AVG(CASE grade " +
                     "WHEN 'A+' THEN 4.0 WHEN 'A' THEN 4.0 WHEN 'A-' THEN 3.7 " +
                     "WHEN 'B+' THEN 3.3 WHEN 'B' THEN 3.0 WHEN 'B-' THEN 2.7 " +
                     "WHEN 'C+' THEN 2.3 WHEN 'C' THEN 2.0 WHEN 'C-' THEN 1.7 " +
                     "WHEN 'D' THEN 1.0 WHEN 'F' THEN 0.0 ELSE 0.0 END) as cgpa " +
                     "FROM student_courses WHERE student_id = ? AND grade IS NOT NULL";
        
        try (Connection conn = DatabaseConnection.getNewConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next() && rs.getObject("cgpa") != null) {
                profileCGPA.setText(String.format("%.2f", rs.getDouble("cgpa")));
            } else {
                profileCGPA.setText("N/A");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            profileCGPA.setText("Error");
        }
    }
    
    private void populateComboBoxes() {
        if (deptComboBox != null) deptComboBox.getItems().addAll("Computer Science", "Information Technology", "Software Engineering");
        if (academicYearComboBox != null) academicYearComboBox.getItems().addAll("First Year", "Second Year", "Third Year", "Fourth Year", "Fifth Year");
        if (semesterComboBox != null) semesterComboBox.getItems().addAll("Semester 1", "Semester 2");
        if (gradeSemesterFilter != null) {
            gradeSemesterFilter.getItems().addAll("All Semesters", "2024 Semester 1", "2023 Semester 2", "2023 Semester 1");
            gradeSemesterFilter.setValue("All Semesters");
            gradeSemesterFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterGradesBySemester(newVal));
        }
    }

    private void loadCoursesFromDatabase() {
        if (deptComboBox == null || academicYearComboBox == null || semesterComboBox == null) return;
        
        String department = deptComboBox.getValue();
        String year = academicYearComboBox.getValue();
        String semester = semesterComboBox.getValue();
        
        if (department == null || year == null || semester == null) return;
        
        coursesList.clear();
        
        // For now, we will use the default courses as the database might be empty
        loadDefaultCourses(year, semester);
        
        updateSelectAllCheckbox();
        updateTotalCreditsPreview();
    }

    private int getYearNumber(String yearText) {
        if (yearText == null) return 1;
        String lowerYearText = yearText.toLowerCase();
        if (lowerYearText.contains("first")) return 1;
        if (lowerYearText.contains("second")) return 2;
        if (lowerYearText.contains("third")) return 3;
        if (lowerYearText.contains("fourth")) return 4;
        if (lowerYearText.contains("fifth")) return 5;
        return 1; // Default
    }
    
    private void loadDefaultCourses(String year, String semester) {
        coursesList.clear();
        if (deptComboBox == null || !"Computer Science".equals(deptComboBox.getValue())) return;

        if ("Second Year".equals(year)) {
            if ("Semester 1".equals(semester)) {
                coursesList.addAll(
                    new Course("1", "CoSc2021", "Digital Logic Design", "5", "3", "2", "3", "0", "None"),
                    new Course("2", "CoSc2011", "Computer Programming II", "5", "3", "2", "3", "2", "CoSc1012"),
                    new Course("3", "MATH2011", "Linear Algebra", "5", "3", "3", "0", "0", "None"),
                    new Course("4", "CoSc2041", "Fundamentals of Database Systems", "5", "3", "2", "3", "2", "None"),
                    new Course("5", "ECON2103", "Economics", "4", "2", "2", "0", "0", "None"),
                    new Course("6", "MATH2053", "Discrete Mathematics", "5", "3", "3", "0", "0", "None"),
                    new Course("7", "SINE2011", "Inclusiveness", "3", "2", "2", "0", "0", "None")
                );
            } else if ("Semester 2".equals(semester)) {
                coursesList.addAll(
                    new Course("1", "CoSc2034", "Data Communication & Computer Networks", "5", "3", "2", "3", "2", "None"),
                    new Course("2", "CoSc2052", "Object Oriented Programming", "5", "3", "2", "3", "2", "CoSc2011"),
                    new Course("3", "MATH2082", "Numerical Analysis", "5", "3", "2", "3", "0", "Math1044"),
                    new Course("4", "STAT2016", "Probability and Statistics", "5", "3", "3", "0", "1", "None"),
                    new Course("5", "CoSc2092", "Data Structures and Algorithms", "5", "3", "2", "3", "2", "CoSc2011"),
                    new Course("6", "CoSc2022", "Computer Organization and Architecture", "5", "3", "2", "3", "2", "CoSc2021")
                );
            }
        } else if ("Third Year".equals(year)) {
            if ("Semester 1".equals(semester)) {
                coursesList.addAll(
                    new Course("1", "CoSc3023", "Operating Systems", "5", "3", "2", "3", "2", "CoSc2022"),
                    new Course("2", "CoSc3045", "Advanced Database Systems", "5", "3", "2", "3", "2", "CoSc2041"),
                    new Course("3", "CoSc3053", "Java Programming", "5", "3", "2", "3", "2", "CoSc2052")
                );
            } else if ("Semester 2".equals(semester)) {
                coursesList.addAll(
                    new Course("1", "CoSc3086", "Web Programming", "7", "4", "3", "3", "1", "None"),
                    new Course("2", "CoSc3112", "Introduction to Artificial Intelligence", "5", "3", "2", "3", "2", "CoSc2092")
                );
            }
        }
        markRegisteredCourses();
    }
    
    private void markRegisteredCourses() {
        // This method would query student_courses and mark courses in coursesList as registered
        // For simplicity in this example, we assume no courses are pre-registered when loading defaults
        String query = "SELECT c.course_code " +
                       "FROM student_courses sc JOIN courses c ON sc.course_id = c.course_id " +
                       "WHERE sc.student_id = ? AND sc.academic_year = ? AND sc.semester = ?";

        try (Connection conn = DatabaseConnection.getNewConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, studentId);
            stmt.setString(2, getCurrentAcademicYear());
            stmt.setString(3, getCurrentSemester());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String courseCode = rs.getString("course_code");
                    for (Course c : coursesList) {
                        if (c.getCode().equals(courseCode)) {
                            c.setRegistered(true);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void loadGradesFromDatabase() {
        gradesList.clear();
        String query = "SELECT c.course_code, c.course_name, c.credit_hours, sc.grade " +
                     "FROM student_courses sc JOIN courses c ON sc.course_id = c.course_id " +
                     "WHERE sc.student_id = ? AND sc.grade IS NOT NULL";
        try (Connection conn = DatabaseConnection.getNewConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                gradesList.add(new Grade(
                    rs.getString("course_code"),
                    rs.getString("course_name"),
                    String.valueOf(rs.getInt("credit_hours")),
                    rs.getString("grade"),
                    calculateGradePoint(rs.getString("grade")),
                    "" // Semester info not in this query, can be added
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String calculateGradePoint(String grade) {
        if (grade == null) return "N/A";
        switch (grade.toUpperCase()) {
            case "A+": case "A": return "4.0";
            case "A-": return "3.7";
            case "B+": return "3.3";
            case "B": return "3.0";
            case "B-": return "2.7";
            case "C+": return "2.3";
            case "C": return "2.0";
            case "C-": return "1.7";
            case "D": return "1.0";
            case "F": return "0.0";
            default: return "N/A";
        }
    }
    
    private void registerCoursesToDatabase() {
        if (studentId <= 0) {
            showAlert("Registration Error", "Student ID not loaded. Please logout and login again.");
            return;
        }
        if (coursesList == null || coursesList.isEmpty()) {
            showAlert("Registration Error", "No courses available to register.");
            return;
        }

        String semester = getCurrentSemester();
        String academicYear = getCurrentAcademicYear();

        int selectedCount = 0;
        for (Course c : coursesList) {
            if (!c.isRegistered() && c.isSelected()) {
                selectedCount++;
            }
        }

        if (selectedCount == 0) {
            showAlert("No Courses Selected", "Please select at least one course to register.");
            return;
        }

        // Enforce max credits (registeredCredits is loaded from DB)
        int newlySelectedCredits = 0;
        for (Course c : coursesList) {
            if (!c.isRegistered() && c.isSelected()) {
                try {
                    newlySelectedCredits += Integer.parseInt(c.getCredit());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (registeredCredits + newlySelectedCredits > MAX_CREDIT_HOURS) {
            showAlert("Credit Limit Exceeded", "You cannot register more than " + MAX_CREDIT_HOURS + " credit hours per semester.");
            return;
        }

        // Resolve course_code -> course_id then insert student_courses.
        // If the course doesn't exist (because we are using a hardcoded default list), create it.
        String courseIdQuery = "SELECT course_id FROM courses WHERE course_code = ?";
        String upsertCourseQuery = "INSERT INTO courses (course_code, course_name, credit_hours, department) " +
                                  "VALUES (?, ?, ?, ?) " +
                                  "ON DUPLICATE KEY UPDATE course_name = VALUES(course_name), credit_hours = VALUES(credit_hours), department = VALUES(department)";
        String insertQuery = "INSERT INTO student_courses (student_id, course_id, semester, academic_year) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getNewConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement courseIdStmt = conn.prepareStatement(courseIdQuery);
                 PreparedStatement upsertCourseStmt = conn.prepareStatement(upsertCourseQuery);
                 PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {

                int inserted = 0;
                for (Course c : coursesList) {
                    if (c.isRegistered() || !c.isSelected()) continue;

                    courseIdStmt.setString(1, c.getCode());
                    try (ResultSet rs = courseIdStmt.executeQuery()) {
                        if (!rs.next()) {
                            // Course not found in DB; seed it from the default list then re-select.
                            int creditHours = 0;
                            try {
                                creditHours = Integer.parseInt(c.getCredit());
                            } catch (NumberFormatException ignored) {
                            }

                            upsertCourseStmt.setString(1, c.getCode());
                            upsertCourseStmt.setString(2, c.getTitle());
                            upsertCourseStmt.setInt(3, creditHours);
                            upsertCourseStmt.setString(4, currentDepartment != null ? currentDepartment : "");
                            upsertCourseStmt.executeUpdate();

                            // Re-query after seeding
                            courseIdStmt.setString(1, c.getCode());
                            try (ResultSet rs2 = courseIdStmt.executeQuery()) {
                                if (!rs2.next()) {
                                    continue;
                                }
                                int courseId = rs2.getInt("course_id");

                                insertStmt.setInt(1, studentId);
                                insertStmt.setInt(2, courseId);
                                insertStmt.setString(3, semester);
                                insertStmt.setString(4, academicYear);

                                try {
                                    inserted += insertStmt.executeUpdate();
                                } catch (SQLException ex) {
                                    String state = ex.getSQLState();
                                    if (state == null || !state.startsWith("23")) {
                                        throw ex;
                                    }
                                }
                            }
                            continue;
                        }
                        int courseId = rs.getInt("course_id");

                        insertStmt.setInt(1, studentId);
                        insertStmt.setInt(2, courseId);
                        insertStmt.setString(3, semester);
                        insertStmt.setString(4, academicYear);

                        try {
                            inserted += insertStmt.executeUpdate();
                        } catch (SQLException ex) {
                            // Ignore duplicates due to UNIQUE(student_id, course_id, academic_year, semester)
                            // but rethrow anything else
                            String state = ex.getSQLState();
                            if (state == null || !state.startsWith("23")) {
                                throw ex;
                            }
                        }
                    }
                }

                conn.commit();

                // Refresh UI state from DB
                loadRegisteredCredits();
                markRegisteredCourses();
                updateSelectAllCheckbox();
                updateTotalCreditsPreview();
                if (coursesTable != null) coursesTable.refresh();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Registration Successful");
                alert.setHeaderText(null);
                alert.setContentText("Successfully registered " + inserted + " course(s). ");
                alert.showAndWait();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Unable to register courses: " + e.getMessage());
        }
    }
    
    private void filterGradesBySemester(String semester) {
        // Implementation for filtering grades
    }

    private void hideAllSections() {
        if (welcomeSection != null) { welcomeSection.setVisible(false); welcomeSection.setManaged(false); }
        if (registrationSection != null) { registrationSection.setVisible(false); registrationSection.setManaged(false); }
        if (gradesSection != null) { gradesSection.setVisible(false); gradesSection.setManaged(false); }
        if (profileSection != null) { profileSection.setVisible(false); profileSection.setManaged(false); }
    }
    
    @FXML private void showRegistrationSection() {
        hideAllSections();
        if (registrationSection != null) { registrationSection.setVisible(true); registrationSection.setManaged(true); }
    }
    
    @FXML private void showGradesSection() {
        hideAllSections();
        if (gradesSection != null) { gradesSection.setVisible(true); gradesSection.setManaged(true); }
    }
    
    @FXML private void showProfileSection() {
        hideAllSections();
        if (profileSection != null) { profileSection.setVisible(true); profileSection.setManaged(true); }
    }
    
    @FXML private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dillauniversity/resources/login.fxml"));
            Parent loginRoot = loader.load();
            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            currentStage.setScene(new Scene(loginRoot));
            currentStage.setTitle("Dilla University - Login");
            currentStage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void handleSelectAll() {
        if (selectAllCheckBox == null) return;
        boolean select = selectAllCheckBox.isSelected();
        for (Course course : coursesList) {
            if (!course.isRegistered()) {
                course.setSelected(select);
            }
        }
        if (coursesTable != null) coursesTable.refresh();
    }
    
    private void updateTotalCreditsPreview() {
        if (totalCreditsLabel == null) return;

        int selectedCredits = 0;
        for (Course c : coursesList) {
            if (!c.isRegistered() && c.isSelected()) {
                try {
                    selectedCredits += Integer.parseInt(c.getCredit());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        int total = registeredCredits + selectedCredits;
        totalCreditsLabel.setText("Total Credits: " + total + " / " + MAX_CREDIT_HOURS);
    }
    
    private void loadRegisteredCredits() {
        registeredCredits = 0;
        if (studentId <= 0) {
            updateTotalCreditsPreview();
            return;
        }


        String query = "SELECT COALESCE(SUM(c.credit_hours), 0) AS total_credits " +
                       "FROM student_courses sc " +
                       "JOIN courses c ON sc.course_id = c.course_id " +
                       "WHERE sc.student_id = ? AND sc.academic_year = ? AND sc.semester = ?";

        try (Connection conn = DatabaseConnection.getNewConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, studentId);
            stmt.setString(2, getCurrentAcademicYear());
            stmt.setString(3, getCurrentSemester());


            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    registeredCredits = rs.getInt("total_credits");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        updateTotalCreditsPreview();
    }

    @FXML
    private void handleEditProfile() {
        // No separate edit-profile screen exists in this project currently.
        // For now, we route to the profile section where the student's info is shown.
        showProfileSection();
    }
    
    private String getCurrentSemester() { return semesterComboBox != null ? semesterComboBox.getValue() : "Semester 1"; }
    private String getCurrentAcademicYear() { return academicYearComboBox != null ? academicYearComboBox.getValue() : "First Year"; }

    // ===== FIXED: setupCoursesTable() method =====
    private void setupCoursesTable() {
        if (coursesTable == null) return;
        
        // Clear existing columns
        coursesTable.getColumns().clear();
        
        // Create table columns
        TableColumn<Course, Boolean> selectCol = new TableColumn<>("Select");
        TableColumn<Course, String> noCol = new TableColumn<>("No");
        TableColumn<Course, String> codeCol = new TableColumn<>("Course Code");
        TableColumn<Course, String> titleCol = new TableColumn<>("Course Title");
        TableColumn<Course, String> ectsCol = new TableColumn<>("ECTS");
        TableColumn<Course, String> creditCol = new TableColumn<>("Cr. Hrs.");
        TableColumn<Course, String> lecCol = new TableColumn<>("Lec. Hrs");
        TableColumn<Course, String> labCol = new TableColumn<>("Lab. Hrs");
        TableColumn<Course, String> tutCol = new TableColumn<>("Tut. Hrs");
        TableColumn<Course, String> prereqCol = new TableColumn<>("Prerequisite");
        TableColumn<Course, String> statusCol = new TableColumn<>("Status");
        
        // Set column widths
        selectCol.setPrefWidth(60);
        noCol.setPrefWidth(40);
        codeCol.setPrefWidth(100);
        titleCol.setPrefWidth(200);
        ectsCol.setPrefWidth(60);
        creditCol.setPrefWidth(60);
        lecCol.setPrefWidth(60);
        labCol.setPrefWidth(60);
        tutCol.setPrefWidth(60);
        prereqCol.setPrefWidth(120);
        statusCol.setPrefWidth(80);
        
        // Bind columns to data
        selectCol.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        noCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getNo()));
        codeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCode()));
        titleCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
        ectsCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getEcts()));
        creditCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCredit()));
        lecCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLecture()));
        labCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLab()));
        tutCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTutorial()));
        prereqCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPrerequisite()));
        statusCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isRegistered() ? "Registered" : "Available"));
        
        // Make select column use checkboxes and disable for registered courses
        selectCol.setCellFactory(col -> new CheckBoxTableCell<Course, Boolean>() {
            @Override
            public void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && getTableRow() != null && getTableRow().getItem() != null) {
                    Course course = (Course) getTableRow().getItem();
                    if (course.isRegistered()) {
                        setDisable(true);
                        setStyle("-fx-opacity: 0.5;");
                    } else {
                        setDisable(false);
                        setStyle("");
                    }
                }
            }
        });
        
        // Add columns to table
        coursesTable.getColumns().addAll(selectCol, noCol, codeCol, titleCol, ectsCol, 
                                        creditCol, lecCol, labCol, tutCol, prereqCol, statusCol);
        
        // Set table data
        coursesTable.setItems(coursesList);
        
        // Add listener for selection changes to update credits
        coursesList.addListener((javafx.collections.ListChangeListener.Change<? extends Course> c) -> {
            updateSelectAllCheckbox();
            updateTotalCreditsPreview();
        });
    }
    
    // ===== FIXED: setupGradesTable() method =====
    private void setupGradesTable() {
        if (gradesTable == null) return;
        
        gradesTable.getColumns().clear();
        
        TableColumn<Grade, String> codeCol = new TableColumn<>("Course Code");
        TableColumn<Grade, String> titleCol = new TableColumn<>("Course Title");
        TableColumn<Grade, String> creditCol = new TableColumn<>("Credit Hrs");
        TableColumn<Grade, String> gradeCol = new TableColumn<>("Grade");
        TableColumn<Grade, String> pointCol = new TableColumn<>("Grade Point");
        TableColumn<Grade, String> semesterCol = new TableColumn<>("Semester");
        
        codeCol.setPrefWidth(100);
        titleCol.setPrefWidth(200);
        creditCol.setPrefWidth(80);
        gradeCol.setPrefWidth(60);
        pointCol.setPrefWidth(80);
        semesterCol.setPrefWidth(120);
        
        codeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCode()));
        titleCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
        creditCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCredit()));
        gradeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGrade()));
        pointCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPoint()));
        semesterCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSemester()));
        
        gradesTable.getColumns().addAll(codeCol, titleCol, creditCol, gradeCol, pointCol, semesterCol);
        gradesTable.setItems(gradesList);
    }
    
    private void updateSelectAllCheckbox() {
        if (selectAllCheckBox == null || coursesList.isEmpty()) {
            if (selectAllCheckBox != null) {
                selectAllCheckBox.setSelected(false);
                selectAllCheckBox.setIndeterminate(false);
            }
            return;
        }
        
        int selectableCount = 0;
        int selectedCount = 0;
        
        for (Course course : coursesList) {
            if (!course.isRegistered()) {
                selectableCount++;
                if (course.isSelected()) {
                    selectedCount++;
                }
            }
        }
        
        if (selectableCount == 0) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setIndeterminate(false);
            selectAllCheckBox.setDisable(true);
        } else {
            selectAllCheckBox.setDisable(false);
            if (selectedCount == 0) {
                selectAllCheckBox.setSelected(false);
                selectAllCheckBox.setIndeterminate(false);
            } else if (selectedCount == selectableCount) {
                selectAllCheckBox.setSelected(true);
                selectAllCheckBox.setIndeterminate(false);
            } else {
                selectAllCheckBox.setSelected(false);
                selectAllCheckBox.setIndeterminate(true);
            }
        }
    }

    // Inner classes Course and Grade remain the same
    public static class Course {
        private final String no, code, title, ects, credit, lecture, lab, tutorial, prerequisite;
        private final BooleanProperty selected;
        private boolean registered;
        
        public Course(String no, String code, String title, String ects, String credit, String lecture, String lab, String tutorial, String prerequisite) {
            this.no = no; this.code = code; this.title = title; this.ects = ects; this.credit = credit;
            this.lecture = lecture; this.lab = lab; this.tutorial = tutorial; this.prerequisite = prerequisite;
            this.selected = new SimpleBooleanProperty(false); this.registered = false;
        }
        public String getNo() { return no; }
        public String getCode() { return code; }
        public String getTitle() { return title; }
        public String getEcts() { return ects; }
        public String getCredit() { return credit; }
        public String getLecture() { return lecture; }
        public String getLab() { return lab; }
        public String getTutorial() { return tutorial; }
        public String getPrerequisite() { return prerequisite; }
        public boolean isRegistered() { return registered; }
        public void setRegistered(boolean registered) { this.registered = registered; if (registered) this.selected.set(true); }
        public BooleanProperty selectedProperty() { return selected; }
        public boolean isSelected() { return selected.get(); }
        public void setSelected(boolean selected) { if (!this.registered) this.selected.set(selected); }
    }
    
    public static class Grade {
        private final String code, title, credit, grade, point, semester;
        public Grade(String code, String title, String credit, String grade, String point, String semester) {
            this.code = code; this.title = title; this.credit = credit; this.grade = grade; this.point = point; this.semester = semester;
        }
        public String getCode() { return code; }
        public String getTitle() { return title; }
        public String getCredit() { return credit; }
        public String getGrade() { return grade; }
        public String getPoint() { return point; }
        public String getSemester() { return semester; }
    }
}