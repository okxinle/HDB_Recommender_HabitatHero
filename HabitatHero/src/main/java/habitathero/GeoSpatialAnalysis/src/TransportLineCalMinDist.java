package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.json.JSONObject;

public class TransportLineCalMinDist extends SQLDbConnect {

    // TransportLineMgr singleton call this class constructor only once
    public TransportLineCalMinDist() {
        super();
    }

    public JSONObject calMinDist(Coordinate coords) {
        double longitude = coords.getLongitude();
        double latitude = coords.getLatitude();
        JSONObject result = new JSONObject();

        String sql = """
                SELECT OBJECTID, RAIL_TYPE,
                       ST_Distance(
                           geom::geography,
                           ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
                       ) AS distance_meters
                FROM TransportLine
                WHERE GRND_LEVEL = 'ABOVEGROUND'
                ORDER BY distance_meters
                LIMIT 1;
                """;

        try {
            super.connectSQL();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setDouble(1, longitude);
            pstmt.setDouble(2, latitude);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int objectId = rs.getInt("OBJECTID");
                String rail_type = rs.getString("RAIL_TYPE");
                double distance = rs.getDouble("distance_meters");

                result.put("objectId", objectId);
                result.put("rail_type", rail_type);
                result.put("distance_meters", distance);
                result.put("latitude", latitude);
                result.put("longitude", longitude);

                System.out.println("Nearest line OBJECTID: " + objectId);
                System.out.println("Rail Type: " + rail_type);
                System.out.println("Distance (meters): " + distance);
            } else {
                result.put("error", "No transport lines found");
            }

            rs.close();
            pstmt.close();
            super.closeConnection();

        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", e.getMessage());
        }

        return result;
    }

}

