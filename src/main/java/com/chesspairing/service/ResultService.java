package com.chesspairing.service;

import com.chesspairing.repo.MatchRepository;
import com.chesspairing.repo.PlayerRepository;
import java.sql.SQLException;
import java.util.Set;

public final class ResultService {
    private static final Set<String> VALID_RESULTS = Set.of("P1_WIN", "DRAW", "P2_WIN", "BYE");

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;

    public ResultService(MatchRepository matchRepository, PlayerRepository playerRepository) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
    }

    public void submitResult(long adminId, long matchId, String resultCode) {
        String normalizedResult = resultCode == null ? "" : resultCode.trim().toUpperCase();
        if (!VALID_RESULTS.contains(normalizedResult)) {
            throw new AppException("Result must be one of: P1_WIN, DRAW, P2_WIN, BYE");
        }

        try {
            MatchRepository.OwnedMatch ownedMatch = matchRepository.findOwnedMatch(matchId, adminId)
                .orElseThrow(() -> new AppException("Match not found"));

            boolean isByeMatch = ownedMatch.player2Id() == null;
            if (isByeMatch && !"BYE".equals(normalizedResult)) {
                throw new AppException("Bye match can only have BYE result");
            }
            if (!isByeMatch && "BYE".equals(normalizedResult)) {
                throw new AppException("Non-bye match cannot have BYE result");
            }

            matchRepository.updateResult(matchId, normalizedResult);
            playerRepository.applyTournamentScores(
                ownedMatch.tournamentId(),
                matchRepository.calculateScores(ownedMatch.tournamentId())
            );
        } catch (SQLException ex) {
            throw new AppException("Could not update match result", ex);
        }
    }
}
