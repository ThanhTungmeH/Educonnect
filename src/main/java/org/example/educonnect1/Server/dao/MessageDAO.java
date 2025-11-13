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

    // ========================================================================
    // NEW METHODS FOR CHAT IMPROVEMENTS
    // ========================================================================

    /**
     * Save a reaction to a message
     * If user already reacted, update the reaction
     */
    public boolean saveReaction(int messageId, int userId, String reaction) {
        String sql = "INSERT INTO message_reactions (message_id, user_id, reaction) " +
                    "VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE reaction = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            ps.setString(3, reaction);
            ps.setString(4, reaction);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all reactions for a message
     * Returns a map of reaction -> count
     */
    public Map<String, Integer> getReactions(int messageId) {
        Map<String, Integer> reactions = new HashMap<>();
        String sql = "SELECT reaction, COUNT(*) as count " +
                    "FROM message_reactions " +
                    "WHERE message_id = ? " +
                    "GROUP BY reaction";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reactions.put(rs.getString("reaction"), rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return reactions;
    }

    /**
     * Edit a message (only if sent by the user and within time limit)
     * @param messageId The message to edit
     * @param userId The user attempting to edit
     * @param newContent The new content
     * @return true if successful, false if not allowed or failed
     */
    public boolean editMessage(int messageId, int userId, String newContent) {
        // Check if user owns the message and it's within 15 minutes
        String checkSql = "SELECT sender_id, created_at FROM messages WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false; // Message not found
                }
                
                int senderId = rs.getInt("sender_id");
                Timestamp createdAt = rs.getTimestamp("created_at");
                
                // Check ownership
                if (senderId != userId) {
                    return false;
                }
                
                // Check time limit (15 minutes = 900 seconds)
                long secondsElapsed = (System.currentTimeMillis() - createdAt.getTime()) / 1000;
                if (secondsElapsed > 900) {
                    return false;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        
        // Update the message
        String updateSql = "UPDATE messages SET content = ?, edited = TRUE, edited_at = CURRENT_TIMESTAMP " +
                          "WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, newContent);
            ps.setInt(2, messageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete a message (soft delete - only if sent by the user)
     * @param messageId The message to delete
     * @param userId The user attempting to delete
     * @return true if successful, false if not allowed or failed
     */
    public boolean deleteMessage(int messageId, int userId) {
        String sql = "UPDATE messages SET deleted = TRUE, content = '[Message deleted]' " +
                    "WHERE id = ? AND sender_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Update typing status for a user in a conversation
     */
    public boolean updateTypingStatus(int conversationId, int userId, boolean isTyping) {
        String sql = "INSERT INTO typing_status (user_id, conversation_id, is_typing) " +
                    "VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE is_typing = ?, updated_at = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, conversationId);
            ps.setBoolean(3, isTyping);
            ps.setBoolean(4, isTyping);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get list of users currently typing in a conversation
     */
    public List<Map<String, Object>> getTypingUsers(int conversationId) {
        List<Map<String, Object>> typingUsers = new ArrayList<>();
        String sql = "SELECT u.id, u.full_name " +
                    "FROM typing_status ts " +
                    "JOIN users u ON ts.user_id = u.id " +
                    "WHERE ts.conversation_id = ? AND ts.is_typing = TRUE " +
                    "AND ts.updated_at > DATE_SUB(NOW(), INTERVAL 5 SECOND)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rs.getInt("id"));
                    user.put("name", rs.getString("full_name"));
                    typingUsers.add(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return typingUsers;
    }

    /**
     * Update message status (sent/delivered/read)
     */
    public boolean updateMessageStatus(int messageId, String status) {
        String sql = "UPDATE messages SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, messageId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get messages with pagination support
     */
    public List<Map<String, Object>> getMessagesWithPagination(int conversationId, int offset, int limit) {
        List<Map<String, Object>> messages = new ArrayList<>();
        String sql = "SELECT m.id, m.sender_id, m.content, m.is_read, m.created_at, " +
                    "       m.status, m.message_type, m.file_url, m.file_name, " +
                    "       m.edited, m.edited_at, m.deleted, " +
                    "       u.full_name as sender_name, u.avatar as sender_avatar " +
                    "FROM messages m " +
                    "JOIN users u ON m.sender_id = u.id " +
                    "WHERE m.conversation_id = ? " +
                    "ORDER BY m.created_at DESC " +
                    "LIMIT ? OFFSET ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> msg = new HashMap<>();
                    msg.put("id", rs.getInt("id"));
                    msg.put("senderId", rs.getInt("sender_id"));
                    msg.put("content", rs.getString("content"));
                    msg.put("isRead", rs.getBoolean("is_read"));
                    msg.put("createdAt", rs.getTimestamp("created_at"));
                    msg.put("status", rs.getString("status"));
                    msg.put("messageType", rs.getString("message_type"));
                    msg.put("fileUrl", rs.getString("file_url"));
                    msg.put("fileName", rs.getString("file_name"));
                    msg.put("edited", rs.getBoolean("edited"));
                    msg.put("editedAt", rs.getTimestamp("edited_at"));
                    msg.put("deleted", rs.getBoolean("deleted"));
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
}
