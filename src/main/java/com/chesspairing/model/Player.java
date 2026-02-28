package com.chesspairing.model;

public record Player(long id, long adminId, String name, double currentScore, int initialRank) {
}
