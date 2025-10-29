package org.example.educonnect1.Server.dao;

import org.example.educonnect1.client.models.User;
import org.example.educonnect1.Server.utils.DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    Connection conn = DB.connect();

    public boolean saveUser(User user) {
        String sql = "INSERT INTO users (email, password, full_name, is_verified, verification_code, verification_expiry,avatar) VALUES (?,?,?,?,?,?,?)";
        try (
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPassWord());
            ps.setString(3, user.getFullName());
            ps.setBoolean(4, user.isVerified());
            ps.setString(5, user.getVerificationCode());
            ps.setString(7, user.getAvatar());
            if (user.getVerificationExpiry() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(user.getVerificationExpiry()));
            } else {
                ps.setTimestamp(6, null);
            }
            int affected = ps.executeUpdate();
            if (affected == 0) return false;

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) user.setId(rs.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public User findByEmail(String email) {
        String sql = "SELECT id, email, password, is_verified, verification_code, verification_expiry from users where email=?";
        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setEmail(rs.getString("email"));
                    u.setPassword(rs.getString("password"));
                    u.setVerified(rs.getBoolean("is_verified"));
                    u.setVerificationCode(rs.getString("verification_code"));
                    Timestamp t = rs.getTimestamp("verification_expiry");
                    if (t != null) u.setVerificationExpiry(t.toLocalDateTime());
                    return u;

                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void markVerified(String email) {
        String sql = "UPDATE users SET is_verified = TRUE, verification_code = NULL WHERE email = ?";
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateVerification(User user) {
        String sql = "UPDATE users SET verification_code = ?, verification_expiry = ? WHERE email = ?";
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getVerificationCode());
            stmt.setTimestamp(2, Timestamp.valueOf(user.getVerificationExpiry()));
            stmt.setString(3, user.getEmail());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //    public void checkUser(User user) {
//        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
//        try (Connection conn = DB.connect();
//             PreparedStatement ps = conn.prepareStatement(sql)) {
//            ps.setString(1, user.getEmail());
//            ps.setString(2, user.getPassWord());
//            try (ResultSet rs = ps.executeQuery()) {
//                if (rs.next()) {
//                    user.setId(rs.getInt("id"));
//                    user.setFullName(rs.getString("full_name"));
//                    user.setRole(rs.getString("role"));
//                    user.setVerified(rs.getBoolean("is_verified"));
//                } else {
//                    user.setId(-1); // Indicate user not found
//                }
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//    }
    public boolean updateAvatar(String email, String avatarUrl) {
        String sql = "UPDATE users SET avatar = ? WHERE email = ?";
        try (
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, avatarUrl);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<User> findByName(String name) {
        List<User> list = new ArrayList<>();
        String sql = "select id, full_name, avatar from users where full_name like ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + name + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setFullName(rs.getString("full_name"));
                    u.setAvatar(rs.getString("avatar"));
                    list.add(u);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
}
