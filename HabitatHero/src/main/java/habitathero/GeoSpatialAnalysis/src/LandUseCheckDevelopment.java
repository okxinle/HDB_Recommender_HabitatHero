import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LandUseCheckDevelopment extends SQLDbConnect {

    // SymbolLineMgr singleton call this class constructor only once
    public LandUseCheckDevelopment() {
        super();
    }

    public ResultSet checkProxNewDev(Coordinate coords, double distance) {
        double longitude = coords.getLongitude();
        double latitude = coords.getLatitude();
        ResultSet rs_return = null;

        String sql = """
                    SELECT
                        OBJECTID,
                        GPR,
                        ST_Distance(
                            ST_Centroid(geom)::geography,
                            ref.ref_point
                        ) AS distance_meters,geom
                    FROM land_use,
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
            rs_return = rs;

            System.out.printf("\nFuture developments within distance: %f\n", distance);

            while (rs.next()) {
                int objectId = rs.getInt("OBJECTID");
                String gpr = rs.getString("GPR");
                double cal_distance = rs.getDouble("distance_meters");

                System.out.println("\nOBJECTID: " + objectId);
                System.out.println("GPR: " + gpr);
                System.out.println("Centroid distance (m): " + cal_distance);
            }

            super.closeConnection();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return rs_return;
    }

}
