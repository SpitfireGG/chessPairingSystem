package com.chesspairing.service;

import com.chesspairing.model.Tournament;
import com.chesspairing.repo.TournamentRepository;
import java.sql.SQLException;
import java.util.List;

public final class TournamentService {
    private final TournamentRepository tournamentRepository;

    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    public long createTournament(long adminId, String tournamentName) {
        String normalized = tournamentName == null ? "" : tournamentName.trim();
        if (normalized.isEmpty()) {
            throw new AppException("Tournament name is required");
        }

        try {
            long tournamentId = tournamentRepository.createTournament(adminId, normalized);
            int linkedPlayers = tournamentRepository.attachCurrentPlayers(tournamentId, adminId);
            if (linkedPlayers < 2) {
                tournamentRepository.deleteTournamentForAdmin(tournamentId, adminId);
                throw new AppException("Add at least two players before creating a tournament");
            }
            return tournamentId;
        } catch (SQLException ex) {
            throw new AppException("Could not create tournament", ex);
        }
    }

    public List<Tournament> listAdminTournaments(long adminId) {
        try {
            return tournamentRepository.listByAdmin(adminId);
        } catch (SQLException ex) {
            throw new AppException("Could not list tournaments", ex);
        }
    }

    public List<Tournament> listAllTournaments() {
        try {
            return tournamentRepository.listAll();
        } catch (SQLException ex) {
            throw new AppException("Could not list tournaments", ex);
        }
    }

    public Tournament requireOwnedTournament(long adminId, long tournamentId) {
        try {
            return tournamentRepository.findByIdForAdmin(tournamentId, adminId)
                .orElseThrow(() -> new AppException("Tournament not found"));
        } catch (SQLException ex) {
            throw new AppException("Could not load tournament", ex);
        }
    }

    public Tournament requireTournament(long tournamentId) {
        try {
            return tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new AppException("Tournament not found"));
        } catch (SQLException ex) {
            throw new AppException("Could not load tournament", ex);
        }
    }
}
