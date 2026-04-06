package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.JSONArray;
import org.json.JSONObject;

public class LandUseCheckDevelopment extends SQLDbConnect {
    private static LandUseCheckDevelopment instance;
    private static final double DEFAULT_DISTANCE = 100;

    // SymbolLineMgr singleton call this class constructor only once
    private LandUseCheckDevelopment() {
        super();
    }

    public static LandUseCheckDevelopment getInstance() {
        if (instance == null) {
            instance = new LandUseCheckDevelopment();
        }
        return instance;
    }

    public JSONObject calFutureDevRisk(String postalCode) {
        return calFutureDevRisk(postalCode, DEFAULT_DISTANCE);
    }

    public JSONObject calFutureDevRisk(String postalCode, double distance) {
        System.out.println("Calculating future development risk for postal code " + postalCode + " with distance " + distance);
        Coordinate coords = HDBBuildingMgr.getInstance().postalCodeToCoordinate(postalCode);
        if (coords == null || (coords.getLatitude() == -1 && coords.getLongitude() == -1)) {
            return invalidPostalCodeResult(postalCode, distance);
        }

        return calFutureDevRisk(postalCode, coords, distance);
    }

    private JSONObject calFutureDevRisk(String postalCode, Coordinate coords, double distance) {
        double longitude = coords.getLongitude();
        double latitude = coords.getLatitude();
        JSONObject result = new JSONObject();
        JSONArray developments = new JSONArray();

        String sql = """
                    SELECT
                        OBJECTID,
                        GPR,
                        ST_Distance(
                            ST_Centroid(geom)::geography,
                            ref.ref_point
                        ) AS distance_meters,
                        ST_AsGeoJSON(geom) AS geojson_geom
                    FROM land_use_dataset,
                    (
                        SELECT
                            ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography AS ref_point,
                            ST_Buffer(ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?) AS buffer_geom
                    ) AS ref
                    WHERE
                        GPR ILIKE '%SDP%'
                        AND (
                            ST_Intersects(geom, ref.buffer_geom::geometry)
                            OR ST_DWithin(geom::geography, ref.ref_point, ?)
                        )
                    ORDER BY distance_meters
                """;

        try {
            super.connectSQL();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setDouble(1, longitude);
            pstmt.setDouble(2, latitude);
            pstmt.setDouble(3, longitude);
            pstmt.setDouble(4, latitude);
            pstmt.setDouble(5, distance);
            pstmt.setDouble(6, distance);

            ResultSet rs = pstmt.executeQuery();

            System.out.printf("Future developments within distance: %.2f m%n", distance);

            while (rs.next()) {
                int objectId = rs.getInt("OBJECTID");
                String gpr = rs.getString("GPR");
                double cal_distance = rs.getDouble("distance_meters");
                String geojson_geom = rs.getString("geojson_geom");

                JSONObject development = new JSONObject();
                development.put("objectId", objectId);
                development.put("gpr", gpr);
                development.put("distance_meters", cal_distance);
                development.put("geom", geojson_geom);
                developments.put(development);
            }

            System.out.println("Future development risk result contains " + developments.length() + " development(s)");

            rs.close();
            pstmt.close();
            super.closeConnection();

        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
            result.put("developments", developments);
            result.put("search_distance", distance);
            result.put("latitude", latitude);
            result.put("longitude", longitude);
            result.put("developmentCount", developments.length());
            result.put("postalCode", postalCode);
            return result;
        }

        result.put("developments", developments);
        result.put("search_distance", distance);
        result.put("latitude", latitude);
        result.put("longitude", longitude);
        result.put("developmentCount", developments.length());
        result.put("status", "OK");
        result.put("message", "NIL");
        result.put("postalCode", postalCode);
        return result;
    }

    private JSONObject invalidPostalCodeResult(String postalCode, double distance) {
        System.out.println("ERROR: Invalid postal code");
        JSONObject result = new JSONObject();
        result.put("postalCode", postalCode == null ? "" : postalCode);
        result.put("status", "ERROR");
        result.put("message", "Invalid postal code: unable to resolve coordinates");
        result.put("search_distance", distance);
        return result;
    }

}
