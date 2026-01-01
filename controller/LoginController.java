package dillauniversity.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;

public class LoginController {
    
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private CheckBox rememberCheckBox;
    @FXML private VBox errorBox;
    @FXML private Label errorLabel;
    @FXML private Label dbStatusLabel;
    
    @FXML
    public void initialize() {
        System.out.println("LoginController initialized");
        
        // Populate role combo box WITH DEAN ROLE
        if (roleComboBox != null) {
            roleComboBox.setItems(FXCollections.observableArrayList(
                "Student", 
                "Teacher", 
                "Dean",        // Added Dean role
                "Admin"
            ));
            roleComboBox.setValue("Student"); // Set default value
        } else {
            System.err.println("roleComboBox is null!");
        }
        
        // Add event handlers
        if (loginButton != null) {
            loginButton.setOnAction(e -> handleLogin());
        } else {
            System.err.println("loginButton is null!");
        }
        
        // Handle register link click
        if (registerLink != null) {
            registerLink.setOnAction(e -> navigateToRegistration());
            System.out.println("Register link found and handler set");
        } else {
            System.err.println("registerLink is null! Check FXML for fx:id='registerLink'");
        }
        
        // Handle forgot password link
        if (forgotPasswordLink != null) {
            forgotPasswordLink.setOnAction(e -> handleForgotPassword());
        }
        
        // Check database connection
        checkDatabaseConnection();
        
        // Load saved credentials if "Remember me" was checked
        loadSavedCredentials();
        
        // Set Enter key to trigger login
        passwordField.setOnAction(e -> handleLogin());
    }
    
    private void handleLogin() {
        System.out.println("Login button clicked");
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();
        
        System.out.println("Attempting login for user: " + username + ", role: " + role);
        
        if (username.isEmpty() || password.isEmpty() || role == null) {
            showError("Please fill all fields!");
            return;
        }
        
        try {
            // Authenticate user with database
            boolean authenticated = authenticateUser(username, password, role);
            System.out.println("Authentication result: " + authenticated);
            
            if (authenticated) {
                // Save credentials if "Remember me" is checked
                if (rememberCheckBox.isSelected()) {
                    saveCredentials(username, password);
                } else {
                    clearSavedCredentials();
                }
                
                // Navigate to appropriate dashboard based on role
                System.out.println("Navigating to dashboard for role: " + role);
                switch (role) {
                    case "Student":
                        navigateToStudentDashboard(username);
                        break;
                    case "Teacher":
                        navigateToTeacherDashboard(username);
                        break;
                    case "Dean":  // NEW: Handle Dean login
                        navigateToDeanDashboard(username);
                        break;
                    case "Admin":
                        navigateToAdminDashboard(username);
                        break;
                    default:
                        showError("Invalid role selected!");
                }
            } else {
                showError("Invalid username or password!");
            }
        } catch (Exception e) {
            System.err.println("Exception during login process:");
            e.printStackTrace();
            showError("Login Error: " + e.getMessage());
        }
    }
    
    private boolean authenticateUser(String username, String password, String role) {
        try {
            // Connect to database and authenticate
            dillauniversity.dao.UserDAO userDAO = new dillauniversity.dao.UserDAO();
            return userDAO.authenticateUser(username, password, role);
        } catch (Exception e) {
            System.err.println("Authentication error: " + e.getMessage());
            return false;
        }
    }
    
    private void navigateToStudentDashboard(String username) {
        try {
            System.out.println("Navigating to Student Dashboard...");
            
            // Get FXML URL
            URL resourceUrl = getClass().getResource("/dillauniversity/resources/StudentDashboard.fxml");
            
            if (resourceUrl == null) {
                System.err.println("ERROR: StudentDashboard.fxml not found!");
                showAlert("Error", "Student dashboard file not found!");
                return;
            }
            
            System.out.println("Loading FXML from: " + resourceUrl);
            
            // Load FXML
            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Parent dashboardRoot = loader.load();
            System.out.println("FXML loaded successfully");
            
            // Get controller and set username
            Object controller = loader.getController();
            if (controller != null) {
                System.out.println("Controller found: " + controller.getClass().getName());
                try {
                    java.lang.reflect.Method method = controller.getClass().getMethod("setUsername", String.class);
                    method.invoke(controller, username);
                    System.out.println("Username set in controller");
                } catch (Exception e) {
                    System.out.println("setUsername method not found in student controller: " + e.getMessage());
                }
            } else {
                System.err.println("Controller is null!");
            }
            
            // Get current stage from login button
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            if (currentStage == null) {
                System.err.println("Current stage is null!");
                return;
            }
            
            // Set new scene
            Scene dashboardScene = new Scene(dashboardRoot);

            // Attach application stylesheet (CSS won't work unless it is added to the Scene)
            URL cssUrl = getClass().getResource("/dillauniversity/resources/styles.css");
            if (cssUrl != null) {
                dashboardScene.getStylesheets().add(cssUrl.toExternalForm());
                System.out.println("Applied stylesheet: " + cssUrl);
            } else {
                System.err.println("styles.css not found at /dillauniversity/resources/styles.css");
            }

            currentStage.setScene(dashboardScene);
            currentStage.setTitle("Dilla University - Student Dashboard");
            currentStage.setMaximized(true);
            currentStage.show();
            
            System.out.println("Student dashboard scene set and shown!");
            
        } catch (Exception e) {
            System.err.println("ERROR loading student dashboard:");
            e.printStackTrace();
            showAlert("Error", "Failed to load student dashboard: " + e.getMessage());
        }
    }
    
