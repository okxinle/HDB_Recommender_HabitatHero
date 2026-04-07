package habitathero.control;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import habitathero.entity.Coordinates;
import habitathero.entity.HDBBlock;
import habitathero.repository.IHDBRepository;

@Service
public class BackfillCoordinatesService {

    private static final Logger log = LoggerFactory.getLogger(BackfillCoordinatesService.class);
    private static final int SAVE_BATCH_SIZE = 500;

    private static final String MATCH_BY_BLOCK_AND_STREET_SPATIAL_SQL = """
        SELECT
            postal_cod,
            ST_Y(ST_Centroid(ST_Collect(geom))) AS latitude,
            ST_X(ST_Centroid(ST_Collect(geom))) AS longitude
        FROM hdb_building
        WHERE blk_no = ?
          AND (
                UPPER(TRIM(st_cod)) = UPPER(TRIM(?))
             OR REPLACE(UPPER(TRIM(st_cod)), ' ', '') = REPLACE(UPPER(TRIM(?)), ' ', '')
          )
          AND postal_cod IS NOT NULL
          AND TRIM(postal_cod) <> ''
        GROUP BY postal_cod
        LIMIT 1
        """;

        private static final String MATCH_BY_BLOCK_AND_STREET_LOOKUP_SQL = """
                SELECT
                        postal_cod,
                        latitude,
                        longitude
                FROM hdb_building_lookup
                WHERE blk_no = ?
                    AND (
                                UPPER(TRIM(st_cod)) = UPPER(TRIM(?))
                         OR REPLACE(UPPER(TRIM(st_cod)), ' ', '') = REPLACE(UPPER(TRIM(?)), ' ', '')
                    )
                    AND postal_cod IS NOT NULL
                    AND TRIM(postal_cod) <> ''
                LIMIT 1
                """;

    private final IHDBRepository hdbRepository;
    private final JdbcTemplate jdbcTemplate;
    private final GeocodingService geocodingService;

    public BackfillCoordinatesService(
            IHDBRepository hdbRepository,
            JdbcTemplate jdbcTemplate,
            GeocodingService geocodingService) {
        this.hdbRepository = hdbRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.geocodingService = geocodingService;
    }

    @Transactional
    public BackfillSummary backfillMissingCoordinates() {
        SpatialSource source = resolveSpatialSource();

        List<HDBBlock> candidates = hdbRepository.findBlocksMissingGeoData();
        if (candidates.isEmpty()) {
            log.info("Backfill skipped: no hdb_blocks rows with missing postalCode/coordinates.");
            return new BackfillSummary(0, 0, 0, source.tableName());
        }

        log.info("Backfill started. sourceTable={}, candidates={}", source.tableName(), candidates.size());

        int updatedCount = 0;
        int scannedCount = 0;
        List<HDBBlock> saveBuffer = new ArrayList<>();

        for (HDBBlock block : candidates) {
            scannedCount++;

            GeocodeResult geocode = findGeocodeForBlock(block.getBlockNumber(), block.getStreetName(), source);
            if (geocode == null) {
                if (scannedCount % 1000 == 0) {
                    log.info("Backfill progress: scanned={}, updated={}, unresolved={}", scannedCount, updatedCount,
                            scannedCount - updatedCount);
                }
                continue;
            }

            block.setPostalCode(geocode.postalCode());
            block.setCoordinates(new Coordinates(geocode.latitude(), geocode.longitude()));
            saveBuffer.add(block);
            updatedCount++;

            if (saveBuffer.size() >= SAVE_BATCH_SIZE) {
                hdbRepository.saveAll(saveBuffer);
                log.info("Backfill batch saved. scanned={}, updated={}", scannedCount, updatedCount);
                saveBuffer.clear();
            }
        }

        if (!saveBuffer.isEmpty()) {
            hdbRepository.saveAll(saveBuffer);
            log.info("Backfill final batch saved. scanned={}, updated={}", scannedCount, updatedCount);
        }

        log.info("Backfill completed. scanned={}, updated={}, unresolved={}", scannedCount, updatedCount,
                scannedCount - updatedCount);
        return new BackfillSummary(scannedCount, updatedCount, scannedCount - updatedCount, source.tableName());
    }

    @Transactional
    public int clearAllGeoData() {
        int updated = jdbcTemplate.update("""
            UPDATE hdb_blocks
            SET postal_code = NULL,
                coordinates = NULL
            WHERE postal_code IS NOT NULL
               OR coordinates IS NOT NULL
            """);

        log.warn("Cleared geo columns for {} hdb_blocks rows before re-ingestion.", updated);
        return updated;
    }

    private GeocodeResult findGeocodeForBlock(String blockNumber, String streetName, SpatialSource source) {
        if (blockNumber == null || blockNumber.isBlank()) {
            return null;
        }

        if (streetName == null || streetName.isBlank()) {
            return null;
        }

        String exactSql = source.usesSpatialFunctions() ? MATCH_BY_BLOCK_AND_STREET_SPATIAL_SQL
                : MATCH_BY_BLOCK_AND_STREET_LOOKUP_SQL;

        List<GeocodeResult> exactMatches = jdbcTemplate.query(
                exactSql,
                geocodeRowMapper(),
                blockNumber.trim(),
                streetName == null ? "" : streetName.trim(),
                streetName == null ? "" : streetName.trim());

        if (!exactMatches.isEmpty()) {
            return exactMatches.get(0);
        }

        return geocodingService.getGeocodeByAddress(blockNumber.trim(), streetName.trim())
            .map(g -> new GeocodeResult(g.postalCode(), g.latitude(), g.longitude()))
            .orElse(null);
    }

    private RowMapper<GeocodeResult> geocodeRowMapper() {
        return (rs, rowNum) -> new GeocodeResult(
                rs.getString("postal_cod"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"));
    }

    private SpatialSource resolveSpatialSource() {
        if (tableHasRows("public.hdb_building")) {
            return new SpatialSource("hdb_building", true);
        }

        if (tableHasRows("public.hdb_building_lookup")) {
            return new SpatialSource("hdb_building_lookup", false);
        }

        throw new IllegalStateException(
                "No spatial source is ready. Initialize hdb_building (PostGIS) or hdb_building_lookup first via /api/admin/init-hdb-building.");
    }

    private boolean tableHasRows(String qualifiedTableName) {
        try {
            String regclass = jdbcTemplate.queryForObject(
                    "SELECT to_regclass(?)",
                    String.class,
                    qualifiedTableName);

            if (regclass == null || regclass.isBlank()) {
                return false;
            }

            Long rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + qualifiedTableName, Long.class);
            return rowCount != null && rowCount > 0L;
        } catch (DataAccessException e) {
            return false;
        }
    }

    private record GeocodeResult(String postalCode, double latitude, double longitude) {
    }

    private record SpatialSource(String tableName, boolean usesSpatialFunctions) {
    }

    public record BackfillSummary(int candidatesScanned, int updatedBlocks, int unresolvedBlocks, String sourceTableUsed) {
    }
}
