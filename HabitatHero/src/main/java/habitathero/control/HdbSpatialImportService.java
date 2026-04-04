package habitathero.control;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import habitathero.GeoSpatialAnalysis.src.HDBBuildingDbMgr;

@Service
public class HdbSpatialImportService {

    private static final Logger log = LoggerFactory.getLogger(HdbSpatialImportService.class);

    private final JdbcTemplate jdbcTemplate;

    public HdbSpatialImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ImportResult initializeAndImportHdbBuilding() {
        HDBBuildingDbMgr mgr = HDBBuildingDbMgr.getInstance();

        Path datasetFile = Path.of(System.getProperty("user.dir"), HDBBuildingDbMgr.getLocalFilePath());
        Path datasetDir = datasetFile.getParent();

        try {
            if (datasetDir != null) {
                Files.createDirectories(datasetDir);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create dataset directory: " + datasetDir, e);
        }

        boolean downloaded = false;
        if (!Files.exists(datasetFile)) {
            log.info("HDB GeoJSON not found at {}. Downloading from Data.gov.sg...", datasetFile);
            downloaded = mgr.forceDownloadGeoJson();
            if (!downloaded || !Files.exists(datasetFile)) {
                throw new IllegalStateException("Failed to download HDB GeoJSON to " + datasetFile);
            }
        }

        if (ensurePostgisExtension()) {
            return importWithPostgis(mgr, downloaded, datasetFile);
        }

        log.warn("PostGIS unavailable. Falling back to non-spatial lookup table import.");
        return importIntoLookupTable(downloaded, datasetFile);
    }

    private ImportResult importWithPostgis(HDBBuildingDbMgr mgr, boolean downloaded, Path datasetFile) {
        log.info("Initializing hdb_building table (if missing)...");
        mgr.createSQLTable();
        ensureTableExists("public.hdb_building", "hdb_building table was not created.");

        log.info("Importing HDB GeoJSON into hdb_building...");
        mgr.importGeoJsonToSQLDb();

        Long rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.hdb_building", Long.class);
        long importedRows = rowCount == null ? 0L : rowCount;

        log.info("hdb_building import complete. rows={}", importedRows);
        return new ImportResult(downloaded, importedRows, datasetFile.toString(), "hdb_building");
    }

    private ImportResult importIntoLookupTable(boolean downloaded, Path datasetFile) {
        log.info("Initializing hdb_building_lookup table (if missing)...");
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS hdb_building_lookup (
                objectid INTEGER PRIMARY KEY,
                blk_no VARCHAR(20),
                st_cod VARCHAR(80),
                postal_cod VARCHAR(20),
                latitude DOUBLE PRECISION,
                longitude DOUBLE PRECISION
            )
            """);

        ensureTableExists("public.hdb_building_lookup", "hdb_building_lookup table was not created.");

        log.info("Importing HDB GeoJSON into hdb_building_lookup...");
        int imported = importGeoJsonIntoLookupTable(datasetFile);
        Long rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.hdb_building_lookup", Long.class);
        long totalRows = rowCount == null ? 0L : rowCount;

        log.info("hdb_building_lookup import complete. batchUpserts={}, rows={}", imported, totalRows);
        return new ImportResult(downloaded, totalRows, datasetFile.toString(), "hdb_building_lookup");
    }

    private int importGeoJsonIntoLookupTable(Path datasetFile) {
        try {
            JSONTokener tokener = new JSONTokener(new FileReader(datasetFile.toFile()));
            JSONObject geojson = new JSONObject(tokener);
            JSONArray features = geojson.getJSONArray("features");

            return jdbcTemplate.execute((ConnectionCallback<Integer>) connection -> {
                String sql = """
                    INSERT INTO hdb_building_lookup (objectid, blk_no, st_cod, postal_cod, latitude, longitude)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (objectid) DO UPDATE SET
                        blk_no = EXCLUDED.blk_no,
                        st_cod = EXCLUDED.st_cod,
                        postal_cod = EXCLUDED.postal_cod,
                        latitude = EXCLUDED.latitude,
                        longitude = EXCLUDED.longitude
                    """;

                PreparedStatement stmt = connection.prepareStatement(sql);
                int batched = 0;

                for (int i = 0; i < features.length(); i++) {
                    JSONObject feature = features.getJSONObject(i);
                    JSONObject properties = feature.getJSONObject("properties");
                    JSONObject geometry = feature.getJSONObject("geometry");

                    int objectId = properties.getInt("OBJECTID");
                    String blkNo = properties.optString("BLK_NO", "");
                    String stCod = properties.optString("ST_COD", "");
                    String postalCode = properties.optString("POSTAL_COD", "");

                    double[] centroid = computeCentroid(geometry.optJSONArray("coordinates"));

                    stmt.setInt(1, objectId);
                    stmt.setString(2, blkNo);
                    stmt.setString(3, stCod);
                    stmt.setString(4, postalCode);
                    stmt.setDouble(5, centroid[1]); // latitude
                    stmt.setDouble(6, centroid[0]); // longitude
                    stmt.addBatch();
                    batched++;

                    if (batched % 2000 == 0) {
                        stmt.executeBatch();
                        log.info("Lookup import progress: {} rows processed", batched);
                    }
                }

                stmt.executeBatch();
                stmt.close();
                return batched;
            });

        } catch (Exception e) {
            throw new IllegalStateException("Failed to import GeoJSON into hdb_building_lookup", e);
        }
    }

    private double[] computeCentroid(JSONArray coordinates) {
        if (coordinates == null) {
            return new double[] { 0.0, 0.0 };
        }

        List<double[]> points = new ArrayList<>();
        collectPoints(coordinates, points);

        if (points.isEmpty()) {
            return new double[] { 0.0, 0.0 };
        }

        double lonSum = 0.0;
        double latSum = 0.0;
        for (double[] p : points) {
            lonSum += p[0];
            latSum += p[1];
        }

        return new double[] { lonSum / points.size(), latSum / points.size() };
    }

    private void collectPoints(JSONArray arr, List<double[]> points) {
        if (arr.isEmpty()) {
            return;
        }

        Object first = arr.get(0);
        if (first instanceof Number && arr.length() >= 2) {
            points.add(new double[] { arr.getDouble(0), arr.getDouble(1) });
            return;
        }

        for (int i = 0; i < arr.length(); i++) {
            Object nested = arr.get(i);
            if (nested instanceof JSONArray nestedArray) {
                collectPoints(nestedArray, points);
            }
        }
    }

    private void ensureTableExists(String qualifiedTableName, String errorMessage) {
        String sql = "SELECT to_regclass(?)";
        String tableName;
        try {
            tableName = jdbcTemplate.queryForObject(sql, String.class, qualifiedTableName);
        } catch (DataAccessException e) {
            throw new IllegalStateException("Failed to verify table existence for " + qualifiedTableName, e);
        }

        if (tableName == null || tableName.isBlank()) {
            throw new IllegalStateException(errorMessage);
        }
    }

    public record ImportResult(boolean downloadedGeoJson, long hdbBuildingRows, String geoJsonPath, String sourceTable) {
    }

    private boolean ensurePostgisExtension() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis");
            return true;
        } catch (DataAccessException e) {
            log.warn("PostGIS not available or insufficient privilege: {}", rootMessage(e));
            return false;
        }
    }

    private String rootMessage(Throwable t) {
        Throwable cursor = t;
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.toString() : cursor.getMessage();
    }
}
