package dillauniversity.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.FileChooser.ExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDate;
import java.util.Arrays;
import dillauniversity.database.DatabaseConnection;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;

public class RegistrationController {
    
    // Personal Information Fields
    @FXML private RadioButton studentRadio;
    @FXML private RadioButton teacherRadio;
    @FXML private RadioButton deanRadio; // NEW: Dean radio button
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField ageField;
    @FXML private RadioButton maleRadio;
    @FXML private RadioButton femaleRadio;
    @FXML private ComboBox<String> countryCodeComboBox;
    @FXML private TextField phoneNumberField;
    @FXML private DatePicker dateOfBirthPicker;
    @FXML private ComboBox<String> regionComboBox;
    @FXML private ComboBox<String> zoneComboBox;
    
    // Student fields
    @FXML private VBox studentFields;
    @FXML private TextField regNumberField;
    @FXML private ComboBox<String> academicYearComboBox;
    @FXML private ComboBox<String> studentDeptComboBox;
    
    // Teacher fields
    @FXML private VBox teacherFields;
    @FXML private TextField employeeIdField;
    @FXML private ComboBox<String> teacherDeptComboBox;
    @FXML private TextField qualificationField;
    
    // Dean fields - NEW
    @FXML private VBox deanFields;
    @FXML private TextField deanIdField;
    @FXML private ComboBox<String> deanDeptComboBox;
    @FXML private DatePicker appointmentDatePicker;
    @FXML private TextField officeLocationField;
    @FXML private RadioButton departmentDeanRadio;
    @FXML private RadioButton collegeDeanRadio;
    
    // Account fields
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    
    // Photo upload fields
    @FXML private ImageView profileImageView;
    @FXML private Label photoStatusLabel;
    @FXML private Button browsePhotoButton;
    @FXML private Button capturePhotoButton;
    @FXML private Button removePhotoButton;
    @FXML private Label photoError;
    
    // Error Labels
    @FXML private Label firstNameError;
    @FXML private Label lastNameError;
    @FXML private Label ageError;
    @FXML private Label genderError;
    @FXML private Label phoneError;
    @FXML private Label dobError;
    @FXML private Label regionError;
    @FXML private Label zoneError;
    @FXML private Label regNumberError;
    @FXML private Label yearError;
    @FXML private Label studentDeptError;
    @FXML private Label employeeIdError;
    @FXML private Label teacherDeptError;
    @FXML private Label qualificationError;
    @FXML private Label deanIdError; // NEW
    @FXML private Label deanDeptError; // NEW
    @FXML private Label appointmentDateError; // NEW
    @FXML private Label officeLocationError; // NEW
    @FXML private Label deanTypeError; // NEW
    @FXML private Label emailError;
    @FXML private Label usernameError;
    @FXML private Label passwordError;
    @FXML private Label confirmPasswordError;
    
    // Message boxes
    @FXML private VBox messageBox;
    @FXML private Label messageIcon;
    @FXML private Label messageLabel;
    @FXML private VBox errorBox;
    @FXML private Label errorIcon;
    @FXML private Label errorMessageLabel;
    
    @FXML private Button registerButton;
    @FXML private Label dbStatusLabel;
    
    // Photo storage
    private File selectedPhotoFile;
    private byte[] photoBytes;
    private static final long MAX_PHOTO_SIZE = 2 * 1024 * 1024; // 2MB
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png"};
    
    @FXML
    public void initialize() {
        // Set up radio button group for user type
        ToggleGroup userTypeGroup = new ToggleGroup();
        studentRadio.setToggleGroup(userTypeGroup);
        teacherRadio.setToggleGroup(userTypeGroup);
        deanRadio.setToggleGroup(userTypeGroup); // NEW
        
        // Gender toggle group
        ToggleGroup genderGroup = new ToggleGroup();
        maleRadio.setToggleGroup(genderGroup);
        femaleRadio.setToggleGroup(genderGroup);
        maleRadio.setSelected(true);
        
        // Dean type toggle group
        ToggleGroup deanTypeGroup = new ToggleGroup();
        departmentDeanRadio.setToggleGroup(deanTypeGroup);
        collegeDeanRadio.setToggleGroup(deanTypeGroup);
        departmentDeanRadio.setSelected(true);
        
        // Set up event handlers for user type change
        studentRadio.setOnAction(e -> showStudentFields());
        teacherRadio.setOnAction(e -> showTeacherFields());
        deanRadio.setOnAction(e -> showDeanFields()); // NEW
        
        // Initialize combo boxes
        initializeComboBoxes();
        
        // Check database connection
        checkDatabaseConnection();
        
        // Set default date for date picker (18 years ago)
        dateOfBirthPicker.setValue(LocalDate.now().minusYears(18));
        appointmentDatePicker.setValue(LocalDate.now()); // NEW: default to today
        
        // Region selection handler
        regionComboBox.setOnAction(e -> populateZonesForRegion());
        
        // Initialize all error labels to be invisible
        clearErrorMessages();
        
        // Show student fields by default
        showStudentFields();
    }
    
