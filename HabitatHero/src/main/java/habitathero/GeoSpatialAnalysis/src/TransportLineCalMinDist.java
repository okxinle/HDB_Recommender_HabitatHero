package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TransportLineCalMinDist extends SQLDbConnect {

    // TransportLineMgr singleton call this class constructor only once
    public TransportLineCalMinDist() {
        super();
    }

    public ResultSet calMinDist(Coordinate coords) {
        double longitude = coords.getLongitude();
        double latitude = coords.getLatitude();
        double distance = 0;

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
                distance = rs.getDouble("distance_meters");

                System.out.println("Nearest line OBJECTID: " + objectId);
                System.out.println("Rail Type: " + rail_type);
                System.out.println("Distance (meters): " + distance);
            }
            super.closeConnection();
            return rs;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}

