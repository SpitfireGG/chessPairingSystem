package com.chesspairing.model;

public record MatchView(
    long id,
    long tournamentId,
    int roundNumber,
    int tableNumber,
    long player1Id,
    String player1Name,
    Long player2Id,
    String player2Name,
    String matchResult
) {
}
