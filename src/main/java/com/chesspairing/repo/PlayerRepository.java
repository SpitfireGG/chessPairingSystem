package com.chesspairing.repo;

import com.chesspairing.db.Database;
import com.chesspairing.model.Player;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PlayerRepository {
    private final Database database;

    public PlayerRepository(Database database) {
        this.database = database;
    }

    public long addPlayer(long adminId, String playerName, int initialRank) throws SQLException {
        String sql = "INSERT INTO players (admin_id, player_name, initial_rank) VALUES (?, ?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, adminId);
            statement.setString(2, playerName);
            statement.setInt(3, initialRank);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Could not create player");
    }

    public boolean removePlayer(long adminId, long playerId) throws SQLException {
        String sql = "DELETE FROM players WHERE player_id = ? AND admin_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, playerId);
            statement.setLong(2, adminId);
            return statement.executeUpdate() > 0;
        }
    }

    public List<Player> listPlayersForAdmin(long adminId) throws SQLException {
        String sql = """
            SELECT player_id, admin_id, player_name, current_score, initial_rank
            FROM players
            WHERE admin_id = ?
            ORDER BY initial_rank ASC, player_name ASC
            """;

        List<Player> players = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, adminId);
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

    public void applyTournamentScores(long tournamentId, Map<Long, Double> scores) throws SQLException {
        String resetSql = """
            UPDATE players p
            JOIN tournament_players tp ON tp.player_id = p.player_id
            SET p.current_score = 0.0
            WHERE tp.tournament_id = ?
            """;

        String updateSql = "UPDATE players SET current_score = ? WHERE player_id = ?";

        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement reset = connection.prepareStatement(resetSql)) {
                    reset.setLong(1, tournamentId);
                    reset.executeUpdate();
                }

                try (PreparedStatement update = connection.prepareStatement(updateSql)) {
                    for (Map.Entry<Long, Double> entry : scores.entrySet()) {
                        update.setDouble(1, entry.getValue());
                        update.setLong(2, entry.getKey());
                        update.addBatch();
                    }
                    update.executeBatch();
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
