package org.example.educonnect1.client.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.example.educonnect1.client.models.User;
import org.example.educonnect1.client.utils.SessionManager;
import org.example.educonnect1.client.utils.SocketManager;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SearchFriendController implements Initializable {

    @FXML
    private VBox resultsContainer;

    private List<User> searchResults;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize if needed
    }

    public void displayResults(List<User> users) {
        this.searchResults = users;
        Platform.runLater(() -> {
            resultsContainer.getChildren().clear();
            
            if (users == null || users.isEmpty()) {
                Label noResults = new Label("No users found");
                noResults.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d;");
                resultsContainer.getChildren().add(noResults);
                return;
            }

            for (User user : users) {
                HBox userCard = createUserCard(user);
                resultsContainer.getChildren().add(userCard);
            }
        });
    }

    private HBox createUserCard(User user) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10px; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // Avatar
        ImageView avatar = new ImageView();
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);
        avatar.setPreserveRatio(true);
        
        String avatarUrl = user.getAvatar();
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            avatarUrl = "https://res.cloudinary.com/do46eak3c/image/upload/v1761648489/anhmd_fqwsrr.jpg";
        }
        
        try {
            Image image = new Image(avatarUrl, true);
            avatar.setImage(image);
            double radius = avatar.getFitWidth() / 2;
            Circle clip = new Circle(radius, radius, radius);
            avatar.setClip(clip);
        } catch (Exception e) {
            System.err.println("Failed to load avatar: " + e.getMessage());
        }

        // User info
        VBox userInfo = new VBox(5);
        Label nameLabel = new Label(user.getFullName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        userInfo.getChildren().add(nameLabel);

        // Add friend button
        Button addButton = new Button("Add Friend");
        addButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                          "-fx-padding: 5px 15px; -fx-background-radius: 5px;");
        addButton.setOnAction(e -> handleAddFriend(user));

        // Layout
        HBox.setHgrow(userInfo, javafx.scene.layout.Priority.ALWAYS);
        card.getChildren().addAll(avatar, userInfo, addButton);

        return card;
    }

    private void handleAddFriend(User user) {
        new Thread(() -> {
            try {
                User currentUser = SessionManager.getCurrentUser();
                SocketManager socket = SocketManager.getInstance();
                socket.sendRequest("ADD_FRIEND", currentUser.getId(), user.getId());
                Object response = socket.readResponse();
                
                if ("SUCCESS".equals(response)) {
                    Platform.runLater(() -> {
                        System.out.println("Friend request sent to: " + user.getFullName());
                        // Show success message
                    });
                } else {
                    Platform.runLater(() -> {
                        System.out.println("Failed to send friend request");
                    });
                }
            } catch (Exception e) {
                System.err.println("Error sending friend request: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }
}
