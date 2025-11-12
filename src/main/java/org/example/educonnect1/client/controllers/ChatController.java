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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getCurrentUser();
        loadConversations();
        
        // Auto-scroll to bottom when new messages appear
        messagesContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            messagesScrollPane.setVvalue(1.0);
        });
    }

    private void loadConversations() {
        new Thread(() -> {
            try {
                SocketManager socket = SocketManager.getInstance();
                socket.sendRequest("GET_CONVERSATIONS", currentUser.getId());
                Object response = socket.readResponse();
                
                if ("SUCCESS".equals(response)) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> conversations = (List<Map<String, Object>>) socket.readResponse();
                    
                    Platform.runLater(() -> {
                        conversationsList.getChildren().clear();
                        for (Map<String, Object> conv : conversations) {
                            conversationsList.getChildren().add(createConversationCard(conv));
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Error loading conversations: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private HBox createConversationCard(Map<String, Object> conv) {
        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: white; -fx-cursor: hand;");
        
        final int conversationId = (Integer) conv.get("conversationId");
        final int otherUserId = (Integer) conv.get("otherUserId");
        final String otherUserName = (String) conv.get("otherUserName");
        final String otherUserAvatar = (String) conv.get("otherUserAvatar");
        String lastMessage = (String) conv.get("lastMessage");
        int unreadCount = (Integer) conv.get("unreadCount");
        
        // Avatar
        ImageView avatar = new ImageView();
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);
        avatar.setPreserveRatio(true);
        
        String avatarUrl = otherUserAvatar;
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
        
        // Conversation info
        VBox info = new VBox(5);
        Label nameLabel = new Label(otherUserName);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        Label messagePreview = new Label(lastMessage != null ? lastMessage : "No messages yet");
        messagePreview.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        
        info.getChildren().addAll(nameLabel, messagePreview);
        
        // Unread badge
        if (unreadCount > 0) {
            Label badge = new Label(String.valueOf(unreadCount));
            badge.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                          "-fx-padding: 2px 6px; -fx-background-radius: 10px; -fx-font-size: 10px;");
            HBox.setMargin(badge, new Insets(0, 0, 0, 10));
            card.getChildren().addAll(avatar, info, badge);
        } else {
            card.getChildren().addAll(avatar, info);
        }
        
        card.setOnMouseClicked(e -> openConversation(conversationId, otherUserId, otherUserName, otherUserAvatar));
        
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #ecf0f1; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-cursor: hand;"));
        
        return card;
    }

    private void openConversation(int conversationId, int otherUserId, String otherUserName, String otherUserAvatar) {
        this.currentConversationId = conversationId;
        this.currentOtherUserId = otherUserId;
        
        // Update header
        chatHeaderName.setText(otherUserName);
        if (otherUserAvatar == null || otherUserAvatar.isEmpty()) {
            otherUserAvatar = "https://res.cloudinary.com/do46eak3c/image/upload/v1761648489/anhmd_fqwsrr.jpg";
        }
        
        try {
            Image image = new Image(otherUserAvatar, true);
            chatHeaderAvatar.setImage(image);
            double radius = chatHeaderAvatar.getFitWidth() / 2;
            Circle clip = new Circle(radius, radius, radius);
            chatHeaderAvatar.setClip(clip);
        } catch (Exception e) {
            System.err.println("Failed to load avatar: " + e.getMessage());
        }
        
        // Load messages
        loadMessages(conversationId);
        
        // Mark messages as read
        markMessagesAsRead(conversationId);
    }

    private void loadMessages(int conversationId) {
        new Thread(() -> {
            try {
                SocketManager socket = SocketManager.getInstance();
                socket.sendRequest("GET_MESSAGES", conversationId, 100);
                Object response = socket.readResponse();
                
                if ("SUCCESS".equals(response)) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) socket.readResponse();
                    
                    Platform.runLater(() -> {
                        messagesContainer.getChildren().clear();
                        // Reverse order to show oldest first
                        for (int i = messages.size() - 1; i >= 0; i--) {
                            Map<String, Object> msg = messages.get(i);
                            messagesContainer.getChildren().add(createMessageBubble(msg));
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Error loading messages: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private HBox createMessageBubble(Map<String, Object> msg) {
        HBox bubble = new HBox();
        int senderId = (Integer) msg.get("senderId");
        String content = (String) msg.get("content");
        Timestamp timestamp = (Timestamp) msg.get("createdAt");
        
        boolean isCurrentUser = senderId == currentUser.getId();
        
        VBox messageBox = new VBox(5);
        messageBox.setMaxWidth(400);
        
        // Message content
        TextFlow textFlow = new TextFlow();
        Text text = new Text(content);
        text.setStyle("-fx-font-size: 14px;");
        textFlow.getChildren().add(text);
        textFlow.setStyle(isCurrentUser ? 
            "-fx-background-color: #3498db; -fx-background-radius: 15px; -fx-padding: 10px; -fx-text-fill: white;" :
            "-fx-background-color: white; -fx-background-radius: 15px; -fx-padding: 10px; -fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 15px;");
        
        // Timestamp
        if (timestamp != null) {
            LocalDateTime dateTime = timestamp.toLocalDateTime();
            String timeStr = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            Label timeLabel = new Label(timeStr);
            timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
            messageBox.getChildren().addAll(textFlow, timeLabel);
        } else {
            messageBox.getChildren().add(textFlow);
        }
        
        bubble.getChildren().add(messageBox);
        bubble.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubble.setPadding(new Insets(5));
        
        return bubble;
    }

    @FXML
    private void onSendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || currentConversationId == -1) {
            return;
        }
        
        new Thread(() -> {
            try {
                SocketManager socket = SocketManager.getInstance();
                socket.sendRequest("SEND_MESSAGE", currentUser.getId(), currentOtherUserId, content);
                Object response = socket.readResponse();
                
                if ("SUCCESS".equals(response)) {
                    Platform.runLater(() -> {
                        messageInput.clear();
                        loadMessages(currentConversationId);
                        loadConversations(); // Refresh conversation list
                    });
                }
            } catch (Exception e) {
                System.err.println("Error sending message: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void markMessagesAsRead(int conversationId) {
        new Thread(() -> {
            try {
                SocketManager socket = SocketManager.getInstance();
                socket.sendRequest("MARK_MESSAGES_READ", conversationId, currentUser.getId());
                socket.readResponse();
                
                Platform.runLater(() -> loadConversations()); // Refresh to update unread counts
            } catch (Exception e) {
                System.err.println("Error marking messages as read: " + e.getMessage());
            }
        }).start();
    }
}
