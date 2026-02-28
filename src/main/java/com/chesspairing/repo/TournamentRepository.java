package com.chesspairing.repo;

import com.chesspairing.db.Database;
import com.chesspairing.model.Player;
import com.chesspairing.model.Tournament;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TournamentRepository {
    private final Database database;

    public TournamentRepository(Database database) {
        this.database = database;
    }

    public long createTournament(long adminId, String tournamentName) throws SQLException {
        String sql = "INSERT INTO tournaments (admin_id, tournament_name, status) VALUES (?, ?, 'CREATED')";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, adminId);
            statement.setString(2, tournamentName);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Could not create tournament");
    }

    public int attachCurrentPlayers(long tournamentId, long adminId) throws SQLException {
        String sql = """
            INSERT INTO tournament_players (tournament_id, player_id)
            SELECT ?, p.player_id
            FROM players p
            WHERE p.admin_id = ?
            ORDER BY p.initial_rank ASC, p.player_id ASC
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            statement.setLong(2, adminId);
            return statement.executeUpdate();
        }
    }

    public void deleteTournamentForAdmin(long tournamentId, long adminId) throws SQLException {
        String sql = "DELETE FROM tournaments WHERE tournament_id = ? AND admin_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            statement.setLong(2, adminId);
            statement.executeUpdate();
        }
    }

    public List<Tournament> listByAdmin(long adminId) throws SQLException {
        String sql = """
            SELECT tournament_id, admin_id, tournament_name, current_round, status
            FROM tournaments
            WHERE admin_id = ?
            ORDER BY tournament_id DESC
            """;

        List<Tournament> tournaments = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tournaments.add(mapTournament(resultSet));
                }
            }
        }
        return tournaments;
    }

    public List<Tournament> listAll() throws SQLException {
        String sql = """
            SELECT tournament_id, admin_id, tournament_name, current_round, status
            FROM tournaments
            ORDER BY tournament_id DESC
            """;

        List<Tournament> tournaments = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                tournaments.add(mapTournament(resultSet));
            }
        }
        return tournaments;
    }

    public Optional<Tournament> findByIdForAdmin(long tournamentId, long adminId) throws SQLException {
        String sql = """
            SELECT tournament_id, admin_id, tournament_name, current_round, status
            FROM tournaments
            WHERE tournament_id = ? AND admin_id = ?
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            statement.setLong(2, adminId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapTournament(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public Optional<Tournament> findById(long tournamentId) throws SQLException {
        String sql = """
            SELECT tournament_id, admin_id, tournament_name, current_round, status
            FROM tournaments
            WHERE tournament_id = ?
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapTournament(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public List<Player> listTournamentPlayers(long tournamentId) throws SQLException {
        String sql = """
            SELECT p.player_id, p.admin_id, p.player_name, p.current_score, p.initial_rank
            FROM tournament_players tp
            JOIN players p ON p.player_id = tp.player_id
            WHERE tp.tournament_id = ?
            ORDER BY p.initial_rank ASC, p.player_name ASC
            """;

        List<Player> players = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    players.add(new Player(
                        resultSet.getLong("player_id"),
                        resultSet.getLong("admin_id"),
                        resultSet.getString("player_name"),
                        resultSet.getDouble("current_score"),
                        resultSet.getInt("initial_rank")
                    ));
                }
            }
        }
        return players;
    }

    public void updateCurrentRound(long tournamentId, int currentRound) throws SQLException {
        String sql = "UPDATE tournaments SET current_round = ?, status = 'IN_PROGRESS' WHERE tournament_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, currentRound);
            statement.setLong(2, tournamentId);
            statement.executeUpdate();
        }
    }

    private Tournament mapTournament(ResultSet resultSet) throws SQLException {
        return new Tournament(
            resultSet.getLong("tournament_id"),
            resultSet.getLong("admin_id"),
            resultSet.getString("tournament_name"),
            resultSet.getInt("current_round"),
            resultSet.getString("status")
        );
    }
}
