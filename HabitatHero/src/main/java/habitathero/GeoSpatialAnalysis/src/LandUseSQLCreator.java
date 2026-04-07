package habitathero.GeoSpatialAnalysis.src;

import java.sql.ResultSet;
import java.sql.Statement;

public class LandUseSQLCreator extends SQLDbConnect {
    private static LandUseSQLCreator instance;

    // SymbolLineMgr singleton call this class constructor only once
    private LandUseSQLCreator() {
        super();
    }

    public static LandUseSQLCreator getInstance() {
        if (instance == null) {
            instance = new LandUseSQLCreator();
        }
        return instance;
    }

    public boolean createSQLTable() {

        String checkSql = "SELECT to_regclass('public.land_use_dataset')";
    
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS Land_Use_Dataset (
                objectid INTEGER PRIMARY KEY,
                lu_desc TEXT,
                lu_text TEXT,
                gpr TEXT,
                whi_q_mx TEXT,
                gpr_b_mn TEXT,
                inc_crc TEXT,
                fmel_upd_d TEXT,
                shape_area DOUBLE PRECISION,
                shape_len DOUBLE PRECISION,
                geom GEOMETRY(Geometry, 4326)
            );
        """;
    
        try {
            super.connectSQL();
            if (conn == null) {
                throw new IllegalStateException("PostgreSQL connection is not initialized.");
            }
            Statement stmt = conn.createStatement();
    
            // Check if table exists
            try (ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next()) {
                    String tableName = rs.getString(1);
                    if (tableName != null) {
                        System.out.println("land_use_dataset table exists: " + tableName);
                        stmt.close();
                        super.closeConnection();
                        return true;
                    } else {
                        System.out.println("land_use_dataset table does not exist");
                    }
                }
            }
    
            // Create table
            stmt.executeUpdate(createTableSQL);
    
            stmt.close();
            super.closeConnection();
    
            System.out.println("land_use_dataset table created successfully.");
            return true;
    
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
