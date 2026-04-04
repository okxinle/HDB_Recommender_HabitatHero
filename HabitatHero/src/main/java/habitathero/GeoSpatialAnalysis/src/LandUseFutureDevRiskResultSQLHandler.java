package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.json.JSONArray;
import org.json.JSONObject;

public class LandUseFutureDevRiskResultSQLHandler extends SQLDbConnect {
    private static LandUseFutureDevRiskResultSQLHandler instance;

    public static void main(String[] args) {
        LandUseFutureDevRiskResultSQLHandler.getInstance().createSQLTable();
    }

    private LandUseFutureDevRiskResultSQLHandler() {
        super();
    }

    public static LandUseFutureDevRiskResultSQLHandler getInstance() {
        if (instance == null) {
            instance = new LandUseFutureDevRiskResultSQLHandler();
        }
        return instance;
    }

    public void createSQLTable() {
        String checkSql = "SELECT to_regclass('public.land_use_future_dev_risk_result')";
        String createTableSQL = """
                CREATE TABLE IF NOT EXISTS land_use_future_dev_risk_result (
                    postal_code TEXT PRIMARY KEY,
                    search_distance DOUBLE PRECISION,
                    latitude DOUBLE PRECISION,
                    longitude DOUBLE PRECISION,
                    development_count INTEGER,
                    status TEXT NOT NULL,
                    message TEXT,
                    future_dev_risk_json JSONB,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                );
                """;

        try {
            super.connectSQL();
            Statement stmt = conn.createStatement();

            try (ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next() && rs.getString(1) != null) {
                    System.out.println("land_use_future_dev_risk_result table already exists");
                    stmt.close();
                    super.closeConnection();
                    return;
                }
            }

            stmt.executeUpdate(createTableSQL);
            stmt.close();
            super.closeConnection();
            System.out.println("land_use_future_dev_risk_result table created successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveFutureDevRiskResult(JSONObject riskResult) {
        System.out.println("Saving future development risk result to cache");
        try {
            super.connectSQL();

            String postalCode = riskResult != null ? riskResult.optString("postalCode", null) : null;
            if (postalCode == null || postalCode.isEmpty()) {
                System.err.println("Error: postal_code is required");
                super.closeConnection();
                return;
            }

            String sql = """
                INSERT INTO land_use_future_dev_risk_result (
                    postal_code, search_distance, latitude, longitude, development_count,
                    status, message, future_dev_risk_json, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)
                ON CONFLICT (postal_code) DO UPDATE SET
                    search_distance = EXCLUDED.search_distance,
                    latitude = EXCLUDED.latitude,
                    longitude = EXCLUDED.longitude,
                    development_count = EXCLUDED.development_count,
                    status = EXCLUDED.status,
                    message = EXCLUDED.message,
                    future_dev_risk_json = EXCLUDED.future_dev_risk_json,
                    updated_at = CURRENT_TIMESTAMP
                """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, postalCode);
            stmt.setObject(2, riskResult != null && riskResult.has("search_distance") ? riskResult.optDouble("search_distance") : null);
            stmt.setObject(3, riskResult != null && riskResult.has("latitude") ? riskResult.optDouble("latitude") : null);
            stmt.setObject(4, riskResult != null && riskResult.has("longitude") ? riskResult.optDouble("longitude") : null);
            stmt.setObject(5, riskResult != null ? Integer.valueOf(getDevelopmentCount(riskResult)) : null);
            stmt.setString(6, riskResult != null ? riskResult.optString("status", "UNKNOWN") : null);
            stmt.setString(7, riskResult != null ? riskResult.optString("message", null) : null);
            stmt.setString(8, riskResult != null ? riskResult.toString() : null);

            stmt.executeUpdate();
            stmt.close();
            super.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JSONObject retrieveFutureDevRiskResult(String postalCode) {
        System.out.println("Retrieving future development risk result from cache");
        JSONObject result = new JSONObject();

        if (postalCode == null || postalCode.isEmpty()) {
            result.put("status", "INVALID_INPUT");
            result.put("message", "postalCode is required");
            return result;
        }

        try {
            super.connectSQL();
            String sql = """
                SELECT postal_code, search_distance, latitude, longitude, development_count,
                       status, message, future_dev_risk_json, created_at, updated_at
                FROM land_use_future_dev_risk_result
                WHERE postal_code = ?
                """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, postalCode);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                putNullable(result, "postalCode", rs.getString("postal_code"));
                putNullable(result, "search_distance", rs.getObject("search_distance"));
                putNullable(result, "latitude", rs.getObject("latitude"));
                putNullable(result, "longitude", rs.getObject("longitude"));
                putNullable(result, "developmentCount", rs.getObject("development_count"));
                putNullable(result, "status", rs.getString("status"));
                putNullable(result, "message", rs.getString("message"));

                String json = rs.getString("future_dev_risk_json");
                if (json != null && !json.isEmpty()) {
                    JSONObject storedJson = new JSONObject(json);
                    for (String key : storedJson.keySet()) {
                        result.put(key, storedJson.get(key));
                    }
                }

                putNullable(result, "createdAt", rs.getTimestamp("created_at"));
                putNullable(result, "updatedAt", rs.getTimestamp("updated_at"));
            } else {
                System.out.println("Future development risk result not found in cache");
                result.put("postalCode", postalCode);
                result.put("status", "NOT_FOUND");
                result.put("message", "Future development risk result not found for postal code");
            }

            rs.close();
            stmt.close();
            super.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private int getDevelopmentCount(JSONObject riskResult) {
        JSONArray developments = riskResult.optJSONArray("developments");
        return developments != null ? developments.length() : 0;
    }

    private void putNullable(JSONObject result, String key, Object value) {
        if (value == null) {
            result.put(key, JSONObject.NULL);
        } else {
            result.put(key, value);
        }
    }
}