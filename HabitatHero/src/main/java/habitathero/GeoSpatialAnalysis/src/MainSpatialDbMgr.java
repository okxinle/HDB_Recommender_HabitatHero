package habitathero.GeoSpatialAnalysis.src;

public class MainSpatialDbMgr {
    private static MainSpatialDbMgr instance;
    private HDBBuildingDbMgr hdbBuildingDbMgr;
    private LandUseDbMgr landUseDbMgr;
    private TransportLineDbMgr transportLineDbMgr;
    private HDBBuildingSunFacingResultSQLHandler hdbBuildingSunFacingResultSQLHandler;
    private LandUseFutureDevRiskResultSQLHandler landUseFutureDevRiskResultSQLHandler;
    private TransportLineCalResultSQLHandler transportLineCalResultSQLHandler;
    private DataGovMetadataMgr dataGovMetadataMgr;

    private MainSpatialDbMgr() {
        this.hdbBuildingDbMgr = HDBBuildingDbMgr.getInstance();
        this.landUseDbMgr = LandUseDbMgr.getInstance();
        this.transportLineDbMgr = TransportLineDbMgr.getInstance();
        this.hdbBuildingSunFacingResultSQLHandler = HDBBuildingSunFacingResultSQLHandler.getInstance();
        this.landUseFutureDevRiskResultSQLHandler = LandUseFutureDevRiskResultSQLHandler.getInstance();
        this.transportLineCalResultSQLHandler = TransportLineCalResultSQLHandler.getInstance();
        this.dataGovMetadataMgr = DataGovMetadataMgr.getInstance();
    }

    /**
     * Returns the singleton instance for centralized spatial database operations.
     *
     * @return singleton MainSpatialDbMgr instance
     */
    public static MainSpatialDbMgr getInstance() {
        if (instance == null) {
            instance = new MainSpatialDbMgr();
        }
        return instance;
    }

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
            createHDBBuildingSunFacingResultTable();
            createLandUseFutureDevRiskResultTable();
            createTransportLineCalResultTable();
            createMetadataTable();

            System.out.println("[SUCCESS] All database tables initialized successfully.");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to initialize database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates the SQL table for HDB building spatial data.
     */
    public void createHDBBuildingTable() {
        System.out.println("[INIT] Creating HDB_Building table...");
        hdbBuildingDbMgr.createSQLTable();
    }

    /**
     * Creates the SQL table for land use spatial data.
     */
    public void createLandUseTable() {
        System.out.println("[INIT] Creating LandUse table...");
        landUseDbMgr.createSQLTable();
    }

    /**
     * Creates the SQL table for transport line spatial data.
     */
    public void createTransportLineTable() {
        System.out.println("[INIT] Creating TransportLine table...");
        transportLineDbMgr.createSQLTable();
    }

    /**
     * Creates the SQL table for transport line calculation results.
     */
    public void createTransportLineCalResultTable() {
        System.out.println("[INIT] Creating TransportLine Cal Result table...");
        transportLineCalResultSQLHandler.createSQLTable();
    }

    /**
     * Creates the SQL table for HDB building sun-facing results.
     */
    public void createHDBBuildingSunFacingResultTable() {
        System.out.println("[INIT] Creating HDB Building Sun Facing Result table...");
        hdbBuildingSunFacingResultSQLHandler.createSQLTable();
    }

    /**
     * Creates the SQL table for future development risk results.
     */
    public void createLandUseFutureDevRiskResultTable() {
        System.out.println("[INIT] Creating LandUse Future Dev Risk Result table...");
        landUseFutureDevRiskResultSQLHandler.createSQLTable();
    }

    /**
     * Creates the SQL table for DataGov metadata.
     */
    public void createMetadataTable() {
        System.out.println("[INIT] Creating Metadata table...");
        dataGovMetadataMgr.createSQLTable();
    }

    // ============ DATA IMPORT OPERATIONS ============

