<<<<<<< HEAD
package habitathero.GeoSpatialAnalysis.src;

import java.sql.ResultSet;
=======
import org.json.JSONObject;
>>>>>>> d70a765e53bb95c730ec303d0b194b9572c4d634

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
        // tlMgr.importGeoJsonToSQLDb();
        tlMgr.createSQLTable();
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

    public JSONObject calNoiseLevel(Coordinate coords){
        JSONObject minDistResult = calMinDistToLine(coords);
        return tlCalNoiseLevel.calNoiseLevel(minDistResult);
    }
}

