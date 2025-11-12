package org.example.educonnect1.client.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.example.educonnect1.client.models.User;
import org.example.educonnect1.client.utils.SessionManager;
import org.example.educonnect1.client.utils.SocketManager;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ChatController implements Initializable {

    @FXML
    private VBox conversationsList;

    @FXML
    private VBox messagesContainer;

    @FXML
    private ScrollPane messagesScrollPane;

    @FXML
    private TextField messageInput;

    @FXML
    private Button sendButton;

    @FXML
    private ImageView chatHeaderAvatar;

    @FXML
    private Label chatHeaderName;

    private int currentConversationId = -1;
    private int currentOtherUserId = -1;
    private User currentUser;

    private volatile boolean isInitialized = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getCurrentUser();

        // Load conversations trong background
        new Thread(() -> {
            loadConversationsSync();
            isInitialized = true;
            System.out.println("✅ ChatController initialized");
        }).start();

        messagesContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
        });

        messageInput.setOnAction(e -> onSendMessage());
    }

    /**
     * Load conversations synchronously - DÙNG sendRequestAndRead2Responses
     */
    private void loadConversationsSync() {
        try {
            System.out.println("📋 Loading conversations for user: " + currentUser.getId());

            SocketManager socket = SocketManager.getInstance();

            // ✅ Đọc 2 responses cùng lúc trong 1 lock
            Object[] responses = socket.sendRequestAndRead2Responses("GET_CONVERSATIONS", currentUser.getId());

            Object statusResponse = responses[0];
            Object dataResponse = responses[1];

            if ("SUCCESS".equals(statusResponse)) {
                if (dataResponse instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> conversations = (List<Map<String, Object>>) dataResponse;

                    System.out.println("✅ Loaded " + conversations.size() + " conversations");

                    Platform.runLater(() -> {
                        conversationsList.getChildren().clear();

                        if (conversations.isEmpty()) {
                            Label noConv = new Label("No conversations yet");
                            noConv.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20px;");
                            conversationsList.getChildren().add(noConv);
                        } else {
                            for (Map<String, Object> conv : conversations) {
                                try {
                                    HBox card = createConversationCard(conv);
                                    if (card != null) {
                                        conversationsList.getChildren().add(card);
                                    }
                                } catch (Exception e) {
                                    System.err.println("❌ Error creating conversation card: " + e.getMessage());
                                }
                            }
                        }
                    });
                } else {
                    System.err.println("❌ Expected List but got: " + dataResponse.getClass().getName());
                }
            } else {
                System.err.println("❌ GET_CONVERSATIONS failed: " + statusResponse);
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading conversations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadConversations() {
        new Thread(this::loadConversationsSync).start();
    }

    private HBox createConversationCard(Map<String, Object> conv) {
        try {
            // Validate
            if (!conv.containsKey("conversationId") || !conv.containsKey("otherUserId") ||
                    !conv.containsKey("otherUserName")) {
                System.err.println("⚠️ Missing required fields in conversation");
                return null;
            }

            Object convIdObj = conv.get("conversationId");
            Object otherUserIdObj = conv.get("otherUserId");
            Object otherUserNameObj = conv.get("otherUserName");

            if (convIdObj == null || otherUserIdObj == null || otherUserNameObj == null) {
                System.err.println("⚠️ Null values in conversation");
                return null;
            }

            final int conversationId = (Integer) convIdObj;
            final int otherUserId = (Integer) otherUserIdObj;
            final String otherUserName = (String) otherUserNameObj;
            final String otherUserAvatar = conv.get("otherUserAvatar") != null ?
                    (String) conv.get("otherUserAvatar") : "";
            String lastMessage = conv.get("lastMessage") != null ?
                    (String) conv.get("lastMessage") : "No messages";
            int unreadCount = conv.get("unreadCount") != null ?
                    (Integer) conv.get("unreadCount") : 0;

            HBox card = new HBox(10);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 5px; " +
                    "-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");

            // Avatar
            ImageView avatar = new ImageView();
            avatar.setFitWidth(40);
            avatar.setFitHeight(40);
            avatar.setPreserveRatio(true);

            String avatarUrl = otherUserAvatar.isEmpty() ?
                    "https://res.cloudinary.com/do46eak3c/image/upload/v1761648489/anhmd_fqwsrr.jpg" : otherUserAvatar;

            try {
                Image image = new Image(avatarUrl, true);
                avatar.setImage(image);
                Circle clip = new Circle(20, 20, 20);
                avatar.setClip(clip);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to load avatar");
            }

            // User info
            VBox userInfo = new VBox(3);
            Label nameLabel = new Label(otherUserName);
            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

            Label messageLabel = new Label(lastMessage);
            messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #95a5a6;");
            messageLabel.setMaxWidth(200);
            messageLabel.setWrapText(false);

            userInfo.getChildren().addAll(nameLabel, messageLabel);
            HBox.setHgrow(userInfo, javafx.scene.layout.Priority.ALWAYS);

            card.getChildren().addAll(avatar, userInfo);

            // Unread badge
            if (unreadCount > 0) {
                Label badge = new Label(String.valueOf(unreadCount));
                badge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                        "-fx-padding: 3px 7px; -fx-background-radius: 10px; -fx-font-size: 10px;");
                card.getChildren().add(badge);
            }

            // Click handler
            card.setOnMouseClicked(e -> {
                openConversation(conversationId, otherUserId, otherUserName, otherUserAvatar);
            });

            // Hover
            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #ecf0f1; " +
                    "-fx-background-radius: 5px; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-cursor: hand;"));
            card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; " +
                    "-fx-background-radius: 5px; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0; -fx-cursor: hand;"));

            return card;

        } catch (Exception e) {
            System.err.println("❌ Error creating conversation card: " + e.getMessage());
            return null;
        }
    }

    private void openConversation(int conversationId, int otherUserId, String otherUserName, String otherUserAvatar) {
        System.out.println("📂 Opening conversation: " + conversationId);

        this.currentConversationId = conversationId;
        this.currentOtherUserId = otherUserId;

        // Update header trên UI thread
        Platform.runLater(() -> {
            chatHeaderName.setText(otherUserName);

            String avatarUrl = (otherUserAvatar == null || otherUserAvatar.isEmpty()) ?
                    "https://res.cloudinary.com/do46eak3c/image/upload/v1761648489/anhmd_fqwsrr.jpg" : otherUserAvatar;

            try {
                Image image = new Image(avatarUrl, true);
                chatHeaderAvatar.setImage(image);
                Circle clip = new Circle(25, 25, 25);
                chatHeaderAvatar.setClip(clip);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to load header avatar");
            }
        });

        // ✅ Load messages trong background thread
        new Thread(() -> {
            try {
                loadMessagesSync(conversationId, true);
                markMessagesAsReadSync(conversationId);
                loadConversationsSync();
            } catch (Exception e) {
                System.err.println("❌ Error in openConversation: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Load messages synchronously - DÙNG sendRequestAndRead2Responses
     */
    private void loadMessagesSync(int conversationId, boolean shouldScroll) {
        try {
            System.out.println("📨 Loading messages for conversation: " + conversationId);

            SocketManager socket = SocketManager.getInstance();

            // ✅ Đọc 2 responses cùng lúc
            Object[] responses = socket.sendRequestAndRead2Responses("GET_MESSAGES", conversationId, 100);

            Object statusResponse = responses[0];
            Object dataResponse = responses[1];

            if ("SUCCESS".equals(statusResponse)) {
                if (!(dataResponse instanceof List)) {
                    System.err.println("❌ Expected List but got: " + dataResponse.getClass().getName());
                    return;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> messages = (List<Map<String, Object>>) dataResponse;

                // Validate
                if (!messages.isEmpty()) {
                    Map<String, Object> firstItem = messages.get(0);

                    if (firstItem.containsKey("conversationId") && !firstItem.containsKey("senderId")) {
                        System.err.println("❌ Server returned conversation list instead of messages!");
                        Platform.runLater(() -> {
                            messagesContainer.getChildren().clear();
                            Label errorLabel = new Label("No messages yet");
                            errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20px;");
                            messagesContainer.getChildren().add(errorLabel);
                        });
                        return;
                    }
                }

                System.out.println("✅ Loaded " + messages.size() + " messages");

                Platform.runLater(() -> {
                    messagesContainer.getChildren().clear();

                    if (messages.isEmpty()) {
                        Label emptyLabel = new Label("No messages yet. Start the conversation!");
                        emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20px;");
                        messagesContainer.getChildren().add(emptyLabel);
                    } else {
                        for (int i = messages.size() - 1; i >= 0; i--) {
                            Map<String, Object> msg = messages.get(i);
                            HBox bubble = createMessageBubble(msg);
                            if (bubble != null) {
                                messagesContainer.getChildren().add(bubble);
                            }
                        }
                    }

                    if (shouldScroll) {
                        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
                    }
                });
            } else {
                System.err.println("❌ GET_MESSAGES failed: " + statusResponse);
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading messages: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private HBox createMessageBubble(Map<String, Object> msg) {
        try {
            if (!msg.containsKey("senderId") || msg.get("senderId") == null) {
                return null;
            }

            int senderId = (Integer) msg.get("senderId");
            String content = msg.get("content") != null ? (String) msg.get("content") : "";

            if (content.isEmpty()) {
                return null;
            }

            Timestamp timestamp = null;
            Object timestampObj = msg.get("createdAt");
            if (timestampObj instanceof Timestamp) {
                timestamp = (Timestamp) timestampObj;
            }

            boolean isCurrentUser = (senderId == currentUser.getId());

            HBox bubble = new HBox();
            bubble.setPadding(new Insets(5));
            bubble.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox messageBox = new VBox(3);
            messageBox.setMaxWidth(400);

            TextFlow textFlow = new TextFlow();
            Text text = new Text(content);
            text.setStyle("-fx-font-size: 14px; " +
                    (isCurrentUser ? "-fx-fill: white;" : "-fx-fill: #2c3e50;"));
            textFlow.getChildren().add(text);

            textFlow.setStyle(isCurrentUser ?
                    "-fx-background-color: #3498db; -fx-background-radius: 15px; -fx-padding: 10px;" :
                    "-fx-background-color: white; -fx-background-radius: 15px; -fx-padding: 10px; " +
                            "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 15px;");

            messageBox.getChildren().add(textFlow);

            if (timestamp != null) {
                try {
                    LocalDateTime dateTime = timestamp.toLocalDateTime();
                    String timeStr = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                    Label timeLabel = new Label(timeStr);
                    timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
                    messageBox.getChildren().add(timeLabel);
                } catch (Exception ignored) {}
            }

            bubble.getChildren().add(messageBox);
            return bubble;

        } catch (Exception e) {
            System.err.println("❌ Error creating message bubble: " + e.getMessage());
            return null;
        }
    }

    /**
     * Mark messages as read - Dùng sendRequest + readResponse
     */
    private void markMessagesAsReadSync(int conversationId) {
        try {
            System.out.println("✅ Marking messages as read: " + conversationId);

            SocketManager socket = SocketManager.getInstance();
            socket.sendRequest("MARK_MESSAGES_READ", conversationId, currentUser.getId());
            Object response = socket.readResponse();

            System.out.println("✅ Mark as read response: " + response);
        } catch (Exception e) {
            System.err.println("❌ Error marking messages as read: " + e.getMessage());
        }
    }

    @FXML
    private void onSendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || currentOtherUserId == -1) {
            return;
        }

        sendButton.setDisable(true);
        final boolean isNewConversation = (currentConversationId == -1);

        new Thread(() -> {
            try {
                SocketManager socket = SocketManager.getInstance();
                socket.sendRequest("SEND_MESSAGE", currentUser.getId(), currentOtherUserId, content);
                Object response = socket.readResponse();

                if ("SUCCESS".equals(response)) {
                    Platform.runLater(() -> {
                        messageInput.clear();
                        sendButton.setDisable(false);
                    });

                    if (isNewConversation) {
                        Thread.sleep(300);

                        // ✅ Dùng sendRequestAndRead2Responses
                        Object[] responses = socket.sendRequestAndRead2Responses("GET_CONVERSATIONS", currentUser.getId());

                        if ("SUCCESS".equals(responses[0]) && responses[1] instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> conversations = (List<Map<String, Object>>) responses[1];

                            for (Map<String, Object> conv : conversations) {
                                Integer otherUserId = (Integer) conv.get("otherUserId");
                                if (otherUserId != null && otherUserId == currentOtherUserId) {
                                    Integer foundConvId = (Integer) conv.get("conversationId");
                                    if (foundConvId != null) {
                                        currentConversationId = foundConvId;
                                        final int finalConvId = foundConvId;

                                        // ✅ Chạy trong thread mới, KHÔNG PHẢI Platform.runLater
                                        new Thread(() -> {
                                            loadConversationsSync();
                                            loadMessagesSync(finalConvId, true);
                                        }).start();

                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        // ✅ Chạy trong thread mới
                        new Thread(() -> {
                            loadMessagesSync(currentConversationId, true);
                            loadConversationsSync();
                        }).start();
                    }
                } else {
                    Platform.runLater(() -> {
                        sendButton.setDisable(false);
                        showAlert("Error", "Failed to send message");
                    });
                }
            } catch (Exception e) {
                System.err.println("❌ Error sending message: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> sendButton.setDisable(false));
            }
        }).start();
    }

    public void openConversationWithUser(User friend) {
        System.out.println("🔍 Attempting to open conversation with: " + friend.getFullName());

        new Thread(() -> {
            try {
                // Đợi initialize xong
                int waitCount = 0;
                while (!isInitialized && waitCount < 20) {
                    Thread.sleep(100);
                    waitCount++;
                }

                System.out.println("✅ ChatController ready, loading conversation...");

                SocketManager socket = SocketManager.getInstance();

                // ✅ Dùng sendRequestAndRead2Responses
                Object[] responses = socket.sendRequestAndRead2Responses("GET_CONVERSATIONS", currentUser.getId());

                if ("SUCCESS".equals(responses[0]) && responses[1] instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> conversations = (List<Map<String, Object>>) responses[1];

                    Map<String, Object> existingConv = null;
                    for (Map<String, Object> conv : conversations) {
                        Integer otherUserId = (Integer) conv.get("otherUserId");
                        if (otherUserId != null && otherUserId == friend.getId()) {
                            existingConv = conv;
                            break;
                        }
                    }

                    if (existingConv != null) {
                        final Map<String, Object> finalConv = existingConv;
                        Platform.runLater(() -> {
                            loadConversations();
                            openConversation(
                                    (Integer) finalConv.get("conversationId"),
                                    (Integer) finalConv.get("otherUserId"),
                                    (String) finalConv.get("otherUserName"),
                                    (String) finalConv.get("otherUserAvatar")
                            );
                        });
                    } else {
                        Platform.runLater(() -> setupNewConversation(friend));
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Error opening conversation: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> setupNewConversation(friend));
            }
        }).start();
    }

    private void setupNewConversation(User friend) {
        currentOtherUserId = friend.getId();
        currentConversationId = -1;

        chatHeaderName.setText(friend.getFullName());
        String avatarUrl = friend.getAvatar();
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            avatarUrl = "https://res.cloudinary.com/do46eak3c/image/upload/v1761648489/anhmd_fqwsrr.jpg";
        }

        try {
            Image image = new Image(avatarUrl, true);
            chatHeaderAvatar.setImage(image);
            Circle clip = new Circle(25, 25, 25);
            chatHeaderAvatar.setClip(clip);
        } catch (Exception e) {
            System.err.println("⚠️ Failed to load avatar");
        }

        messagesContainer.getChildren().clear();

        Label welcomeLabel = new Label("Start a conversation with " + friend.getFullName());
        welcomeLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 20px;");
        messagesContainer.getChildren().add(welcomeLabel);

        System.out.println("✅ Ready to start new conversation with: " + friend.getFullName());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}