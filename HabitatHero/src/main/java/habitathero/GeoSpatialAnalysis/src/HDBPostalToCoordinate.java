package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HDBPostalToCoordinate extends SQLDbConnect {
    public HDBPostalToCoordinate() {
        super();
    }

    public Coordinate postalToCoordinate(String postalCode) {
        String sql = """
                    SELECT
                        POSTAL_COD,
                        ST_X(ST_Centroid(ST_Collect(geom))) AS longitude,
                        ST_Y(ST_Centroid(ST_Collect(geom))) AS latitude
                    FROM HDB_Building
                    WHERE POSTAL_COD = ?
                    GROUP BY POSTAL_COD
                """;

        Coordinate coords = new Coordinate();

        try {
            super.connectSQL();
            PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, postalCode);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                double longitude = rs.getDouble("longitude");
                double latitude = rs.getDouble("latitude");
                coords.setLatitude(latitude);
                coords.setLongitude(longitude);

                System.out.printf("Coordinates: %f , %f\n", latitude, longitude);
            }
            super.closeConnection();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return coords;
    }
}

