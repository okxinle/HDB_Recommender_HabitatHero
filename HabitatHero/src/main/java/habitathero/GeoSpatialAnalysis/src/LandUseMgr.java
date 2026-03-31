package habitathero.GeoSpatialAnalysis.src;

import java.sql.ResultSet;
import org.json.JSONObject;

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
        JSONObject result = LandUseMgr.getInstance().calFutureDevRisk(coords, 600);
        System.out.println("Result: " + result.toString());
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

    public Boolean checkCurrency(){
        return DataGovAPIHandler.getInstance().checkAPIDataCurrency(DATASET_ID);
    }

    public void createSQLTable() {
        landUseSQLCreator.createSQLTable();
    }

    public JSONObject calFutureDevRisk(Coordinate coords, double distance) {
        return landUseChkDev.calFutureDevRisk(coords, distance);
    }

    public JSONObject calFutureDevRisk(String postalCode, double distance){
        Coordinate coords = HDBBuildingMgr.getInstance().postalCodeToCoordinate(postalCode);
        return this.calFutureDevRisk(coords, distance);
    }

}

