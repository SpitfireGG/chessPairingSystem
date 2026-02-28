package com.chesspairing.web;

import com.chesspairing.config.AppConfig;
import com.chesspairing.db.Database;
import com.chesspairing.db.SchemaInitializer;
import com.chesspairing.repo.MatchRepository;
import com.chesspairing.repo.OrganizerRepository;
import com.chesspairing.repo.PlayerRepository;
import com.chesspairing.repo.TournamentRepository;
import com.chesspairing.service.AuthService;
import com.chesspairing.service.LeaderboardService;
import com.chesspairing.service.PlayerService;
import com.chesspairing.service.ResultService;
import com.chesspairing.service.SessionManager;
import com.chesspairing.service.SwissPairingService;
import com.chesspairing.service.TournamentService;
import com.chesspairing.util.PasswordHasher;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.Executors;

public final class ChessPairingApplication {
    private static final Map<String, String> PAGE_ROUTES = Map.of(
        "/", "/static/landing.html",
        "/login", "/static/login.html",
        "/app", "/static/dashboard.html",
        "/live", "/static/live.html"
    );

    private ChessPairingApplication() {
    }

    public static void main(String[] args) throws IOException {
        AppConfig config = AppConfig.fromEnvironment();
        Database database = new Database(config);

        initializeDatabase(database, config);

        OrganizerRepository organizerRepository = new OrganizerRepository(database);
        PlayerRepository playerRepository = new PlayerRepository(database);
        TournamentRepository tournamentRepository = new TournamentRepository(database);
        MatchRepository matchRepository = new MatchRepository(database);

        SessionManager sessionManager = new SessionManager();
        AuthService authService = new AuthService(organizerRepository, sessionManager);
        PlayerService playerService = new PlayerService(playerRepository);
        TournamentService tournamentService = new TournamentService(tournamentRepository);
        SwissPairingService swissPairingService = new SwissPairingService(tournamentRepository, matchRepository, playerRepository);
        ResultService resultService = new ResultService(matchRepository, playerRepository);
        LeaderboardService leaderboardService = new LeaderboardService(tournamentRepository, matchRepository);

        ApiController apiController = new ApiController(
            authService,
            playerService,
            tournamentService,
            swissPairingService,
            resultService,
            leaderboardService
        );

        HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        server.createContext("/api/", apiController::handle);
        server.createContext("/static/", ChessPairingApplication::handleStatic);
        server.createContext("/", ChessPairingApplication::handlePages);
        server.setExecutor(Executors.newFixedThreadPool(12));
        server.start();

        System.out.printf("Chess Pairing Web App running at http://localhost:%d%n", config.port());
        System.out.printf("Default organizer login: %s / %s%n", config.defaultAdminUsername(), config.defaultAdminPassword());
    }

    private static void initializeDatabase(Database database, AppConfig config) {
        try {
            SchemaInitializer.initialize(database);
            OrganizerRepository organizerRepository = new OrganizerRepository(database);
            organizerRepository.ensureDefaultAdmin(
                config.defaultAdminUsername(),
                PasswordHasher.sha256(config.defaultAdminPassword())
            );
        } catch (SQLException ex) {
            throw new IllegalStateException(
                "Cannot initialize MySQL schema. Check DB_URL/DB_USER/DB_PASSWORD and ensure database exists.",
                ex
            );
        }
    }

    private static void handlePages(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendMethodNotAllowed(exchange, "GET");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String resourcePath = PAGE_ROUTES.get(path);
            if (resourcePath == null) {
                HttpUtil.sendText(exchange, 404, "Not Found");
                return;
            }

            byte[] content = readResource(resourcePath);
            HttpUtil.sendHtml(exchange, 200, content);
        } finally {
            exchange.close();
        }
    }

    private static void handleStatic(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                HttpUtil.sendMethodNotAllowed(exchange, "GET");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (!path.startsWith("/static/") || path.contains("..")) {
                HttpUtil.sendText(exchange, 400, "Bad Request");
                return;
            }

            byte[] content;
            try {
                content = readResource(path);
            } catch (IllegalStateException ex) {
                HttpUtil.sendText(exchange, 404, "Not Found");
                return;
            }

            HttpUtil.sendStatic(exchange, 200, contentTypeFor(path), content);
        } finally {
            exchange.close();
        }
    }

    private static byte[] readResource(String resourcePath) {
        try (InputStream inputStream = ChessPairingApplication.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing resource: " + resourcePath);
            }
            return inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load resource " + resourcePath, ex);
        }
    }

    private static String contentTypeFor(String path) {
        if (path.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (path.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (path.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }
        return "application/octet-stream";
    }
}
