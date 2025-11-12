package org.example.educonnect1.client.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Conversation implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int conversationId;
    private int otherUserId;
    private String otherUserName;
    private String otherUserAvatar;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private int unreadCount;

    public Conversation() {
    }

    public Conversation(int conversationId, int otherUserId, String otherUserName, 
                       String otherUserAvatar, String lastMessage, 
                       LocalDateTime lastMessageTime, int unreadCount) {
        this.conversationId = conversationId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.otherUserAvatar = otherUserAvatar;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.unreadCount = unreadCount;
    }

    // Getters and Setters
    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public int getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(int otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getOtherUserAvatar() {
        return otherUserAvatar;
    }

    public void setOtherUserAvatar(String otherUserAvatar) {
        this.otherUserAvatar = otherUserAvatar;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
