package habitathero.GeoSpatialAnalysis.src;

import org.json.JSONObject;

public class HDBBuildingMgr {
    private static final String DATASET_ID = "d_16b157c52ed637edd6ba1232e026258d";
    private static final String LOCALFILEPATH = "dataset/HDBExistingBuilding.geojson";
    
    public static HDBBuildingMgr instance;
    private HDBBuildingGeoJsonImporter hdbDbImporter;
    private HDBBuildingSQLCreator hdbSQLCreator;
    private HDBPostalToCoordinate hdbPostalToCoordinate;
    private HDBBuildingSunFacingAnalysis hdbSunFacingAnalysis;
    private HDBBuildingGeoJsonDownloader hdbGJDownloader;

    public static void main(String[] args) {
        //HDBBuildingMgr.getInstance().importGeoJsonToSQLDb();
        HDBBuildingMgr.getInstance().downloadGeoJson();
    }

    // singleton initalization
    private HDBBuildingMgr() {
        hdbDbImporter = new HDBBuildingGeoJsonImporter();
        hdbSQLCreator = new HDBBuildingSQLCreator();
        hdbPostalToCoordinate = new HDBPostalToCoordinate();
        hdbGJDownloader = new HDBBuildingGeoJsonDownloader();
        hdbSunFacingAnalysis = new HDBBuildingSunFacingAnalysis();
    }

    public static HDBBuildingMgr getInstance() {
        if (instance == null) {
            instance = new HDBBuildingMgr();
        }
        return instance;
    }

    public void downloadGeoJson() {
        hdbGJDownloader.downloadGeoJson(DATASET_ID, LOCALFILEPATH);
    }

    public void importGeoJsonToSQLDb() {
        hdbDbImporter.importGeoJsonToSQLDb(LOCALFILEPATH);
    }

    public Boolean checkCurrency(){
        return DataGovAPIHandler.getInstance().checkAPIDataCurrency(DATASET_ID);
    }

    public void createSQLTable() {
        hdbSQLCreator.createSQLTable();
    }

    public Coordinate postalCodeToCoordinate(String postalCode) {
        return hdbPostalToCoordinate.postalToCoordinate(postalCode);
    }

    public JSONObject calSunFacing(String postalCode){
        return hdbSunFacingAnalysis.calSunFacing(postalCode);
    }

    public JSONObject calSunFacing(String postalCode, double sunAzimuth) {
        return hdbSunFacingAnalysis.calSunFacing(postalCode, sunAzimuth);
    }

    public JSONObject calSunFacing(String postalCode, double eastAzimuth, double westAzimuth){
        return hdbSunFacingAnalysis.calSunFacing(postalCode, eastAzimuth, westAzimuth);
    }

}
