package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HDBPostalToCoordinate extends SQLDbConnect {
    private static HDBPostalToCoordinate instance;

    private HDBPostalToCoordinate() {
        super();
    }

    public static HDBPostalToCoordinate getInstance() {
        if (instance == null) {
            instance = new HDBPostalToCoordinate();
        }
        return instance;
    }

    // when postalCode is invalid, coordinate will return with lattitude = longtitude = -1
    public Coordinate postalToCoordinate(String postalCode) {
        String sql = """
                    SELECT
                        POSTAL_COD,
                        ST_X(ST_Centroid(ST_Collect(geom))) AS longitude,
                        ST_Y(ST_Centroid(ST_Collect(geom))) AS latitude
                    FROM HDB_Building_Dataset
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

                System.out.printf("Converting postal to Coordinates: %f , %f\n", latitude, longitude);
            }
            super.closeConnection();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return coords;
    }
}
