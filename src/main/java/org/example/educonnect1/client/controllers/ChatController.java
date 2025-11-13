package org.example.educonnect1.client.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.example.educonnect1.client.models.User;
import org.example.educonnect1.client.utils.SessionManager;
import org.example.educonnect1.client.utils.SocketManager;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

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

    // New UI elements
    @FXML
    private Button attachButton;

    @FXML
    private Button emojiButton;

    @FXML
    private Button searchButton;

    @FXML
    private Button scrollToBottomButton;

    @FXML
    private HBox searchBar;

    @FXML
    private TextField searchField;

    @FXML
    private HBox typingIndicatorBox;

    @FXML
    private Label typingLabel;

    private int currentConversationId = -1;
    private int currentOtherUserId = -1;
    private User currentUser;

    private volatile boolean isInitialized = false;

    // New features
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> messageRefreshTask;
    private ScheduledFuture<?> typingCheckTask;
    private long lastTypingTime = 0;
    private boolean isTyping = false;
    
    // Cloudinary configuration
    private Cloudinary cloudinary;
    
    // Retry mechanism
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getCurrentUser();

        // Initialize Cloudinary
        cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "do46eak3c",
            "api_key", "your_api_key_here",
            "api_secret", "your_api_secret_here"
        ));

        // Initialize scheduler for auto-refresh
        scheduler = Executors.newScheduledThreadPool(2);

        // Load conversations trong background
        new Thread(() -> {
            loadConversationsSync();
            isInitialized = true;
            System.out.println("✅ ChatController initialized");
        }).start();

        messagesContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
        });

        // Show scroll to bottom button when not at bottom
        messagesScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isAtBottom = newVal.doubleValue() >= 0.95;
            scrollToBottomButton.setVisible(!isAtBottom);
            scrollToBottomButton.setManaged(!isAtBottom);
        });

        messageInput.setOnAction(e -> onSendMessage());

        // Search functionality
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                onSearchMessages(newVal);
            });
        }
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

        // Stop previous auto-refresh
        stopAutoRefresh();

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
                
                // Start auto-refresh for this conversation
                startAutoRefresh();
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

            int messageId = msg.containsKey("id") ? (Integer) msg.get("id") : -1;
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
            boolean isEdited = msg.containsKey("edited") && (Boolean) msg.get("edited");
            String messageType = msg.containsKey("messageType") ? (String) msg.get("messageType") : "text";

            HBox bubble = new HBox();
            bubble.setPadding(new Insets(5));
            bubble.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

            VBox messageBox = new VBox(3);
            messageBox.setMaxWidth(400);

            // Message content with gradient for sent messages
            TextFlow textFlow = new TextFlow();
            Text text = new Text(content);
            text.setStyle("-fx-font-size: 14px; " +
                    (isCurrentUser ? "-fx-fill: white;" : "-fx-fill: #2c3e50;"));
            textFlow.getChildren().add(text);

            String bubbleStyle = isCurrentUser ?
                    "-fx-background-color: linear-gradient(to right, #3498db, #2980b9); -fx-background-radius: 15px; -fx-padding: 10px;" :
                    "-fx-background-color: white; -fx-background-radius: 15px; -fx-padding: 10px; " +
                            "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-border-radius: 15px;";
            textFlow.setStyle(bubbleStyle);

            messageBox.getChildren().add(textFlow);

            // Time and edited indicator
            HBox metaInfo = new HBox(5);
            metaInfo.setAlignment(Pos.CENTER_LEFT);
            
            if (timestamp != null) {
                try {
                    LocalDateTime dateTime = timestamp.toLocalDateTime();
                    String timeStr = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                    Label timeLabel = new Label(timeStr);
                    timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
                    metaInfo.getChildren().add(timeLabel);
                } catch (Exception ignored) {}
            }

            if (isEdited) {
                Label editedLabel = new Label("(edited)");
                editedLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #95a5a6; -fx-font-style: italic;");
                metaInfo.getChildren().add(editedLabel);
            }

            // Message status for sent messages
            if (isCurrentUser && msg.containsKey("status")) {
                String status = (String) msg.get("status");
                Label statusLabel = new Label(getStatusIcon(status));
                statusLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + 
                    ("read".equals(status) ? "#27ae60" : "#95a5a6") + ";");
                metaInfo.getChildren().add(statusLabel);
            }

            if (metaInfo.getChildren().size() > 0) {
                messageBox.getChildren().add(metaInfo);
            }

            bubble.getChildren().add(messageBox);

            // Add context menu on right-click
            if (messageId != -1) {
                bubble.setOnContextMenuRequested(e -> {
                    ContextMenu menu = createMessageContextMenu(messageId, senderId, content);
                    menu.show(bubble, e.getScreenX(), e.getScreenY());
                    e.consume();
                });

                // Hover effect
                bubble.setOnMouseEntered(e -> {
                    textFlow.setStyle(bubbleStyle + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
                });
                bubble.setOnMouseExited(e -> {
                    textFlow.setStyle(bubbleStyle);
                });
            }

            // Fade in animation
            FadeTransition fade = new FadeTransition(Duration.millis(300), bubble);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();

            return bubble;

        } catch (Exception e) {
            System.err.println("❌ Error creating message bubble: " + e.getMessage());
            return null;
        }
    }

    private String getStatusIcon(String status) {
        switch (status) {
            case "sent": return "○";
            case "delivered": return "✓";
            case "read": return "✓✓";
            default: return "";
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

    // ========================================================================
    // NEW FEATURE HANDLERS
    // ========================================================================

    /**
     * Start auto-refresh for messages every 3 seconds
     */
    private void startAutoRefresh() {
        if (messageRefreshTask != null) {
            messageRefreshTask.cancel(false);
        }
        
        messageRefreshTask = scheduler.scheduleAtFixedRate(() -> {
            if (currentConversationId != -1) {
                loadMessagesSync(currentConversationId, false);
                checkTypingStatus();
            }
        }, 3, 3, TimeUnit.SECONDS);
    }

    /**
     * Stop auto-refresh
     */
    private void stopAutoRefresh() {
        if (messageRefreshTask != null) {
            messageRefreshTask.cancel(false);
            messageRefreshTask = null;
        }
    }

    /**
     * Handle typing indicator
     */
    @FXML
    private void onMessageTyping(KeyEvent event) {
        long currentTime = System.currentTimeMillis();
        
        if (!isTyping && currentConversationId != -1) {
            isTyping = true;
            sendTypingStatus(true);
        }
        
        lastTypingTime = currentTime;
        
        // Schedule to stop typing after 3 seconds of inactivity
        scheduler.schedule(() -> {
            long timeSinceLastType = System.currentTimeMillis() - lastTypingTime;
            if (timeSinceLastType >= 3000 && isTyping) {
                isTyping = false;
                sendTypingStatus(false);
            }
        }, 3, TimeUnit.SECONDS);
    }

    /**
     * Send typing status to server
     */
    private void sendTypingStatus(boolean typing) {
        if (currentConversationId == -1) return;
        
        new Thread(() -> {
            try {
                SocketManager socket = SocketManager.getInstance();
                socket.sendRequest("SEND_TYPING", currentConversationId, currentUser.getId(), typing);
                Object response = socket.readResponse();
                // Silently handle response
            } catch (Exception e) {
                System.err.println("Error sending typing status: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Check typing status from other users
     */
    private void checkTypingStatus() {
        if (currentConversationId == -1) return;
        
        try {
            SocketManager socket = SocketManager.getInstance();
            Object[] responses = socket.sendRequestAndRead2Responses("GET_TYPING_STATUS", currentConversationId);
            
            if ("SUCCESS".equals(responses[0]) && responses[1] instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> typingUsers = (List<Map<String, Object>>) responses[1];
                
                Platform.runLater(() -> {
                    if (!typingUsers.isEmpty()) {
                        StringBuilder names = new StringBuilder();
                        for (int i = 0; i < typingUsers.size(); i++) {
                            if (i > 0) names.append(", ");
                            names.append(typingUsers.get(i).get("name"));
                        }
                        typingLabel.setText(names.toString() + " is typing...");
                        typingIndicatorBox.setVisible(true);
                        typingIndicatorBox.setManaged(true);
                    } else {
                        typingIndicatorBox.setVisible(false);
                        typingIndicatorBox.setManaged(false);
                    }
                });
            }
        } catch (Exception e) {
            // Silently handle - typing status is not critical
        }
    }

    /**
     * Handle attach file button
     */
    @FXML
    private void onAttachFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Upload");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"),
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.docx", "*.xlsx"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File file = fileChooser.showOpenDialog(attachButton.getScene().getWindow());
        if (file != null) {
            uploadFile(file);
        }
    }

    /**
     * Upload file to Cloudinary
     */
    private void uploadFile(File file) {
        sendButton.setDisable(true);
        
        new Thread(() -> {
            try {
                // Upload to Cloudinary
                Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
                String fileUrl = (String) uploadResult.get("secure_url");
                final String fileName = file.getName();
                
                // Determine message type
                final String messageType;
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                if (Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension)) {
                    messageType = "image";
                } else {
                    messageType = "file";
                }
                
                // Send message with file
                Platform.runLater(() -> {
                    messageInput.setText("[" + messageType.toUpperCase() + "] " + fileName);
                    onSendMessage();
                    sendButton.setDisable(false);
                });
                
            } catch (Exception e) {
                System.err.println("Error uploading file: " + e.getMessage());
                Platform.runLater(() -> {
                    sendButton.setDisable(false);
                    showAlert("Upload Error", "Failed to upload file: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Handle emoji button click
     */
    @FXML
    private void onEmojiClick() {
        // Show emoji picker popup (simplified version)
        ContextMenu emojiMenu = new ContextMenu();
        String[] emojis = {"👍", "❤️", "😂", "😮", "😢", "🔥", "👏", "🎉", "😊", "😍"};
        
        for (String emoji : emojis) {
            MenuItem item = new MenuItem(emoji);
            item.setOnAction(e -> {
                messageInput.appendText(emoji);
                messageInput.requestFocus();
            });
            emojiMenu.getItems().add(item);
        }
        
        emojiMenu.show(emojiButton, javafx.geometry.Side.TOP, 0, 0);
    }

    /**
     * Handle search button click
     */
    @FXML
    private void onSearchClick() {
        boolean isVisible = searchBar.isVisible();
        searchBar.setVisible(!isVisible);
        searchBar.setManaged(!isVisible);
        
        if (!isVisible) {
            searchField.requestFocus();
        } else {
            searchField.clear();
        }
    }

    /**
     * Close search bar
     */
    @FXML
    private void onCloseSearch() {
        searchBar.setVisible(false);
        searchBar.setManaged(false);
        searchField.clear();
        // Remove highlights
        loadMessagesSync(currentConversationId, false);
    }

    /**
     * Search messages
     */
    private void onSearchMessages(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Clear highlights
            return;
        }
        
        // Highlight messages containing query
        messagesContainer.getChildren().forEach(node -> {
            if (node instanceof HBox) {
                HBox bubble = (HBox) node;
                // Simple highlighting by changing background color
                String lowerQuery = query.toLowerCase();
                // This is a simplified version - full implementation would parse text
            }
        });
    }

    /**
     * Scroll to bottom
     */
    @FXML
    private void onScrollToBottom() {
        messagesScrollPane.setVvalue(1.0);
    }

    /**
     * Add reaction to message
     */
    private void addReactionToMessage(int messageId, String reaction) {
        new Thread(() -> {
            try {
                SocketManager socket = SocketManager.getInstance();
                Object[] responses = socket.sendRequestAndRead2Responses("ADD_REACTION", 
                    messageId, currentUser.getId(), reaction);
                
                if ("SUCCESS".equals(responses[0])) {
                    // Refresh messages to show updated reactions
                    loadMessagesSync(currentConversationId, false);
                }
            } catch (Exception e) {
                System.err.println("Error adding reaction: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Show context menu for message (right-click)
     */
    private ContextMenu createMessageContextMenu(int messageId, int senderId, String content) {
        ContextMenu menu = new ContextMenu();
        
        // Add reactions
        Menu reactionsMenu = new Menu("React");
        String[] reactions = {"👍", "❤️", "😂", "😮", "😢", "🔥"};
        for (String reaction : reactions) {
            MenuItem item = new MenuItem(reaction);
            item.setOnAction(e -> addReactionToMessage(messageId, reaction));
            reactionsMenu.getItems().add(item);
        }
        menu.getItems().add(reactionsMenu);
        
        // Edit/Delete only for own messages
        if (senderId == currentUser.getId()) {
            MenuItem editItem = new MenuItem("Edit");
            editItem.setOnAction(e -> editMessage(messageId, content));
            
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(e -> deleteMessage(messageId));
            
            menu.getItems().addAll(new SeparatorMenuItem(), editItem, deleteItem);
        }
        
        return menu;
    }

    /**
     * Edit message
     */
    private void editMessage(int messageId, String currentContent) {
        TextInputDialog dialog = new TextInputDialog(currentContent);
        dialog.setTitle("Edit Message");
        dialog.setHeaderText("Edit your message");
        dialog.setContentText("Message:");
        
        dialog.showAndWait().ifPresent(newContent -> {
            if (!newContent.trim().isEmpty() && !newContent.equals(currentContent)) {
                new Thread(() -> {
                    try {
                        SocketManager socket = SocketManager.getInstance();
                        socket.sendRequest("EDIT_MESSAGE", messageId, currentUser.getId(), newContent);
                        Object response = socket.readResponse();
                        
                        if ("SUCCESS".equals(response)) {
                            loadMessagesSync(currentConversationId, false);
                        } else {
                            Platform.runLater(() -> 
                                showAlert("Edit Failed", "Could not edit message. It may be too old (15 min limit).")
                            );
                        }
                    } catch (Exception e) {
                        System.err.println("Error editing message: " + e.getMessage());
                    }
                }).start();
            }
        });
    }

    /**
     * Delete message
     */
    private void deleteMessage(int messageId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Message");
        confirm.setHeaderText("Are you sure you want to delete this message?");
        confirm.setContentText("This action cannot be undone.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        SocketManager socket = SocketManager.getInstance();
                        socket.sendRequest("DELETE_MESSAGE", messageId, currentUser.getId());
                        Object result = socket.readResponse();
                        
                        if ("SUCCESS".equals(result)) {
                            loadMessagesSync(currentConversationId, false);
                        }
                    } catch (Exception e) {
                        System.err.println("Error deleting message: " + e.getMessage());
                    }
                }).start();
            }
        });
    }

    /**
     * Cleanup when controller is destroyed
     */
    public void cleanup() {
        stopAutoRefresh();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

}