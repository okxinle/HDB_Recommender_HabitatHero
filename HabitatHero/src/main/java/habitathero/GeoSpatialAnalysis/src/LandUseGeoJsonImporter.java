package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.io.FileReader;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class LandUseGeoJsonImporter extends SQLDbConnect {
    private static LandUseGeoJsonImporter instance;
    private volatile String lastErrorMessage = "";
    private volatile int lastImportedCount = 0;
    private volatile int lastSkippedCount = 0;
    private volatile int lastTotalFeatures = 0;

    // SymbolLineMgr singleton call this class constructor only once
    private LandUseGeoJsonImporter() {
        super();
    }

    public static LandUseGeoJsonImporter getInstance() {
        if (instance == null) {
            instance = new LandUseGeoJsonImporter();
        }
        return instance;
    }

    public boolean importGeoJsonToSQLDb(String landUseGeoJsonDbPath) {
        try {
            lastErrorMessage = "";
            lastImportedCount = 0;
            lastSkippedCount = 0;
            lastTotalFeatures = 0;

            // connect to postgres api to access Database
            super.connectSQL();
            if (conn == null) {
                throw new IllegalStateException("PostgreSQL connection is not initialized.");
            }

            // Tokenize geojson file for efficient file streaming and reading
            JSONTokener tokener = new JSONTokener(new FileReader(landUseGeoJsonDbPath));
            JSONObject geojson = new JSONObject(tokener);
            JSONArray features = geojson.getJSONArray("features");
            lastTotalFeatures = features.length();

            // format for SQL command to create/update row entries from geojson file
            String sql = """
                INSERT INTO Land_Use_Dataset (
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
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // extract every geojson entry and place into sql querying syntax through
                // PreparedStatement
                for (int i = 0; i < features.length(); i++) {
                    JSONObject feature = features.getJSONObject(i);
                    JSONObject properties = feature.optJSONObject("properties");
                    JSONObject geometry = feature.optJSONObject("geometry");

                    if (properties == null || geometry == null) {
                        lastSkippedCount++;
                        continue;
                    }

                    if (!properties.has("OBJECTID")) {
                        lastSkippedCount++;
                        continue;
                    }

                    int objectId = properties.optInt("OBJECTID", -1);
                    if (objectId < 0) {
                        lastSkippedCount++;
                        continue;
                    }

                    String luDesc = properties.optString("LU_DESC", null);
                    String luText = properties.optString("LU_TEXT", null);
                    String gpr = properties.optString("GPR", null);
                    String whiQMx = properties.optString("WHI_Q_MX", null);
                    String gprBMn = properties.optString("GPR_B_MN", null);
                    String incCrc = properties.optString("INC_CRC", null);
                    String fmelUpd = properties.optString("FMEL_UPD_D", null);

                    Double shapeArea = properties.optDouble("SHAPE.AREA", Double.NaN);
                    Double shapeLen = properties.optDouble("SHAPE.LEN", Double.NaN);

                    try {
                        stmt.setInt(1, objectId);
                        stmt.setString(2, luDesc);
                        stmt.setString(3, luText);
                        stmt.setString(4, gpr);
                        stmt.setString(5, whiQMx);
                        stmt.setString(6, gprBMn);
                        stmt.setString(7, incCrc);
                        stmt.setString(8, fmelUpd);

                        if (shapeArea == null || Double.isNaN(shapeArea)) {
                            stmt.setNull(9, Types.DOUBLE);
                        } else {
                            stmt.setDouble(9, shapeArea);
                        }

                        if (shapeLen == null || Double.isNaN(shapeLen)) {
                            stmt.setNull(10, Types.DOUBLE);
                        } else {
                            stmt.setDouble(10, shapeLen);
                        }

                        stmt.setString(11, geometry.toString());
                        stmt.executeUpdate();
                        lastImportedCount++;
                    } catch (Exception rowError) {
                        lastSkippedCount++;
                        if (lastErrorMessage.isEmpty()) {
                            lastErrorMessage = "First row error at index " + i + " (OBJECTID=" + objectId + "): "
                                    + rowError.getMessage();
                        }
                    }
                }
            } finally {
                super.closeConnection();
            }

            if (lastImportedCount == 0) {
                if (lastErrorMessage.isEmpty()) {
                    lastErrorMessage = "Import completed with zero inserted rows.";
                }
                return false;
            }

            System.out.println("Land Use GeoJSON import complete. Imported=" + lastImportedCount
                    + ", Skipped=" + lastSkippedCount + ", Total=" + lastTotalFeatures);
            return true;

        } catch (Exception e) {
            lastErrorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public int getLastImportedCount() {
        return lastImportedCount;
    }

    public int getLastSkippedCount() {
        return lastSkippedCount;
    }

    public int getLastTotalFeatures() {
        return lastTotalFeatures;
    }
}
