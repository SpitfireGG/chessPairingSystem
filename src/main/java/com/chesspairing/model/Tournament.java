package com.chesspairing.model;

public record Tournament(long id, long adminId, String name, int currentRound, String status) {
}
