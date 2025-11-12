package org.example.educonnect1.Server.dao;

import org.example.educonnect1.Server.utils.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageDAO {
    Connection conn = DB.connect();

    /**
     * Get or create a conversation between two users
     */
    public int getOrCreateConversation(int userId1, int userId2) {
        // First, check if conversation already exists
        String checkSql = "SELECT DISTINCT cp1.conversation_id " +
                         "FROM conversation_participants cp1 " +
                         "JOIN conversation_participants cp2 ON cp1.conversation_id = cp2.conversation_id " +
                         "WHERE cp1.user_id = ? AND cp2.user_id = ? " +
                         "AND (SELECT COUNT(*) FROM conversation_participants WHERE conversation_id = cp1.conversation_id) = 2";
        
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("conversation_id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // Create new conversation
        String createConvSql = "INSERT INTO conversations () VALUES ()";
        try (PreparedStatement ps = conn.prepareStatement(createConvSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int conversationId = rs.getInt(1);
                    
                    // Add participants
                    String addParticipantSql = "INSERT INTO conversation_participants (conversation_id, user_id) VALUES (?, ?)";
                    try (PreparedStatement ps2 = conn.prepareStatement(addParticipantSql)) {
                        ps2.setInt(1, conversationId);
                        ps2.setInt(2, userId1);
                        ps2.executeUpdate();
                        
                        ps2.setInt(1, conversationId);
                        ps2.setInt(2, userId2);
                        ps2.executeUpdate();
                    }
                    
                    return conversationId;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    /**
     * Save a message to database
     */
    public boolean saveMessage(int conversationId, int senderId, String content) {
        String sql = "INSERT INTO messages (conversation_id, sender_id, content) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setInt(2, senderId);
            ps.setString(3, content);
            
            // Update conversation timestamp
            String updateConvSql = "UPDATE conversations SET updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement ps2 = conn.prepareStatement(updateConvSql)) {
                ps2.setInt(1, conversationId);
                ps2.executeUpdate();
            }
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all conversations for a user with last message info
     */
    public List<Map<String, Object>> getUserConversations(int userId) {
        List<Map<String, Object>> conversations = new ArrayList<>();
        String sql = "SELECT c.id as conversation_id, c.updated_at, " +
                    "       u.id as other_user_id, u.full_name as other_user_name, u.avatar as other_user_avatar, " +
                    "       (SELECT content FROM messages WHERE conversation_id = c.id ORDER BY created_at DESC LIMIT 1) as last_message, " +
                    "       (SELECT created_at FROM messages WHERE conversation_id = c.id ORDER BY created_at DESC LIMIT 1) as last_message_time, " +
                    "       (SELECT COUNT(*) FROM messages WHERE conversation_id = c.id AND sender_id != ? AND is_read = FALSE) as unread_count " +
                    "FROM conversations c " +
                    "JOIN conversation_participants cp ON c.id = cp.conversation_id " +
                    "JOIN conversation_participants cp2 ON c.id = cp2.conversation_id AND cp2.user_id != ? " +
                    "JOIN users u ON cp2.user_id = u.id " +
                    "WHERE cp.user_id = ? " +
                    "ORDER BY c.updated_at DESC";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("conversationId", rs.getInt("conversation_id"));
                    conv.put("otherUserId", rs.getInt("other_user_id"));
                    conv.put("otherUserName", rs.getString("other_user_name"));
                    conv.put("otherUserAvatar", rs.getString("other_user_avatar"));
                    conv.put("lastMessage", rs.getString("last_message"));
                    conv.put("lastMessageTime", rs.getTimestamp("last_message_time"));
                    conv.put("unreadCount", rs.getInt("unread_count"));
                    conversations.add(conv);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return conversations;
    }

    /**
     * Get all messages in a conversation
     */
    public List<Map<String, Object>> getMessages(int conversationId, int limit) {
        List<Map<String, Object>> messages = new ArrayList<>();
        String sql = "SELECT m.id, m.sender_id, m.content, m.is_read, m.created_at, " +
                    "       u.full_name as sender_name, u.avatar as sender_avatar " +
                    "FROM messages m " +
                    "JOIN users u ON m.sender_id = u.id " +
                    "WHERE m.conversation_id = ? " +
                    "ORDER BY m.created_at DESC " +
                    "LIMIT ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("id", rs.getInt("id"));
                    msg.put("senderId", rs.getInt("sender_id"));
                    msg.put("content", rs.getString("content"));
                    msg.put("isRead", rs.getBoolean("is_read"));
                    msg.put("createdAt", rs.getTimestamp("created_at"));
                    msg.put("senderName", rs.getString("sender_name"));
                    msg.put("senderAvatar", rs.getString("sender_avatar"));
                    messages.add(msg);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return messages;
    }

    /**
     * Mark all messages in a conversation as read for a specific user
     */
    public boolean markMessagesAsRead(int conversationId, int userId) {
        String sql = "UPDATE messages SET is_read = TRUE " +
                    "WHERE conversation_id = ? AND sender_id != ? AND is_read = FALSE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setInt(2, userId);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
