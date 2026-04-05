package habitathero.GeoSpatialAnalysis.src;

public class HDBBuildingDbMgr {
    private static final String DATASET_ID = "d_16b157c52ed637edd6ba1232e026258d";
    private static final String LOCALFILEPATH = "dataset/HDBExistingBuilding.geojson";

    private static HDBBuildingDbMgr instance;
    private HDBBuildingGeoJsonImporter hdbDbImporter;
    private HDBBuildingSQLCreator hdbSQLCreator;
    private HDBBuildingGeoJsonDownloader hdbGJDownloader;
    private DataGovAPIHandler dataGovAPIHandler;

    private HDBBuildingDbMgr() {
        hdbDbImporter = HDBBuildingGeoJsonImporter.getInstance();
        hdbSQLCreator = HDBBuildingSQLCreator.getInstance();
        hdbGJDownloader = HDBBuildingGeoJsonDownloader.getInstance();
        dataGovAPIHandler = DataGovAPIHandler.getInstance();
    }

    public static HDBBuildingDbMgr getInstance() {
        if (instance == null) {
            instance = new HDBBuildingDbMgr();
        }
        return instance;
    }

    public static String getLocalFilePath() {
        return LOCALFILEPATH;
    }


    public boolean forceDownloadGeoJson(){
        return hdbGJDownloader.downloadGeoJson(DATASET_ID, LOCALFILEPATH);
    }


    public Boolean downloadGeoJson() {
        return hdbGJDownloader.downloadGeoJson(DATASET_ID, LOCALFILEPATH);
    }

    public boolean importGeoJsonToSQLDb() {
        return hdbDbImporter.importGeoJsonToSQLDb(LOCALFILEPATH);
    }

    public Boolean checkCurrency() {
        return dataGovAPIHandler.checkAPIDataCurrency(DATASET_ID);
    }

    public boolean createSQLTable() {
        return hdbSQLCreator.createSQLTable();
    }
}