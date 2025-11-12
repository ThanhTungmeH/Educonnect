package org.example.educonnect1.client.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.example.educonnect1.client.models.User;
import org.example.educonnect1.client.utils.SessionManager;
import org.example.educonnect1.client.utils.SocketManager;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class FriendsListController implements Initializable {

    @FXML
    private VBox friendRequestsContainer;

    @FXML
    private VBox friendsListContainer;

    @FXML
    private Label requestCountBadge;

    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getCurrentUser();

        // Load tuần tự, không song song
        new Thread(() -> {
            loadFriendRequestsSync();
            loadFriendsSync();
        }).start();
    }

    /**
     * Load friend requests synchronously (trong thread hiện tại)
     */
    private void loadFriendRequestsSync() {
        try {
            System.out.println("👥 Loading friend requests for user: " + currentUser.getId());

            SocketManager socket = SocketManager.getInstance();

            // ✅ Đọc 2 responses cùng lúc
            Object[] responses = socket.sendRequestAndRead2Responses("GET_FRIEND_REQUESTS", currentUser.getId());

            Object statusResponse = responses[0];
            Object dataResponse = responses[1];

            if ("SUCCESS".equals(statusResponse)) {
                if (dataResponse instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> requests = (List<Map<String, Object>>) dataResponse;

                    System.out.println("✅ Loaded " + requests.size() + " friend requests");

                    Platform.runLater(() -> {
                        friendRequestsContainer.getChildren().clear();

                        if (requests.isEmpty()) {
                            requestCountBadge.setVisible(false);
                            Label noRequests = new Label("No pending friend requests");
                            noRequests.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
                            friendRequestsContainer.getChildren().add(noRequests);
                        } else {
                            requestCountBadge.setText(String.valueOf(requests.size()));
                            requestCountBadge.setVisible(true);

                            for (Map<String, Object> request : requests) {
                                HBox card = createFriendRequestCard(request);
                                if (card != null) {
                                    friendRequestsContainer.getChildren().add(card);
                                }
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading friend requests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadFriendsSync() {
        try {
            System.out.println("👥 Loading friends for user: " + currentUser.getId());

            SocketManager socket = SocketManager.getInstance();

            // ✅ Đọc 2 responses cùng lúc
            Object[] responses = socket.sendRequestAndRead2Responses("GET_FRIENDS", currentUser.getId());

            Object statusResponse = responses[0];
            Object dataResponse = responses[1];

            if ("SUCCESS".equals(statusResponse)) {
                if (dataResponse instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<User> friends = (List<User>) dataResponse;

                    System.out.println("✅ Loaded " + friends.size() + " friends");

                    Platform.runLater(() -> {
                        friendsListContainer.getChildren().clear();

                        if (friends.isEmpty()) {
                            Label noFriends = new Label("No friends yet. Start by searching for people!");
                            noFriends.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
                            friendsListContainer.getChildren().add(noFriends);
                        } else {
                            for (User friend : friends) {
                                HBox card = createFriendCard(friend);
                                if (card != null) {
                                    friendsListContainer.getChildren().add(card);
                                }
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading friends: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Async wrappers (tạo thread mới)
     */
    private void loadFriendRequests() {
        new Thread(this::loadFriendRequestsSync).start();
    }

    private void loadFriends() {
        new Thread(this::loadFriendsSync).start();
    }

    private HBox createFriendRequestCard(Map<String, Object> request) {
        try {
            // Validate
            if (!request.containsKey("id") || !request.containsKey("senderName")) {
                System.err.println("⚠️ Invalid friend request data");
                return null;
            }

            int requestId = (Integer) request.get("id");
            String senderName = (String) request.get("senderName");
            String senderAvatar = request.get("senderAvatar") != null ?
                    (String) request.get("senderAvatar") : "";

            HBox card = new HBox(15);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color: #fff3cd; -fx-background-radius: 10px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

            // Avatar
            ImageView avatar = new ImageView();
            avatar.setFitWidth(50);
            avatar.setFitHeight(50);
            avatar.setPreserveRatio(true);

            if (senderAvatar.isEmpty()) {
                senderAvatar = "https://res.cloudinary.com/do46eak3c/image/upload/v1761648489/anhmd_fqwsrr.jpg";
            }

            try {
                Image image = new Image(senderAvatar, true);
                avatar.setImage(image);
                Circle clip = new Circle(25, 25, 25);
                avatar.setClip(clip);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to load avatar");
            }

            // User info
            VBox userInfo = new VBox(5);
            Label nameLabel = new Label(senderName);
            nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            Label requestLabel = new Label("wants to be your friend");
            requestLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
            userInfo.getChildren().addAll(nameLabel, requestLabel);

            // Action buttons
            HBox buttons = new HBox(10);
            Button acceptButton = new Button("Accept");
            acceptButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                    "-fx-padding: 5px 15px; -fx-background-radius: 5px;");
            acceptButton.setOnAction(e -> handleAcceptFriend(requestId));

            Button rejectButton = new Button("Reject");
            rejectButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                    "-fx-padding: 5px 15px; -fx-background-radius: 5px;");
            rejectButton.setOnAction(e -> handleRejectFriend(requestId));

            buttons.getChildren().addAll(acceptButton, rejectButton);

            HBox.setHgrow(userInfo, javafx.scene.layout.Priority.ALWAYS);
            card.getChildren().addAll(avatar, userInfo, buttons);

            return card;

        } catch (Exception e) {
            System.err.println("❌ Error creating friend request card: " + e.getMessage());
            return null;
        }
    }

    private HBox createFriendCard(User friend) {
        try {
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

            String avatarUrl = friend.getAvatar();
            if (avatarUrl == null || avatarUrl.isEmpty()) {
                avatarUrl = "https://res.cloudinary.com/do46eak3c/image/upload/v1761648489/anhmd_fqwsrr.jpg";
            }

            try {
                Image image = new Image(avatarUrl, true);
                avatar.setImage(image);
                Circle clip = new Circle(25, 25, 25);
                avatar.setClip(clip);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to load avatar");
            }

            // User info
            VBox userInfo = new VBox(5);
            Label nameLabel = new Label(friend.getFullName());
            nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            userInfo.getChildren().add(nameLabel);

            // Message button
            Button messageButton = new Button("Nhắn tin");
            messageButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                    "-fx-padding: 5px 15px; -fx-background-radius: 5px;");
            messageButton.setOnAction(e -> handleMessageFriend(friend));

            HBox.setHgrow(userInfo, javafx.scene.layout.Priority.ALWAYS);
            card.getChildren().addAll(avatar, userInfo, messageButton);

            return card;

        } catch (Exception e) {
            System.err.println("❌ Error creating friend card: " + e.getMessage());
            return null;
        }
    }

    private void handleAcceptFriend(int requestId) {
        new Thread(() -> {
            try {
                System.out.println("✅ Accepting friend request: " + requestId);

                SocketManager socket = SocketManager.getInstance();
                socket.sendRequest("ACCEPT_FRIEND", requestId, currentUser.getId());
                Object response = socket.readResponse();

                System.out.println("📨 ACCEPT_FRIEND response: " + response);

                if ("SUCCESS".equals(response)) {
                    Platform.runLater(() -> {
                        // Reload tuần tự
                        new Thread(() -> {
                            loadFriendRequestsSync();
                            loadFriendsSync();
                        }).start();
                        System.out.println("✅ Friend request accepted");
                    });
                }
            } catch (Exception e) {
                System.err.println("❌ Error accepting friend request: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void handleRejectFriend(int requestId) {
        new Thread(() -> {
            try {
                System.out.println("❌ Rejecting friend request: " + requestId);

                SocketManager socket = SocketManager.getInstance();
                socket.sendRequest("REJECT_FRIEND", requestId, currentUser.getId());
                Object response = socket.readResponse();

                System.out.println("📨 REJECT_FRIEND response: " + response);

                if ("SUCCESS".equals(response)) {
                    Platform.runLater(() -> {
                        loadFriendRequests();
                        System.out.println("✅ Friend request rejected");
                    });
                }
            } catch (Exception e) {
                System.err.println("❌ Error rejecting friend request: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void handleMessageFriend(User friend) {
        try {
            System.out.println("💬 Opening chat with: " + friend.getFullName());

            // Get the main BorderPane from the scene
            BorderPane mainBorderPane = (BorderPane) friendsListContainer.getScene().getRoot();
            StackPane contentPane = (StackPane) mainBorderPane.getCenter();

            // Load Chat.fxml
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/educonnect1/Client/Chat.fxml"));
            Parent chatView = loader.load();

            // Display the chat view TRƯỚC
            contentPane.getChildren().clear();
            contentPane.getChildren().add(chatView);

            // ĐỢI một chút để ChatController.initialize() chạy xong
            new Thread(() -> {
                try {
                    Thread.sleep(500); // Đợi 500ms

                    // Sau đó mới gọi openConversationWithUser
                    ChatController chatController = loader.getController();
                    Platform.runLater(() -> chatController.openConversationWithUser(friend));

                    System.out.println("✅ Opened chat with: " + friend.getFullName());

                } catch (Exception e) {
                    System.err.println("❌ Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            System.err.println("❌ Error opening chat: " + e.getMessage());
            e.printStackTrace();
        }
    }
}