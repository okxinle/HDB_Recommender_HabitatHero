package habitathero.auth.db;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class Db {
    private static final String DB_URL = "jdbc:sqlite:database/habitathero.db";

    private Db() {}

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initSchema() throws Exception {
        String sql;
        try (var in = Db.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) throw new IllegalStateException("schema.sql not found in resources");
            sql = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}