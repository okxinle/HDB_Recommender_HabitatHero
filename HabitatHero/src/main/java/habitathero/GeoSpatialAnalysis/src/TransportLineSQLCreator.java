package habitathero.GeoSpatialAnalysis.src;

import java.sql.ResultSet;
import java.sql.Statement;

public class TransportLineSQLCreator extends SQLDbConnect {
    private static TransportLineSQLCreator instance;

    // TransportLineMgr singleton call this class constructor only once
    private TransportLineSQLCreator() {
        super();
    }

    public static TransportLineSQLCreator getInstance(){
        if(instance == null){
            instance = new TransportLineSQLCreator();
        }
        return instance;
    }

    public boolean createSQLTable() {
        String checkSql = "SELECT to_regclass('public.Transport_Line_Dataset')";
        String createExtension = "CREATE EXTENSION IF NOT EXISTS postgis;";
        String createTableSQL = """
                CREATE TABLE IF NOT EXISTS Transport_Line_Dataset (
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
                        System.out.println("transport_line_dataset table exists: " + tableName);
                        stmt.close();
                        super.closeConnection();
                        return true;
                    } else {
                        System.out.println("transport_line_dataset table does not exist");
                    }
                }
            }

            // Table does not exist, create it
            stmt.executeUpdate(createExtension);
            stmt.executeUpdate(createTableSQL);
            stmt.close();
            super.closeConnection();

            System.out.println("transport_line_dataset table created successfully.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
