package com.chesspairing.service;

import com.chesspairing.model.MatchView;
import com.chesspairing.model.Player;
import com.chesspairing.model.Tournament;
import com.chesspairing.repo.MatchRepository;
import com.chesspairing.repo.PlayerRepository;
import com.chesspairing.repo.TournamentRepository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SwissPairingService {
    private final TournamentRepository tournamentRepository;
    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    public SwissPairingService(
        TournamentRepository tournamentRepository,
        MatchRepository matchRepository,
        PlayerRepository playerRepository
    ) {
        this.tournamentRepository = tournamentRepository;
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
    }

    public List<MatchView> generateNextRound(long adminId, long tournamentId) {
        try {
            Tournament tournament = tournamentRepository.findByIdForAdmin(tournamentId, adminId)
                .orElseThrow(() -> new AppException("Tournament not found"));

            int currentRound = tournament.currentRound();
            if (currentRound > 0 && matchRepository.hasPendingResults(tournamentId, currentRound)) {
                throw new AppException("Enter all results for current round before generating next round");
            }

            int nextRound = currentRound + 1;
            if (matchRepository.hasMatchesInRound(tournamentId, nextRound)) {
                return matchRepository.listMatches(tournamentId, nextRound);
            }

            List<Player> players = tournamentRepository.listTournamentPlayers(tournamentId);
            if (players.size() < 2) {
                throw new AppException("Tournament needs at least two players");
            }

            Map<Long, Double> scores = matchRepository.calculateScores(tournamentId);
            Map<Long, Set<Long>> opponents = matchRepository.loadOpponents(tournamentId);

            List<Player> standings = new ArrayList<>(players);
            standings.sort(
                Comparator.comparingDouble((Player player) -> scores.getOrDefault(player.id(), 0.0)).reversed()
                    .thenComparingInt(Player::initialRank)
                    .thenComparing(Player::name)
            );

            List<Player> unpaired = new ArrayList<>(standings);
            int table = 1;
            while (unpaired.size() > 1) {
                Player player1 = unpaired.removeFirst();
                int opponentIndex = pickOpponentIndex(player1, unpaired, opponents);
                Player player2 = unpaired.remove(opponentIndex);
                matchRepository.createMatch(tournamentId, nextRound, table, player1.id(), player2.id(), null);
                table++;
            }

            if (!unpaired.isEmpty()) {
                Player byePlayer = unpaired.removeFirst();
                matchRepository.createMatch(tournamentId, nextRound, table, byePlayer.id(), null, "BYE");
            }

            tournamentRepository.updateCurrentRound(tournamentId, nextRound);
            playerRepository.applyTournamentScores(tournamentId, matchRepository.calculateScores(tournamentId));

            return matchRepository.listMatches(tournamentId, nextRound);
        } catch (SQLException ex) {
            throw new AppException("Could not generate pairings", ex);
        }
    }

    public MatchRoundResponse listRound(long tournamentId, String roundArg) {
        try {
            int roundNumber;
            if (roundArg == null || roundArg.isBlank() || "current".equalsIgnoreCase(roundArg)) {
                roundNumber = matchRepository.findCurrentRoundWithMatches(tournamentId);
            } else {
                roundNumber = Integer.parseInt(roundArg);
            }

            if (roundNumber <= 0) {
                return new MatchRoundResponse(0, List.of());
            }

            return new MatchRoundResponse(roundNumber, matchRepository.listMatches(tournamentId, roundNumber));
        } catch (NumberFormatException ex) {
            throw new AppException("Round must be a number or 'current'");
        } catch (SQLException ex) {
            throw new AppException("Could not list pairings", ex);
        }
    }

    private int pickOpponentIndex(Player player, List<Player> candidates, Map<Long, Set<Long>> opponents) {
        Set<Long> playedAgainst = opponents.getOrDefault(player.id(), Set.of());
        for (int i = 0; i < candidates.size(); i++) {
            if (!playedAgainst.contains(candidates.get(i).id())) {
                return i;
            }
        }
        return 0;
    }

    public record MatchRoundResponse(int roundNumber, List<MatchView> matches) {
    }
}
