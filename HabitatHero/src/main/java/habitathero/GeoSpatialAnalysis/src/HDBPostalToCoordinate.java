package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        Coordinate coords = new Coordinate();

        try {
            super.connectSQL();
            List<String> postalCandidates = buildPostalCandidates(postalCode);

            for (String candidate : postalCandidates) {
                if (tryResolveFromPostgisTable(candidate, coords)) {
                    System.out.printf("Converting postal to Coordinates (hdb_building): %f , %f\n", coords.getLatitude(), coords.getLongitude());
                    super.closeConnection();
                    return coords;
                }

                if (tryResolveFromLookupTable(candidate, coords)) {
                    System.out.printf("Converting postal to Coordinates (hdb_building_lookup): %f , %f\n", coords.getLatitude(), coords.getLongitude());
                    super.closeConnection();
                    return coords;
                }

                // Backward compatibility with the legacy table name.
                if (tryResolveFromLegacyTable(candidate, coords)) {
                    System.out.printf("Converting postal to Coordinates (hdb_building_dataset): %f , %f\n", coords.getLatitude(), coords.getLongitude());
                    super.closeConnection();
                    return coords;
                }
            }

            super.closeConnection();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return coords;
    }

    private List<String> buildPostalCandidates(String postalCode) {
        String raw = postalCode == null ? "" : postalCode.trim();
        Set<String> candidates = new LinkedHashSet<>();

        if (!raw.isEmpty()) {
            candidates.add(raw);
        }

        if (raw.matches("\\d{6}")) {
            candidates.add(raw + "000");
        }

        if (raw.matches("\\d{9}") && raw.endsWith("000")) {
            candidates.add(raw.substring(0, 6));
        }

        return new ArrayList<>(candidates);
    }

    private boolean tryResolveFromPostgisTable(String postalCode, Coordinate coords) throws Exception {
        String sql = """
                SELECT
                    ST_X(ST_Centroid(ST_Collect(geom))) AS longitude,
                    ST_Y(ST_Centroid(ST_Collect(geom))) AS latitude
                FROM hdb_building
                WHERE postal_cod = ?
                GROUP BY postal_cod
                LIMIT 1
                """;
        return executeCoordinateQuery(sql, postalCode, coords);
    }

    private boolean tryResolveFromLookupTable(String postalCode, Coordinate coords) throws Exception {
        String sql = """
                SELECT longitude, latitude
                FROM hdb_building_lookup
                WHERE postal_cod = ?
                LIMIT 1
                """;
        return executeCoordinateQuery(sql, postalCode, coords);
    }

    private boolean tryResolveFromLegacyTable(String postalCode, Coordinate coords) throws Exception {
        String sql = """
                SELECT
                    ST_X(ST_Centroid(ST_Collect(geom))) AS longitude,
                    ST_Y(ST_Centroid(ST_Collect(geom))) AS latitude
                FROM hdb_building_dataset
                WHERE postal_cod = ?
                GROUP BY postal_cod
                LIMIT 1
                """;
        return executeCoordinateQuery(sql, postalCode, coords);
    }

    private boolean executeCoordinateQuery(String sql, String postalCode, Coordinate coords) throws Exception {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postalCode);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }

                double longitude = rs.getDouble("longitude");
                double latitude = rs.getDouble("latitude");
                if (rs.wasNull()) {
                    return false;
                }

                coords.setLatitude(latitude);
                coords.setLongitude(longitude);
                return true;
            }
        }
    }
}
