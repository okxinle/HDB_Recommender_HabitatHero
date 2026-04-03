package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HDBPostalToCoordinate extends SQLDbConnect {
    private static final String POSTGIS_SQL = """
                SELECT
                    POSTAL_COD,
                    ST_X(ST_Centroid(ST_Collect(geom))) AS longitude,
                    ST_Y(ST_Centroid(ST_Collect(geom))) AS latitude
                FROM HDB_Building
                WHERE POSTAL_COD = ?
                GROUP BY POSTAL_COD
            """;

    private static final String LOOKUP_SQL = """
                SELECT
                    postal_cod,
                    longitude,
                    latitude
                FROM hdb_building_lookup
                WHERE postal_cod = ?
                LIMIT 1
            """;

    public HDBPostalToCoordinate() {
        super();
    }

    public Coordinate postalToCoordinate(String postalCode) {
        Coordinate coords = new Coordinate();

        try {
            super.connectSQL();
            if (!populateCoordsWithQuery(coords, POSTGIS_SQL, postalCode)) {
                populateCoordsWithQuery(coords, LOOKUP_SQL, postalCode);
            }
            super.closeConnection();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return coords;
    }

    private boolean populateCoordsWithQuery(Coordinate coords, String sql, String postalCode) {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postalCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    double longitude = rs.getDouble("longitude");
                    double latitude = rs.getDouble("latitude");
                    coords.setLatitude(latitude);
                    coords.setLongitude(longitude);

                    System.out.printf("Coordinates: %f , %f\n", latitude, longitude);
                    return true;
                }
            }
        } catch (Exception ignored) {
            // Try fallback source if primary source is unavailable.
        }
        return false;
    }
}