    private void initializeComboBoxes() {
        // Country codes
        ObservableList<String> countryCodes = FXCollections.observableArrayList(
            "+251 (Ethiopia)", "+254 (Kenya)", "+256 (Uganda)", 
            "+255 (Tanzania)", "+252 (Somalia)", "+253 (Djibouti)", 
            "+291 (Eritrea)", "+211 (South Sudan)", "+249 (Sudan)", 
            "+250 (Rwanda)", "+257 (Burundi)", "+234 (Nigeria)", 
            "+27 (South Africa)", "+20 (Egypt)", "+212 (Morocco)", 
            "+233 (Ghana)", "+258 (Mozambique)", "+260 (Zambia)", 
            "+263 (Zimbabwe)", "+1 (USA/Canada)", "+44 (UK)", 
            "+49 (Germany)", "+33 (France)", "+39 (Italy)", 
            "+34 (Spain)", "+31 (Netherlands)", "+41 (Switzerland)", 
            "+46 (Sweden)", "+47 (Norway)", "+91 (India)", 
            "+86 (China)", "+81 (Japan)", "+82 (South Korea)", 
            "+971 (UAE)", "+966 (Saudi Arabia)", "+90 (Turkey)", 
            "+61 (Australia)", "+64 (New Zealand)", "+55 (Brazil)", 
            "+52 (Mexico)"
        );
        countryCodeComboBox.setItems(countryCodes);
        countryCodeComboBox.setValue("+251 (Ethiopia)");
        
        // Regions of Ethiopia
        ObservableList<String> regions = FXCollections.observableArrayList(
            "Addis Ababa", "Afar", "Amhara", "Benishangul-Gumuz",
            "Dire Dawa", "Gambela", "Harari", "Oromia", "Sidama",
            "Somali", "South West Ethiopia Peoples", 
            "Southern Nations, Nationalities, and Peoples' Region", "Tigray"
        );
        regionComboBox.setItems(regions);
        regionComboBox.setValue("Addis Ababa");
        
        // Academic years
        ObservableList<String> years = FXCollections.observableArrayList(
            "2020/2021", "2021/2022", "2022/2023",
            "2023/2024", "2024/2025", "2025/2026"
        );
        academicYearComboBox.setItems(years);
        academicYearComboBox.setValue("2024/2025");
        
        // Departments for all roles
        ObservableList<String> departments = FXCollections.observableArrayList(
            "Computer Science", "Information Technology", "Software Engineering",
            "Information System", "Data Science", "Cybersecurity",
            "Electrical Engineering", "Mechanical Engineering", "Civil Engineering",
            "Business Administration", "Accounting", "Economics",
            "Law", "Medicine", "Nursing", "Pharmacy",
            "College of Natural and Computational Sciences", // NEW for deans
            "College of Business and Economics", // NEW for deans
            "College of Engineering and Technology", // NEW for deans
            "College of Social Sciences" // NEW for deans
        );
        studentDeptComboBox.setItems(departments);
        teacherDeptComboBox.setItems(departments);
        deanDeptComboBox.setItems(departments); // NEW
        studentDeptComboBox.setValue("Computer Science");
        teacherDeptComboBox.setValue("Computer Science");
        deanDeptComboBox.setValue("Computer Science"); // NEW
    }
    
