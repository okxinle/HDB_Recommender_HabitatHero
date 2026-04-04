package habitathero.GeoSpatialAnalysis.src;

import java.sql.ResultSet;
import java.sql.Statement;

public class HDBBuildingSQLCreator extends SQLDbConnect {
    private static HDBBuildingSQLCreator instance;

    // TransportLineMgr singleton call this class constructor only once
    private HDBBuildingSQLCreator() {
        super();
    }

    public static HDBBuildingSQLCreator getInstance() {
        if (instance == null) {
            instance = new HDBBuildingSQLCreator();
        }
        return instance;
    }

    public boolean createSQLTable() {
        String checkSql = "SELECT to_regclass('public.hdb_building_dataset')";
        String createTableSQL = """
                CREATE TABLE IF NOT EXISTS HDB_Building_Dataset (
                    OBJECTID INTEGER PRIMARY KEY,
                    BLK_NO VARCHAR(20),
                    ST_COD VARCHAR(20),
                    ENTITYID INTEGER,
                    POSTAL_COD VARCHAR(20),
                    INC_CRC VARCHAR(50),
                    FMEL_UPD_D VARCHAR(20),
                    SHAPE_AREA DOUBLE PRECISION,
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
                        System.out.println("hdb_building_dataset table exists: " + tableName);
                        stmt.close();
                        super.closeConnection();
                        return true;
                    } else {
                        System.out.println("hdb_building_dataset table does not exist");
                    }
                }
            }

            // Table does not exist, create it
            stmt.executeUpdate(createTableSQL);
            stmt.close();
            super.closeConnection();

            System.out.println("hdb_building_dataset table created successfully.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