    private void navigateToTeacherDashboard(String username) {
        try {
            System.out.println("Navigating to Teacher Dashboard...");

            URL fxmlUrl = getClass().getResource("/dillauniversity/resources/teacher_dashboard.fxml");
            if (fxmlUrl == null) {
                System.err.println("❌ Teacher dashboard not found: /dillauniversity/resources/teacher_dashboard.fxml");
                showError("Teacher dashboard not found!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent dashboardRoot = loader.load();

            // Get controller and pass username
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    java.lang.reflect.Method method = controller.getClass().getMethod("setUsername", String.class);
                    method.invoke(controller, username);
                    System.out.println("✓ Username set in teacher controller");
                } catch (Exception e) {
                    System.out.println("setUsername method not found in teacher controller");
                }
            }

            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            Scene dashboardScene = new Scene(dashboardRoot);

            URL baseCssUrl = getClass().getResource("/dillauniversity/resources/styles.css");
            if (baseCssUrl != null) {
                dashboardScene.getStylesheets().add(baseCssUrl.toExternalForm());
                System.out.println("Applied teacher stylesheet: " + baseCssUrl);
            }

            currentStage.setScene(dashboardScene);
            currentStage.setTitle("Dilla University - Teacher Dashboard");
            currentStage.setMaximized(true);
            currentStage.show();

            System.out.println("Successfully loaded teacher dashboard!");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to load teacher dashboard: " + e.getMessage());
        }
    }

    private void navigateToDeanDashboard(String username) {
        try {
            System.out.println("Navigating to Dean Dashboard...");

            URL fxmlUrl = getClass().getResource("/dillauniversity/resources/dean_dashboard.fxml");
            if (fxmlUrl == null) {
                System.err.println("❌ Dean dashboard not found: /dillauniversity/resources/dean_dashboard.fxml");
                showError("Dean dashboard not found. Please contact administrator.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent dashboardRoot = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try {
                    java.lang.reflect.Method method = controller.getClass().getMethod("setUsername", String.class);
                    method.invoke(controller, username);
                    System.out.println("✓ Username set in dean controller");
                } catch (Exception e) {
                    System.out.println("Note: setUsername method not found in dean controller");
                }
            }

            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            Scene dashboardScene = new Scene(dashboardRoot);

            URL baseCssUrl = getClass().getResource("/dillauniversity/resources/styles.css");
            if (baseCssUrl != null) {
                dashboardScene.getStylesheets().add(baseCssUrl.toExternalForm());
                System.out.println("Applied dean stylesheet: " + baseCssUrl);
            }

            currentStage.setScene(dashboardScene);
            currentStage.setTitle("Dilla University - Dean Dashboard");
            currentStage.setMaximized(true);
            currentStage.show();

            System.out.println("✓ Dean dashboard loaded successfully!");
        } catch (Exception e) {
            System.err.println("❌ ERROR loading dean dashboard:");
            e.printStackTrace();
            showError("Unable to load dean dashboard: " + e.getMessage());
        }
    }

