import java.sql.ResultSet;

public class LandUseMgr {
    private static final String DATASET_ID = "d_90d86daa5bfaa371668b84fa5f01424f";
    private static final String LOCALFILEPATH = "dataset/MasterPlan2019LandUselayer.geojson";
    
    private static LandUseMgr instance;
    private LandUseGeoJsonDownloader landUseGJDownloader;
    private LandUseGeoJsonImporter landUseDbImporter;
    private LandUseSQLCreator landUseSQLCreator;
    private LandUseCheckDevelopment landUseChkDev;

    public static void main(String[] args) {
        Coordinate coords = new Coordinate(1.2684017921823554,103.8073577035617);
        LandUseMgr.getInstance().checkProxNewDev(coords, 600);
        //LandUseMgr.getInstance().importGeoJsonToSQLDb();
    }

    private LandUseMgr() {
        landUseDbImporter = new LandUseGeoJsonImporter();
        landUseSQLCreator = new LandUseSQLCreator();
        landUseChkDev = new LandUseCheckDevelopment();
        landUseGJDownloader = new LandUseGeoJsonDownloader();
    }

    public static LandUseMgr getInstance() {
        if (instance == null) {
            instance = new LandUseMgr();
        }
        return instance;
    }

    public void downloadGeoJson() {
        landUseGJDownloader.downloadGeoJson(DATASET_ID, LOCALFILEPATH);
    }

    public void importGeoJsonToSQLDb() {
        landUseDbImporter.importGeoJsonToSQLDb(LOCALFILEPATH);
    }

    public void createSQLTable() {
        landUseSQLCreator.createSQLTable();
    }

    public ResultSet checkProxNewDev(Coordinate coords, double distance) {
        return landUseChkDev.checkProxNewDev(coords, distance);
    }

}