    private void populateZonesForRegion() {
        String selectedRegion = regionComboBox.getValue();
        ObservableList<String> zones = FXCollections.observableArrayList();
        
        if ("Addis Ababa".equals(selectedRegion)) {
            zones.addAll("Arada", "Addis Ketema", "Bole", "Gulele", "Kirkos", 
                         "Kolfe Keranio", "Lideta", "Nifas Silk-Lafto", "Yeka");
        } else if ("Amhara".equals(selectedRegion)) {
            zones.addAll("Semen Gojjam", "Debub Gojjam", "Semen Gondar", "south Gondar", "Gondar Magaalaa",
                    "south Wollo", "north Wollo", "north Shewa", "Awi", "Wag Hemra", "Oromia (Special Zone)", "Bahir Dar ", "Mirab Gojjam", "Misraq Gojjam", "central Gondar", "Meketew Gondar");
        } else if ("Oromia".equals(selectedRegion)) {
            zones.addAll("Arsi", "West Arsi", "Bale", "East Bale", "Borana", "Bunno Bedele", "West Guji", "East Guji", "Horro Guduru Wollega",
                    "Illu Abba Bor", "Jimma", "East Shewa", "North Shewa", "West Shewa", "South West Shewa", "East Hararghe", "West Hararghe",
                    "East Wollega", "West Wollega", "Qellem Wollega", "Finfinne Surrounding Special Zone");
        } else if ("Tigray".equals(selectedRegion)) {
            zones.addAll("Central Tigray", "East Tigray", "North West Tigray", 
                    "South Tigray", "South East Tigray", "West Tigray", "Mekelle Special Zone");
        } else if ("Southern Nations, Nationalities, and Peoples' Region".equals(selectedRegion)) {
            zones.addAll("Gurage", "Hadiya", "Kembata Tembaro", "Sidama",
                         "Wolayita", "Gamo Gofa", "Bench Maji");
        } else {
            zones.add("Central Zone");
        }
        
        zoneComboBox.setItems(zones);
        if (!zones.isEmpty()) {
            zoneComboBox.setValue(zones.get(0));
            zoneComboBox.setDisable(false);
        } else {
            zoneComboBox.setDisable(true);
        }
    }
    
    private void showStudentFields() {
        studentFields.setVisible(true);
        studentFields.setManaged(true);
        teacherFields.setVisible(false);
        teacherFields.setManaged(false);
        deanFields.setVisible(false);
        deanFields.setManaged(false);
    }
    
    private void showTeacherFields() {
        studentFields.setVisible(false);
        studentFields.setManaged(false);
        teacherFields.setVisible(true);
        teacherFields.setManaged(true);
        deanFields.setVisible(false);
        deanFields.setManaged(false);
    }
    
    private void showDeanFields() { // NEW
        studentFields.setVisible(false);
        studentFields.setManaged(false);
        teacherFields.setVisible(false);
        teacherFields.setManaged(false);
        deanFields.setVisible(true);
        deanFields.setManaged(true);
    }
    
