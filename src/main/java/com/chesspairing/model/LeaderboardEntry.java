package com.chesspairing.model;

public record LeaderboardEntry(int rank, long playerId, String playerName, double score, int initialRank) {
}
