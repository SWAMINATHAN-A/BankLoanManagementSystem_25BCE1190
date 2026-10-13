package com.bankloan.security;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side in-memory session manager with secure tokens and automatic expiry.
 */
public class SessionManager {

    public static class Session {
        private final String sessionId;
        private final long userId;
        private final String username;
        private final String fullName;
        private final String role;
        private final Instant createdAt;
        private volatile Instant lastAccessedAt;

        public Session(String sessionId, long userId, String username, String fullName, String role) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
            this.createdAt = Instant.now();
            this.lastAccessedAt = Instant.now();
        }

        public void touch() {
            this.lastAccessedAt = Instant.now();
        }

        public boolean isExpired(long timeoutSeconds) {
            return Instant.now().isAfter(lastAccessedAt.plusSeconds(timeoutSeconds));
        }

        public String getSessionId() { return sessionId; }
        public long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
        public Instant getCreatedAt() { return createdAt; }
    }

    private static final long SESSION_TIMEOUT_SECONDS = 3600; // 1 hour idle timeout
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    private static final SessionManager INSTANCE = new SessionManager();

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public Session createSession(long userId, String username, String fullName, String role) {
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        Session session = new Session(token, userId, username, fullName, role);
        SESSIONS.put(token, session);
        return session;
    }

    public Session getValidSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }
        Session session = SESSIONS.get(sessionId.trim());
        if (session == null) {
            return null;
        }
        if (session.isExpired(SESSION_TIMEOUT_SECONDS)) {
            SESSIONS.remove(sessionId.trim());
            return null;
        }
        session.touch();
        return session;
    }

    public void invalidateSession(String sessionId) {
        if (sessionId != null) {
            SESSIONS.remove(sessionId.trim());
        }
    }
}
