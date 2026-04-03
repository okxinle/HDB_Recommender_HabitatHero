package habitathero.boundary;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import habitathero.control.BackfillCoordinatesService;
import habitathero.control.DataPipelineService;
import habitathero.control.HdbSpatialImportService;
import habitathero.entity.AuditLog;
import habitathero.entity.HDBBlock;
import habitathero.repository.AuditLogRepository;
import habitathero.repository.IHDBRepository;

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
    private DataPipelineService dataPipelineService;

    @Autowired
    private BackfillCoordinatesService backfillCoordinatesService;

    @Autowired
    private HdbSpatialImportService hdbSpatialImportService;

    @Autowired
    private AuditLogRepository auditLogRepository;

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
                "message", "hdb_building initialized and imported successfully.",
                "downloadedGeoJson", result.downloadedGeoJson(),
                "hdbBuildingRows", result.hdbBuildingRows(),
                "geoJsonPath", result.geoJsonPath(),
                "sourceTable", result.sourceTable()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "hdb_building initialization failed: " + e.getMessage()
            ));
        }
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
}