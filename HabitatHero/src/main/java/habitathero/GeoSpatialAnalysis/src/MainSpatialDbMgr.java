package habitathero.GeoSpatialAnalysis.src;

public class MainSpatialDbMgr {

    // ============ TABLE CREATION OPERATIONS ============

    /**
     * Initialize all required database tables
     * Creates: HDB_Building, LandUse, TransportLine, and metadata tables
     */
    public void initializeAllDatabaseTables() {
        System.out.println("[INIT] Starting database table initialization...");

        try {
            createHDBBuildingTable();
            createLandUseTable();
            createTransportLineTable();
            createMetadataTable();

            System.out.println("[SUCCESS] All database tables initialized successfully.");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to initialize database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void createHDBBuildingTable() {
        System.out.println("[INIT] Creating HDB_Building table...");
        HDBBuildingMgr hdbMgr = HDBBuildingMgr.getInstance();
        hdbMgr.createSQLTable();
    }

    public void createLandUseTable() {
        System.out.println("[INIT] Creating LandUse table...");
        LandUseMgr landUseMgr = LandUseMgr.getInstance();
        landUseMgr.createSQLTable();
    }

    public void createTransportLineTable() {
        System.out.println("[INIT] Creating TransportLine table...");
        TransportLineMgr transportMgr = TransportLineMgr.getInstance();
        transportMgr.createSQLTable();
    }

    public void createMetadataTable() {
        System.out.println("[INIT] Creating Metadata table...");
        DataGovMetadataMgr metadataMgr = DataGovMetadataMgr.getInstance();
        metadataMgr.createSQLTable();
    }

    // ============ DATA IMPORT OPERATIONS ============

    /**
     * Import all GeoJSON datasets from their respective files
     */
    public void importAllGeoJsonDatasets() {
        System.out.println("[IMPORT] Starting data import operations...");

        try {
            importHDBBuildingGeoJson("dataset/HDBExistingBuilding.geojson");
            importLandUseGeoJson("dataset/MasterPlan2019LandUselayer.geojson");
            importTransportLineGeoJson("dataset/MasterPlan2019RailLinelayer.geojson");

            System.out.println("[SUCCESS] All GeoJSON datasets imported successfully.");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to import GeoJSON datasets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void importHDBBuildingGeoJson(String filePath) {
        System.out.println("[IMPORT] Importing HDB Building GeoJSON from: " + filePath);
        HDBBuildingMgr hdbMgr = HDBBuildingMgr.getInstance();
        hdbMgr.importGeoJsonToSQLDb();
    }

    public void importLandUseGeoJson(String filePath) {
        System.out.println("[IMPORT] Importing LandUse GeoJSON from: " + filePath);
        LandUseMgr landUseMgr = LandUseMgr.getInstance();
        landUseMgr.importGeoJsonToSQLDb();
    }

    public void importTransportLineGeoJson(String filePath) {
        System.out.println("[IMPORT] Importing TransportLine GeoJSON from: " + filePath);
        TransportLineMgr transportMgr = TransportLineMgr.getInstance();
        transportMgr.importGeoJsonToSQLDb();
    }

    // ============ DATA GOV API OPERATIONS (Download & Update) ============

    /**
     * Download dataset from DataGov API and save to local file
     * Checks for updates and only downloads if newer data is available
     */
    public boolean downloadDatasetFromAPI(String datasetId, String localFilePath) {
        System.out.println("[API] Downloading dataset " + datasetId + " from DataGov API...");

        try {
            Boolean result = DataGovAPIHandler.getInstance().pollDownloadAndSave(datasetId, localFilePath);

            if (result != null && result) {
                System.out.println("[SUCCESS] Dataset " + datasetId + " downloaded and saved to: " + localFilePath);
                return true;
            } else {
                System.out.println("[INFO] Dataset " + datasetId + " is up-to-date, no download needed.");
                return false;
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to download dataset " + datasetId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Force download dataset from DataGov API, ignoring update checks
     * Always downloads regardless of local data currency
     */
    public boolean forceDownloadDatasetFromAPI(String datasetId, String localFilePath) {
        System.out.println("[API] Force downloading dataset " + datasetId + " from DataGov API...");

        try {
            Boolean result = DataGovAPIHandler.getInstance().pollForcedDownloadAndSave(datasetId, localFilePath);

            if (result != null && result) {
                System.out
                        .println("[SUCCESS] Dataset " + datasetId + " force downloaded and saved to: " + localFilePath);
                return true;
            } else {
                System.out.println("[ERROR] Failed to force download dataset " + datasetId);
                return false;
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Exception during force download of " + datasetId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if DataGov API has newer data than what's stored locally
     */
    public boolean isDatasetOutdated(String datasetId) {
        System.out.println("[API] Checking if dataset " + datasetId + " is outdated...");

        try {
            Boolean isCurrent = DataGovAPIHandler.getInstance().checkAPIDataCurrency(datasetId);

            if (isCurrent == null) {
                System.out.println("[INFO] Could not determine currency status for dataset " + datasetId);
                return false;
            }

            boolean isOutdated = !isCurrent;
            String status = isOutdated ? "OUTDATED" : "CURRENT";
            System.out.println("[INFO] Dataset " + datasetId + " is " + status);

            return isOutdated;
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to check API currency for " + datasetId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ============ COMPREHENSIVE WORKFLOWS ============

    /**
     * Complete initial setup: create tables + import all data from local GeoJSON
     * files
     */
    public void setupDatabaseFromLocalFiles() {
        System.out.println("\n===== INITIAL DATABASE SETUP FROM LOCAL FILES =====");
        initializeAllDatabaseTables();
        importAllGeoJsonDatasets();
        System.out.println("===== SETUP COMPLETE =====\n");
    }

    /**
     * Refresh data from DataGov API: check for updates, download if needed, and
     * re-import
     */
    public void refreshAllGeoJsonDataFromAPI() {
        System.out.println("\n===== REFRESHING DATA FROM DATAGOV API =====");

        // Refresh HDB Building data
        refreshHDBBuildingGeoJsonFromAPI();

        // Refresh Land Use data
        refreshLandUseGeoJsonFromAPI();

        // Refresh Transport Line data
        refreshTransportLineGeoJsonFromAPI();

        System.out.println("\n===== API REFRESH COMPLETE =====\n");
    }

    /**
     * Check and refresh HDB Building data from API
     */
    private void refreshHDBBuildingGeoJsonFromAPI() {
        System.out.println("\n--- Processing HDB Building ---");
        HDBBuildingMgr hdbMgr = HDBBuildingMgr.getInstance();

        try {
            hdbMgr.downloadGeoJson();
            System.out.println("[IMPORT] Re-importing HDB Building data after download...");
            hdbMgr.importGeoJsonToSQLDb();
            System.out.println("[SUCCESS] HDB Building data refreshed");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to refresh HDB Building: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check and refresh Land Use data from API
     */
    private void refreshLandUseGeoJsonFromAPI() {
        System.out.println("\n--- Processing Land Use ---");
        LandUseMgr landUseMgr = LandUseMgr.getInstance();

        try {
            landUseMgr.downloadGeoJson();
            System.out.println("[IMPORT] Re-importing Land Use data after download...");
            landUseMgr.importGeoJsonToSQLDb();
            System.out.println("[SUCCESS] Land Use data refreshed");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to refresh Land Use: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check and refresh Transport Line data from API
     */
    private void refreshTransportLineGeoJsonFromAPI() {
        System.out.println("\n--- Processing Transport Line ---");
        TransportLineMgr transportMgr = TransportLineMgr.getInstance();

        try {
            System.out.println("[API] Transport Line data is outdated, downloading...");
            transportMgr.downloadGeoJson();
            System.out.println("[IMPORT] Re-importing Transport Line data after download...");
            transportMgr.importGeoJsonToSQLDb();
            System.out.println("[SUCCESS] Transport Line data refreshed");

        } catch (Exception e) {
            System.out.println("[ERROR] Failed to refresh Transport Line: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Comprehensive setup and refresh: tables + import from files + check API for
     * updates
     */
    public void setupAndSyncDatabase() {
        System.out.println("\n===== COMPREHENSIVE DATABASE SETUP AND SYNC =====");
        refreshAllGeoJsonDataFromAPI(); //download all GeoJson data
        setupDatabaseFromLocalFiles(); //
        System.out.println("===== COMPREHENSIVE SETUP COMPLETE =====\n");
    }

}
