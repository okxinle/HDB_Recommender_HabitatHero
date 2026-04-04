package habitathero.GeoSpatialAnalysis.src;

public class LandUseDbMgr {
    private static final String DATASET_ID = "d_90d86daa5bfaa371668b84fa5f01424f";
    private static final String LOCALFILEPATH = "dataset/MasterPlan2019LandUselayer.geojson";

    private static LandUseDbMgr instance;
    private LandUseGeoJsonDownloader landUseGJDownloader;
    private LandUseGeoJsonImporter landUseDbImporter;
    private LandUseSQLCreator landUseSQLCreator;
    private DataGovAPIHandler dataGovAPIHandler;

    private LandUseDbMgr() {
        landUseDbImporter = LandUseGeoJsonImporter.getInstance();
        landUseSQLCreator = LandUseSQLCreator.getInstance();
        landUseGJDownloader = LandUseGeoJsonDownloader.getInstance();
        dataGovAPIHandler = DataGovAPIHandler.getInstance();
    }

    public static LandUseDbMgr getInstance() {
        if (instance == null) {
            instance = new LandUseDbMgr();
        }
        return instance;
    }

    public boolean forceDownloadGeoJson(){
        return landUseGJDownloader.forceDownloadGeoJson(DATASET_ID, LOCALFILEPATH);
    }

    public Boolean downloadGeoJson() {
        return landUseGJDownloader.downloadGeoJson(DATASET_ID, LOCALFILEPATH);
    }

    public boolean importGeoJsonToSQLDb() {
        return landUseDbImporter.importGeoJsonToSQLDb(LOCALFILEPATH);
    }

    public Boolean checkCurrency() {
        return dataGovAPIHandler.checkAPIDataCurrency(DATASET_ID);
    }

    public boolean createSQLTable() {
        return landUseSQLCreator.createSQLTable();
    }
}