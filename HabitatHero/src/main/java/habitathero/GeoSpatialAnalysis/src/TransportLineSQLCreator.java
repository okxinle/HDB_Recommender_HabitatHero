package habitathero.GeoSpatialAnalysis.src;

import java.sql.ResultSet;
import java.sql.Statement;

public class TransportLineSQLCreator extends SQLDbConnect {

    // TransportLineMgr singleton call this class constructor only once
    public TransportLineSQLCreator() {
        super();
    }

    public void createSQLTable() {
        String checkSql = "SELECT to_regclass('public.Transport_Line')";
        String createTableSQL = """
                CREATE TABLE IF NOT EXISTS Transport_Line (
                    OBJECTID INTEGER PRIMARY KEY,
                    GRND_LEVEL VARCHAR(50),
                    RAIL_TYPE VARCHAR(50),
                    INC_CRC VARCHAR(50),
                    FMEL_UPD_D VARCHAR(20),
                    SHAPE_LEN DOUBLE PRECISION,
                    geom GEOMETRY(Geometry, 4326)
                );
                """;

        try {
            super.connectSQL();
            Statement stmt = conn.createStatement();

            // Check if table exists
            try (ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next()) {
                    String tableName = rs.getString(1); // get first column
                    if (tableName != null) {
                        System.out.println("transport_line table exists: " + tableName);
                        return;
                    } else {
                        System.out.println("transport_line table does not exist");
                    }
                }
            }

            // Table does not exist, create it
            stmt.executeUpdate(createTableSQL);
            stmt.close();
            super.closeConnection();

            System.out.println("transport_line table created successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
