package com.chesspairing.service;

import com.chesspairing.model.LeaderboardEntry;
import com.chesspairing.model.Player;
import com.chesspairing.repo.MatchRepository;
import com.chesspairing.repo.TournamentRepository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class LeaderboardService {
    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;

    public LeaderboardService(TournamentRepository tournamentRepository, MatchRepository matchRepository) {
        this.tournamentRepository = tournamentRepository;
        this.matchRepository = matchRepository;
    }

    public List<LeaderboardEntry> getLeaderboard(long tournamentId) {
        try {
            List<Player> players = tournamentRepository.listTournamentPlayers(tournamentId);
            Map<Long, Double> scores = matchRepository.calculateScores(tournamentId);

            List<PlayerScore> rows = new ArrayList<>();
            for (Player player : players) {
                rows.add(new PlayerScore(player, scores.getOrDefault(player.id(), 0.0)));
            }

            rows.sort(
                Comparator.comparingDouble(PlayerScore::score).reversed()
                    .thenComparingInt(row -> row.player().initialRank())
                    .thenComparing(row -> row.player().name())
            );

            List<LeaderboardEntry> leaderboard = new ArrayList<>();
            int rank = 1;
            for (PlayerScore row : rows) {
                leaderboard.add(new LeaderboardEntry(
                    rank,
                    row.player().id(),
                    row.player().name(),
                    row.score(),
                    row.player().initialRank()
                ));
                rank++;
            }

            return leaderboard;
        } catch (SQLException ex) {
            throw new AppException("Could not load leaderboard", ex);
        }
    }

    private record PlayerScore(Player player, double score) {
    }
}
