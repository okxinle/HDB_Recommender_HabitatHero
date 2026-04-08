package habitathero.boundary;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
import habitathero.repository.GlobalWeightConfigRepository;
import habitathero.repository.UserRepository;
import habitathero.repository.AuditLogRepository;
import habitathero.repository.IHDBRepository;
import habitathero.repository.PoiRepository;

@RestController
@RequestMapping("/api/admin")
public class SystemAdminController {

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
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PoiRepository poiRepository;

    @Autowired
    private GlobalWeightConfigRepository globalWeightConfigRepository;

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
                "message", "hdb_blocks initialized and imported successfully.",
                "downloadedGeoJson", result.downloadedGeoJson(),
                "hdbBuildingRows", result.hdbBuildingRows(),
                "geoJsonPath", result.geoJsonPath(),
                "sourceTable", result.sourceTable()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "hdb_blocks initialization failed: " + e.getMessage()
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
            globalWeightConfigRepository.save(config);
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