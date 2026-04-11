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
            boolean allSuccessful = true;
            allSuccessful &= createHDBBuildingTable();
            allSuccessful &= createLandUseTable();
            allSuccessful &= createTransportLineTable();
            allSuccessful &= createHDBBuildingSunFacingResultTable();
            allSuccessful &= createLandUseFutureDevRiskResultTable();
            allSuccessful &= createTransportLineCalResultTable();
            allSuccessful &= createMetadataTable();

            if (allSuccessful) {
                System.out.println("[SUCCESS] All database tables initialized successfully.");
            } else {
                System.out.println("[WARN] Database table initialization completed with failures. Check prior logs.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to initialize database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates the SQL table for HDB building spatial data.
     */
    public boolean createHDBBuildingTable() {
        System.out.println("[INIT] Creating HDB_Building table...");
        return hdbBuildingDbMgr.createSQLTable();
    }

    /**
     * Creates the SQL table for land use spatial data.
     */
    public boolean createLandUseTable() {
        System.out.println("[INIT] Creating LandUse table...");
        return landUseDbMgr.createSQLTable();
    }

    /**
     * Creates the SQL table for transport line spatial data.
     */
    public boolean createTransportLineTable() {
        System.out.println("[INIT] Creating TransportLine table...");
        return transportLineDbMgr.createSQLTable();
    }

    /**
     * Creates the SQL table for transport line calculation results.
     */
    public boolean createTransportLineCalResultTable() {
        System.out.println("[INIT] Creating TransportLine Cal Result table...");
        return transportLineCalResultSQLHandler.createSQLTable();
    }

    /**
     * Creates the SQL table for HDB building sun-facing results.
     */
    public boolean createHDBBuildingSunFacingResultTable() {
        System.out.println("[INIT] Creating HDB Building Sun Facing Result table...");
        return hdbBuildingSunFacingResultSQLHandler.createSQLTable();
    }

    /**
     * Creates the SQL table for future development risk results.
     */
    public boolean createLandUseFutureDevRiskResultTable() {
        System.out.println("[INIT] Creating LandUse Future Dev Risk Result table...");
        return landUseFutureDevRiskResultSQLHandler.createSQLTable();
    }

    /**
     * Creates the SQL table for DataGov metadata.
     */
    public boolean createMetadataTable() {
        System.out.println("[INIT] Creating Metadata table...");
        return dataGovMetadataMgr.createSQLTable();
    }

    // ============ DATA IMPORT OPERATIONS ============

    /**
     * Import all GeoJSON datasets from their respective files
     */
    public void importAllGeoJsonDatasets() {
        System.out.println("[IMPORT] Starting data import operations...");

        try {
            boolean allSuccessful = true;
            allSuccessful &= importHDBBuildingGeoJson();
            allSuccessful &= importLandUseGeoJson();
            allSuccessful &= importTransportLineGeoJson();

            if (allSuccessful) {
                System.out.println("[SUCCESS] All GeoJSON datasets imported successfully.");
            } else {
                System.out.println("[WARN] GeoJSON import completed with failures. Check prior logs.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to import GeoJSON datasets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Imports HDB building GeoJSON data into SQL storage.
     */
    public boolean importHDBBuildingGeoJson() {
        System.out.println("[IMPORT] Importing HDB Building GeoJSON...");
        return hdbBuildingDbMgr.importGeoJsonToSQLDb();
    }

    /**
     * Imports land use GeoJSON data into SQL storage.
     */
    public boolean importLandUseGeoJson() {
        System.out.println("[IMPORT] Importing LandUse GeoJSON...");
        return landUseDbMgr.importGeoJsonToSQLDb();
    }

    /**
     * Imports transport line GeoJSON data into SQL storage.
     */
    public boolean importTransportLineGeoJson() {
        System.out.println("[IMPORT] Importing TransportLine GeoJSON...");
        return transportLineDbMgr.importGeoJsonToSQLDb();
    }

    // ============ DATA DOWNLOAD OPERATIONS ============

    /**
     * Download all GeoJSON datasets from DataGov API, include prior data currency
     * checking before download
     */
    public void downloadAllGeoJsonDatasets() {
        System.out.println("[DOWNLOAD] Starting data download operations...");

        try {
            boolean allSuccessful = true;
            allSuccessful &= downloadHDBBuildingGeoJson();
            allSuccessful &= downloadLandUseGeoJson();
            allSuccessful &= downloadTransportLineGeoJson();

            if (allSuccessful) {
                System.out.println("[SUCCESS] All GeoJSON datasets downloaded successfully.");
            } else {
                System.out.println("[WARN] GeoJSON download completed with failures or skips. Check prior logs.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to download GeoJSON datasets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Downloads the HDB building GeoJSON dataset.
     */
    public boolean downloadHDBBuildingGeoJson() {
        System.out.println("[DOWNLOAD] Downloading HDB Building GeoJSON...");
        return Boolean.TRUE.equals(hdbBuildingDbMgr.downloadGeoJson());
    }

    /**
     * Downloads the land use GeoJSON dataset.
     */
    public boolean downloadLandUseGeoJson() {
        System.out.println("[DOWNLOAD] Downloading LandUse GeoJSON...");
        return Boolean.TRUE.equals(landUseDbMgr.downloadGeoJson());
    }

    /**
     * Downloads the transport line GeoJSON dataset.
     */
    public boolean downloadTransportLineGeoJson() {
        System.out.println("[DOWNLOAD] Downloading TransportLine GeoJSON...");
        return Boolean.TRUE.equals(transportLineDbMgr.downloadGeoJson());
    }

    /**
     * Force downloads the HDB building GeoJSON dataset.
     */
    public boolean forceDownloadHDBBuildingGeoJson() {
        System.out.println("[FORCE-DOWNLOAD] Downloading HDB Building GeoJSON...");
        return hdbBuildingDbMgr.forceDownloadGeoJson();
    }

    /**
     * Force downloads the land use GeoJSON dataset.
     */
    public boolean forceDownloadLandUseGeoJson() {
        System.out.println("[FORCE-DOWNLOAD] Downloading LandUse GeoJSON...");
        return landUseDbMgr.forceDownloadGeoJson();
    }

    /**
     * Force downloads the transport line GeoJSON dataset.
     */
    public boolean forceDownloadTransportLineGeoJson() {
        System.out.println("[FORCE-DOWNLOAD] Downloading TransportLine GeoJSON...");
        return transportLineDbMgr.forceDownloadGeoJson();
    }

    /**
     * Force downloads all GeoJSON datasets from DataGov API.
     */
    public void forceDownloadAllGeoJsonDatasets() {
        System.out.println("[FORCE-DOWNLOAD] Starting forced download operations...");

        try {
            boolean allSuccessful = true;
            allSuccessful &= forceDownloadHDBBuildingGeoJson();
            allSuccessful &= forceDownloadLandUseGeoJson();
            allSuccessful &= forceDownloadTransportLineGeoJson();

            if (allSuccessful) {
                System.out.println("[SUCCESS] All GeoJSON datasets force downloaded successfully.");
            } else {
                System.out.println("[WARN] Forced GeoJSON download completed with failures. Check prior logs.");
            }
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

        System.out.println("===== DATABASE SETUP COMPLETE (see stage logs for status) =====\n");
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
