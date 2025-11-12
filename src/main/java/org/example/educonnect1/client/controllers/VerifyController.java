package org.example.educonnect1.client.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.educonnect1.client.utils.SocketManager;
public class VerifyController {
    private Stage signupStage;
    @FXML
    private TextField codeField;
    private Stage stage = new Stage();
    @FXML
    private Label statusLabel;
    @FXML
    private Button btnVerify;
    @FXML private ProgressIndicator loadingIndicator;
    private String email;

    public void setEmail(String email) {
        this.email = email;
    }

    public void setSignupStage(Stage stage) {
        this.signupStage = stage;
    }

    @FXML
    public void handleVerify() {
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please enter verification code.");
            return;
        }

        btnVerify.setDisable(true);
        showLoading(true);

        // Background task
        Task<VerifyResult> verifyTask = new Task<VerifyResult>() {
            @Override
            protected VerifyResult call() throws Exception {
                try {
                    SocketManager socketManager = SocketManager.getInstance();

                    // Gửi VERIFY request (VerifyCommand sẽ xử lý)
                    socketManager.sendRequest("VERIFY", email, code);

                    // Đọc response
                    String response = (String) socketManager.readResponse();

                    return new VerifyResult(true, response, null);

                } catch (Exception e) {
                    return new VerifyResult(false, "ERROR", e.getMessage());
                }
            }
        };

        verifyTask.setOnSucceeded(event -> {
            btnVerify.setDisable(false);
            showLoading(false);

            VerifyResult result = verifyTask.getValue();

            if ("SUCCESS".equals(result.status)) {
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Email verified successfully! You can now login.");

                // Đóng verify window và signup window
                ((Stage) btnVerify.getScene().getWindow()).close();
                if (signupStage != null) {
                    signupStage.close();
                }

                // Mở login window
                try {
                    openLoginWindow();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if ("INVALID_CODE".equals(result.status)) {
                showAlert(Alert.AlertType.ERROR, "Invalid Code",
                        "The verification code is incorrect.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error",
                        "Verification failed: " + result.errorMessage);
            }
        });

        new Thread(verifyTask).start();
    }

    private void openLoginWindow() throws Exception {
        Parent root= FXMLLoader.load(getClass().getResource("/org/example/educonnect1/Client/Login.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/org/example/educonnect1/Client/Login.css").toExternalForm());
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(show);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class VerifyResult {
        boolean success;
        String status;
        String errorMessage;

        VerifyResult(boolean success, String status, String errorMessage) {
            this.success = success;
            this.status = status;
            this.errorMessage = errorMessage;
        }
    }
}