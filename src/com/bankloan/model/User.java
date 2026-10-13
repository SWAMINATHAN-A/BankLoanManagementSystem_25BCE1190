package com.bankloan.model;

/**
 * Model representing a user from USERS table.
 * Never stores or exposes plain text passwords.
 */
public class User {
    private long userId;
    private String username;
    private String fullName;
    private String role;
    private String status;
    private String createdAt;

    public User() {}

    public User(long userId, String username, String fullName, String role, String status, String createdAt) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
