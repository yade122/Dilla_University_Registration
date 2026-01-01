package dillauniversity.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.sql.*;
import dillauniversity.database.DatabaseConnection;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;

public class AdminDashboardController {
    
    // Navigation Buttons
    @FXML private Button dashboardButton;
    @FXML private Button usersButton;
    @FXML private Button coursesButton;
    @FXML private Button departmentsButton;
    @FXML private Button settingsButton;
    @FXML private Button logoutButton;
    
    // Dashboard Stats
    @FXML private Label totalUsersLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalTeachersLabel;
    @FXML private Label totalCoursesLabel;
    @FXML private Label systemStatusLabel;
    
    // Sections
    @FXML private VBox dashboardSection;
    @FXML private VBox usersSection;
    @FXML private VBox coursesSection;
    @FXML private VBox departmentsSection;
    @FXML private VBox settingsSection;
    
    // User Management
    @FXML private TableView<?> usersTable;
    @FXML private TextField searchUserField;
    @FXML private ComboBox<String> roleFilterCombo;
    
    // Course Management
    @FXML private TableView<?> coursesTable;
    @FXML private TextField searchCourseField;
    
    private String username;
    
    public void setUsername(String username) {
        this.username = username;
        loadDashboardStats();
    }
    
    @FXML
    public void initialize() {
        // Set up navigation
        if (dashboardButton != null) dashboardButton.setOnAction(e -> showDashboardSection());
        if (usersButton != null) usersButton.setOnAction(e -> showUsersSection());
        if (coursesButton != null) coursesButton.setOnAction(e -> showCoursesSection());
        if (departmentsButton != null) departmentsButton.setOnAction(e -> showDepartmentsSection());
        if (settingsButton != null) settingsButton.setOnAction(e -> showSettingsSection());
        if (logoutButton != null) logoutButton.setOnAction(e -> handleLogout());
        
        // Initialize filters
        if (roleFilterCombo != null) {
            roleFilterCombo.setItems(FXCollections.observableArrayList("All", "Student", "Teacher", "Dean", "Admin"));
            roleFilterCombo.setValue("All");
        }
        
        // Show dashboard by default
        showDashboardSection();
    }
    
    private void loadDashboardStats() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Total Users
            String userQuery = "SELECT COUNT(*) FROM users";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(userQuery)) {
                if (rs.next() && totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(rs.getInt(1)));
            }
            
            // Total Students
            String studentQuery = "SELECT COUNT(*) FROM students";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(studentQuery)) {
                if (rs.next() && totalStudentsLabel != null) totalStudentsLabel.setText(String.valueOf(rs.getInt(1)));
            }
            
            // Total Teachers
            String teacherQuery = "SELECT COUNT(*) FROM teachers";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(teacherQuery)) {
                if (rs.next() && totalTeachersLabel != null) totalTeachersLabel.setText(String.valueOf(rs.getInt(1)));
            }
            
            // Total Courses
            String courseQuery = "SELECT COUNT(*) FROM courses";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(courseQuery)) {
                if (rs.next() && totalCoursesLabel != null) totalCoursesLabel.setText(String.valueOf(rs.getInt(1)));
            }
            
            if (systemStatusLabel != null) systemStatusLabel.setText("Online");
            
        } catch (SQLException e) {
            e.printStackTrace();
            if (systemStatusLabel != null) systemStatusLabel.setText("Database Error");
        }
    }
    
    private void showDashboardSection() {
        hideAllSections();
        if (dashboardSection != null) dashboardSection.setVisible(true);
        highlightButton(dashboardButton);
        loadDashboardStats();
    }
    
    private void showUsersSection() {
        hideAllSections();
        if (usersSection != null) usersSection.setVisible(true);
        highlightButton(usersButton);
        // loadUsers();
    }
    
    private void showCoursesSection() {
        hideAllSections();
        if (coursesSection != null) coursesSection.setVisible(true);
        highlightButton(coursesButton);
        // loadCourses();
    }
    
    private void showDepartmentsSection() {
        hideAllSections();
        if (departmentsSection != null) departmentsSection.setVisible(true);
        highlightButton(departmentsButton);
    }
    
    private void showSettingsSection() {
        hideAllSections();
        if (settingsSection != null) settingsSection.setVisible(true);
        highlightButton(settingsButton);
    }
    
    private void hideAllSections() {
        if (dashboardSection != null) dashboardSection.setVisible(false);
        if (usersSection != null) usersSection.setVisible(false);
        if (coursesSection != null) coursesSection.setVisible(false);
        if (departmentsSection != null) departmentsSection.setVisible(false);
        if (settingsSection != null) settingsSection.setVisible(false);
    }
    
    private void highlightButton(Button btn) {
        // Reset styles
        String defaultStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-padding: 10 20; -fx-cursor: hand;";
        if (dashboardButton != null) dashboardButton.setStyle(defaultStyle);
        if (usersButton != null) usersButton.setStyle(defaultStyle);
        if (coursesButton != null) coursesButton.setStyle(defaultStyle);
        if (departmentsButton != null) departmentsButton.setStyle(defaultStyle);
        if (settingsButton != null) settingsButton.setStyle(defaultStyle);
        
        // Highlight active
        if (btn != null) {
            btn.setStyle("-fx-background-color: #004d40; -fx-text-fill: white; -fx-font-weight: bold; -fx-alignment: CENTER_LEFT; -fx-padding: 10 20; -fx-cursor: hand;");
        }
    }
    
    @FXML
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
        }
    }
}