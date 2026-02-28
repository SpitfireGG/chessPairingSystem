package com.chesspairing.service;

import com.chesspairing.model.Organizer;
import com.chesspairing.repo.OrganizerRepository;
import com.chesspairing.util.PasswordHasher;
import java.sql.SQLException;
import java.util.Optional;

public final class AuthService {
    private final OrganizerRepository organizerRepository;
    private final SessionManager sessionManager;

    public AuthService(OrganizerRepository organizerRepository, SessionManager sessionManager) {
        this.organizerRepository = organizerRepository;
        this.sessionManager = sessionManager;
    }

    public Optional<String> login(String username, String plainPassword) {
        try {
            Optional<Organizer> organizer = organizerRepository.findByUsername(username);
            if (organizer.isEmpty()) {
                return Optional.empty();
            }

            String hashed = PasswordHasher.sha256(plainPassword);
            if (!hashed.equals(organizer.get().passwordHash())) {
                return Optional.empty();
            }

            String token = sessionManager.createSession(organizer.get().id(), organizer.get().username());
            return Optional.of(token);
        } catch (SQLException ex) {
            throw new AppException("Login failed because database is unavailable", ex);
        }
    }

    public Optional<SessionManager.Session> getSession(String token) {
        return sessionManager.findSession(token);
    }

    public void logout(String token) {
        sessionManager.invalidate(token);
    }
}
