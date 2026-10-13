package com.bankloan.service;

import com.bankloan.db.DatabaseConnection;
import com.bankloan.model.User;
import com.bankloan.security.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Service managing user authentication, login validation, and user creation.
 */
public class UserService {

    /**
     * Authenticates a user using secure PBKDF2 password verification.
     * Returns User model without password/salt if authenticated, or null.
     */
    public User authenticate(String username, String candidatePassword) throws SQLException {
        if (username == null || candidatePassword == null) {
            return null;
        }

        String sql = "SELECT user_id, username, password_hash, salt, full_name, role, status, " +
                     "TO_CHAR(created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at " +
                     "FROM USERS WHERE LOWER(username) = LOWER(?) AND status = 'ACTIVE'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String storedSalt = rs.getString("salt");

                    // Check if candidate password matches stored PBKDF2 hash
                    boolean matches = PasswordUtil.verifyPassword(candidatePassword, storedHash, storedSalt);
                    if (matches) {
                        return new User(
                            rs.getLong("user_id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("role"),
                            rs.getString("status"),
                            rs.getString("created_at")
                        );
                    }
                }
            }
        }
        return null;
    }

    /**
     * Provisions or updates an administrator user with PBKDF2 hash and salt.
     */
    public void provisionUser(String username, String password, String fullName, String role) throws SQLException {
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        String checkSql = "SELECT user_id FROM USERS WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean exists = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username.trim());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                    }
                }
            }

            if (exists) {
                String updateSql = "UPDATE USERS SET password_hash = ?, salt = ?, full_name = ?, role = ?, status = 'ACTIVE' WHERE LOWER(username) = LOWER(?)";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, hash);
                    updateStmt.setString(2, salt);
                    updateStmt.setString(3, fullName);
                    updateStmt.setString(4, role);
                    updateStmt.setString(5, username.trim());
                    updateStmt.executeUpdate();
                    System.out.println("[AUTH] Updated existing user with PBKDF2 credentials: " + username);
                }
            } else {
                String insertSql = "INSERT INTO USERS (user_id, username, password_hash, salt, full_name, role, status) " +
                                   "VALUES (seq_users.NEXTVAL, ?, ?, ?, ?, ?, 'ACTIVE')";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, username.trim());
                    insertStmt.setString(2, hash);
                    insertStmt.setString(3, salt);
                    insertStmt.setString(4, fullName);
                    insertStmt.setString(5, role);
                    insertStmt.executeUpdate();
                    System.out.println("[AUTH] Provisioned new user with PBKDF2 credentials: " + username);
                }
            }
        }
    }
}
