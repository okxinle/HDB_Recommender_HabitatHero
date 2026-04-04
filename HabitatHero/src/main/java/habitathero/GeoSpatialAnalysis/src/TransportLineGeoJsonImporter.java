package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.io.FileReader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class TransportLineGeoJsonImporter extends SQLDbConnect {
    private static TransportLineGeoJsonImporter instance;

    // TransportLineMgr singleton call this class constructor only once
    private TransportLineGeoJsonImporter() {
        super();
    }

    public static TransportLineGeoJsonImporter getInstance(){
        if (instance == null){
            instance = new TransportLineGeoJsonImporter();
        }
        return instance;
    }

    public boolean importGeoJsonToSQLDb(String transportLineGeoJsonPath) {
        try {
            // connect to postgres api to access Database
            super.connectSQL();

            // Tokenize geojson file for efficient file streaming and reading
            JSONTokener tokener = new JSONTokener(new FileReader(transportLineGeoJsonPath));
            JSONObject geojson = new JSONObject(tokener);
            JSONArray features = geojson.getJSONArray("features");

            // format for SQL command to create/update row entries from geojson file
            String sql = "INSERT INTO Transport_Line_Dataset " +
                    "(objectid, grnd_level, rail_type, inc_crc, fmel_upd_d, shape_len, geom) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ST_SetSRID(ST_GeomFromGeoJSON(?),4326)) " +
                    "ON CONFLICT (objectid) DO UPDATE SET " +
                    "grnd_level = EXCLUDED.grnd_level, " +
                    "rail_type = EXCLUDED.rail_type, " +
                    "inc_crc = EXCLUDED.inc_crc, " +
                    "fmel_upd_d = EXCLUDED.fmel_upd_d, " +
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
                String groundLevel = properties.getString("GRND_LEVEL");
                String railType = properties.getString("RAIL_TYPE");
                String crc = properties.getString("INC_CRC");
                String updateDate = properties.getString("FMEL_UPD_D");
                double shapeLen = properties.getDouble("SHAPE.LEN");

                stmt.setInt(1, objectId);
                stmt.setString(2, groundLevel);
                stmt.setString(3, railType);
                stmt.setString(4, crc);
                stmt.setString(5, updateDate);
                stmt.setDouble(6, shapeLen);
                stmt.setString(7, geometry.toString());

                stmt.addBatch();
            }

            stmt.executeBatch();
            stmt.close();
            super.closeConnection();

            System.out.println("Rail GeoJSON successfully imported.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
