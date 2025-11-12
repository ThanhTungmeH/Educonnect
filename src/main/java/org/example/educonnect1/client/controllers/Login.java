package org.example.educonnect1.client.controllers;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.example.educonnect1.client.models.User;
import org.example.educonnect1.client.utils.SessionManager;
import org.example.educonnect1.client.utils.SocketManager;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
public class Login implements Initializable {

    @FXML public Label forgotPW;
    @FXML private TextField EmailTextfield;
    @FXML private PasswordField PasswordtextField;
    @FXML private Button btnsignin;
    Stage stage;
    @FXML
    public void handleLogin() {
        String email = EmailTextfield.getText().trim();
        String password = PasswordtextField.getText();
        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter both email and password.");
            return;
        }
        // Disable button và show loading
        btnsignin.setDisable(true);
        // Thực hiện login trong background thread
        Task<LoginResult> loginTask = new Task<LoginResult>() {
            @Override
            protected LoginResult call() throws Exception {
                try {
                    SocketManager socketManager = SocketManager.getInstance();
                    // Gửi login request
                    socketManager.sendRequest("LOGIN", email, password);
                    // Đọc response
                    String response = (String) socketManager.readResponse();
                    if ("SUCCESS".equals(response)) {
                        User user = (User) socketManager.readResponse();
                        return new LoginResult(true, response, user, null);
                    } else {
                        return new LoginResult(false, response, null, null);
                    }

                } catch (IOException e) {
                    return new LoginResult(false, "CONNECTION_ERROR", null, e.getMessage());
                } catch (ClassNotFoundException e) {
                    return new LoginResult(false, "INVALID_RESPONSE", null, e.getMessage());
                } catch (Exception e) {
                    return new LoginResult(false, "ERROR", null, e.getMessage());
                }
            }
        };

        // Xử lý kết quả trên JavaFX thread
        loginTask.setOnSucceeded(event -> {
            btnsignin.setDisable(false);
            LoginResult result = loginTask.getValue();
            handleLoginResult(result);
        });
        loginTask.setOnFailed(event -> {
            btnsignin.setDisable(false);
            Throwable exception = loginTask.getException();
            showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred: " + exception.getMessage());
        });
        // Chạy task trong background thread
        new Thread(loginTask).start();
    }
    /**
     * Xử lý kết quả login
     */
    private void handleLoginResult(LoginResult result) {
        switch (result.status) {
            case "SUCCESS":
                try {
                    // Lưu user hiện tại
                    SessionManager.setCurrentUser(result.user);

                    // Đóng cửa sổ login
                    btnsignin.getScene().getWindow().hide();
                    // Mở giao diện phù hợp
                    stage = new Stage();
                    Parent root;
                    if ("admin".equalsIgnoreCase(result.user.getRole())) {
                        root = FXMLLoader.load(getClass().getResource("/org/example/educonnect1/Client/AdminDashboard.fxml"));
                    } else {
                        root = FXMLLoader.load(getClass().getResource("/org/example/educonnect1/Client/MainLayout.fxml"));
                    }
                    Scene scene = new Scene(root, 1500, 750);
                    stage.setScene(scene);
                    stage.centerOnScreen();
                    stage.setResizable(false);

                    // Xử lý khi đóng app
                    stage.setOnCloseRequest(e -> {
                        SocketManager.getInstance().disconnect();
                        Platform.exit();
                    });

                    stage.show();

                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not load main screen.");
                }
                break;

            case "NOT_VERIFIED":
                showAlert(Alert.AlertType.INFORMATION, "Account Not Verified",
                        "Please verify your email before signing in.");
                break;
            case "FAILED":
                showAlert(Alert.AlertType.ERROR, "Login Failed",
                        "Invalid email or password.");
                break;
            case "CONNECTION_ERROR":
                showAlert(Alert.AlertType.ERROR, "Connection Error",
                        "Could not connect to server. Please check:\n" +
                                "1. Server is running\n" +
                                "2. Network connection\n" +
                                "3. Firewall settings");
                break;

            case "INVALID_RESPONSE":
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Invalid response from server. Please try again.");
                break;

            default:
                showAlert(Alert.AlertType.ERROR, "Error",
                        "An error occurred: " + (result.errorMessage != null ? result.errorMessage : "Unknown error"));
                break;
        }
    }
    @FXML
    public void SwitchToForgetPW(MouseEvent mouseEvent) {
        // TODO: Implement forgot password
        showAlert(Alert.AlertType.INFORMATION, "Coming Soon", "Forgot password feature will be available soon.");
    }
    @FXML
    public void SwitchToSignup(MouseEvent mouseEvent) {
        try {
            btnsignin.getScene().getWindow().hide();
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/educonnect1/Client/Signup.fxml"));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/org/example/educonnect1/Client/Login.css").toExternalForm());
            stage = new Stage();
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load signup screen.");
        }
    }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize loading overlay
    }
    private static class LoginResult {
        boolean success;
        String status;
        User user;
        String errorMessage;

        LoginResult(boolean success, String status, User user, String errorMessage) {
            this.success = success;
            this.status = status;
            this.user = user;
            this.errorMessage = errorMessage;
        }
    }
}