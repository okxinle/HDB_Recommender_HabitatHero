package habitathero.boundary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import habitathero.GeoSpatialAnalysis.src.HDBBuildingDbMgr;
import habitathero.GeoSpatialAnalysis.src.HDBBuildingSunFacingResultSQLHandler;
import habitathero.GeoSpatialAnalysis.src.LandUseDbMgr;
import habitathero.GeoSpatialAnalysis.src.LandUseMgr;
import habitathero.GeoSpatialAnalysis.src.MainSpatialMgr;
import habitathero.GeoSpatialAnalysis.src.TransportLineCalResultSQLHandler;
import habitathero.GeoSpatialAnalysis.src.TransportLineDbMgr;
import habitathero.control.BackfillCoordinatesService;
import habitathero.control.DataPipelineService;
import habitathero.control.HdbSpatialImportService;
import habitathero.entity.AuditLog;
import habitathero.entity.HDBBlock;
import habitathero.entity.PointOfInterest;
import habitathero.entity.UserAccount;
import habitathero.entity.GlobalWeightConfig;
import habitathero.repository.GlobalConfigService;
import habitathero.repository.UserRepository;
import habitathero.repository.AuditLogService;
import habitathero.repository.IHDBRepository;
import habitathero.repository.PoiRepository;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private IHDBRepository hdbRepository;

    // Add this new endpoint
    @GetMapping("/hdb-data")
    public ResponseEntity<List<HDBBlock>> getAllHdbData() {
        // Fetches all the records you just synced from the database
        List<HDBBlock> allBlocks = hdbRepository.findAll();
        return ResponseEntity.ok(allBlocks);
    }

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private DataPipelineService dataPipelineService;

    @Autowired
    private BackfillCoordinatesService backfillCoordinatesService;

    @Autowired
    private HdbSpatialImportService hdbSpatialImportService;

    @Autowired
    private AuditLogService auditLogRepository;

    @Autowired
    private PoiRepository poiRepository;

    @Autowired
    private GlobalConfigService globalConfigService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Headless API to manually trigger the sync (useful for testing)
    @PostMapping("/trigger-sync")
    public ResponseEntity<?> manualTriggerSync() {
        // Run the sync process asynchronously or synchronously. 
        // For immediate feedback in a headless API, we call it directly.
        try {
            dataPipelineService.syncHdbData();
            return ResponseEntity.ok(Map.of("message", "Sync triggered and completed successfully. Check logs for details."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Sync failed critically: " + e.getMessage()));
        }
    }

    @PostMapping("/backfill-coordinates")
    public ResponseEntity<?> backfillCoordinates() {
        try {
            BackfillCoordinatesService.BackfillSummary summary = backfillCoordinatesService.backfillMissingCoordinates();
            return ResponseEntity.ok(Map.of(
                "message", "Coordinate backfill completed successfully.",
                "candidatesScanned", summary.candidatesScanned(),
                "updatedBlocks", summary.updatedBlocks(),
                "unresolvedBlocks", summary.unresolvedBlocks(),
                "sourceTableUsed", summary.sourceTableUsed()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Coordinate backfill failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/init-hdb-building")
    public ResponseEntity<?> initializeHdbBuilding() {
        try {
            HdbSpatialImportService.ImportResult result = hdbSpatialImportService.initializeAndImportHdbBuilding();

            return ResponseEntity.ok(Map.of(
                "message", "hdb_building_dataset initialized and imported successfully.",
                "downloadedGeoJson", result.downloadedGeoJson(),
                "hdbBuildingRows", result.hdbBuildingRows(),
                "geoJsonPath", result.geoJsonPath(),
                "sourceTable", result.sourceTable()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "hdb_building_dataset initialization failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/init-land-use")
    public ResponseEntity<?> initializeLandUse() {
        try {
            LandUseDbMgr landUseDbMgr = LandUseDbMgr.getInstance();

            boolean tableReady = landUseDbMgr.createSQLTable();
            boolean downloadedGeoJson = landUseDbMgr.forceDownloadGeoJson();
            boolean importedToDb = downloadedGeoJson && landUseDbMgr.importGeoJsonToSQLDb();
            String importError = landUseDbMgr.getLastImportErrorMessage();
            int importedCount = landUseDbMgr.getLastImportedCount();
            int skippedCount = landUseDbMgr.getLastSkippedCount();
            int totalFeatures = landUseDbMgr.getLastTotalFeatures();

            if (!tableReady || !downloadedGeoJson || !importedToDb) {
                return ResponseEntity.status(500).body(Map.of(
                    "error", "Land use initialization incomplete.",
                    "tableReady", tableReady,
                    "downloadedGeoJson", downloadedGeoJson,
                    "importedToDb", importedToDb,
                    "totalFeatures", totalFeatures,
                    "importedCount", importedCount,
                    "skippedCount", skippedCount,
                    "importError", importError == null ? "" : importError
                ));
            }

            return ResponseEntity.ok(Map.of(
                "message", "land_use_dataset initialized and imported successfully.",
                "tableReady", tableReady,
                "downloadedGeoJson", downloadedGeoJson,
                "importedToDb", importedToDb,
                "totalFeatures", totalFeatures,
                "importedCount", importedCount,
                "skippedCount", skippedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "land_use_dataset initialization failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/reingest-hdb-coordinates")
    public ResponseEntity<?> reingestHdbCoordinates() {
        try {
            HdbSpatialImportService.ImportResult importResult = hdbSpatialImportService.initializeAndImportHdbBuilding();
            int clearedRows = backfillCoordinatesService.clearAllGeoData();
            BackfillCoordinatesService.BackfillSummary backfillSummary = backfillCoordinatesService.backfillMissingCoordinates();

            return ResponseEntity.ok(Map.of(
                "message", "HDB coordinate ingestion rerun completed successfully.",
                "clearedRows", clearedRows,
                "sourceTable", importResult.sourceTable(),
                "importedRows", importResult.hdbBuildingRows(),
                "updatedBlocks", backfillSummary.updatedBlocks(),
                "unresolvedBlocks", backfillSummary.unresolvedBlocks()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "HDB coordinate re-ingestion failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/init-hdb-building-dataset")
    public ResponseEntity<?> initializeHdbBuildingDataset() {
        try {
            HDBBuildingDbMgr hdbBuildingDbMgr = HDBBuildingDbMgr.getInstance();
            
            boolean tableReady = hdbBuildingDbMgr.createSQLTable();
            
            boolean downloadedGeoJson = hdbBuildingDbMgr.forceDownloadGeoJson();
            
            boolean importedToDb = downloadedGeoJson && hdbBuildingDbMgr.importGeoJsonToSQLDb();
            
            HDBBuildingSunFacingResultSQLHandler.getInstance().createSQLTable();

            if (!tableReady || !downloadedGeoJson || !importedToDb) {
                return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "HDB Building initialization incomplete.",
                    "tableReady", tableReady,
                    "downloadedGeoJson", downloadedGeoJson,
                    "importedToDb", importedToDb
                ));
            }

            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "HDB Building dataset initialized and imported successfully.",
                "tableReady", tableReady,
                "downloadedGeoJson", downloadedGeoJson,
                "importedToDb", importedToDb,
                "resultsTable", "sun_facing_analysis_result verified"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "message", "HDB Building dataset initialization failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/init-transport-dataset")
    public ResponseEntity<?> initializeTransportDataset() {
        try {
            TransportLineDbMgr transportDbMgr = TransportLineDbMgr.getInstance();

            boolean tableCreated = transportDbMgr.createSQLTable();
            boolean downloaded = transportDbMgr.forceDownloadGeoJson();

            boolean imported = false;
            if (downloaded) {
                imported = transportDbMgr.importGeoJsonToSQLDb();
            }
            
            TransportLineCalResultSQLHandler.getInstance().createSQLTable();

            if (tableCreated && imported) {
                return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Transport Line dataset and result tables are ready.",
                    "tableCreated", tableCreated,
                    "dataImported", imported
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", "Failed to complete transport dataset initialization."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/cache/purge-placeholders")
    public ResponseEntity<?> purgePlaceholderCaches() {
        try {
            Integer sunBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sun_facing_analysis_result",
                Integer.class);
            Integer noiseBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transport_line_cal_result",
                Integer.class);

            int deletedSun = jdbcTemplate.update("""
                DELETE FROM sun_facing_analysis_result
                WHERE message = 'DEFAULT_RESULT_NO_GEOMETRY'
                   OR COALESCE(perimeter, 0) = 0
                   OR COALESCE(sunlight_steps, 0) = 0
                """);

            int deletedNoise = jdbcTemplate.update("""
                DELETE FROM transport_line_cal_result
                WHERE noise_message = 'DEFAULT_RESULT_NO_TRANSPORT_DATA'
                   OR object_id IS NULL
                   OR distance_meters IS NULL
                   OR noise_level_db IS NULL
                """);

            Integer sunAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sun_facing_analysis_result",
                Integer.class);
            Integer noiseAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transport_line_cal_result",
                Integer.class);

            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Placeholder cache rows purged.",
                "sunFacingRowsBefore", sunBefore == null ? 0 : sunBefore,
                "sunFacingRowsDeleted", deletedSun,
                "sunFacingRowsAfter", sunAfter == null ? 0 : sunAfter,
                "noiseRowsBefore", noiseBefore == null ? 0 : noiseBefore,
                "noiseRowsDeleted", deletedNoise,
                "noiseRowsAfter", noiseAfter == null ? 0 : noiseAfter
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "message", "Failed to purge placeholder caches: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/cache/precompute-spatial")
    public ResponseEntity<?> precomputeSpatialCaches(@RequestBody(required = false) Map<String, Object> options) {
        try {
            long startedAtMs = System.currentTimeMillis();
            boolean includeFutureRisk = parseBooleanOption(options, "includeFutureRisk", false);
            double futureRiskDistance = parseDoubleOption(options, "futureRiskDistance", 500.0);
            int limit = parseIntOption(options, "limit", 0);
            int offset = parseIntOption(options, "offset", 0);
            boolean stopOnError = parseBooleanOption(options, "stopOnError", false);
            boolean onlyMissing = parseBooleanOption(options, "onlyMissing", true);
            boolean fastSunFacing = parseBooleanOption(options, "fastSunFacing", true);
            double sunFullSweepStepDegrees = parseDoubleOption(options, "sunFullSweepStepDegrees", 5.0);
            double sunDayArcStepDegrees = parseDoubleOption(options, "sunDayArcStepDegrees", 15.0);

            List<String> sourcePostals = jdbcTemplate.queryForList("""
                SELECT DISTINCT TRIM(postal_cod)
                FROM hdb_building_lookup
                WHERE postal_cod IS NOT NULL
                  AND TRIM(postal_cod) <> ''
                ORDER BY TRIM(postal_cod)
                """, String.class);

            List<String> allPostals;
            if (onlyMissing) {
                if (includeFutureRisk) {
                    allPostals = jdbcTemplate.queryForList("""
                        SELECT DISTINCT TRIM(l.postal_cod) AS postal
                        FROM hdb_building_lookup l
                        LEFT JOIN sun_facing_analysis_result s
                            ON s.postal_code = TRIM(l.postal_cod)
                        LEFT JOIN transport_line_cal_result t
                            ON t.postal_code = TRIM(l.postal_cod)
                        LEFT JOIN land_use_future_dev_risk_result f
                            ON f.postal_code = TRIM(l.postal_cod)
                        WHERE l.postal_cod IS NOT NULL
                          AND TRIM(l.postal_cod) <> ''
                          AND (
                                s.postal_code IS NULL
                                OR NOT (
                                    s.status = 'OK'
                                    AND COALESCE(s.perimeter, 0) > 0
                                    AND COALESCE(s.sunlight_steps, 0) > 0
                                )
                                OR t.postal_code IS NULL
                                OR NOT (
                                    t.noise_status = 'OK'
                                    AND t.object_id IS NOT NULL
                                    AND t.distance_meters IS NOT NULL
                                    AND t.noise_level_db IS NOT NULL
                                )
                                OR f.postal_code IS NULL
                                OR NOT (f.status = 'OK')
                          )
                        ORDER BY postal
                        """, String.class);
                } else {
                    allPostals = jdbcTemplate.queryForList("""
                        SELECT DISTINCT TRIM(l.postal_cod) AS postal
                        FROM hdb_building_lookup l
                        LEFT JOIN sun_facing_analysis_result s
                            ON s.postal_code = TRIM(l.postal_cod)
                        LEFT JOIN transport_line_cal_result t
                            ON t.postal_code = TRIM(l.postal_cod)
                        WHERE l.postal_cod IS NOT NULL
                          AND TRIM(l.postal_cod) <> ''
                          AND (
                                s.postal_code IS NULL
                                OR NOT (
                                    s.status = 'OK'
                                    AND COALESCE(s.perimeter, 0) > 0
                                    AND COALESCE(s.sunlight_steps, 0) > 0
                                )
                                OR t.postal_code IS NULL
                                OR NOT (
                                    t.noise_status = 'OK'
                                    AND t.object_id IS NOT NULL
                                    AND t.distance_meters IS NOT NULL
                                    AND t.noise_level_db IS NOT NULL
                                )
                          )
                        ORDER BY postal
                        """, String.class);
                }
            } else {
                allPostals = sourcePostals;
            }

            if (allPostals == null || allPostals.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", onlyMissing
                            ? "No missing spatial cache entries found."
                            : "No postals found in hdb_building_lookup.",
                    "processed", 0
                ));
            }

            int normalizedOffset = Math.max(0, Math.min(offset, allPostals.size()));
            int endExclusive = limit > 0
                ? Math.min(normalizedOffset + limit, allPostals.size())
                : allPostals.size();
            int capped = Math.max(0, endExclusive - normalizedOffset);

            MainSpatialMgr spatialMgr = MainSpatialMgr.getInstance();
            LandUseMgr landUseMgr = LandUseMgr.getInstance();

            int sunOk = 0;
            int sunErr = 0;
            int noiseOk = 0;
            int noiseErr = 0;
            int futureOk = 0;
            int futureErr = 0;
            int unexpectedErrors = 0;
            List<Map<String, Object>> sampleErrors = new ArrayList<>();

            for (int i = normalizedOffset; i < endExclusive; i++) {
                String postal = allPostals.get(i);

                try {
                    JSONObject sun = fastSunFacing
                            ? spatialMgr.getSunFacingFast(postal, sunFullSweepStepDegrees, sunDayArcStepDegrees)
                            : spatialMgr.getSunFacing(postal);
                    if ("OK".equalsIgnoreCase(sun.optString("status", ""))) {
                        sunOk++;
                    } else {
                        sunErr++;
                    }
                } catch (Exception factorError) {
                    sunErr++;
                    unexpectedErrors++;
                    if (sampleErrors.size() < 10) {
                        sampleErrors.add(Map.of(
                            "postal", postal,
                            "factor", "sun",
                            "error", factorError.getMessage() == null ? "Unknown error" : factorError.getMessage()
                        ));
                    }
                    if (stopOnError) {
                        break;
                    }
                }

                try {
                    JSONObject noise = spatialMgr.getNoiseLevel(postal);
                    if ("OK".equalsIgnoreCase(noise.optString("status", ""))) {
                        noiseOk++;
                    } else {
                        noiseErr++;
                    }
                } catch (Exception factorError) {
                    noiseErr++;
                    unexpectedErrors++;
                    if (sampleErrors.size() < 10) {
                        sampleErrors.add(Map.of(
                            "postal", postal,
                            "factor", "noise",
                            "error", factorError.getMessage() == null ? "Unknown error" : factorError.getMessage()
                        ));
                    }
                    if (stopOnError) {
                        break;
                    }
                }

                if (includeFutureRisk) {
                    try {
                        JSONObject future = landUseMgr.getFutureDevRisk(postal, futureRiskDistance);
                        if ("OK".equalsIgnoreCase(future.optString("status", ""))) {
                            futureOk++;
                        } else {
                            futureErr++;
                        }
                    } catch (Exception factorError) {
                        futureErr++;
                        unexpectedErrors++;
                        if (sampleErrors.size() < 10) {
                            sampleErrors.add(Map.of(
                                "postal", postal,
                                "factor", "futureRisk",
                                "error", factorError.getMessage() == null ? "Unknown error" : factorError.getMessage()
                            ));
                        }
                        if (stopOnError) {
                            break;
                        }
                    }
                }

                if ((i - normalizedOffset + 1) % 200 == 0) {
                    System.out.println("Precompute progress: processed " + (i - normalizedOffset + 1) + " / " + capped);
                }
            }

            int processed = sunOk + sunErr;
            boolean hasMore = endExclusive < allPostals.size();
            long durationMs = System.currentTimeMillis() - startedAtMs;

            return ResponseEntity.ok(Map.ofEntries(
                Map.entry("status", "SUCCESS"),
                Map.entry("message", "Spatial cache precompute completed."),
                Map.entry("onlyMissing", onlyMissing),
                Map.entry("totalSourcePostals", sourcePostals == null ? 0 : sourcePostals.size()),
                Map.entry("totalTargetPostals", allPostals.size()),
                Map.entry("offset", normalizedOffset),
                Map.entry("limit", limit),
                Map.entry("processed", processed),
                Map.entry("hasMore", hasMore),
                Map.entry("nextOffset", hasMore ? endExclusive : -1),
                Map.entry("includeFutureRisk", includeFutureRisk),
                Map.entry("futureRiskDistance", futureRiskDistance),
                Map.entry("stopOnError", stopOnError),
                Map.entry("fastSunFacing", fastSunFacing),
                Map.entry("sunFullSweepStepDegrees", sunFullSweepStepDegrees),
                Map.entry("sunDayArcStepDegrees", sunDayArcStepDegrees),
                Map.entry("sunOk", sunOk),
                Map.entry("sunError", sunErr),
                Map.entry("noiseOk", noiseOk),
                Map.entry("noiseError", noiseErr),
                Map.entry("futureRiskOk", futureOk),
                Map.entry("futureRiskError", futureErr),
                Map.entry("unexpectedErrors", unexpectedErrors),
                Map.entry("sampleErrors", sampleErrors),
                Map.entry("durationMs", durationMs)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "message", "Failed to precompute spatial caches: " + e.getMessage()
            ));
        }
    }

    private boolean parseBooleanOption(Map<String, Object> options, String key, boolean defaultValue) {
        if (options == null || !options.containsKey(key) || options.get(key) == null) {
            return defaultValue;
        }
        Object value = options.get(key);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private double parseDoubleOption(Map<String, Object> options, String key, double defaultValue) {
        if (options == null || !options.containsKey(key) || options.get(key) == null) {
            return defaultValue;
        }
        Object value = options.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int parseIntOption(Map<String, Object> options, String key, int defaultValue) {
        if (options == null || !options.containsKey(key) || options.get(key) == null) {
            return defaultValue;
        }
        Object value = options.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @PostMapping("/pois/load")
    public ResponseEntity<?> loadPois(@RequestBody List<PointOfInterest> pois) {
        if (pois == null || pois.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "POI payload must contain at least one item."
            ));
        }

        try {
            List<PointOfInterest> saved = poiRepository.saveAll(pois);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "savedCount", saved.size()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Failed to load POIs: " + e.getMessage()
            ));
        }
    }

    // Tune global algorithm weights
    @PostMapping("/weights")
    public ResponseEntity<?> updateGlobalWeights(@RequestBody Map<String, Double> weights) {
        if (weights == null || weights.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "No weights provided"
            ));
        }

        for (Map.Entry<String, Double> entry : weights.entrySet()) {
            GlobalWeightConfig config = new GlobalWeightConfig(entry.getKey(), entry.getValue());
            globalConfigService.save(config);
        }

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Global weights updated",
            "updatedKeys", weights.keySet()
        ));
    }

    // API to view the audit logs
    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        // Retrieves all logs, ordered from newest to oldest
        List<AuditLog> logs = auditLogRepository.findAll(
            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "timestamp")
        );
        return ResponseEntity.ok(logs);
    }

    // Revoke user access by deactivating their account
    @PostMapping("/users/{userId}/revoke")
    public ResponseEntity<?> revokeUserAccess(@PathVariable int userId) {
        UserAccount user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of(
                "status", "error",
                "message", "User not found"
            ));
        }
        user.setActive(false);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "User " + userId + " access revoked"
        ));
    }

}

