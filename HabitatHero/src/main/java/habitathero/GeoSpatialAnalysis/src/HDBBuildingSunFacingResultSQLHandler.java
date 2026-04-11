package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.json.JSONObject;
public class HDBBuildingSunFacingResultSQLHandler extends SQLDbConnect{
    private static HDBBuildingSunFacingResultSQLHandler instance;

    private HDBBuildingSunFacingResultSQLHandler() {
        super();
    }

    public static HDBBuildingSunFacingResultSQLHandler getInstance() {
        if (instance == null) {
            instance = new HDBBuildingSunFacingResultSQLHandler();
        }
        return instance;
    }

    public boolean createSQLTable() {
        String checkSql = "SELECT to_regclass('public.sun_facing_analysis_result')";
        String createTableSQL = """
                    CREATE TABLE IF NOT EXISTS sun_facing_analysis_result (
                        postal_code TEXT PRIMARY KEY,
                        status TEXT NOT NULL,
                        message TEXT,
                        perimeter DOUBLE PRECISION,
                        east_azimuth DOUBLE PRECISION,
                        west_azimuth DOUBLE PRECISION,
                        east_score DOUBLE PRECISION,
                        west_score DOUBLE PRECISION,
                        east_ratio DOUBLE PRECISION,
                        west_ratio DOUBLE PRECISION,
                        dominant TEXT,
                        sunlight_index DOUBLE PRECISION,
                        sunlight_average DOUBLE PRECISION,
                        sunlight_steps INTEGER,
                        absolute_min_score DOUBLE PRECISION,
                        absolute_max_score DOUBLE PRECISION,
                        east_score_relative_exposure_pct DOUBLE PRECISION,
                        west_score_relative_exposure_pct DOUBLE PRECISION,
                        sunlight_index_relative_exposure_pct DOUBLE PRECISION,
                        analysis_json JSONB,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                    );
                """;

        try {
            super.connectSQL();
            Statement stmt = conn.createStatement();

            try (ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next() && rs.getString(1) != null) {
                    System.out.println("sun_facing_analysis_result table exists: " + rs.getString(1));
                    stmt.close();
                    super.closeConnection();
                    return true;
                }
            }

            stmt.executeUpdate(createTableSQL);
            stmt.close();
            super.closeConnection();
            System.out.println("sun_facing_analysis_result table created successfully.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Save sun facing analysis results from calSunFacing output
    public void saveSunFacingAnalysis(JSONObject analysisResult) {
        System.out.println("Saving sun-facing result");
        try {
            super.connectSQL();

            String postalCode = analysisResult.optString("postalCode", null);
            if (postalCode == null || postalCode.isEmpty()) {
                System.err.println("Error: postal_code is required");
                super.closeConnection();
                return;
            }

            String sql = """
                INSERT INTO sun_facing_analysis_result (
                    postal_code, status, message, perimeter, east_azimuth, west_azimuth,
                    east_score, west_score, east_ratio, west_ratio, dominant,
                    sunlight_index, sunlight_average, sunlight_steps,
                    absolute_min_score, absolute_max_score,
                    east_score_relative_exposure_pct, west_score_relative_exposure_pct,
                    sunlight_index_relative_exposure_pct, analysis_json, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, CURRENT_TIMESTAMP)
                ON CONFLICT (postal_code) DO UPDATE SET
                    status = EXCLUDED.status,
                    message = EXCLUDED.message,
                    perimeter = EXCLUDED.perimeter,
                    east_azimuth = EXCLUDED.east_azimuth,
                    west_azimuth = EXCLUDED.west_azimuth,
                    east_score = EXCLUDED.east_score,
                    west_score = EXCLUDED.west_score,
                    east_ratio = EXCLUDED.east_ratio,
                    west_ratio = EXCLUDED.west_ratio,
                    dominant = EXCLUDED.dominant,
                    sunlight_index = EXCLUDED.sunlight_index,
                    sunlight_average = EXCLUDED.sunlight_average,
                    sunlight_steps = EXCLUDED.sunlight_steps,
                    absolute_min_score = EXCLUDED.absolute_min_score,
                    absolute_max_score = EXCLUDED.absolute_max_score,
                    east_score_relative_exposure_pct = EXCLUDED.east_score_relative_exposure_pct,
                    west_score_relative_exposure_pct = EXCLUDED.west_score_relative_exposure_pct,
                    sunlight_index_relative_exposure_pct = EXCLUDED.sunlight_index_relative_exposure_pct,
                    analysis_json = EXCLUDED.analysis_json,
                    updated_at = CURRENT_TIMESTAMP
                """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, postalCode);
            stmt.setString(2, analysisResult.optString("status", "UNKNOWN"));
            stmt.setString(3, analysisResult.optString("message", null));
            stmt.setDouble(4, analysisResult.optDouble("perimeter", 0.0));
            stmt.setDouble(5, analysisResult.optDouble("eastAzimuth", 0.0));
            stmt.setDouble(6, analysisResult.optDouble("westAzimuth", 0.0));
            stmt.setDouble(7, analysisResult.optDouble("eastScore", 0.0));
            stmt.setDouble(8, analysisResult.optDouble("westScore", 0.0));
            stmt.setDouble(9, analysisResult.optDouble("eastRatio", 0.0));
            stmt.setDouble(10, analysisResult.optDouble("westRatio", 0.0));
            stmt.setString(11, analysisResult.optString("dominant", null));
            stmt.setDouble(12, analysisResult.optDouble("sunlightIndex", 0.0));
            stmt.setDouble(13, analysisResult.optDouble("sunlightAverage", 0.0));
            stmt.setInt(14, analysisResult.optInt("sunlightSteps", 0));
            stmt.setDouble(15, analysisResult.optDouble("absoluteMinScore", 0.0));
            stmt.setDouble(16, analysisResult.optDouble("absoluteMaxScore", 0.0));
            stmt.setDouble(17, analysisResult.optDouble("eastScoreRelativeExposurePct", 0.0));
            stmt.setDouble(18, analysisResult.optDouble("westScoreRelativeExposurePct", 0.0));
            stmt.setDouble(19, analysisResult.optDouble("sunlightIndexRelativeExposurePct", 0.0));
            stmt.setString(20, analysisResult.toString());

            stmt.executeUpdate();
            stmt.close();
            super.closeConnection();

            System.out.println("Sun facing analysis for postal code " + postalCode + " saved successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Retrieve sun facing analysis results by postal code (returns most recent)
    public JSONObject retrieveSunFacingAnalysis(String postalCode) {
        System.out.println("Retrieving sun-facing result");
        JSONObject result = new JSONObject();

        if (postalCode == null || postalCode.isEmpty()) {
            result.put("status", "ERROR");
            result.put("message", "postalCode is required");
            return result;
        }

        try {
            super.connectSQL();
            String sql = """
                SELECT postal_code, status, message, perimeter, east_azimuth, west_azimuth,
                       east_score, west_score, east_ratio, west_ratio, dominant,
                       sunlight_index, sunlight_average, sunlight_steps,
                       absolute_min_score, absolute_max_score,
                       east_score_relative_exposure_pct, west_score_relative_exposure_pct,
                       sunlight_index_relative_exposure_pct, analysis_json, created_at
                FROM sun_facing_analysis_result
                WHERE postal_code = ?
                ORDER BY created_at DESC
                LIMIT 1
                """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, postalCode);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                result.put("postalCode", rs.getString("postal_code"));
                String storedStatus = rs.getString("status");
                String storedMessage = rs.getString("message");
                if ("OK".equalsIgnoreCase(storedStatus)) {
                    result.put("status", "OK");
                    result.put("message", "NIL");
                } else {
                    result.put("status", "ERROR");
                    result.put("message", (storedMessage == null || storedMessage.isEmpty())
                            ? "Sun facing analysis unavailable"
                            : storedMessage);
                }
                result.put("perimeter", rs.getDouble("perimeter"));
                result.put("eastAzimuth", rs.getDouble("east_azimuth"));
                result.put("westAzimuth", rs.getDouble("west_azimuth"));
                result.put("eastScore", rs.getDouble("east_score"));
                result.put("westScore", rs.getDouble("west_score"));
                result.put("eastRatio", rs.getDouble("east_ratio"));
                result.put("westRatio", rs.getDouble("west_ratio"));
                result.put("dominant", rs.getString("dominant"));
                result.put("sunlightIndex", rs.getDouble("sunlight_index"));
                result.put("sunlightAverage", rs.getDouble("sunlight_average"));
                result.put("sunlightSteps", rs.getInt("sunlight_steps"));
                result.put("absoluteMinScore", rs.getDouble("absolute_min_score"));
                result.put("absoluteMaxScore", rs.getDouble("absolute_max_score"));
                result.put("eastScoreRelativeExposurePct", rs.getDouble("east_score_relative_exposure_pct"));
                result.put("westScoreRelativeExposurePct", rs.getDouble("west_score_relative_exposure_pct"));
                result.put("sunlightIndexRelativeExposurePct", rs.getDouble("sunlight_index_relative_exposure_pct"));
                result.put("createdAt", rs.getTimestamp("created_at"));
            } else {
                System.out.println("Sun-facing result not found");
                result.put("postalCode", postalCode);
                result.put("status", "ERROR");
                result.put("message", "Sun facing analysis result not found for postal code");
            }

            rs.close();
            stmt.close();
            super.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
