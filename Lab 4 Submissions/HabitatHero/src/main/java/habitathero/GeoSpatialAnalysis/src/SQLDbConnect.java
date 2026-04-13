package habitathero.GeoSpatialAnalysis.src;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

//Super class generalizing all SQL connections establishment and closing

public class SQLDbConnect {
    private static final Properties ENV_FILE = loadDotEnv();
    protected static Connection conn;

    public SQLDbConnect() {
        // Keep constructor side-effect free. Resetting a shared static connection here
        // can null-out an active connection used by another singleton.
    }

    public void connectSQL() {
        conn = null;
        try {
            String dbHost = getConfig("DB_HOST", "localhost");
            String dbPort = getConfig("DB_PORT", "5432");
            String dbName = getConfig("DB_NAME", "habitathero_db");
            String dbUser = getConfig("DB_USERNAME", "postgres");
            String dbPassword = getConfig("DB_PASSWORD", "");

            String dbUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;

            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (Exception e) {
            conn = null;
            System.err.println("Failed to connect to PostgreSQL at " +
                getConfig("DB_HOST", "localhost") + ":" + getConfig("DB_PORT", "5432") +
                "/" + getConfig("DB_NAME", "habitathero_db") +
                " as user '" + getConfig("DB_USERNAME", "postgres") + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getConfig(String key, String defaultValue) {
        String valueFromEnv = System.getenv(key);
        if (valueFromEnv != null && !valueFromEnv.isBlank()) {
            return valueFromEnv.trim();
        }

        String valueFromProperties = System.getProperty(key);
        if (valueFromProperties != null && !valueFromProperties.isBlank()) {
            return valueFromProperties.trim();
        }

        String valueFromDotEnv = ENV_FILE.getProperty(key);
        if (valueFromDotEnv != null && !valueFromDotEnv.isBlank()) {
            return valueFromDotEnv.trim();
        }

        return defaultValue;
    }

    private static Properties loadDotEnv() {
        Properties props = new Properties();

        Path workingDirEnv = Path.of(System.getProperty("user.dir"), ".env");
        if (Files.exists(workingDirEnv)) {
            try (InputStream stream = Files.newInputStream(workingDirEnv)) {
                props.load(stream);
                return props;
            } catch (IOException ignored) {
                // Fall back to classpath lookup below.
            }
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (InputStream stream = classLoader.getResourceAsStream(".env")) {
            if (stream != null) {
                props.load(stream);
            }
        } catch (IOException ignored) {
            // Best-effort only. Environment variables remain the primary source.
        }

        return props;
    }

}
