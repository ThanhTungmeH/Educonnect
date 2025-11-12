package org.example.educonnect1.Server.dao;

import org.example.educonnect1.Server.utils.DB;
import org.example.educonnect1.client.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendDAO {
    Connection conn = DB.connect();

    /**
     * Send a friend request
     */
    public boolean sendFriendRequest(int senderId, int receiverId) {
        // Check if already friends
        if (areFriends(senderId, receiverId)) {
            return false;
        }
        
        // Check if request already exists
        String checkSql = "SELECT id FROM friend_requests WHERE " +
                         "(sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)";
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setInt(3, receiverId);
            ps.setInt(4, senderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return false; // Request already exists
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        
        String sql = "INSERT INTO friend_requests (sender_id, receiver_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Accept a friend request
     */
    public boolean acceptFriendRequest(int requestId, int userId) {
        // Get request details
        String getSql = "SELECT sender_id, receiver_id FROM friend_requests WHERE id = ? AND receiver_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(getSql)) {
            ps.setInt(1, requestId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int senderId = rs.getInt("sender_id");
                    int receiverId = rs.getInt("receiver_id");
                    
                    // Add to friends table (both directions)
                    String addFriendSql = "INSERT INTO friends (user_id, friend_id) VALUES (?, ?), (?, ?)";
                    try (PreparedStatement ps2 = conn.prepareStatement(addFriendSql)) {
                        ps2.setInt(1, senderId);
                        ps2.setInt(2, receiverId);
                        ps2.setInt(3, receiverId);
                        ps2.setInt(4, senderId);
                        ps2.executeUpdate();
                    }
                    
                    // Update request status
                    String updateSql = "UPDATE friend_requests SET status = 'accepted' WHERE id = ?";
                    try (PreparedStatement ps3 = conn.prepareStatement(updateSql)) {
                        ps3.setInt(1, requestId);
                        ps3.executeUpdate();
                    }
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    /**
     * Reject a friend request
     */
    public boolean rejectFriendRequest(int requestId, int userId) {
        String sql = "UPDATE friend_requests SET status = 'rejected' " +
                    "WHERE id = ? AND receiver_id = ? AND status = 'pending'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all friends for a user
     */
    public List<User> getFriends(int userId) {
        List<User> friends = new ArrayList<>();
        String sql = "SELECT u.id, u.full_name, u.avatar, u.email " +
                    "FROM users u " +
                    "JOIN friends f ON u.id = f.friend_id " +
                    "WHERE f.user_id = ? " +
                    "ORDER BY u.full_name";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setFullName(rs.getString("full_name"));
                    user.setAvatar(rs.getString("avatar"));
                    user.setEmail(rs.getString("email"));
                    friends.add(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return friends;
    }

    /**
     * Get pending friend requests for a user
     */
    public List<Map<String, Object>> getPendingRequests(int userId) {
        List<Map<String, Object>> requests = new ArrayList<>();
        String sql = "SELECT fr.id, fr.sender_id, u.full_name, u.avatar, fr.created_at " +
                    "FROM friend_requests fr " +
                    "JOIN users u ON fr.sender_id = u.id " +
                    "WHERE fr.receiver_id = ? AND fr.status = 'pending' " +
                    "ORDER BY fr.created_at DESC";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> request = new HashMap<>();
                    request.put("id", rs.getInt("id"));
                    request.put("senderId", rs.getInt("sender_id"));
                    request.put("senderName", rs.getString("full_name"));
                    request.put("senderAvatar", rs.getString("avatar"));
                    request.put("createdAt", rs.getTimestamp("created_at"));
                    requests.add(request);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return requests;
    }

    /**
     * Check if two users are friends
     */
    public boolean areFriends(int userId1, int userId2) {
        String sql = "SELECT id FROM friends WHERE user_id = ? AND friend_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId1);
            ps.setInt(2, userId2);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
