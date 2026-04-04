package habitathero.GeoSpatialAnalysis.src;

public class TransportLineDbMgr {
    private static final String DATASET_ID = "d_222bfc84eb86c7c11994d02f8939da8d";
    private static final String LOCALFILEPATH = "dataset/MasterPlan2019RailLinelayer.geojson";

    private static TransportLineDbMgr instance;
    private TransportLineGeoJsonDownloader tlGJDownloader;
    private TransportLineGeoJsonImporter tlDbImporter;
    private TransportLineSQLCreator tlSQLCreator;
    private DataGovAPIHandler dataGovAPIHandler;

    private TransportLineDbMgr() {
        tlDbImporter = TransportLineGeoJsonImporter.getInstance();
        tlSQLCreator = TransportLineSQLCreator.getInstance();
        tlGJDownloader = TransportLineGeoJsonDownloader.getInstance();
        dataGovAPIHandler = DataGovAPIHandler.getInstance();
    }

    public static TransportLineDbMgr getInstance() {
        if (instance == null) {
            instance = new TransportLineDbMgr();
        }
        return instance;
    }

    public Boolean downloadGeoJson() {
        return tlGJDownloader.downloadGeoJson(DATASET_ID, LOCALFILEPATH);
    }

    public boolean importGeoJsonToSQLDb() {
        return tlDbImporter.importGeoJsonToSQLDb(LOCALFILEPATH);
    }

    public Boolean checkCurrency() {
        return dataGovAPIHandler.checkAPIDataCurrency(DATASET_ID);
    }

    public boolean createSQLTable() {
        return tlSQLCreator.createSQLTable();
    }
}