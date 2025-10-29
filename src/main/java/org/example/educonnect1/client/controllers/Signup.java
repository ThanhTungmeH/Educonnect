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
import org.example.educonnect1.Server.dao.UserDAO;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
    private final UserDAO usDAO = new UserDAO();

    @FXML
    public void btnUserSignup(ActionEvent event) throws IOException {
        String name = yourname.getText().trim();
        String email = EMSignup.getText().trim().toLowerCase();
        String pass = PWSignup.getText();
        String rePass = Re_enterPW.getText();

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
        // Hiển thị overlay loading
        showLoading(true);
        btnsignup.setDisable(true);

        try (Socket socket = new Socket("localhost", 2005);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("SIGNUP");
            out.writeObject(name);
            out.writeObject(email);
            out.writeObject(pass);

            String response = (String) in.readObject();

            showLoading(false);
            btnsignup.setDisable(false);
            switch (response) {
                case "SUCCESS":

                case "RESENT_CODE":
                    if (response.equals("RESENT_CODE")) {
                        showAlert(Alert.AlertType.INFORMATION, "Resent Verification Code", "We've sent you a new verification code!");
                    }
                    openVerifyWindow(email, event);
                    break;
                case "EMAIL_EXISTS":
                    showAlert(Alert.AlertType.ERROR, "Registration", "Email already exists and is verified.");
                    break;
                case "FAILED":
                    showAlert(Alert.AlertType.ERROR, "Signup Failed", "Could not complete registration.");
                    break;
            }
        } catch (Exception e) {
            showLoading(false);
            btnsignup.setDisable(false);
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Something went wrong during signup.");
        }
    }
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
        verifyStage.show();
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisible(show);
    }
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    public void goToLogin(MouseEvent mouseEvent) throws IOException {
        labelLogin.getScene().getWindow().hide();
        Parent root= FXMLLoader.load(getClass().getResource("/org/example/educonnect1/Client/Login.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/org/example/educonnect1/Client/Login.css").toExternalForm());
        stage = new Stage();
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}
