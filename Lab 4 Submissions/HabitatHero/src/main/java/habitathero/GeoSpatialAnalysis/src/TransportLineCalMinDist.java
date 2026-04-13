package habitathero.GeoSpatialAnalysis.src;

import org.json.JSONObject;

public class TransportLineCalMinDist {
    private static TransportLineCalMinDist instance;
    private TransportLineSQLHandler transportLineSQLHandler;

    // TransportLineMgr singleton call this class constructor only once
    private TransportLineCalMinDist() {
        transportLineSQLHandler = TransportLineSQLHandler.getInstance();
    }

    public static TransportLineCalMinDist getInstance(){
        if(instance ==null){
            instance = new TransportLineCalMinDist();
        }
        return instance;
    }

    public JSONObject calMinDist(String postalCode){
        return transportLineSQLHandler.calMinDist(postalCode);
    }

    public JSONObject calMinDist(String postalCode, double radius){
        return transportLineSQLHandler.calMinDist(postalCode, radius);
    }

}
