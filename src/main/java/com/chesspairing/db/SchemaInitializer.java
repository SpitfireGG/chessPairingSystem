package com.chesspairing.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaInitializer {
    private SchemaInitializer() {
    }

    public static void initialize(Database database) throws SQLException {
        String sql = loadSchemaSql();
        String[] statements = sql.split(";");

        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            for (String raw : statements) {
                String query = raw.trim();
                if (!query.isEmpty()) {
                    statement.execute(query);
                }
            }
        }
    }

    private static String loadSchemaSql() {
        try (InputStream inputStream = SchemaInitializer.class.getResourceAsStream("/db/schema.sql")) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing resource: /db/schema.sql");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read /db/schema.sql", ex);
        }
    }
}
