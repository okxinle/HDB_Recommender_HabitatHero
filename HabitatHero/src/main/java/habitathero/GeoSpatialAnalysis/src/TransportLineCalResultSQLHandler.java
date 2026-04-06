package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.json.JSONObject;

public class TransportLineCalResultSQLHandler extends SQLDbConnect {
    private static TransportLineCalResultSQLHandler instance;

    public static void main(String[] args) {
        TransportLineCalResultSQLHandler.getInstance().createSQLTable();
    }

    private TransportLineCalResultSQLHandler() {
        super();
    }

    public static TransportLineCalResultSQLHandler getInstance() {
        if (instance == null) {
            instance = new TransportLineCalResultSQLHandler();
        }
        return instance;
    }

    public void createSQLTable() {
        String checkSql = "SELECT to_regclass('public.transport_line_cal_result')";
        String createTableSQL = """
                CREATE TABLE IF NOT EXISTS transport_line_cal_result (
                    postal_code TEXT PRIMARY KEY,
                    object_id INTEGER,
                    rail_type TEXT,
                    distance_meters DOUBLE PRECISION,
                    hdb_latitude DOUBLE PRECISION,
                    hdb_longitude DOUBLE PRECISION,
                    search_radius DOUBLE PRECISION,
                    noise_level_db DOUBLE PRECISION,
                    noise_status TEXT,
                    noise_message TEXT,
                    noise_result_json JSONB,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                );
                """;

        try {
            super.connectSQL();
            Statement stmt = conn.createStatement();

            try (ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next() && rs.getString(1) != null) {
                    System.out.println("transport_line_cal_result table exists: " + rs.getString(1));
                    stmt.close();
                    super.closeConnection();
                    return;
                }
            }

            stmt.executeUpdate(createTableSQL);
            stmt.close();
            super.closeConnection();
            System.out.println("transport_line_cal_result table created successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveTransportLineCalResult(JSONObject noiseResult) {
        System.out.println("Saving noise level result");
        try {
            super.connectSQL();

            String sql = """
                INSERT INTO transport_line_cal_result (
                    postal_code, object_id, rail_type, distance_meters, hdb_latitude,
                    hdb_longitude, search_radius, noise_level_db, noise_status, noise_message,
                    noise_result_json, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)
                ON CONFLICT (postal_code) DO UPDATE SET
                    object_id = COALESCE(EXCLUDED.object_id, transport_line_cal_result.object_id),
                    rail_type = COALESCE(EXCLUDED.rail_type, transport_line_cal_result.rail_type),
                    distance_meters = COALESCE(EXCLUDED.distance_meters, transport_line_cal_result.distance_meters),
                    hdb_latitude = COALESCE(EXCLUDED.hdb_latitude, transport_line_cal_result.hdb_latitude),
                    hdb_longitude = COALESCE(EXCLUDED.hdb_longitude, transport_line_cal_result.hdb_longitude),
                    search_radius = COALESCE(EXCLUDED.search_radius, transport_line_cal_result.search_radius),
                    noise_level_db = COALESCE(EXCLUDED.noise_level_db, transport_line_cal_result.noise_level_db),
                    noise_status = COALESCE(EXCLUDED.noise_status, transport_line_cal_result.noise_status),
                    noise_message = COALESCE(EXCLUDED.noise_message, transport_line_cal_result.noise_message),
                    noise_result_json = COALESCE(EXCLUDED.noise_result_json, transport_line_cal_result.noise_result_json),
                    updated_at = CURRENT_TIMESTAMP
                """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            String postalCode = noiseResult != null ? noiseResult.optString("postalCode", null) : null;
            if (postalCode == null || postalCode.isEmpty()) {
                stmt.close();
                super.closeConnection();
                return;
            }

            stmt.setString(1, postalCode);
            stmt.setObject(2, noiseResult != null && noiseResult.has("objectId") ? noiseResult.optInt("objectId") : null);
            stmt.setString(3, noiseResult != null ? noiseResult.optString("rail_type", null) : null);
            stmt.setObject(4, noiseResult != null && noiseResult.has("distance_meters") ? noiseResult.optDouble("distance_meters") : null);
            stmt.setObject(5, noiseResult != null && noiseResult.has("hdb_latitude") ? noiseResult.optDouble("hdb_latitude") : null);
            stmt.setObject(6, noiseResult != null && noiseResult.has("hdb_longitude") ? noiseResult.optDouble("hdb_longitude") : null);
            stmt.setObject(7, noiseResult != null && noiseResult.has("search_radius") ? noiseResult.optDouble("search_radius") : null);
            stmt.setObject(8, noiseResult != null && noiseResult.has("noise_level_db") ? noiseResult.optDouble("noise_level_db") : null);
            stmt.setString(9, noiseResult != null ? noiseResult.optString("status", "OK") : null);
            stmt.setString(10, noiseResult != null ? noiseResult.optString("message", null) : null);
            stmt.setString(11, noiseResult != null ? noiseResult.toString() : null);

            stmt.executeUpdate();
            stmt.close();
            super.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject retrieveTransportLineCalResult(String postalCode) {
        System.out.println("Retrieving noise level result");
        JSONObject result = new JSONObject();

        if (postalCode == null || postalCode.isEmpty()) {
            result.put("status", "ERROR");
            result.put("message", "postalCode is required");
            return result;
        }

        try {
            super.connectSQL();

            String sql = """
                  SELECT postal_code, object_id, rail_type,
                     distance_meters, hdb_latitude, hdb_longitude, search_radius,
                      noise_level_db, noise_status, noise_message, noise_result_json,
                       created_at, updated_at
                FROM transport_line_cal_result
                WHERE postal_code = ?
                """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, postalCode);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                putNullable(result, "postalCode", rs.getString("postal_code"));
                putNullable(result, "objectId", rs.getObject("object_id"));
                putNullable(result, "rail_type", rs.getString("rail_type"));
                putNullable(result, "distance_meters", rs.getObject("distance_meters"));
                putNullable(result, "hdb_latitude", rs.getObject("hdb_latitude"));
                putNullable(result, "hdb_longitude", rs.getObject("hdb_longitude"));
                putNullable(result, "search_radius", rs.getObject("search_radius"));
                putNullable(result, "noise_level_db", rs.getObject("noise_level_db"));
                putNullable(result, "noiseStatus", rs.getString("noise_status"));
                putNullable(result, "noiseMessage", rs.getString("noise_message"));
                putNullable(result, "noiseResultJson", rs.getString("noise_result_json"));
                putNullable(result, "createdAt", rs.getTimestamp("created_at"));
                putNullable(result, "updatedAt", rs.getTimestamp("updated_at"));

                String storedStatus = rs.getString("noise_status");
                String storedMessage = rs.getString("noise_message");
                if ("OK".equalsIgnoreCase(storedStatus)) {
                    result.put("status", "OK");
                    result.put("message", "NIL");
                } else {
                    result.put("status", "ERROR");
                    result.put("message", (storedMessage == null || storedMessage.isEmpty())
                            ? "Transport line calculation unavailable"
                            : storedMessage);
                }
            } else {
                System.out.println("Noise result not found");
                result.put("postalCode", postalCode);
                result.put("status", "ERROR");
                result.put("message", "Transport line calculation result not found for postal code");
            }

            rs.close();
            stmt.close();
            super.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private void putNullable(JSONObject result, String key, Object value) {
        if (value == null) {
            result.put(key, JSONObject.NULL);
        } else {
            result.put(key, value);
        }
    }
}