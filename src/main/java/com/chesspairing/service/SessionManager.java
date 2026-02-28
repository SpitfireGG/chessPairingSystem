package com.chesspairing.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public String createSession(long adminId, String username) {
        byte[] tokenBytes = new byte[24];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        sessions.put(token, new Session(adminId, username));
        return token;
    }

    public Optional<Session> findSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(token));
    }

    public void invalidate(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public record Session(long adminId, String username) {
    }
}
