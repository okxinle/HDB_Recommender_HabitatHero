package habitathero.GeoSpatialAnalysis.src;

import java.sql.ResultSet;
import org.json.JSONObject;

public class TransportLineMgr {
    private static final String DATASET_ID = "d_222bfc84eb86c7c11994d02f8939da8d";
    private static final String LOCALFILEPATH = "dataset/MasterPlan2019RailLinelayer.geojson";
    
    private static TransportLineMgr instance;
    private TransportLineGeoJsonDownloader tlGJDownloader;
    private TransportLineGeoJsonImporter tlDbImporter;
    private TransportLineSQLCreator tlSQLCreator;
    private TransportLineCalMinDist tlCalMinDist;
    private TransportLineCalNoiseLevel tlCalNoiseLevel;

    public static void main(String[] args) {
        TransportLineMgr tlMgr = TransportLineMgr.getInstance();
        tlMgr.calNoiseLevel("670180");
        
    }

    // singleton initalization of TransportLineMgr
    private TransportLineMgr() {
        tlDbImporter = new TransportLineGeoJsonImporter();
        tlSQLCreator = new TransportLineSQLCreator();
        tlCalMinDist = new TransportLineCalMinDist();
        tlGJDownloader = new TransportLineGeoJsonDownloader();
        tlCalNoiseLevel = new TransportLineCalNoiseLevel();
    }

    // call to create TransportLineMgr
    public static TransportLineMgr getInstance() {
        if (instance == null) {
            instance = new TransportLineMgr();
        }
        return instance;
    }

    public boolean getAllTransportLineData() {
        return true;
    }

    public void downloadGeoJson() {
        tlGJDownloader.downloadGeoJson(DATASET_ID, LOCALFILEPATH);
    }

    public void importGeoJsonToSQLDb() {
        tlDbImporter.importGeoJsonToSQLDb(LOCALFILEPATH);
    }
    
    public Boolean checkCurrency(){
        return DataGovAPIHandler.getInstance().checkAPIDataCurrency(DATASET_ID);
    }

    public void createSQLTable() {
        tlSQLCreator.createSQLTable();
    }

    public JSONObject calMinDistToLine(Coordinate coords) {
        return tlCalMinDist.calMinDist(coords);
    }

    public JSONObject calMinDistToLine(String postalCode) {
        return tlCalMinDist.calMinDist(postalCode);
    }

    public JSONObject calNoiseLevel(Coordinate coords){
        JSONObject minDistResult = calMinDistToLine(coords);
        return tlCalNoiseLevel.calNoiseLevel(minDistResult);
    }

    public JSONObject calNoiseLevel(String postalCode){
        JSONObject minDistResult = calMinDistToLine(postalCode);
        return tlCalNoiseLevel.calNoiseLevel(minDistResult);
    }

    public JSONObject calMinDistToLine(Coordinate coords, double radius) {
        return tlCalMinDist.calMinDist(coords, radius);
    }

    public JSONObject calMinDistToLine(String postalCode, double radius) {
        return tlCalMinDist.calMinDist(postalCode, radius);
    }

    public JSONObject calNoiseLevel(Coordinate coords, double radius){
        JSONObject minDistResult = calMinDistToLine(coords, radius);
        return tlCalNoiseLevel.calNoiseLevel(minDistResult);
    }

    public JSONObject calNoiseLevel(String postalCode, double radius){
        JSONObject minDistResult = calMinDistToLine(postalCode, radius);
        return tlCalNoiseLevel.calNoiseLevel(minDistResult);
    }

}
