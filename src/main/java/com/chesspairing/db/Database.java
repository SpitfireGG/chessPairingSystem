package com.chesspairing.db;

import com.chesspairing.config.AppConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private final AppConfig config;

    public Database(AppConfig config) {
        this.config = config;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPassword());
    }
}