    @FXML
    private void browsePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png"),
            new ExtensionFilter("All Files", "*.*")
        );
        
        File file = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (file != null) {
            processSelectedPhoto(file);
        }
    }
    
    @FXML
    private void capturePhoto() {
        browsePhoto();
    }
    
    @FXML
    private void removePhoto() {
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-avatar.png"));
            profileImageView.setImage(defaultImage);
            photoStatusLabel.setText("No photo selected");
            selectedPhotoFile = null;
            photoBytes = null;
            photoError.setVisible(false);
        } catch (Exception e) {
            profileImageView.setImage(null);
            photoStatusLabel.setText("Default image not found");
        }
    }
    
    private void processSelectedPhoto(File file) {
        try {
            if (file.length() > MAX_PHOTO_SIZE) {
                showPhotoError("Photo size exceeds 2MB limit");
                return;
            }
            
            String fileName = file.getName().toLowerCase();
            boolean validExtension = Arrays.stream(ALLOWED_EXTENSIONS)
                .anyMatch(fileName::endsWith);
            
            if (!validExtension) {
                showPhotoError("Only JPG and PNG files are allowed");
                return;
            }
            
            photoBytes = Files.readAllBytes(file.toPath());
            selectedPhotoFile = file;
            
            Image image = new Image(file.toURI().toString());
            profileImageView.setImage(image);
            photoStatusLabel.setText(file.getName() + " (" + (file.length() / 1024) + " KB)");
            photoError.setVisible(false);
            
        } catch (IOException e) {
            showPhotoError("Error reading photo file: " + e.getMessage());
        }
    }
    
    private void showPhotoError(String message) {
        photoError.setText(message);
        photoError.setVisible(true);
    }
    
    private boolean validatePhoto() {
        if (photoBytes == null) {
            showPhotoError("Profile photo is required");
            return false;
        }
        photoError.setVisible(false);
        return true;
    }
    
    @FXML
    private void submitRegistration() {
        System.out.println("DEBUG: submitRegistration called");
        clearErrorMessages();
        
        if (!validateAllFields()) {
            showErrorMessage("Please correct all errors before submitting");
            return;
        }
        
        Connection conn = null;
        try {
            System.out.println("DEBUG: Getting new connection...");
            conn = DatabaseConnection.getNewConnection();
            System.out.println("DEBUG: Connection obtained: " + conn);
            conn.setAutoCommit(false);
            
            try {
                String hashedPassword = hashPassword(passwordField.getText());
                
                String insertUserSQL = "INSERT INTO users (username, password, email, full_name, " +
                                      "date_of_birth, gender, phone_number, region, zone, " +
                                      "profile_photo, role, created_at) " +
                                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
                
                PreparedStatement userStmt = conn.prepareStatement(insertUserSQL, Statement.RETURN_GENERATED_KEYS);
                
                String fullName = firstNameField.getText() + " " + lastNameField.getText();
                String gender = maleRadio.isSelected() ? "Male" : "Female";
                String phone = countryCodeComboBox.getValue() + phoneNumberField.getText();
                String region = regionComboBox.getValue();
                String zone = zoneComboBox.getValue();
                String role = "";
                
                if (studentRadio.isSelected()) {
                    role = "student";
                } else if (teacherRadio.isSelected()) {
                    role = "teacher";
                } else if (deanRadio.isSelected()) { // NEW
                    role = "dean";
                }
                
                userStmt.setString(1, usernameField.getText());
                userStmt.setString(2, hashedPassword);
                userStmt.setString(3, emailField.getText());
                userStmt.setString(4, fullName);
                userStmt.setDate(5, Date.valueOf(dateOfBirthPicker.getValue()));
                userStmt.setString(6, gender);
                userStmt.setString(7, phone);
                userStmt.setString(8, region);
                userStmt.setString(9, zone);
                
                if (photoBytes != null) {
                    userStmt.setBytes(10, photoBytes);
                } else {
                    userStmt.setNull(10, Types.BLOB);
                }
                
                userStmt.setString(11, role);
                
                int userRows = userStmt.executeUpdate();
                
                if (userRows > 0) {
                    ResultSet generatedKeys = userStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int userId = generatedKeys.getInt(1);
                        
                        if (studentRadio.isSelected()) {
                            // Generate student ID
                            String studentId = generateStudentId();
                            
                            String insertStudentSQL = "INSERT INTO students (user_id, student_id, " +
                                                    "registration_number, department, academic_year, " +
                                                    "registration_date, password) " +
                                                    "VALUES (?, ?, ?, ?, ?, NOW(), ?)";
                            
                            PreparedStatement studentStmt = conn.prepareStatement(insertStudentSQL);
                            studentStmt.setInt(1, userId);
                            studentStmt.setString(2, studentId);
                            studentStmt.setString(3, regNumberField.getText());
                            studentStmt.setString(4, studentDeptComboBox.getValue());
                            studentStmt.setString(5, academicYearComboBox.getValue());
                            studentStmt.setString(6, hashedPassword);
                            studentStmt.executeUpdate();
                            
                            showSuccessMessage(
                                "Registration Successful!",
                                "Student account created successfully.\n" +
                                "Student ID: " + studentId + "\n" +
                                "Registration No: " + regNumberField.getText() + "\n" +
                                "You can now login with your username and password."
                            );
                            
                        } else if (teacherRadio.isSelected()) {
                            // Generate teacher ID
                            String teacherId = generateTeacherId();
                            
                            String insertTeacherSQL = "INSERT INTO teachers (user_id, teacher_id, " +
                                                     "employee_id, department, qualification, " +
                                                     "employment_date, password) " +
                                                     "VALUES (?, ?, ?, ?, ?, NOW(), ?)";
                            
                            PreparedStatement teacherStmt = conn.prepareStatement(insertTeacherSQL);
                            teacherStmt.setInt(1, userId);
                            teacherStmt.setString(2, teacherId);
                            teacherStmt.setString(3, employeeIdField.getText());
                            teacherStmt.setString(4, teacherDeptComboBox.getValue());
                            teacherStmt.setString(5, qualificationField.getText());
                            teacherStmt.setString(6, hashedPassword);
                            teacherStmt.executeUpdate();
                            
                            showSuccessMessage(
                                "Registration Successful!",
                                "Teacher account created successfully.\n" +
                                "Teacher ID: " + teacherId + "\n" +
                                "Employee ID: " + employeeIdField.getText() + "\n" +
                                "You can now login with your username and password."
                            );
                            
                        } else if (deanRadio.isSelected()) { // NEW: Dean registration
                            // Generate dean ID
                            String deanId = generateDeanId();
                            String deanType = departmentDeanRadio.isSelected() ? "Department Dean" : "College Dean";
                            
                            String insertDeanSQL = "INSERT INTO deans (user_id, dean_id, " +
                                                  "department, appointment_date, office_location, " +
                                                  "dean_type, password) " +
                                                  "VALUES (?, ?, ?, ?, ?, ?, ?)";
                            
                            PreparedStatement deanStmt = conn.prepareStatement(insertDeanSQL);
                            deanStmt.setInt(1, userId);
                            deanStmt.setString(2, deanId);
                            deanStmt.setString(3, deanDeptComboBox.getValue());
                            deanStmt.setDate(4, Date.valueOf(appointmentDatePicker.getValue()));
                            deanStmt.setString(5, officeLocationField.getText());
                            deanStmt.setString(6, deanType);
                            deanStmt.setString(7, hashedPassword);
                            deanStmt.executeUpdate();
                            
                            showSuccessMessage(
                                "Registration Successful!",
                                "Dean account created successfully.\n" +
                                "Dean ID: " + deanId + "\n" +
                                "Department: " + deanDeptComboBox.getValue() + "\n" +
                                "Dean Type: " + deanType + "\n" +
                                "You can now login with your username and password."
                            );
                            
                            // After successful dean registration, go to dean dashboard
                            goToDeanDashboard(usernameField.getText());
                        }
                        
                        conn.commit();
                        if (!deanRadio.isSelected()) { // Don't clear form for deans since we're navigating away
                            clearForm();
                        }
                        
                    } else {
                        conn.rollback();
                        showErrorMessage("Failed to get user ID. Please try again.");
                    }
                } else {
                    conn.rollback();
                    showErrorMessage("Failed to create user account. Please try again.");
                }
                
                userStmt.close();
                
            } catch (SQLException e) {
                if (conn != null && !conn.isClosed()) {
                    conn.rollback();
                }
                
                if (e.getMessage().contains("Duplicate entry")) {
                    handleDuplicateEntryError(e);
                } else {
                    showErrorMessage("Database error: " + e.getMessage());
                }
                e.printStackTrace();
            } finally {
                if (conn != null && !conn.isClosed()) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            }
            
        } catch (SQLException e) {
            showErrorMessage("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleDuplicateEntryError(SQLException e) {
        if (e.getMessage().contains("username")) {
            showFieldError(usernameError, "Username already exists");
        } else if (e.getMessage().contains("email")) {
            showFieldError(emailError, "Email already registered");
        } else if (e.getMessage().contains("registration_number")) {
            showFieldError(regNumberError, "Registration number already exists");
        } else if (e.getMessage().contains("employee_id")) {
            showFieldError(employeeIdError, "Employee ID already exists");
        } else if (e.getMessage().contains("dean_id")) { // NEW
            showFieldError(deanIdError, "Dean ID already exists");
        } else {
            showErrorMessage("Duplicate entry detected. Please check your information.");
        }
    }
    
    private String hashPassword(String password) {
        // Simple SHA-256 hashing (use BCrypt in production)
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password;
        }
    }
    
    private String generateStudentId() {
        String year = academicYearComboBox.getValue().split("/")[0];
        int sequence = getNextSequence("students");
        return "DU-" + year + "-" + String.format("%05d", sequence);
    }
    
    private String generateTeacherId() {
        String year = String.valueOf(LocalDate.now().getYear());
        int sequence = getNextSequence("teachers");
        return "T-" + year + "-" + String.format("%03d", sequence);
    }
    
    private String generateDeanId() { // NEW
        String year = String.valueOf(LocalDate.now().getYear());
        int sequence = getNextSequence("deans");
        return "D-" + year + "-" + String.format("%03d", sequence);
    }
    
    private int getNextSequence(String tableName) {
        try (Connection conn = DatabaseConnection.getNewConnection()) {
            String sql = "SELECT COUNT(*) + 1 FROM " + tableName;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return (int) (Math.random() * 999) + 1;
    }
    
    private boolean validateAllFields() {
        boolean isValid = true;
        
        // Validate photo
        if (!validatePhoto()) {
            isValid = false;
        }
        
        // Validate first name
        if (firstNameField.getText().trim().isEmpty()) {
            showFieldError(firstNameError, "First name is required");
            isValid = false;
        }
        
        // Validate last name
        if (lastNameField.getText().trim().isEmpty()) {
            showFieldError(lastNameError, "Last name is required");
            isValid = false;
        }
        
        // Validate age
        try {
            int age = Integer.parseInt(ageField.getText().trim());
            if (age < 18 || age > 100) {
                showFieldError(ageError, "Age must be between 18 and 100");
                isValid = false;
            }
        } catch (NumberFormatException e) {
            showFieldError(ageError, "Age must be a valid number");
            isValid = false;
        }
        
        // Validate phone number
        if (phoneNumberField.getText().trim().isEmpty()) {
            showFieldError(phoneError, "Phone number is required");
            isValid = false;
        } else if (!phoneNumberField.getText().matches("\\d{9,10}")) {
            showFieldError(phoneError, "Phone number must be 9-10 digits");
            isValid = false;
        }
        
        // Validate date of birth
        if (dateOfBirthPicker.getValue() == null) {
            showFieldError(dobError, "Date of birth is required");
            isValid = false;
        } else if (dateOfBirthPicker.getValue().isAfter(LocalDate.now().minusYears(18))) {
            showFieldError(dobError, "Must be at least 18 years old");
            isValid = false;
        }
        
        // Validate region
        if (regionComboBox.getValue() == null) {
            showFieldError(regionError, "Region is required");
            isValid = false;
        }
        
        // Role-specific validation
        if (studentRadio.isSelected()) {
            if (regNumberField.getText().trim().isEmpty()) {
                showFieldError(regNumberError, "Registration number is required");
                isValid = false;
            }
            
            if (academicYearComboBox.getValue() == null) {
                showFieldError(yearError, "Academic year is required");
                isValid = false;
            }
            
            if (studentDeptComboBox.getValue() == null) {
                showFieldError(studentDeptError, "Department is required");
                isValid = false;
            }
            
        } else if (teacherRadio.isSelected()) {
            if (employeeIdField.getText().trim().isEmpty()) {
                showFieldError(employeeIdError, "Employee ID is required");
                isValid = false;
            }
            
            if (teacherDeptComboBox.getValue() == null) {
                showFieldError(teacherDeptError, "Department is required");
                isValid = false;
            }
            
            if (qualificationField.getText().trim().isEmpty()) {
                showFieldError(qualificationError, "Qualification is required");
                isValid = false;
            }
            
        } else if (deanRadio.isSelected()) { // NEW: Dean validation
            if (deanIdField.getText().trim().isEmpty()) {
                showFieldError(deanIdError, "Dean ID is required");
                isValid = false;
            }
            
            if (deanDeptComboBox.getValue() == null) {
                showFieldError(deanDeptError, "Department is required");
                isValid = false;
            }
            
            if (appointmentDatePicker.getValue() == null) {
                showFieldError(appointmentDateError, "Appointment date is required");
                isValid = false;
            }
            
            if (officeLocationField.getText().trim().isEmpty()) {
                showFieldError(officeLocationError, "Office location is required");
                isValid = false;
            }
        }
        
        // Validate email
        if (emailField.getText().trim().isEmpty()) {
            showFieldError(emailError, "Email is required");
            isValid = false;
        } else if (!emailField.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showFieldError(emailError, "Invalid email format");
            isValid = false;
        }
        
        // Validate username
        if (usernameField.getText().trim().isEmpty()) {
            showFieldError(usernameError, "Username is required");
            isValid = false;
        } else if (usernameField.getText().length() < 4) {
            showFieldError(usernameError, "Username must be at least 4 characters");
            isValid = false;
        }
        
        // Validate password
        if (passwordField.getText().isEmpty()) {
            showFieldError(passwordError, "Password is required");
            isValid = false;
        } else if (passwordField.getText().length() < 8) {
            showFieldError(passwordError, "Password must be at least 8 characters");
            isValid = false;
        }
        
        // Validate confirm password
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showFieldError(confirmPasswordError, "Passwords do not match");
            isValid = false;
        }
        
        return isValid;
    }
    
    private void showFieldError(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        }
    }
    
    private void clearErrorMessages() {
        // Clear all error labels
        if (firstNameError != null) firstNameError.setVisible(false);
        if (lastNameError != null) lastNameError.setVisible(false);
        if (ageError != null) ageError.setVisible(false);
        if (genderError != null) genderError.setVisible(false);
        if (phoneError != null) phoneError.setVisible(false);
        if (dobError != null) dobError.setVisible(false);
        if (regionError != null) regionError.setVisible(false);
        if (zoneError != null) zoneError.setVisible(false);
        if (regNumberError != null) regNumberError.setVisible(false);
        if (yearError != null) yearError.setVisible(false);
        if (studentDeptError != null) studentDeptError.setVisible(false);
        if (employeeIdError != null) employeeIdError.setVisible(false);
        if (teacherDeptError != null) teacherDeptError.setVisible(false);
        if (qualificationError != null) qualificationError.setVisible(false);
        if (deanIdError != null) deanIdError.setVisible(false); // NEW
        if (deanDeptError != null) deanDeptError.setVisible(false); // NEW
        if (appointmentDateError != null) appointmentDateError.setVisible(false); // NEW
        if (officeLocationError != null) officeLocationError.setVisible(false); // NEW
        if (deanTypeError != null) deanTypeError.setVisible(false); // NEW
        if (emailError != null) emailError.setVisible(false);
        if (usernameError != null) usernameError.setVisible(false);
        if (passwordError != null) passwordError.setVisible(false);
        if (confirmPasswordError != null) confirmPasswordError.setVisible(false);
        if (photoError != null) photoError.setVisible(false);
        if (errorBox != null) errorBox.setVisible(false);
        if (messageBox != null) messageBox.setVisible(false);
    }
    
    private void showSuccessMessage(String title, String message) {
        if (messageIcon != null) messageIcon.setText("✓");
        if (messageLabel != null) messageLabel.setText(message);
        if (messageBox != null) {
            messageBox.setStyle("-fx-background-color: #e8f5e9; -fx-border-color: #c8e6c9;");
            messageBox.setVisible(true);
            messageBox.setManaged(true);
        }
        if (errorBox != null) errorBox.setVisible(false);
    }
    
    private void showErrorMessage(String message) {
        if (errorIcon != null) errorIcon.setText("✗");
        if (errorMessageLabel != null) errorMessageLabel.setText(message);
        if (errorBox != null) {
            errorBox.setStyle("-fx-background-color: #ffebee; -fx-border-color: #ffcdd2;");
            errorBox.setVisible(true);
            errorBox.setManaged(true);
        }
        if (messageBox != null) messageBox.setVisible(false);
    }
    
    @FXML
    private void clearForm() {
        // Clear all fields
        firstNameField.clear();
        lastNameField.clear();
        ageField.clear();
        maleRadio.setSelected(true);
        countryCodeComboBox.setValue("+251 (Ethiopia)");
        phoneNumberField.clear();
        dateOfBirthPicker.setValue(LocalDate.now().minusYears(18));
        regionComboBox.setValue("Addis Ababa");
        if (zoneComboBox.getItems() != null && !zoneComboBox.getItems().isEmpty()) {
            zoneComboBox.setValue(zoneComboBox.getItems().get(0));
        }
        
        // Clear student fields
        regNumberField.clear();
        academicYearComboBox.setValue("2024/2025");
        studentDeptComboBox.setValue("Computer Science");
        
        // Clear teacher fields
        employeeIdField.clear();
        teacherDeptComboBox.setValue("Computer Science");
        qualificationField.clear();
        
        // Clear dean fields
        deanIdField.clear(); // NEW
        deanDeptComboBox.setValue("Computer Science"); // NEW
        appointmentDatePicker.setValue(LocalDate.now()); // NEW
        officeLocationField.clear(); // NEW
        departmentDeanRadio.setSelected(true); // NEW
        
        // Clear account fields
        emailField.clear();
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        
        // Clear photo
        removePhoto();
        
        // Clear messages
        clearErrorMessages();
        
        // Set focus back to first field
        firstNameField.requestFocus();
        
        // Show student fields by default
        studentRadio.setSelected(true);
        showStudentFields();
    }
    
    private void checkDatabaseConnection() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            if (conn != null && !conn.isClosed()) {
                dbStatusLabel.setText("Database: Connected ✓");
                dbStatusLabel.setStyle("-fx-text-fill: #2e7d32;");
            } else {
                dbStatusLabel.setText("Database: Disconnected ✗");
                dbStatusLabel.setStyle("-fx-text-fill: #c62828;");
            }
        } catch (SQLException e) {
            dbStatusLabel.setText("Database: Connection Error");
            dbStatusLabel.setStyle("-fx-text-fill: #c62828;");
        }
    }
    
    @FXML
    private void goBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dillauniversity/resources/login.fxml"));
            Parent loginRoot = loader.load();
            
            Stage currentStage = (Stage) firstNameField.getScene().getWindow();
            Scene loginScene = new Scene(loginRoot);
            currentStage.setScene(loginScene);
            currentStage.setTitle("Dilla University - Login");
            currentStage.centerOnScreen();
            
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("Unable to go back to login: " + e.getMessage());
        }
    }
    
    // NEW: Method to navigate to dean dashboard after registration
    private void goToDeanDashboard(String username) {
        try {
            // First, try to get the resource URL
            java.net.URL resourceUrl = getClass().getResource("/dillauniversity/resources/dean_dashboard.fxml");
            if (resourceUrl == null) {
                // Try alternative paths
                String[] possiblePaths = {
                    "/dean_dashboard.fxml",
                    "../resources/dean_dashboard.fxml",
                    "dean_dashboard.fxml"
                };
                
                for (String path : possiblePaths) {
                    resourceUrl = getClass().getResource(path);
                    if (resourceUrl != null) break;
                }
            }
            
            if (resourceUrl == null) {
                throw new IOException("Dean dashboard FXML file not found");
            }
            
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent deanDashboardRoot = loader.load();
            
            // Try to get the controller - it might be null if not specified in FXML
            Object controller = loader.getController();
            if (controller != null) {
                // Try to set username using common method name
                try {
                    // First try: setUsername
                    controller.getClass().getMethod("setUsername", String.class).invoke(controller, username);
                } catch (NoSuchMethodException e1) {
                    try {
                        // Second try: setUserName (different capitalization)
                        controller.getClass().getMethod("setUserName", String.class).invoke(controller, username);
                    } catch (NoSuchMethodException e2) {
                        // Third try: initUser or similar
                        try {
                            controller.getClass().getMethod("initUser", String.class).invoke(controller, username);
                        } catch (NoSuchMethodException e3) {
                            // Method not found - this is okay
                            System.out.println("Note: Could not find username setter method in dean controller");
                        }
                    }
                }
            }
            
            Stage currentStage = (Stage) firstNameField.getScene().getWindow();
            Scene deanScene = new Scene(deanDashboardRoot);
            currentStage.setScene(deanScene);
            currentStage.setTitle("Dilla University - Dean Dashboard");
            currentStage.centerOnScreen();
            
        } catch (Exception e) {
            e.printStackTrace();
            showErrorMessage("Unable to load dean dashboard: " + e.getMessage());
            
            // Fallback: Show success message and go back to login
            showSuccessMessage("Registration Successful", 
                "Dean account created successfully! Please login with your credentials.");
            goBackToLogin();
        }
    }
}