package org.example.educonnect1.client.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class VerifyController {
    private Stage signupStage;
    private Stage verifyStage;
    @FXML
    private TextField codeField;
    private Stage stage;
    @FXML
    private Label statusLabel;
    @FXML
    private Button btnVerify;

    private String email; // Email được truyền từ bước đăng ký

    public void setEmail(String email) {
        this.email = email;
    }

    @FXML
    private void handleVerify() throws IOException {
        String code = codeField.getText().trim();

        if (code.isEmpty()) {
            statusLabel.setText("Please enter the verification code.");
            return;
        }


        try (Socket socket = new Socket("localhost", 2005);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject("VERIFY");
            out.writeObject(email);
            out.writeObject(code);
            String response = (String) in.readObject();
            switch (response) {
                case "SUCCESS":
                    statusLabel.setText("Verification successful!");
                    if (verifyStage != null) verifyStage.close();
                    if (signupStage != null) signupStage.close();
                    Parent root = FXMLLoader.load(getClass().getResource("/org/example/educonnect1/Client/Login.fxml"));
                    Scene scene = new Scene(root);
                    scene.getStylesheets().add(getClass().getResource("/org/example/educonnect1/Client/Login.css").toExternalForm());
                    stage = new Stage();
                    stage.setScene(scene);
                    stage.setResizable(false);
                    stage.show();
                    break;
                case "USER_NOT_FOUND":
                    statusLabel.setText("User not found!");
                    break;
                case "INVALID_CODE":
                    statusLabel.setText("Invalid verification code!");
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Connection error occurred!");
        }
    }

    public void setSignupStage(Stage signupStage) {
        this.signupStage = signupStage;
    }
}