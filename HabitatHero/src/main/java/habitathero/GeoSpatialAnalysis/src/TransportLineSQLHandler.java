package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

public class TransportLineSQLHandler extends SQLDbConnect {
    private static TransportLineSQLHandler instance;
    
    private TransportLineSQLHandler(){
        super();
    }

    public static TransportLineSQLHandler getInstance(){
        if(instance == null){
            instance = new TransportLineSQLHandler();
        }
        return instance;
    }

    public JSONObject calMinDist(String postalCode) {
        System.out.println("Calculating minimum distance for postal code " + postalCode);
        Coordinate coords = HDBBuildingMgr.getInstance().postalCodeToCoordinate(postalCode);
        if (isInvalidCoordinate(coords)) {
            return invalidPostalCodeResult(postalCode, null);
        }
        return calMinDist(coords);
    }

    private JSONObject calMinDist(Coordinate coords) {
        double hdb_longitude = coords.getLongitude();
        double hdb_latitude = coords.getLatitude();
        JSONObject result = new JSONObject();

        String sql = """
                SELECT OBJECTID, RAIL_TYPE,
                       ST_Distance(
                           geom::geography,
                           ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
                       ) AS distance_meters,
                       ST_AsGeoJSON(geom) AS geojson_geom
                FROM Transport_Line_Dataset
                WHERE GRND_LEVEL = 'ABOVEGROUND'
                ORDER BY distance_meters
                LIMIT 1;
                """;

        try {
            super.connectSQL();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setDouble(1, hdb_longitude);
            pstmt.setDouble(2, hdb_latitude);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int objectId = rs.getInt("OBJECTID");
                String rail_type = rs.getString("RAIL_TYPE");
                double distance = rs.getDouble("distance_meters");

                result.put("objectId", objectId);
                result.put("rail_type", rail_type);
                result.put("distance_meters", distance);
                result.put("hdb_latitude", hdb_latitude);
                result.put("hdb_longitude", hdb_longitude);
                result.put("status", "OK");
                result.put("message", "NIL");

                System.out.println("Nearest line OBJECTID: " + objectId);
                System.out.println("Rail Type: " + rail_type);
                System.out.println("Distance (meters): " + distance);
            } else {
                result.put("status", "ERROR");
                result.put("message", "No transport lines found");
            }

            rs.close();
            pstmt.close();
            super.closeConnection();

        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }

        return result;
    }

    public JSONObject calMinDist(String postalCode, double radius) {
        System.out.println("Calculating minimum distance for postal code " + postalCode + " with radius " + radius);
        Coordinate coords = HDBBuildingMgr.getInstance().postalCodeToCoordinate(postalCode);
        if (isInvalidCoordinate(coords)) {
            return invalidPostalCodeResult(postalCode, radius);
        }
        return calMinDist(coords, radius);
    }

    private JSONObject calMinDist(Coordinate coords, double radius) {
        double hdb_longitude = coords.getLongitude();
        double hdb_latitude = coords.getLatitude();
        JSONObject result = new JSONObject();

        String sql = """
                WITH distances AS (
                    SELECT OBJECTID, RAIL_TYPE,
                           ST_Distance(
                               geom::geography,
                               ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
                           ) AS distance_meters,
                           ST_AsGeoJSON(geom) AS geojson_geom
                    FROM Transport_Line_Dataset
                    WHERE GRND_LEVEL = 'ABOVEGROUND'
                          AND RAIL_TYPE IN ('MRT', 'LRT')
                )
                SELECT OBJECTID, RAIL_TYPE, distance_meters, geojson_geom FROM (
                    SELECT *, ROW_NUMBER() OVER (PARTITION BY RAIL_TYPE ORDER BY distance_meters) as rn
                    FROM distances
                    WHERE distance_meters <= ?
                ) t WHERE rn = 1;
                """;

        try {
            super.connectSQL();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setDouble(1, hdb_longitude);
            pstmt.setDouble(2, hdb_latitude);
            pstmt.setDouble(3, radius);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int objectId = rs.getInt("OBJECTID");
                String rail_type = rs.getString("RAIL_TYPE");
                double distance = rs.getDouble("distance_meters");
                String geojson_geom = rs.getString("geojson_geom");

                result.put("objectId", objectId);
                result.put("rail_type", rail_type);
                result.put("distance_meters", distance);
                result.put("geojson_geom", geojson_geom);
                result.put("hdb_latitude", hdb_latitude);
                result.put("hdb_longitude", hdb_longitude);
                result.put("status", "OK");
                result.put("message", "NIL");

                System.out.println("Nearest line OBJECTID: " + objectId);
                System.out.println("Rail Type: " + rail_type);
                System.out.println("Distance (meters): " + distance);
            } else {
                result.put("status", "ERROR");
                result.put("message", "No MRT or LRT transport lines found within " + radius + " meters");
            }

            rs.close();
            pstmt.close();
            super.closeConnection();

        } catch (Exception e) {
            e.printStackTrace();
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }

        result.put("search_radius", radius);
        return result;
    }

    private boolean isInvalidCoordinate(Coordinate coords) {
        return coords == null || (coords.getLatitude() == -1 && coords.getLongitude() == -1);
    }

    private JSONObject invalidPostalCodeResult(String postalCode, Double radius) {
        System.out.println("ERROR: Invalid postal code");
        JSONObject result = new JSONObject();
        result.put("status", "ERROR");
        result.put("message", "Invalid postal code: unable to resolve coordinates");
        result.put("postalCode", postalCode == null ? "" : postalCode);
        if (radius != null) {
            result.put("search_radius", radius);
        }
        return result;
    }

}
