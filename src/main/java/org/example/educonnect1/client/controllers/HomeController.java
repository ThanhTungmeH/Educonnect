package org.example.educonnect1.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import org.example.educonnect1.client.models.User;
import org.example.educonnect1.client.utils.SessionManager;
import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {
    @FXML
    private Label lblUserName;
    @FXML
    private ImageView avatarImage;
    @FXML
    private TextField searchField;

    public void onHome(ActionEvent actionEvent) {
    }

    public void onFriends(ActionEvent actionEvent) {
    }

    public void onGroups(ActionEvent actionEvent) {
    }

    public void onNotifications(ActionEvent actionEvent) {
    }

    public void onMessages(ActionEvent actionEvent) {
    }

    public void onProfile(MouseEvent mouseEvent) {
    }

    public void onCreateStory(MouseEvent mouseEvent) {
    }

    public void onLibrary(MouseEvent mouseEvent) {
    }

    public void onEvents(MouseEvent mouseEvent) {
    }
    public void Search(ActionEvent actionEvent) {
        String query= searchField.getText().trim();

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            lblUserName.setText(currentUser.getFullName());
            String avatarUrl = currentUser.getAvatar();
            if (avatarUrl == null || avatarUrl.isEmpty()) {
                avatarUrl = "https://res.cloudinary.com/do46eak3c/image/upload/v1761648489/anhmd_fqwsrr.jpg";
            }
            try {
                Image image = new Image(avatarUrl, true); // load background
                avatarImage.setImage(image);

                // Clip hình tròn
                double radius = avatarImage.getFitWidth() / 2;
                ;
                Circle clip = new Circle(radius, radius, radius);
                avatarImage.setClip(clip);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to load avatar image");
            }
        } else {
            System.out.println("Current user is null! Session not set correctly.");
        }

    }


}

