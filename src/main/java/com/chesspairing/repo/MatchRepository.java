package com.chesspairing.repo;

import com.chesspairing.db.Database;
import com.chesspairing.model.MatchView;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MatchRepository {
    private final Database database;

    public MatchRepository(Database database) {
        this.database = database;
    }

    public boolean hasMatchesInRound(long tournamentId, int roundNumber) throws SQLException {
        String sql = "SELECT 1 FROM matches WHERE tournament_id = ? AND round_number = ? LIMIT 1";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            statement.setInt(2, roundNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public boolean hasPendingResults(long tournamentId, int roundNumber) throws SQLException {
        String sql = """
            SELECT 1
            FROM matches
            WHERE tournament_id = ?
              AND round_number = ?
              AND player2_id IS NOT NULL
              AND match_result IS NULL
            LIMIT 1
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            statement.setInt(2, roundNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public void createMatch(
        long tournamentId,
        int roundNumber,
        int tableNumber,
        long player1Id,
        Long player2Id,
        String result
    ) throws SQLException {
        String sql = """
            INSERT INTO matches (tournament_id, round_number, table_number, player1_id, player2_id, match_result)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            statement.setInt(2, roundNumber);
            statement.setInt(3, tableNumber);
            statement.setLong(4, player1Id);
            if (player2Id == null) {
                statement.setNull(5, java.sql.Types.BIGINT);
            } else {
                statement.setLong(5, player2Id);
            }
            if (result == null) {
                statement.setNull(6, java.sql.Types.VARCHAR);
            } else {
                statement.setString(6, result);
            }
            statement.executeUpdate();
        }
    }

    public List<MatchView> listMatches(long tournamentId, int roundNumber) throws SQLException {
        String sql = """
            SELECT m.match_id, m.tournament_id, m.round_number, m.table_number,
                   p1.player_id AS player1_id, p1.player_name AS player1_name,
                   p2.player_id AS player2_id, p2.player_name AS player2_name,
                   m.match_result
            FROM matches m
            JOIN players p1 ON p1.player_id = m.player1_id
            LEFT JOIN players p2 ON p2.player_id = m.player2_id
            WHERE m.tournament_id = ? AND m.round_number = ?
            ORDER BY m.table_number ASC
            """;

        List<MatchView> matches = new ArrayList<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            statement.setInt(2, roundNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    matches.add(mapMatch(resultSet));
                }
            }
        }
        return matches;
    }

    public List<MatchView> listMatchesForCurrentRound(long tournamentId) throws SQLException {
        int currentRound = findCurrentRoundWithMatches(tournamentId);
        if (currentRound == 0) {
            return List.of();
        }
        return listMatches(tournamentId, currentRound);
    }

    public int findCurrentRoundWithMatches(long tournamentId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(round_number), 0) AS current_round FROM matches WHERE tournament_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("current_round");
                }
                return 0;
            }
        }
    }

    public Map<Long, Set<Long>> loadOpponents(long tournamentId) throws SQLException {
        String sql = """
            SELECT player1_id, player2_id
            FROM matches
            WHERE tournament_id = ?
              AND player2_id IS NOT NULL
            """;

        Map<Long, Set<Long>> opponents = new HashMap<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long player1 = resultSet.getLong("player1_id");
                    long player2 = resultSet.getLong("player2_id");
                    opponents.computeIfAbsent(player1, key -> new HashSet<>()).add(player2);
                    opponents.computeIfAbsent(player2, key -> new HashSet<>()).add(player1);
                }
            }
        }
        return opponents;
    }

    public Map<Long, Double> calculateScores(long tournamentId) throws SQLException {
        String sql = """
            SELECT player_id, SUM(score) AS total_score
            FROM (
                SELECT m.player1_id AS player_id,
                       CASE m.match_result
                           WHEN 'P1_WIN' THEN 1.0
                           WHEN 'DRAW' THEN 0.5
                           WHEN 'P2_WIN' THEN 0.0
                           WHEN 'BYE' THEN 1.0
                           ELSE 0.0
                       END AS score
                FROM matches m
                WHERE m.tournament_id = ?
                  AND m.match_result IS NOT NULL
                UNION ALL
                SELECT m.player2_id AS player_id,
                       CASE m.match_result
                           WHEN 'P1_WIN' THEN 0.0
                           WHEN 'DRAW' THEN 0.5
                           WHEN 'P2_WIN' THEN 1.0
                           ELSE 0.0
                       END AS score
                FROM matches m
                WHERE m.tournament_id = ?
                  AND m.player2_id IS NOT NULL
                  AND m.match_result IS NOT NULL
            ) scores
            GROUP BY player_id
            """;

        Map<Long, Double> result = new HashMap<>();
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, tournamentId);
            statement.setLong(2, tournamentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.put(resultSet.getLong("player_id"), resultSet.getDouble("total_score"));
                }
            }
        }

        return result;
    }

    public Optional<OwnedMatch> findOwnedMatch(long matchId, long adminId) throws SQLException {
        String sql = """
            SELECT m.match_id, m.tournament_id, m.player2_id, m.match_result
            FROM matches m
            JOIN tournaments t ON t.tournament_id = m.tournament_id
            WHERE m.match_id = ? AND t.admin_id = ?
            """;

        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, matchId);
            statement.setLong(2, adminId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Long player2Id = resultSet.getObject("player2_id") == null
                        ? null
                        : resultSet.getLong("player2_id");

                    return Optional.of(new OwnedMatch(
                        resultSet.getLong("match_id"),
                        resultSet.getLong("tournament_id"),
                        player2Id,
                        resultSet.getString("match_result")
                    ));
                }
                return Optional.empty();
            }
        }
    }

    public void updateResult(long matchId, String result) throws SQLException {
        String sql = "UPDATE matches SET match_result = ? WHERE match_id = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, result);
            statement.setLong(2, matchId);
            statement.executeUpdate();
        }
    }

    private MatchView mapMatch(ResultSet resultSet) throws SQLException {
        Long player2Id = resultSet.getObject("player2_id") == null ? null : resultSet.getLong("player2_id");
        return new MatchView(
            resultSet.getLong("match_id"),
            resultSet.getLong("tournament_id"),
            resultSet.getInt("round_number"),
            resultSet.getInt("table_number"),
            resultSet.getLong("player1_id"),
            resultSet.getString("player1_name"),
            player2Id,
            resultSet.getString("player2_name"),
            resultSet.getString("match_result")
        );
    }

    public record OwnedMatch(long matchId, long tournamentId, Long player2Id, String matchResult) {
    }
}
