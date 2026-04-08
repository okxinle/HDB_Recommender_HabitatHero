package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.io.FileReader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class HDBBuildingGeoJsonImporter extends SQLDbConnect {
    private static HDBBuildingGeoJsonImporter instance;

    // HDBBuildingMgr singleton call this class constructor only once
    private HDBBuildingGeoJsonImporter() {
        super();
    }

    public static HDBBuildingGeoJsonImporter getInstance() {
        if (instance == null) {
            instance = new HDBBuildingGeoJsonImporter();
        }
        return instance;
    }

    public boolean importGeoJsonToSQLDb(String HDBBuildingGeoJsonDbPath) {
        try {
            // connect to postgres api to access Database
            super.connectSQL();

            // Tokenize geojson file for efficient file streaming and reading
            JSONTokener tokener = new JSONTokener(new FileReader(HDBBuildingGeoJsonDbPath));
            JSONObject geojson = new JSONObject(tokener);
            JSONArray features = geojson.getJSONArray("features");

            // format for SQL command to create row entires from geojson file
            String sql = "INSERT INTO hdb_blocks " +
            "(objectid, blk_no, st_cod, entityid, postal_cod, inc_crc, fmel_upd_d, shape_area, shape_len, geom) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ST_SetSRID(ST_GeomFromGeoJSON(?),4326)) " +
            "ON CONFLICT (objectid) DO UPDATE SET " +
            "blk_no = EXCLUDED.blk_no, " +
            "st_cod = EXCLUDED.st_cod, " +
            "entityid = EXCLUDED.entityid, " +
            "postal_cod = EXCLUDED.postal_cod, " +
            "inc_crc = EXCLUDED.inc_crc, " +
            "fmel_upd_d = EXCLUDED.fmel_upd_d, " +
            "shape_area = EXCLUDED.shape_area, " +
            "shape_len = EXCLUDED.shape_len, " +
            "geom = EXCLUDED.geom";

            PreparedStatement stmt = conn.prepareStatement(sql);

            // extract every geojson entry and place into sql querying syntax through
            // PreparedStatement
            for (int i = 0; i < features.length(); i++) {

                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");
                JSONObject geometry = feature.getJSONObject("geometry");

                int objectId = properties.getInt("OBJECTID");
                String blk_no = properties.getString("BLK_NO");
                String st_cod = properties.getString("ST_COD");
                int entityId = properties.getInt("ENTITYID");
                String postal_cod = properties.getString("POSTAL_COD");
                String inc_crc = properties.getString("INC_CRC");
                String fmel_upd_d = properties.getString("FMEL_UPD_D");
                double shape_area = properties.getDouble("SHAPE.AREA");
                double shape_len = properties.getDouble("SHAPE.LEN");

                stmt.setInt(1, objectId);
                stmt.setString(2, blk_no);
                stmt.setString(3, st_cod);
                stmt.setInt(4, entityId);
                stmt.setString(5, postal_cod);
                stmt.setString(6, inc_crc);
                stmt.setString(7, fmel_upd_d);
                stmt.setDouble(8, shape_area);
                stmt.setDouble(9, shape_len);
                stmt.setString(10, geometry.toString());

                stmt.addBatch();
            }

            stmt.executeBatch();
            stmt.close();
            super.closeConnection();

            System.out.println("HDBBuilding GeoJSON successfully imported.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
