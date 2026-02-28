package com.chesspairing.service;

import com.chesspairing.model.Player;
import com.chesspairing.repo.PlayerRepository;
import java.sql.SQLException;
import java.util.List;

public final class PlayerService {
    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public long addPlayer(long adminId, String playerName, int initialRank) {
        String normalized = playerName == null ? "" : playerName.trim();
        if (normalized.isEmpty()) {
            throw new AppException("Player name is required");
        }
        if (initialRank <= 0) {
            throw new AppException("Initial rank must be a positive number");
        }

        try {
            return playerRepository.addPlayer(adminId, normalized, initialRank);
        } catch (SQLException ex) {
            throw new AppException("Could not create player. Name may already exist.", ex);
        }
    }

    public List<Player> listPlayers(long adminId) {
        try {
            return playerRepository.listPlayersForAdmin(adminId);
        } catch (SQLException ex) {
            throw new AppException("Could not list players", ex);
        }
    }

    public void deletePlayer(long adminId, long playerId) {
        try {
            boolean removed = playerRepository.removePlayer(adminId, playerId);
            if (!removed) {
                throw new AppException("Player not found");
            }
        } catch (SQLException ex) {
            throw new AppException("Could not remove player", ex);
        }
    }
}
