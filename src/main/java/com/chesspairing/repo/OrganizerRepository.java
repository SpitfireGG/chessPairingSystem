package com.chesspairing.repo;

import com.chesspairing.db.Database;
import com.chesspairing.model.Organizer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class OrganizerRepository {
    private final Database database;

    public OrganizerRepository(Database database) {
        this.database = database;
    }

    public Optional<Organizer> findByUsername(String username) throws SQLException {
        String sql = """
            SELECT admin_id, username, password_hash
            FROM organizers
            WHERE username = ?
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new Organizer(
                        resultSet.getLong("admin_id"),
                        resultSet.getString("username"),
                        resultSet.getString("password_hash")
                    ));
                }
                return Optional.empty();
            }
        }
    }

    public Optional<Organizer> findById(long adminId) throws SQLException {
        String sql = """
            SELECT admin_id, username, password_hash
            FROM organizers
            WHERE admin_id = ?
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new Organizer(
                        resultSet.getLong("admin_id"),
                        resultSet.getString("username"),
                        resultSet.getString("password_hash")
                    ));
                }
                return Optional.empty();
            }
        }
    }

    public void ensureDefaultAdmin(String username, String passwordHash) throws SQLException {
        if (findByUsername(username).isPresent()) {
            return;
        }

        String sql = "INSERT INTO organizers (username, password_hash) VALUES (?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            statement.executeUpdate();
        }
    }
}
