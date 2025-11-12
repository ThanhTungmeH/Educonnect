package org.example.educonnect1.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import org.example.educonnect1.client.models.User;
import org.example.educonnect1.client.utils.SessionManager;
import org.example.educonnect1.client.utils.SocketManager;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class MainLayoutController implements Initializable {
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private StackPane contentPane;
    @FXML
    private Label lblUserName;

    @FXML
    private ImageView avatarImage;
    @FXML
    private TextField searchField;
    private final Map<String, Parent> viewCache = new HashMap<>();

    private final Map<String, Node> scenes = new HashMap<>();

    public void onHome(ActionEvent actionEvent) {
        showView("Home.fxml");
    }

    public void onFriends(ActionEvent actionEvent) {
        showView("FriendsList.fxml");
    }

    public void onGroups(ActionEvent actionEvent) {
    }

    public void onNotifications(ActionEvent actionEvent) {
    }

    public void onMessages(ActionEvent actionEvent) {
        showView("Chat.fxml");
    }

    public void onProfile(MouseEvent mouseEvent) {
        showView("Profile.fxml");

    }

    public void onLibrary(MouseEvent mouseEvent) {
    }

    public void onEvents(MouseEvent mouseEvent) {
    }

    public void Search(ActionEvent actionEvent) {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            System.out.println("Search query is empty");
            return;
        }

        new Thread(() -> {
            try {
                SocketManager socket = SocketManager.getInstance();
                socket.sendRequest("SEARCH_FRIEND", query);
                Object response = socket.readResponse();
                
                if (response instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<User> users = (List<User>) response;
                    
                    javafx.application.Platform.runLater(() -> {
                        try {
                            URL fxmlUrl = getClass().getResource("/org/example/educonnect1/Client/SearchFriend.fxml");
                            FXMLLoader loader = new FXMLLoader(fxmlUrl);
                            Parent view = loader.load();
                            
                            SearchFriendController controller = loader.getController();
                            controller.displayResults(users);
                            
                            contentPane.getChildren().clear();
                            contentPane.getChildren().add(view);
                            
                            System.out.println("✅ Search completed: " + users.size() + " results");
                        } catch (IOException e) {
                            System.err.println("❌ Error loading search view: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                } else {
                    System.err.println("Unexpected response type: " + response);
                }
            } catch (Exception e) {
                System.err.println("Search failed: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
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
                Circle clip = new Circle(radius, radius, radius);
                avatarImage.setClip(clip);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Failed to load avatar image");
            }
        } else {
            System.out.println("Current user is null! Session not set correctly.");
        }
        javafx.application.Platform.runLater(() -> showView("Home.fxml"));

    }
    /**
     * Load view từ cache hoặc FXML file và hiển thị trong contentPane
     * @param fxmlFileName Tên file FXML (ví dụ: "Profile.fxml")
     */
    private void showView(String fxmlFileName) {
        try {
            Parent view = viewCache.get(fxmlFileName);
            // Nếu chưa cache, load từ FXML
            if (view == null) {
                URL fxmlUrl = getClass().getResource("/org/example/educonnect1/Client/" + fxmlFileName);
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                view = loader.load();
                viewCache.put(fxmlFileName, view); // Lưu vào cache
                System.out.println("📦 Loaded từ FXML: " + fxmlFileName);
            } else {
                System.out.println("⚡ Loaded từ cache: " + fxmlFileName);
            }

            // Hiển thị view
            contentPane.getChildren().clear();
            contentPane.getChildren().add(view);
            System.out.println("✅ Chuyển sang: " + fxmlFileName);

        } catch (IOException e) {
            System.out.println("❌ Lỗi load " + fxmlFileName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}