    /**
     * Import all GeoJSON datasets from their respective files
     */
    public void importAllGeoJsonDatasets() {
        System.out.println("[IMPORT] Starting data import operations...");

        try {
            importHDBBuildingGeoJson();
            importLandUseGeoJson();
            importTransportLineGeoJson();

            System.out.println("[SUCCESS] All GeoJSON datasets imported successfully.");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to import GeoJSON datasets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Imports HDB building GeoJSON data into SQL storage.
     */
    public void importHDBBuildingGeoJson() {
        System.out.println("[IMPORT] Importing HDB Building GeoJSON...");
        hdbBuildingDbMgr.importGeoJsonToSQLDb();
    }

    /**
     * Imports land use GeoJSON data into SQL storage.
     */
    public void importLandUseGeoJson() {
        System.out.println("[IMPORT] Importing LandUse GeoJSON...");
        landUseDbMgr.importGeoJsonToSQLDb();
    }

    /**
     * Imports transport line GeoJSON data into SQL storage.
     */
    public void importTransportLineGeoJson() {
        System.out.println("[IMPORT] Importing TransportLine GeoJSON...");
        transportLineDbMgr.importGeoJsonToSQLDb();
    }

    // ============ DATA DOWNLOAD OPERATIONS ============

    /**
     * Download all GeoJSON datasets from DataGov API, include prior data currency
     * checking before download
     */
    public void downloadAllGeoJsonDatasets() {
        System.out.println("[DOWNLOAD] Starting data download operations...");

        try {
            downloadHDBBuildingGeoJson();
            downloadLandUseGeoJson();
            downloadTransportLineGeoJson();

            System.out.println("[SUCCESS] All GeoJSON datasets downloaded successfully.");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to download GeoJSON datasets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Downloads the HDB building GeoJSON dataset.
     */
    public void downloadHDBBuildingGeoJson() {
        System.out.println("[DOWNLOAD] Downloading HDB Building GeoJSON...");
        hdbBuildingDbMgr.downloadGeoJson();
    }

    /**
     * Downloads the land use GeoJSON dataset.
     */
    public void downloadLandUseGeoJson() {
        System.out.println("[DOWNLOAD] Downloading LandUse GeoJSON...");
        landUseDbMgr.downloadGeoJson();
    }

    /**
     * Downloads the transport line GeoJSON dataset.
     */
    public void downloadTransportLineGeoJson() {
        System.out.println("[DOWNLOAD] Downloading TransportLine GeoJSON...");
        transportLineDbMgr.downloadGeoJson();
    }

    /**
     * Force downloads the HDB building GeoJSON dataset.
     */
    public void forceDownloadHDBBuildingGeoJson() {
        System.out.println("[FORCE-DOWNLOAD] Downloading HDB Building GeoJSON...");
        hdbBuildingDbMgr.forceDownloadGeoJson();
    }

    /**
     * Force downloads the land use GeoJSON dataset.
     */
    public void forceDownloadLandUseGeoJson() {
        System.out.println("[FORCE-DOWNLOAD] Downloading LandUse GeoJSON...");
        landUseDbMgr.forceDownloadGeoJson();
    }

    /**
     * Force downloads the transport line GeoJSON dataset.
     */
    public void forceDownloadTransportLineGeoJson() {
        System.out.println("[FORCE-DOWNLOAD] Downloading TransportLine GeoJSON...");
        transportLineDbMgr.forceDownloadGeoJson();
    }

    /**
     * Force downloads all GeoJSON datasets from DataGov API.
     */
    public void forceDownloadAllGeoJsonDatasets() {
        System.out.println("[FORCE-DOWNLOAD] Starting forced download operations...");

        try {
            forceDownloadHDBBuildingGeoJson();
            forceDownloadLandUseGeoJson();
            forceDownloadTransportLineGeoJson();

            System.out.println("[SUCCESS] All GeoJSON datasets force downloaded successfully.");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to force download GeoJSON datasets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============ COMPREHENSIVE WORKFLOWS ============

    /**
     * Complete setup: create tables, download latest datasets, then import data.
     */
    public void setupDatabase() {
        System.out.println("\n===== DATABASE SETUP START =====");

        initializeAllDatabaseTables();
        downloadAllGeoJsonDatasets();
        importAllGeoJsonDatasets();

        System.out.println("===== DATABASE SETUP COMPLETE =====\n");
    }

    /**
     * Refresh data from DataGov API: check for updates, download if needed, and
     * re-import
     */
    public void refreshAllGeoJsonDataFromAPI() {
        System.out.println("\n===== REFRESHING DATA FROM DATAGOV API =====");

        downloadAllGeoJsonDatasets();
        importAllGeoJsonDatasets();

        System.out.println("\n===== API REFRESH COMPLETE =====\n");
    }

    /**
     * Force refresh data from DataGov API: bypass checks, force download all,
     * then re-import
     */
    public void forceRefreshAllGeoJsonDataFromAPI() {
        System.out.println("\n===== FORCE REFRESHING DATA FROM DATAGOV API =====");

        try {
            forceDownloadAllGeoJsonDatasets();

            importAllGeoJsonDatasets();
            System.out.println("\n===== API FORCE REFRESH COMPLETE =====\n");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to force refresh GeoJSON datasets: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
