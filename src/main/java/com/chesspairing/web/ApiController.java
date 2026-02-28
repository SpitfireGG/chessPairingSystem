package com.chesspairing.web;

import com.chesspairing.model.LeaderboardEntry;
import com.chesspairing.model.MatchView;
import com.chesspairing.model.Player;
import com.chesspairing.model.Tournament;
import com.chesspairing.service.AppException;
import com.chesspairing.service.AuthService;
import com.chesspairing.service.LeaderboardService;
import com.chesspairing.service.PlayerService;
import com.chesspairing.service.ResultService;
import com.chesspairing.service.SessionManager;
import com.chesspairing.service.SwissPairingService;
import com.chesspairing.service.TournamentService;
import com.chesspairing.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ApiController {
    private static final String SESSION_COOKIE = "CPS_SESSION";

    private final AuthService authService;
    private final PlayerService playerService;
    private final TournamentService tournamentService;
    private final SwissPairingService swissPairingService;
    private final ResultService resultService;
    private final LeaderboardService leaderboardService;

    public ApiController(
        AuthService authService,
        PlayerService playerService,
        TournamentService tournamentService,
        SwissPairingService swissPairingService,
        ResultService resultService,
        LeaderboardService leaderboardService
    ) {
        this.authService = authService;
        this.playerService = playerService;
        this.tournamentService = tournamentService;
        this.swissPairingService = swissPairingService;
        this.resultService = resultService;
        this.leaderboardService = leaderboardService;
    }

    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if ("/api/auth/login".equals(path)) {
                handleLogin(exchange);
                return;
            }
            if ("/api/auth/logout".equals(path)) {
                handleLogout(exchange);
                return;
            }
            if ("/api/admin/session".equals(path)) {
                handleSession(exchange);
                return;
            }
            if ("/api/admin/players".equals(path)) {
                handlePlayersCollection(exchange);
                return;
            }
            if (path.startsWith("/api/admin/players/")) {
                handlePlayerItem(exchange, path);
                return;
            }
            if ("/api/admin/tournaments".equals(path)) {
                handleTournamentsCollection(exchange);
                return;
            }
            if (path.startsWith("/api/admin/tournaments/")) {
                handleTournamentActions(exchange, path);
                return;
            }
            if (path.startsWith("/api/admin/matches/")) {
                handleMatchActions(exchange, path);
                return;
            }
            if ("/api/public/tournaments".equals(path)) {
                handlePublicTournaments(exchange);
                return;
            }
            if (path.startsWith("/api/public/tournaments/")) {
                handlePublicTournamentActions(exchange, path);
                return;
            }

            HttpUtil.sendJson(exchange, 404, "{\"error\":\"API endpoint not found\"}");
        } catch (UnauthorizedException ex) {
            HttpUtil.sendJson(exchange, 401, "{\"error\":\"Unauthorized\"}");
        } catch (AppException ex) {
            HttpUtil.sendJson(exchange, 400, "{\"error\":\"" + JsonUtil.escape(ex.getMessage()) + "\"}");
        } finally {
            exchange.close();
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendMethodNotAllowed(exchange, "POST");
            return;
        }

        Map<String, String> form = HttpUtil.parseFormEncoded(HttpUtil.readBody(exchange.getRequestBody()));
        String username = form.getOrDefault("username", "").trim();
        String password = form.getOrDefault("password", "");

        Optional<String> token = authService.login(username, password);
        if (token.isEmpty()) {
            HttpUtil.sendJson(exchange, 401, "{\"error\":\"Invalid username or password\"}");
            return;
        }

        exchange.getResponseHeaders().add(
            "Set-Cookie",
            SESSION_COOKIE + "=" + token.get() + "; Path=/; HttpOnly; SameSite=Lax"
        );
        HttpUtil.sendJson(exchange, 200, "{\"ok\":true}");
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendMethodNotAllowed(exchange, "POST");
            return;
        }

        String token = extractSessionToken(exchange);
        authService.logout(token);
        exchange.getResponseHeaders().add(
            "Set-Cookie",
            SESSION_COOKIE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"
        );
        HttpUtil.sendJson(exchange, 200, "{\"ok\":true}");
    }

    private void handleSession(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendMethodNotAllowed(exchange, "GET");
            return;
        }

        SessionManager.Session session = requireSession(exchange);
        HttpUtil.sendJson(
            exchange,
            200,
            "{\"adminId\":" + session.adminId() + ",\"username\":\"" + JsonUtil.escape(session.username()) + "\"}"
        );
    }

    private void handlePlayersCollection(HttpExchange exchange) throws IOException {
        SessionManager.Session session = requireSession(exchange);

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            List<Player> players = playerService.listPlayers(session.adminId());
            HttpUtil.sendJson(exchange, 200, playersJson(players));
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Map<String, String> form = HttpUtil.parseFormEncoded(HttpUtil.readBody(exchange.getRequestBody()));
            String name = form.get("name");
            int initialRank = parseInteger(form.getOrDefault("initialRank", "0"), "initialRank");
            long playerId = playerService.addPlayer(session.adminId(), name, initialRank);
            HttpUtil.sendJson(exchange, 201, "{\"ok\":true,\"playerId\":" + playerId + "}");
            return;
        }

        HttpUtil.sendMethodNotAllowed(exchange, "GET, POST");
    }

    private void handlePlayerItem(HttpExchange exchange, String path) throws IOException {
        SessionManager.Session session = requireSession(exchange);

        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendMethodNotAllowed(exchange, "DELETE");
            return;
        }

        long playerId = parsePathId(path, "/api/admin/players/");
        playerService.deletePlayer(session.adminId(), playerId);
        HttpUtil.sendJson(exchange, 200, "{\"ok\":true}");
    }

    private void handleTournamentsCollection(HttpExchange exchange) throws IOException {
        SessionManager.Session session = requireSession(exchange);

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            List<Tournament> tournaments = tournamentService.listAdminTournaments(session.adminId());
            HttpUtil.sendJson(exchange, 200, tournamentsJson(tournaments));
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Map<String, String> form = HttpUtil.parseFormEncoded(HttpUtil.readBody(exchange.getRequestBody()));
            long tournamentId = tournamentService.createTournament(session.adminId(), form.get("name"));
            HttpUtil.sendJson(exchange, 201, "{\"ok\":true,\"tournamentId\":" + tournamentId + "}");
            return;
        }

        HttpUtil.sendMethodNotAllowed(exchange, "GET, POST");
    }

    private void handleTournamentActions(HttpExchange exchange, String path) throws IOException {
        SessionManager.Session session = requireSession(exchange);
        String prefix = "/api/admin/tournaments/";
        if (!path.startsWith(prefix)) {
            HttpUtil.sendJson(exchange, 404, "{\"error\":\"Invalid tournament path\"}");
            return;
        }
        String remainder = path.substring(prefix.length());
        String[] parts = remainder.split("/");
        if (parts.length == 0 || parts[0].isBlank()) {
            HttpUtil.sendJson(exchange, 404, "{\"error\":\"Invalid tournament path\"}");
            return;
        }
        long tournamentId = parseLong(parts[0], "tournamentId");
        tournamentService.requireOwnedTournament(session.adminId(), tournamentId);

        if (parts.length == 3 && "pairings".equals(parts[1]) && "generate".equals(parts[2])) {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendMethodNotAllowed(exchange, "POST");
                return;
            }
            List<MatchView> matches = swissPairingService.generateNextRound(session.adminId(), tournamentId);
            int roundNumber = matches.isEmpty() ? 0 : matches.getFirst().roundNumber();
            HttpUtil.sendJson(exchange, 200, matchesJson(tournamentId, roundNumber, matches));
            return;
        }

        if (parts.length == 2 && "matches".equals(parts[1])) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendMethodNotAllowed(exchange, "GET");
                return;
            }
            Map<String, String> query = HttpUtil.parseFormEncoded(exchange.getRequestURI().getRawQuery());
            SwissPairingService.MatchRoundResponse response = swissPairingService.listRound(tournamentId, query.get("round"));
            HttpUtil.sendJson(exchange, 200, matchesJson(tournamentId, response.roundNumber(), response.matches()));
            return;
        }

        if (parts.length == 2 && "leaderboard".equals(parts[1])) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendMethodNotAllowed(exchange, "GET");
                return;
            }
            List<LeaderboardEntry> entries = leaderboardService.getLeaderboard(tournamentId);
            HttpUtil.sendJson(exchange, 200, leaderboardJson(tournamentId, entries));
            return;
        }

        HttpUtil.sendJson(exchange, 404, "{\"error\":\"Unknown tournament action\"}");
    }

    private void handleMatchActions(HttpExchange exchange, String path) throws IOException {
        SessionManager.Session session = requireSession(exchange);
        String prefix = "/api/admin/matches/";
        if (!path.startsWith(prefix)) {
            HttpUtil.sendJson(exchange, 404, "{\"error\":\"Invalid match path\"}");
            return;
        }
        String remainder = path.substring(prefix.length());
        String[] parts = remainder.split("/");

        if (parts.length == 2 && "result".equals(parts[1])) {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendMethodNotAllowed(exchange, "POST");
                return;
            }
            long matchId = parseLong(parts[0], "matchId");
            Map<String, String> form = HttpUtil.parseFormEncoded(HttpUtil.readBody(exchange.getRequestBody()));
            resultService.submitResult(session.adminId(), matchId, form.get("result"));
            HttpUtil.sendJson(exchange, 200, "{\"ok\":true}");
            return;
        }

        HttpUtil.sendJson(exchange, 404, "{\"error\":\"Unknown match action\"}");
    }

    private void handlePublicTournaments(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendMethodNotAllowed(exchange, "GET");
            return;
        }

        List<Tournament> tournaments = tournamentService.listAllTournaments();
        HttpUtil.sendJson(exchange, 200, tournamentsJson(tournaments));
    }

    private void handlePublicTournamentActions(HttpExchange exchange, String path) throws IOException {
        String prefix = "/api/public/tournaments/";
        if (!path.startsWith(prefix)) {
            HttpUtil.sendJson(exchange, 404, "{\"error\":\"Invalid tournament path\"}");
            return;
        }
        String remainder = path.substring(prefix.length());
        String[] parts = remainder.split("/");
        if (parts.length < 2) {
            HttpUtil.sendJson(exchange, 404, "{\"error\":\"Invalid tournament path\"}");
            return;
        }

        long tournamentId = parseLong(parts[0], "tournamentId");
        tournamentService.requireTournament(tournamentId);

        if (parts.length == 3 && "matches".equals(parts[1]) && "current".equals(parts[2])) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendMethodNotAllowed(exchange, "GET");
                return;
            }
            SwissPairingService.MatchRoundResponse response = swissPairingService.listRound(tournamentId, "current");
            HttpUtil.sendJson(exchange, 200, matchesJson(tournamentId, response.roundNumber(), response.matches()));
            return;
        }

        if (parts.length == 2 && "leaderboard".equals(parts[1])) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendMethodNotAllowed(exchange, "GET");
                return;
            }
            List<LeaderboardEntry> entries = leaderboardService.getLeaderboard(tournamentId);
            HttpUtil.sendJson(exchange, 200, leaderboardJson(tournamentId, entries));
            return;
        }

        HttpUtil.sendJson(exchange, 404, "{\"error\":\"Unknown public action\"}");
    }

    private SessionManager.Session requireSession(HttpExchange exchange) throws IOException {
        String token = extractSessionToken(exchange);
        Optional<SessionManager.Session> session = authService.getSession(token);
        if (session.isEmpty()) {
            throw new UnauthorizedException();
        }
        return session.get();
    }

    private String extractSessionToken(HttpExchange exchange) {
        return HttpUtil.parseCookies(exchange.getRequestHeaders()).get(SESSION_COOKIE);
    }

    private long parsePathId(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            throw new AppException("Invalid path");
        }
        String value = path.substring(prefix.length());
        return parseLong(value, "path id");
    }

    private static int parseInteger(String raw, String fieldName) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ex) {
            throw new AppException("Invalid integer for " + fieldName);
        }
    }

    private static long parseLong(String raw, String fieldName) {
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception ex) {
            throw new AppException("Invalid number for " + fieldName);
        }
    }

    private static final class UnauthorizedException extends RuntimeException {
    }

    private String playersJson(List<Player> players) {
        StringBuilder json = new StringBuilder();
        json.append("{\"players\":[");
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"id\":").append(player.id())
                .append(",\"name\":\"").append(JsonUtil.escape(player.name())).append('"')
                .append(",\"currentScore\":").append(player.currentScore())
                .append(",\"initialRank\":").append(player.initialRank())
                .append('}');
        }
        json.append("]}");
        return json.toString();
    }

    private String tournamentsJson(List<Tournament> tournaments) {
        StringBuilder json = new StringBuilder();
        json.append("{\"tournaments\":[");
        for (int i = 0; i < tournaments.size(); i++) {
            Tournament tournament = tournaments.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"id\":").append(tournament.id())
                .append(",\"name\":\"").append(JsonUtil.escape(tournament.name())).append('"')
                .append(",\"currentRound\":").append(tournament.currentRound())
                .append(",\"status\":\"").append(JsonUtil.escape(tournament.status())).append('"')
                .append('}');
        }
        json.append("]}");
        return json.toString();
    }

    private String matchesJson(long tournamentId, int roundNumber, List<MatchView> matches) {
        StringBuilder json = new StringBuilder();
        json.append("{\"tournamentId\":").append(tournamentId)
            .append(",\"roundNumber\":").append(roundNumber)
            .append(",\"matches\":[");

        for (int i = 0; i < matches.size(); i++) {
            MatchView match = matches.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"matchId\":").append(match.id())
                .append(",\"tableNumber\":").append(match.tableNumber())
                .append(",\"player1Id\":").append(match.player1Id())
                .append(",\"player1Name\":\"").append(JsonUtil.escape(match.player1Name())).append('"')
                .append(",\"player2Id\":");

            if (match.player2Id() == null) {
                json.append("null");
            } else {
                json.append(match.player2Id());
            }

            json.append(",\"player2Name\":");
            if (match.player2Name() == null) {
                json.append("null");
            } else {
                json.append('"').append(JsonUtil.escape(match.player2Name())).append('"');
            }

            json.append(",\"result\":");
            if (match.matchResult() == null) {
                json.append("null");
            } else {
                json.append('"').append(JsonUtil.escape(match.matchResult())).append('"');
            }
            json.append('}');
        }

        json.append("]}");
        return json.toString();
    }

    private String leaderboardJson(long tournamentId, List<LeaderboardEntry> entries) {
        StringBuilder json = new StringBuilder();
        json.append("{\"tournamentId\":").append(tournamentId).append(",\"entries\":[");

        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"rank\":").append(entry.rank())
                .append(",\"playerId\":").append(entry.playerId())
                .append(",\"playerName\":\"").append(JsonUtil.escape(entry.playerName())).append('"')
                .append(",\"score\":").append(entry.score())
                .append(",\"initialRank\":").append(entry.initialRank())
                .append('}');
        }

        json.append("]}");
        return json.toString();
    }
}
