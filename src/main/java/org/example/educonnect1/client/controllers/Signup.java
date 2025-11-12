package org.example.educonnect1.client.controllers;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.*;
import org.example.educonnect1.client.utils.SocketManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Signup implements Initializable {

    @FXML private TextField yourname;
    @FXML private TextField EMSignup;
    @FXML private PasswordField PWSignup;
    @FXML private PasswordField Re_enterPW;
    @FXML private Button btnsignup;
    @FXML private Label labelLogin;
    @FXML private StackPane loadingOverlay;
    @FXML private ProgressIndicator loadingIndicator;

    Stage stage;

    @FXML
    public void btnUserSignup(ActionEvent event) {
        String name = yourname.getText().trim();
        String email = EMSignup.getText().trim().toLowerCase();
        String pass = PWSignup.getText();
        String rePass = Re_enterPW.getText();

        // Validation
        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || rePass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please enter all required fields.");
            return;
        }
        if (!pass.equals(rePass)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Passwords do not match.");
            return;
        }
        if (!isValidEmail(email)) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Invalid email format.");
            return;
        }
        if (pass.length() < 6) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Password must be at least 6 characters.");
            return;
        }

        // Disable button và show loading
        showLoading(true);
        btnsignup.setDisable(true);
        // Thực hiện signup trong background thread
        Task<SignupResult> signupTask = new Task<SignupResult>() {
            @Override
            protected SignupResult call() throws Exception {
                try {
                    SocketManager socketManager = SocketManager.getInstance();
                    // Gửi signup request
                    socketManager.sendRequest("SIGNUP", name, email, pass);
                    // Đọc response
                    String response = (String) socketManager.readResponse();
                    return new SignupResult(true, response, null);
                } catch (IOException e) {
                    return new SignupResult(false, "CONNECTION_ERROR", e.getMessage());
                } catch (ClassNotFoundException e) {
                    return new SignupResult(false, "INVALID_RESPONSE", e.getMessage());
                } catch (Exception e) {
                    return new SignupResult(false, "ERROR", e.getMessage());
                }
            }
        };

        // Xử lý kết quả trên JavaFX thread
        signupTask.setOnSucceeded(e -> {
            showLoading(false);
            btnsignup.setDisable(false);

            SignupResult result = signupTask.getValue();
            handleSignupResult(result, email, event);
        });

        signupTask.setOnFailed(e -> {
            showLoading(false);
            btnsignup.setDisable(false);

            Throwable exception = signupTask.getException();
            showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred: " + exception.getMessage());
        });

        // Chạy task trong background thread
        new Thread(signupTask).start();
    }

    /**
     * Xử lý kết quả signup
     */
    private void handleSignupResult(SignupResult result, String email, ActionEvent event) {
        switch (result.status) {
            case "SUCCESS":
                try {
                    openVerifyWindow(email, event);
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not open verification window.");
                }
                break;

            case "RESENT_CODE":
                showAlert(Alert.AlertType.INFORMATION, "Resent Verification Code",
                        "We've sent you a new verification code!");
                try {
                    openVerifyWindow(email, event);
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Error", "Could not open verification window.");
                }
                break;

            case "EMAIL_EXISTS":
                showAlert(Alert.AlertType.ERROR, "Registration",
                        "Email already exists and is verified.");
                break;

            case "FAILED":
                showAlert(Alert.AlertType.ERROR, "Signup Failed",
                        "Could not complete registration. Please try again.");
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
                        "Something went wrong: " + (result.errorMessage != null ? result.errorMessage : "Unknown error"));
                break;
        }
    }

    /**
     * Mở cửa sổ verify email
     */
    private void openVerifyWindow(String email, ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/educonnect1/Client/verify.fxml"));
        Parent root = loader.load();

        VerifyController verifyController = loader.getController();
        verifyController.setEmail(email);

        Stage signupStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        verifyController.setSignupStage(signupStage);

        Stage verifyStage = new Stage();
        verifyStage.setTitle("Email Verification");
        verifyStage.initModality(Modality.APPLICATION_MODAL);
        verifyStage.initOwner(signupStage);
        verifyStage.setScene(new Scene(root));
        verifyStage.setResizable(false);
        verifyStage.show();
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(show);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    @FXML
    public void goToLogin(MouseEvent mouseEvent) {
        try {
            labelLogin.getScene().getWindow().hide();
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/educonnect1/Client/Login.fxml"));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/org/example/educonnect1/Client/Login.css").toExternalForm());

            stage = new Stage();
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load login screen.");
        }
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize loading overlay
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(false);
        }
    }
    private static class SignupResult {
        boolean success;
        String status;
        String errorMessage;
        SignupResult(boolean success, String status, String errorMessage) {
            this.success = success;
            this.status = status;
            this.errorMessage = errorMessage;
        }
    }
}