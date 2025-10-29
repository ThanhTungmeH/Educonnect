package org.example.educonnect1.client.controllers;

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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
public class Login implements Initializable {
    public Label forgotPW;
    @FXML
    private TextField EmailTextfield;
    @FXML
    private PasswordField PasswordtextField;
    @FXML
    private Button btnsignin;
    Stage stage;
    @FXML
    public void handleLogin() {
        String email = EmailTextfield.getText();
        String password = PasswordtextField.getText();
        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter both email and password.");
            return;
        }
        try (Socket socket = new Socket("localhost", 2005);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("LOGIN");
            out.writeObject(email);
            out.writeObject(password);

            String response = (String) in.readObject();

            switch (response) {
                case "SUCCESS":
                    User user = (User) in.readObject();
                    //  Lưu user hiện tại
                    SessionManager.setCurrentUser(user);
                    //  Kiểm tra role để load giao diện phù hợp
                    btnsignin.getScene().getWindow().hide();
                    stage = new Stage();
                    Parent root;
                  if("admin".equalsIgnoreCase(user.getRole())) {
                        root = FXMLLoader.load(getClass().getResource("/org/example/educonnect1/Client/AdminDashboard.fxml"));
                    } else {
                        root = FXMLLoader.load(getClass().getResource("/org/example/educonnect1/Client/Home.fxml"));
                    }
                    Scene scene = new Scene(root, 1500, 750);

                    stage.setScene(scene);
                    stage.centerOnScreen();
                    stage.setResizable(false);
                    stage.show();
                    break;
                case "NOT_VERIFIED":
                    showAlert(Alert.AlertType.INFORMATION, "Account Not Verified", "Please verify your email before signing in.");
                    break;
                case "FAILED":
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid email or password.");
                    break;
            }

        }
        catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while trying to sign in.");
        }
    }

    public void SwitchToForgetPW(MouseEvent mouseEvent) {
    }

    public void SwitchToSignup(MouseEvent mouseEvent) throws IOException {
        btnsignin.getScene().getWindow().hide();
        Parent root= FXMLLoader.load(getClass().getResource("/org/example/educonnect1/Client/Signup.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/org/example/educonnect1/Client/Login.css").toExternalForm());
        stage = new Stage();
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

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

    }
}
