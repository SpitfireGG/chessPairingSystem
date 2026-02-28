package com.chesspairing.config;

public record AppConfig(
    int port,
    String dbUrl,
    String dbUser,
    String dbPassword,
    String defaultAdminUsername,
    String defaultAdminPassword
) {
    public static AppConfig fromEnvironment() {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        String dbUrl = System.getenv().getOrDefault(
            "DB_URL",
            "jdbc:mysql://localhost:3306/chess_pairing?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        );
        String dbUser = System.getenv().getOrDefault("DB_USER", "root");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "root");
        String adminUser = System.getenv().getOrDefault("APP_DEFAULT_ADMIN_USER", "admin");
        String adminPassword = System.getenv().getOrDefault("APP_DEFAULT_ADMIN_PASSWORD", "admin123");

        return new AppConfig(port, dbUrl, dbUser, dbPassword, adminUser, adminPassword);
    }
}
