package habitathero.GeoSpatialAnalysis.src;

import java.sql.ResultSet;

public class TransportLineMgr {
    private static final String DATASET_ID = "d_222bfc84eb86c7c11994d02f8939da8d";
    private static final String LOCALFILEPATH = "dataset/MasterPlan2019RailLinelayer.geojson";
    
    private static TransportLineMgr instance;
    private TransportLineGeoJsonDownloader tlGJDownloader;
    private TransportLineGeoJsonImporter tlDbImporter;
    private TransportLineSQLCreator tlSQLCreator;
    private TransportLineCalMinDist tlCalMinDist;

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

    public void createSQLTable() {
        tlSQLCreator.createSQLTable();
    }

    public ResultSet calMinDistToLine(Coordinate coords) {
        return tlCalMinDist.calMinDist(coords);
    }
}

