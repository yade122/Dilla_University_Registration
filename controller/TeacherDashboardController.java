package dillauniversity.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;

import dillauniversity.dao.TeacherDAO;
import dillauniversity.models.Teacher;
import dillauniversity.models.Student;
import dillauniversity.models.Course;
import dillauniversity.models.Assessment;
import dillauniversity.models.Report;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TeacherDashboardController {
    
    // Navigation Buttons
    @FXML private Button dashboardButton;
    @FXML private Button gradeSubmissionButton;
    @FXML private Button reportsButton;
    @FXML private Button profileButton;
    @FXML private Button logoutButton;
    
    // Profile Display
    @FXML private ImageView profileImageView;
    @FXML private ImageView profileImageLarge;
    @FXML private Label teacherNameLabel;
    @FXML private Label teacherIdLabel;
    @FXML private Label teacherDeptLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label teacherInfoLabel;
    @FXML private Label courseCountLabel;
    
    // Sections
    @FXML private VBox dashboardSection;
    @FXML private VBox gradeSection;
    @FXML private VBox reportsSection;
    @FXML private VBox profileSection;
    
    // Dashboard Components
    @FXML private ListView<String> coursesListView;
    
    // Grade Submission Components
    @FXML private ComboBox<Course> courseComboBox;
    @FXML private ComboBox<String> gradeSemesterComboBox;
    @FXML private ComboBox<String> gradeYearComboBox;
    @FXML private TableView<Student> gradingTable;
    @FXML private TableColumn<Student, String> studentIdCol;
    @FXML private TableColumn<Student, String> studentNameCol;
    @FXML private TableColumn<Student, String> departmentCol;
    @FXML private TableColumn<Student, String> assessmentCol;
    @FXML private TableColumn<Student, Double> totalCol;
    @FXML private TableColumn<Student, String> gradeCol;
    @FXML private VBox gradeMessageBox;
    @FXML private Label gradeMessageIcon;
    @FXML private Label gradeMessageLabel;
    
    // Reports Components
    @FXML private ComboBox<String> reportTypeComboBox;
    @FXML private TextField reportTitleField;
    @FXML private TextArea reportDescriptionArea;
    @FXML private TableView<Report> reportsTable;
    @FXML private TableColumn<Report, String> dateCol;
    @FXML private TableColumn<Report, String> typeCol;
    @FXML private TableColumn<Report, String> titleCol;
    @FXML private TableColumn<Report, String> statusCol;
    @FXML private TableColumn<Report, String> responseCol;
    
    // Profile Components
    @FXML private Label profileFullName;
    @FXML private Label profileTeacherId;
    @FXML private Label profileDepartment;
    @FXML private Label profileQualification;
    @FXML private Label profileEmail;
    @FXML private Label profilePhone;
    @FXML private Label profileOfficeNumber;
    @FXML private Label profileOfficeHours;
    @FXML private Label profileEmploymentDate;
    
    // Assessment buttons
    @FXML private Button finalExamBtn;
    @FXML private Button labBtn;
    @FXML private Button midExamBtn;
    @FXML private Button quizBtn;
    
    private String username;
    private Teacher currentTeacher;
    private TeacherDAO teacherDAO = new TeacherDAO();
    private Map<String, Map<String, Double>> studentAssessments = new HashMap<>();
    private Student selectedStudentForAssessment;
    private String currentAssessmentType;

    private static final LinkedHashMap<String, String> COURSE_DISPLAY_NAMES = new LinkedHashMap<>();

    static {
        COURSE_DISPLAY_NAMES.put("CoSc4038", "Distributed Systems");
        COURSE_DISPLAY_NAMES.put("CoSc4021", "Real-time & Embedded Systems");
        COURSE_DISPLAY_NAMES.put("CoSc4031", "Computer Security");
        COURSE_DISPLAY_NAMES.put("CoSc3061", "Software Engineering");
        COURSE_DISPLAY_NAMES.put("CoSc2052", "Object-Oriented Programming");
        COURSE_DISPLAY_NAMES.put("CoSc3112", "Introduction to AI");
        COURSE_DISPLAY_NAMES.put("CoSc4113", "Computer Vision & Image Processing");
        COURSE_DISPLAY_NAMES.put("CoSc2034", "Data Communication and Computer Networks");
        COURSE_DISPLAY_NAMES.put("CoSc4036", "Network and System Administration");
        COURSE_DISPLAY_NAMES.put("CoSc3034", "Wireless Communication");
        COURSE_DISPLAY_NAMES.put("CoSc2041", "Fundamentals of Database Systems");
        COURSE_DISPLAY_NAMES.put("CoSc3045", "Advanced Database Management");
        COURSE_DISPLAY_NAMES.put("CoSc2022", "Computer Organization and Architecture");
        COURSE_DISPLAY_NAMES.put("CoSc3023", "Operating Systems");
        COURSE_DISPLAY_NAMES.put("CoSc3025", "Microprocessor & Assembly Language");
        COURSE_DISPLAY_NAMES.put("CoSc2064", "Fundamentals of Programming I");
        COURSE_DISPLAY_NAMES.put("CoSc2011", "Fundamentals of Programming II");
        COURSE_DISPLAY_NAMES.put("CoSc3053", "Object-Oriented Programming (Java)");
        COURSE_DISPLAY_NAMES.put("CoSc3086", "Web Programming");
    }
    
    public void setUsername(String username) {
        this.username = username;
        loadTeacherData();
    }
    
    @FXML
    public void initialize() {
        // Set up navigation buttons
        dashboardButton.setOnAction(e -> showDashboardSection());
        gradeSubmissionButton.setOnAction(e -> showGradeSubmissionSection());
        reportsButton.setOnAction(e -> showReportsSection());
        profileButton.setOnAction(e -> showProfileSection());
        logoutButton.setOnAction(e -> handleLogout());
        
        // Initialize combo boxes
        populateComboBoxes();
        
        // Setup grading table
        setupGradingTable();
        
        // Setup assessment buttons
        setupAssessmentButtons();
        
        // Setup reports table
        setupReportsTable();
        
        // Auto-load students when course/semester/year selection changes
        if (courseComboBox != null) {
            courseComboBox.setOnAction(e -> loadStudentsForGrading());
        }
        if (gradeSemesterComboBox != null) {
            gradeSemesterComboBox.setOnAction(e -> loadStudentsForGrading());
        }
        if (gradeYearComboBox != null) {
            gradeYearComboBox.setOnAction(e -> loadStudentsForGrading());
        }
        
        // Initially show dashboard
        showDashboardSection();
    }
    
    private void loadTeacherData() {
        currentTeacher = teacherDAO.getTeacherByUsername(username);
        
        if (currentTeacher != null && currentTeacher.getTeacherId() != null) {
            updateTeacherUI();
            loadAssignedCourses();
        } else {
            System.err.println("Teacher data not found for username: " + username);
        }
    }
    
    private void updateTeacherUI() {
        // Update sidebar
        teacherNameLabel.setText(currentTeacher.getFullName());
        teacherIdLabel.setText("ID: " + currentTeacher.getTeacherId());
        teacherDeptLabel.setText(currentTeacher.getDepartment());
        
        // Update welcome section
        welcomeLabel.setText("Welcome, Prof. " + currentTeacher.getFullName());
        teacherInfoLabel.setText(currentTeacher.getDepartment() + " Department");
        
        // Update profile section
        updateProfileSection();
        
        // Load profile image
        loadProfileImage();
    }
    
    private void loadProfileImage() {
        try {
            byte[] photoBytes = currentTeacher.getProfilePhotoBytes();
            Image image;
            if (photoBytes != null && photoBytes.length > 0) {
                image = new Image(new ByteArrayInputStream(photoBytes));
            } else {
                // Use default image if no profile photo
                java.io.InputStream is = getClass().getResourceAsStream("/dillauniversity/resources/logo.png");
                if (is == null) {
                    image = null;
                } else {
                    image = new Image(is);
                }
            }
            
            if (image != null) {
                profileImageView.setImage(image);
                profileImageLarge.setImage(image);
            }
        } catch (Exception e) {
            System.out.println("Could not load profile image: " + e.getMessage());
        }
    }
    
    private void loadAssignedCourses() {
        if (currentTeacher != null) {
            ObservableList<Course> courses = teacherDAO.getTeacherCourses(currentTeacher.getTeacherId());

            ObservableList<Course> filteredCourses = FXCollections.observableArrayList();
            if (courses != null && !courses.isEmpty()) {
                Map<String, Course> byCode = new HashMap<>();
                for (Course c : courses) {
                    byCode.put(c.getCourseCode(), c);
                }
                for (String code : COURSE_DISPLAY_NAMES.keySet()) {
                    Course c = byCode.get(code);
                    if (c != null) {
                        filteredCourses.add(c);
                    }
                }
            }

            if (filteredCourses.isEmpty()) {
                ObservableList<Course> allCourses = teacherDAO.getAllCourses();
                if (allCourses != null && !allCourses.isEmpty()) {
                    Map<String, Course> byCode = new HashMap<>();
                    for (Course c : allCourses) {
                        byCode.put(c.getCourseCode(), c);
                    }
                    for (String code : COURSE_DISPLAY_NAMES.keySet()) {
                        Course c = byCode.get(code);
                        if (c != null) {
                            filteredCourses.add(c);
                        }
                    }
                }
            }
            
            // For ListView (display purposes)
            ObservableList<String> courseStrings = FXCollections.observableArrayList();
            if (filteredCourses != null) {
                for (Course course : filteredCourses) {
                    String displayName = COURSE_DISPLAY_NAMES.getOrDefault(course.getCourseCode(), course.getCourseName());
                    courseStrings.add(displayName);
                }
                
                if (coursesListView != null) {
                    coursesListView.setItems(courseStrings);
                }
                
                if (courseCountLabel != null) {
                    courseCountLabel.setText("Assigned Courses: " + filteredCourses.size());
                }
                
                // For ComboBox (with Course objects)
                if (courseComboBox != null) {
                    courseComboBox.setItems(filteredCourses);
                    
                    // Set custom display for Course objects in ComboBox
                    courseComboBox.setCellFactory(param -> new ListCell<Course>() {
                        @Override
                        protected void updateItem(Course course, boolean empty) {
                            super.updateItem(course, empty);
                            if (empty || course == null) {
                                setText(null);
                            } else {
                                setText(COURSE_DISPLAY_NAMES.getOrDefault(course.getCourseCode(), course.getCourseName()));
                            }
                        }
                    });
                    
                    courseComboBox.setButtonCell(new ListCell<Course>() {
                        @Override
                        protected void updateItem(Course course, boolean empty) {
                            super.updateItem(course, empty);
                            if (empty || course == null) {
                                setText(null);
                            } else {
                                setText(COURSE_DISPLAY_NAMES.getOrDefault(course.getCourseCode(), course.getCourseName()));
                            }
                        }
                    });
                }
            }
        }
    }
    
    private void populateComboBoxes() {
        // Report types
        if (reportTypeComboBox != null) {
            reportTypeComboBox.getItems().addAll(
                "Student Issue",
                "Course Issue", 
                "System Issue",
                "Faculty Concern",
                "Curriculum Feedback",
                "Other"
            );
        }
        
        // Semester and year for grading
        if (gradeSemesterComboBox != null) {
            gradeSemesterComboBox.getItems().addAll(
                "Semester 1",
                "Semester 2",
                "Semester I",
                "Semester II",
                "Summer Session",
                "Special Semester"
            );
        }
        
        if (gradeYearComboBox != null) {
            gradeYearComboBox.getItems().addAll(
                "2024",
                "2025",
                "2023", 
                "2022",
                "2021",
                "2020"
            );
        }
    }
    
    private void setupGradingTable() {
        if (gradingTable == null) return;
        
        // Bind columns to student properties
        if (studentIdCol != null) {
            studentIdCol.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getStudentId()));
        }
        
        if (studentNameCol != null) {
            studentNameCol.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getFullName()));
        }
        
        if (departmentCol != null) {
            departmentCol.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getDepartment()));
        }
        
        // Assessment column with custom cell
        if (assessmentCol != null) {
            assessmentCol.setCellFactory(column -> new TableCell<Student, String>() {
                private final HBox buttonContainer = new HBox(5);
                private final Button finalExamBtn = new Button("Final (50)");
                private final Button midExamBtn = new Button("Mid (20)");
                private final Button labBtn = new Button("Lab (20)");
                private final Button quizBtn = new Button("Quiz (10)");
                private final Label scoreLabel = new Label("Total: 0");
                
                {
                    buttonContainer.setAlignment(Pos.CENTER);
                    finalExamBtn.setStyle("-fx-background-color: #1a237e; -fx-text-fill: white; -fx-font-size: 10;");
                    midExamBtn.setStyle("-fx-background-color: #3949ab; -fx-text-fill: white; -fx-font-size: 10;");
                    labBtn.setStyle("-fx-background-color: #5c6bc0; -fx-text-fill: white; -fx-font-size: 10;");
                    quizBtn.setStyle("-fx-background-color: #7986cb; -fx-text-fill: white; -fx-font-size: 10;");
                    
                    finalExamBtn.setOnAction(e -> {
                        Student student = getTableView().getItems().get(getIndex());
                        selectedStudentForAssessment = student;
                        currentAssessmentType = "FINAL_EXAM";
                        showAssessmentDialog(student, "Final Exam (50%)", 50);
                    });
                    
                    midExamBtn.setOnAction(e -> {
                        Student student = getTableView().getItems().get(getIndex());
                        selectedStudentForAssessment = student;
                        currentAssessmentType = "MID_EXAM";
                        showAssessmentDialog(student, "Mid Exam (20%)", 20);
                    });
                    
                    labBtn.setOnAction(e -> {
                        Student student = getTableView().getItems().get(getIndex());
                        selectedStudentForAssessment = student;
                        currentAssessmentType = "LAB";
                        showAssessmentDialog(student, "Lab Work (20%)", 20);
                    });
                    
                    quizBtn.setOnAction(e -> {
                        Student student = getTableView().getItems().get(getIndex());
                        selectedStudentForAssessment = student;
                        currentAssessmentType = "QUIZ";
                        showAssessmentDialog(student, "Quiz (10%)", 10);
                    });
                    
                    buttonContainer.getChildren().addAll(finalExamBtn, midExamBtn, labBtn, quizBtn, scoreLabel);
                }
                
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        Student student = getTableView().getItems().get(getIndex());
                        String studentId = student.getStudentId();
                        
                        if (studentAssessments.containsKey(studentId)) {
                            Map<String, Double> studentMarks = studentAssessments.get(studentId);
                            double total = studentMarks.values().stream().mapToDouble(Double::doubleValue).sum();
                            scoreLabel.setText(String.format("Total: %.1f", total));
                            
                            // Update button colors based on whether marks are entered
                            updateButtonStatus(finalExamBtn, studentMarks.containsKey("FINAL_EXAM"));
                            updateButtonStatus(midExamBtn, studentMarks.containsKey("MID_EXAM"));
                            updateButtonStatus(labBtn, studentMarks.containsKey("LAB"));
                            updateButtonStatus(quizBtn, studentMarks.containsKey("QUIZ"));
                        } else {
                            scoreLabel.setText("Total: 0");
                            resetButtonColors();
                        }
                        setGraphic(buttonContainer);
                    }
                }
            });
        }
        
        // Total column
        if (totalCol != null) {
            totalCol.setCellValueFactory(cellData -> {
                String studentId = cellData.getValue().getStudentId();
                if (studentAssessments.containsKey(studentId)) {
                    double total = studentAssessments.get(studentId).values().stream()
                        .mapToDouble(Double::doubleValue).sum();
                    return new SimpleDoubleProperty(total).asObject();
                }
                return new SimpleDoubleProperty(0.0).asObject();
            });
            
            totalCol.setCellFactory(column -> new TableCell<Student, Double>() {
                @Override
                protected void updateItem(Double total, boolean empty) {
                    super.updateItem(total, empty);
                    if (empty || total == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(String.format("%.1f", total));
                        if (total >= 90) setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                        else if (total >= 80) setStyle("-fx-text-fill: #689f38; -fx-font-weight: bold;");
                        else if (total >= 70) setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;");
                        else if (total >= 60) setStyle("-fx-text-fill: #ef6c00; -fx-font-weight: bold;");
                        else setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
                    }
                }
            });
        }
        
        // Grade column
        if (gradeCol != null) {
            gradeCol.setCellValueFactory(cellData -> {
                String studentId = cellData.getValue().getStudentId();
                if (studentAssessments.containsKey(studentId)) {
                    double total = studentAssessments.get(studentId).values().stream()
                        .mapToDouble(Double::doubleValue).sum();
                    return new SimpleStringProperty(calculateGrade(total));
                }
                return new SimpleStringProperty("");
            });
        }
    }
    
    private void setupReportsTable() {
        if (reportsTable == null) return;
        
        // Bind columns to Report properties
        if (dateCol != null) {
            dateCol.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getDate()));
        }
        
        if (typeCol != null) {
            typeCol.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getType()));
        }
        
        if (titleCol != null) {
            titleCol.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getTitle()));
        }
        
        if (statusCol != null) {
            statusCol.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getStatus()));
            
            // Color code status
            statusCol.setCellFactory(column -> new TableCell<Report, String>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(status);
                        switch (status.toLowerCase()) {
                            case "pending":
                                setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;");
                                break;
                            case "reviewed":
                                setStyle("-fx-text-fill: #3949ab; -fx-font-weight: bold;");
                                break;
                            case "resolved":
                                setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                                break;
                            default:
                                setStyle("");
                        }
                    }
                }
            });
        }
        
        if (responseCol != null) {
            responseCol.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getResponse()));
        }
    }
    
    private void updateButtonStatus(Button button, boolean hasMark) {
        if (hasMark) {
            button.setStyle(button.getStyle() + "-fx-border-color: #4caf50; -fx-border-width: 2;");
        } else {
            button.setStyle(button.getStyle().replace("-fx-border-color: #4caf50; -fx-border-width: 2;", ""));
        }
    }
    
    private void resetButtonColors() {
        // Reset all buttons to default - implement if needed
    }
    
    private void setupAssessmentButtons() {
        if (finalExamBtn != null) {
            finalExamBtn.setOnAction(e -> {
                Student selected = gradingTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selectedStudentForAssessment = selected;
                    currentAssessmentType = "FINAL_EXAM";
                    showAssessmentDialog(selected, "Final Exam (50%)", 50);
                } else {
                    showGradeMessage("Please select a student first", false);
                }
            });
        }
        
        if (midExamBtn != null) {
            midExamBtn.setOnAction(e -> {
                Student selected = gradingTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selectedStudentForAssessment = selected;
                    currentAssessmentType = "MID_EXAM";
                    showAssessmentDialog(selected, "Mid Exam (20%)", 20);
                } else {
                    showGradeMessage("Please select a student first", false);
                }
            });
        }
        
        if (labBtn != null) {
            labBtn.setOnAction(e -> {
                Student selected = gradingTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selectedStudentForAssessment = selected;
                    currentAssessmentType = "LAB";
                    showAssessmentDialog(selected, "Lab Work (20%)", 20);
                } else {
                    showGradeMessage("Please select a student first", false);
                }
            });
        }
        
        if (quizBtn != null) {
            quizBtn.setOnAction(e -> {
                Student selected = gradingTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selectedStudentForAssessment = selected;
                    currentAssessmentType = "QUIZ";
                    showAssessmentDialog(selected, "Quiz (10%)", 10);
                } else {
                    showGradeMessage("Please select a student first", false);
                }
            });
        }
    }
    
    private void showAssessmentDialog(Student student, String assessmentName, double maxScore) {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Enter " + assessmentName + " Score");
        dialog.setHeaderText("Student: " + student.getFullName() + 
                           "\nStudent ID: " + student.getStudentId());
        
        ButtonType enterButton = new ButtonType("Enter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(enterButton, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField scoreField = new TextField();
        scoreField.setPromptText("Enter score (0-" + maxScore + ")");
        
        Label maxLabel = new Label("/ " + maxScore);
        maxLabel.setTextFill(Color.GRAY);
        
        grid.add(new Label("Score:"), 0, 0);
        grid.add(scoreField, 1, 0);
        grid.add(maxLabel, 2, 0);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == enterButton) {
                try {
                    double score = Double.parseDouble(scoreField.getText());
                    if (score < 0 || score > maxScore) {
                        showAlert("Invalid Score", "Score must be between 0 and " + maxScore);
                        return null;
                    }
                    return score;
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter a valid number");
                    return null;
                }
            }
            return null;
        });
        
        dialog.showAndWait().ifPresent(score -> {
            String studentId = student.getStudentId();
            if (!studentAssessments.containsKey(studentId)) {
                studentAssessments.put(studentId, new HashMap<>());
            }
            studentAssessments.get(studentId).put(currentAssessmentType, score);
            
            // Refresh table to show updated scores
            if (gradingTable != null) {
                gradingTable.refresh();
            }
            
            showGradeMessage(assessmentName + " score (" + score + "/" + maxScore + 
                           ") saved for " + student.getFullName(), true);
        });
    }
    
    @FXML
    private void loadStudentsForGrading() {
        if (courseComboBox == null || gradeSemesterComboBox == null || gradeYearComboBox == null) return;

        Course selectedCourse = courseComboBox.getValue();
        String semester = gradeSemesterComboBox.getValue();
        String year = gradeYearComboBox.getValue();
        
        if (selectedCourse == null || semester == null || year == null) {
            showGradeMessage("Please select course, semester, and year", false);
            return;
        }
        
        System.out.println("DEBUG: Loading students for course: " + selectedCourse.getCourseCode() + 
                          ", semester: " + semester + ", year: " + year);
        
        ObservableList<Student> students = teacherDAO.getStudentsByCourse(
            selectedCourse.getCourseCode(), currentTeacher.getTeacherId(), semester, year);
        
        System.out.println("DEBUG: Found " + students.size() + " students");
        
        // Debug: Print student details
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            System.out.println("DEBUG: Student " + i + ": ID=" + s.getStudentId() + 
                              ", Name=" + s.getFullName() + 
                              ", Dept=" + s.getDepartment());
        }
        
        if (gradingTable != null) {
            gradingTable.setItems(students);
            System.out.println("DEBUG: Set " + students.size() + " students to grading table");
        } else {
            System.out.println("DEBUG: gradingTable is null!");
        }
        
        studentAssessments.clear(); // Clear previous assessments
        
        if (students.isEmpty()) {
            showGradeMessage("No students found for this course", false);
        } else {
            showGradeMessage("Loaded " + students.size() + " students for " + 
                           selectedCourse.getCourseName(), true);
        }
    }
    
    @FXML
    private void submitGrades() {
        if (courseComboBox == null || gradeSemesterComboBox == null || gradeYearComboBox == null) return;

        Course selectedCourse = courseComboBox.getValue();
        String semester = gradeSemesterComboBox.getValue();
        String year = gradeYearComboBox.getValue();
        
        if (selectedCourse == null || semester == null || year == null) {
            showGradeMessage("Please select course, semester, and year", false);
            return;
        }
        
        String courseCode = selectedCourse.getCourseCode();
        int submittedCount = 0;
        int failedCount = 0;
        
        if (gradingTable != null) {
            for (Student student : gradingTable.getItems()) {
                String studentId = student.getStudentId();
                if (studentAssessments.containsKey(studentId)) {
                    Map<String, Double> assessments = studentAssessments.get(studentId);
                    double total = assessments.values().stream().mapToDouble(Double::doubleValue).sum();
                    String grade = calculateGrade(total);
                    String comments = generateComments(total, assessments);
                    
                    boolean success = teacherDAO.submitGrade(
                        studentId,
                        courseCode,
                        currentTeacher.getTeacherId(),
                        grade,
                        total,
                        semester,
                        year,
                        comments
                    );
                    
                    if (success) {
                        // Also save individual assessments
                        saveIndividualAssessments(studentId, courseCode, semester, year, assessments);
                        submittedCount++;
                    } else {
                        failedCount++;
                    }
                }
            }
        }
        
        if (submittedCount > 0) {
            showGradeMessage("Successfully submitted grades for " + submittedCount + " students", true);
            
            // Clear assessments after submission
            studentAssessments.clear();
            if (gradingTable != null) {
                gradingTable.refresh();
            }
        } else {
            showGradeMessage("No grades submitted. Please enter assessment scores for students.", false);
        }
    }
    
    private void saveIndividualAssessments(String studentId, String courseCode, 
                                         String semester, String year, 
                                         Map<String, Double> assessments) {
        for (Map.Entry<String, Double> entry : assessments.entrySet()) {
            Assessment assessment = new Assessment(
                studentId,
                courseCode,
                entry.getKey(),
                entry.getValue(),
                getMaxScoreForAssessment(entry.getKey()),
                semester,
                year,
                currentTeacher.getTeacherId()
            );
            teacherDAO.saveAssessment(assessment);
        }
    }
    
    private double getMaxScoreForAssessment(String assessmentType) {
        switch (assessmentType) {
            case "FINAL_EXAM": return 50;
            case "MID_EXAM": return 20;
            case "LAB": return 20;
            case "QUIZ": return 10;
            default: return 0;
        }
    }
    
    private String calculateGrade(double total) {
        if (total >= 90) return "A+";
        else if (total >= 85) return "A";
        else if (total >= 80) return "A-";
        else if (total >= 75) return "B+";
        else if (total >= 70) return "B";
        else if (total >= 65) return "B-";
        else if (total >= 60) return "C+";
        else if (total >= 55) return "C";
        else if (total >= 50) return "C-";
        else if (total >= 45) return "D";
        else return "F";
    }
    
    private String generateComments(double total, Map<String, Double> assessments) {
        StringBuilder comments = new StringBuilder();
        comments.append("Total Score: ").append(String.format("%.1f", total)).append("\n");
        comments.append("Assessments: ");
        
        for (Map.Entry<String, Double> entry : assessments.entrySet()) {
            String type = entry.getKey().replace("_", " ");
            comments.append(type).append(": ").append(entry.getValue()).append("; ");
        }
        
        if (total >= 90) comments.append("\nExcellent performance!");
        else if (total >= 80) comments.append("\nVery good performance.");
        else if (total >= 70) comments.append("\nGood performance.");
        else if (total >= 60) comments.append("\nSatisfactory performance.");
        else comments.append("\nNeeds improvement.");
        
        return comments.toString();
    }
    
    @FXML
    private void submitReport() {
        if (reportTypeComboBox == null || reportTitleField == null || reportDescriptionArea == null) return;

        String reportType = reportTypeComboBox.getValue();
        String title = reportTitleField.getText().trim();
        String description = reportDescriptionArea.getText().trim();
        
        if (reportType == null || title.isEmpty() || description.isEmpty()) {
            showAlert("Validation Error", "Please fill all report fields");
            return;
        }
        
        boolean success = teacherDAO.submitReport(
            currentTeacher.getTeacherId(),
            currentTeacher.getFullName(),
            currentTeacher.getDepartment(),
            reportType,
            title,
            description
        );
        
        if (success) {
            showAlert("Success", "Report submitted successfully to the Dean");
            clearReportForm();
            loadPreviousReports();
        } else {
            showAlert("Error", "Failed to submit report. Please try again.");
        }
    }
    
    private void loadPreviousReports() {
        if (reportsTable != null && currentTeacher != null) {
            ObservableList<Report> reports = teacherDAO.getTeacherReports(currentTeacher.getTeacherId());
            if (reports != null) {
                reportsTable.setItems(reports);
            } else {
                reportsTable.setItems(FXCollections.observableArrayList());
            }
        }
    }
    
    private void updateProfileSection() {
        if (currentTeacher == null) return;

        if (profileFullName != null) profileFullName.setText(currentTeacher.getFullName());
        if (profileTeacherId != null) profileTeacherId.setText(currentTeacher.getTeacherId());
        if (profileDepartment != null) profileDepartment.setText(currentTeacher.getDepartment());
        if (profileQualification != null) profileQualification.setText(
            currentTeacher.getQualification() != null ? currentTeacher.getQualification() : "Not specified");
        if (profileEmail != null) profileEmail.setText(
            currentTeacher.getEmail() != null ? currentTeacher.getEmail() : "Not provided");
        if (profilePhone != null) profilePhone.setText(
            currentTeacher.getPhone() != null ? currentTeacher.getPhone() : "Not provided");
        if (profileOfficeNumber != null) profileOfficeNumber.setText(
            currentTeacher.getOfficeNumber() != null ? currentTeacher.getOfficeNumber() : "Not assigned");
        if (profileOfficeHours != null) profileOfficeHours.setText(
            currentTeacher.getOfficeHours() != null ? currentTeacher.getOfficeHours() : "Not specified");
        if (profileEmploymentDate != null) profileEmploymentDate.setText(
            currentTeacher.getEmploymentDate() != null ? currentTeacher.getEmploymentDate() : "Not specified");
    }
    
    private void showDashboardSection() {
        hideAllSections();
        if (dashboardSection != null) {
            dashboardSection.setVisible(true);
            dashboardSection.setManaged(true);
        }
        highlightActiveButton(dashboardButton);
    }
    
    @FXML
    private void showGradeSubmissionSection() {
        hideAllSections();
        if (gradeSection != null) {
            gradeSection.setVisible(true);
            gradeSection.setManaged(true);
        }
        highlightActiveButton(gradeSubmissionButton);
    }
    
    @FXML
    private void showReportsSection() {
        hideAllSections();
        if (reportsSection != null) {
            reportsSection.setVisible(true);
            reportsSection.setManaged(true);
        }
        highlightActiveButton(reportsButton);
        loadPreviousReports();
    }
    
    @FXML
    private void showProfileSection() {
        hideAllSections();
        if (profileSection != null) {
            profileSection.setVisible(true);
            profileSection.setManaged(true);
        }
        highlightActiveButton(profileButton);
        updateProfileSection();
    }
    
    private void hideAllSections() {
        if (dashboardSection != null) {
            dashboardSection.setVisible(false);
            dashboardSection.setManaged(false);
        }
        if (gradeSection != null) {
            gradeSection.setVisible(false);
            gradeSection.setManaged(false);
        }
        if (reportsSection != null) {
            reportsSection.setVisible(false);
            reportsSection.setManaged(false);
        }
        if (profileSection != null) {
            profileSection.setVisible(false);
            profileSection.setManaged(false);
        }
    }
    
    private void highlightActiveButton(Button activeButton) {
        // Reset all buttons
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: white; " +
                               "-fx-alignment: CENTER_LEFT; -fx-padding: 12 15; -fx-font-size: 14; " +
                               "-fx-cursor: hand;";
        
        if (dashboardButton != null) dashboardButton.setStyle(defaultStyle);
        if (gradeSubmissionButton != null) gradeSubmissionButton.setStyle(defaultStyle);
        if (reportsButton != null) reportsButton.setStyle(defaultStyle);
        if (profileButton != null) profileButton.setStyle(defaultStyle);
        
        // Highlight active button
        if (activeButton != null) {
            activeButton.setStyle("-fx-background-color: #3949ab; -fx-text-fill: white; -fx-font-weight: bold; " +
                                "-fx-alignment: CENTER_LEFT; -fx-padding: 12 15; -fx-font-size: 14; " +
                                "-fx-cursor: hand; -fx-background-radius: 5;");
        }
    }
    
    private void showGradeMessage(String message, boolean isSuccess) {
        if (gradeMessageLabel != null) gradeMessageLabel.setText(message);
        
        if (gradeMessageIcon != null) {
            if (isSuccess) {
                gradeMessageIcon.setText("✓");
                gradeMessageIcon.setStyle("-fx-text-fill: #2e7d32;");
            } else {
                gradeMessageIcon.setText("✗");
                gradeMessageIcon.setStyle("-fx-text-fill: #c62828;");
            }
        }
        
        if (gradeMessageBox != null) {
            if (isSuccess) {
                gradeMessageBox.setStyle("-fx-background-color: #e8f5e9; -fx-border-color: #c8e6c9;");
            } else {
                gradeMessageBox.setStyle("-fx-background-color: #ffebee; -fx-border-color: #ffcdd2;");
            }
            gradeMessageBox.setVisible(true);
            gradeMessageBox.setManaged(true);
        }
    }
    
    @FXML
    private void clearGradingForm() {
        if (gradingTable != null) gradingTable.getItems().clear();
        studentAssessments.clear();
        if (courseComboBox != null) courseComboBox.setValue(null);
        if (gradeSemesterComboBox != null) gradeSemesterComboBox.setValue(null);
        if (gradeYearComboBox != null) gradeYearComboBox.setValue(null);
        if (gradeMessageBox != null) gradeMessageBox.setVisible(false);
    }
    
    @FXML
    private void clearReportForm() {
        if (reportTypeComboBox != null) reportTypeComboBox.setValue(null);
        if (reportTitleField != null) reportTitleField.clear();
        if (reportDescriptionArea != null) reportDescriptionArea.clear();
    }
    
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dillauniversity/resources/login.fxml"));
            Parent loginRoot = loader.load();
            
            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            Scene loginScene = new Scene(loginRoot);
            currentStage.setScene(loginScene);
            currentStage.setTitle("Dilla University - Login");
            currentStage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Logout Error", "Unable to logout: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}