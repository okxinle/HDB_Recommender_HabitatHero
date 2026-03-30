import java.sql.PreparedStatement;
import java.io.FileReader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class LandUseGeoJsonImporter extends SQLDbConnect {

    // SymbolLineMgr singleton call this class constructor only once
    public LandUseGeoJsonImporter() {
        super();
    }

    public void importGeoJsonToSQLDb(String landUseGeoJsonDbPath) {
        try {
            // connect to postgres api to access Database
            super.connectSQL();

            // Tokenize geojson file for efficient file streaming and reading
            JSONTokener tokener = new JSONTokener(new FileReader(landUseGeoJsonDbPath));
            JSONObject geojson = new JSONObject(tokener);
            JSONArray features = geojson.getJSONArray("features");

            // format for SQL command to create/update row entries from geojson file
            String sql = """
                INSERT INTO Land_Use (
                    objectid,
                    lu_desc,
                    lu_text,
                    gpr,
                    whi_q_mx,
                    gpr_b_mn,
                    inc_crc,
                    fmel_upd_d,
                    shape_area,
                    shape_len,
                    geom
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 
                    ST_SetSRID(ST_GeomFromGeoJSON(?),4326)
                )
                ON CONFLICT (objectid) DO UPDATE SET
                    lu_desc = EXCLUDED.lu_desc,
                    lu_text = EXCLUDED.lu_text,
                    gpr = EXCLUDED.gpr,
                    whi_q_mx = EXCLUDED.whi_q_mx,
                    gpr_b_mn = EXCLUDED.gpr_b_mn,
                    inc_crc = EXCLUDED.inc_crc,
                    fmel_upd_d = EXCLUDED.fmel_upd_d,
                    shape_area = EXCLUDED.shape_area,
                    shape_len = EXCLUDED.shape_len,
                    geom = EXCLUDED.geom
            """;
            
            PreparedStatement stmt = conn.prepareStatement(sql);

            // extract every geojson entry and place into sql querying syntax through
            // PreparedStatement
            for (int i = 0; i < features.length(); i++) {

                JSONObject feature = features.getJSONObject(i);
                JSONObject properties = feature.getJSONObject("properties");
                JSONObject geometry = feature.getJSONObject("geometry");
    
                int objectId = properties.getInt("OBJECTID");
    
                String luDesc = properties.optString("LU_DESC", null);
                String luText = properties.optString("LU_TEXT", null);
                String gpr = properties.optString("GPR", null);
                String whiQMx = properties.optString("WHI_Q_MX", null);
                String gprBMn = properties.optString("GPR_B_MN", null);
                String incCrc = properties.optString("INC_CRC", null);
                String fmelUpd = properties.optString("FMEL_UPD_D", null);
    
                Double shapeArea = properties.getDouble("SHAPE.AREA");
                Double shapeLen = properties.getDouble("SHAPE.LEN");
    
                stmt.setInt(1, objectId);
                stmt.setString(2, luDesc);
                stmt.setString(3, luText);
                stmt.setString(4, gpr);
                stmt.setString(5, whiQMx);
                stmt.setString(6, gprBMn);
                stmt.setString(7, incCrc);
                stmt.setString(8, fmelUpd);
                stmt.setDouble(9, shapeArea);
                stmt.setDouble(10, shapeLen);

                stmt.setString(11, geometry.toString());
    
                stmt.addBatch();
            }

            stmt.executeBatch();
            stmt.close();
            super.closeConnection();

            System.out.println("Land Use GeoJSON successfully imported");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