    private void navigateToAdminDashboard(String username) {
        try {
            System.out.println("Navigating to Admin Dashboard...");
            URL fxmlUrl = getClass().getResource("/dillauniversity/resources/admin_dashboard.fxml");
            if (fxmlUrl == null) {
                System.err.println("❌ Admin dashboard not found: /dillauniversity/resources/admin_dashboard.fxml");
                showError("Admin dashboard not found!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent dashboardRoot = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try {
                    java.lang.reflect.Method method = controller.getClass().getMethod("setUsername", String.class);
                    method.invoke(controller, username);
                    System.out.println("✓ Username set in admin controller");
                } catch (Exception e) {
                    System.out.println("setUsername method not found in admin controller");
                }
            }

            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            Scene dashboardScene = new Scene(dashboardRoot);

            URL baseCssUrl = getClass().getResource("/dillauniversity/resources/styles.css");
            if (baseCssUrl != null) {
                dashboardScene.getStylesheets().add(baseCssUrl.toExternalForm());
                System.out.println("Applied admin stylesheet: " + baseCssUrl);
            }

            currentStage.setScene(dashboardScene);
            currentStage.setTitle("Dilla University - Admin Dashboard");
            currentStage.setMaximized(true);
            currentStage.show();

            System.out.println("Successfully loaded admin dashboard!");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to load admin dashboard: " + e.getMessage());
        }
    }

    private void navigateToRegistration() {
        try {
            System.out.println("Navigating to registration...");
            URL fxmlUrl = getClass().getResource("/dillauniversity/resources/RegistrationForm.fxml");
            if (fxmlUrl == null) {
                System.err.println("❌ Registration form not found: /dillauniversity/resources/RegistrationForm.fxml");
                showError("Registration form not found!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent registrationRoot = loader.load();

            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            Scene registrationScene = new Scene(registrationRoot);

            URL baseCssUrl = getClass().getResource("/dillauniversity/resources/styles.css");
            if (baseCssUrl != null) {
                registrationScene.getStylesheets().add(baseCssUrl.toExternalForm());
                System.out.println("Applied base stylesheet: " + baseCssUrl);
            }

            URL registrationCssUrl = getClass().getResource("/dillauniversity/resources/registration-styles.css");
            if (registrationCssUrl != null) {
                registrationScene.getStylesheets().add(registrationCssUrl.toExternalForm());
                System.out.println("Applied registration stylesheet: " + registrationCssUrl);
            }

            currentStage.setScene(registrationScene);
            currentStage.setTitle("Dilla University - Registration");
            currentStage.centerOnScreen();
            System.out.println("Successfully loaded registration form!");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to load registration form: " + e.getMessage());
        }
    }

    private void handleForgotPassword() {
        // Show forgot password dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Forgot Password");
        alert.setHeaderText("Password Recovery");
        alert.setContentText("Please contact the system administrator at:\nadmin@dilla.edu.et\nPhone: +251-XXX-XXXXXX");
        alert.showAndWait();
    }
    
    private void checkDatabaseConnection() {
        try {
            // Check database connection
            boolean connected = dillauniversity.database.DatabaseConnection.testConnection();
            if (connected) {
                dbStatusLabel.setText("✓ Database Connected");
                dbStatusLabel.setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
            } else {
                dbStatusLabel.setText("✗ Database Connection Failed");
                dbStatusLabel.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            dbStatusLabel.setText("✗ Database Error");
            dbStatusLabel.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
            System.err.println("Database connection error: " + e.getMessage());
        }
    }
    
    private void loadSavedCredentials() {
        // TODO: Load saved credentials from file or preferences
        // For now, just clear fields
        usernameField.clear();
        passwordField.clear();
        rememberCheckBox.setSelected(false);
    }
    
    private void saveCredentials(String username, String password) {
        // TODO: Save credentials to file or preferences (use encryption)
        System.out.println("Credentials saved for: " + username);
    }
    
    private void clearSavedCredentials() {
        // TODO: Clear saved credentials from storage
        System.out.println("Credentials cleared");
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        if (errorBox != null && errorLabel != null) {
            errorLabel.setText(message);
            errorBox.setVisible(true);
            errorBox.setManaged(true);
            
            // Auto-hide error after 5 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    javafx.application.Platform.runLater(() -> {
                        errorBox.setVisible(false);
                        errorBox.setManaged(false);
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            System.err.println("Error display components not found: " + message);
            // Show alert as fallback
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Login Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }
    
    // Method to clear form (can be called if needed)
    public void clearForm() {
        usernameField.clear();
        passwordField.clear();
        roleComboBox.setValue("Student");
        rememberCheckBox.setSelected(false);
        if (errorBox != null) {
            errorBox.setVisible(false);
            errorBox.setManaged(false);
        }
    }
    
    // Getter for testing
    public ComboBox<String> getRoleComboBox() {
        return roleComboBox;
    }
}