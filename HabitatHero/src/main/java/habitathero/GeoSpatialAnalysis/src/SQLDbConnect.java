package habitathero.GeoSpatialAnalysis.src;

import java.sql.Connection;
import java.sql.DriverManager;

//Super class generalizing all SQL connections establishment and closing

public class SQLDbConnect {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/Habitat_hero";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "123";
    protected static Connection conn;

    public SQLDbConnect() {
        conn = null;
    }

    public void connectSQL() {
        conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

