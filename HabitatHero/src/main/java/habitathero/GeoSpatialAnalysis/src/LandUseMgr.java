package habitathero.GeoSpatialAnalysis.src;

import org.json.JSONObject;

public class LandUseMgr {
    private static LandUseMgr instance;
    private LandUseCheckDevelopment landUseChkDev;

    public static void main(String[] args) {
        //JSONObject result = LandUseMgr.getInstance().getFutureDevRisk(670180, 600);
        //System.out.println("Result: " + result.toString());
    }

    private LandUseMgr() {
        landUseChkDev = LandUseCheckDevelopment.getInstance();
    }

    public static LandUseMgr getInstance() {
        if (instance == null) {
            instance = new LandUseMgr();
        }
        return instance;
    }

    public JSONObject getFutureDevRisk(String postalCode){
        System.out.println("Starting future development risk flow for postal code " + postalCode + " with default distance");
        return calFutureDevRisk(postalCode);
    }

    public JSONObject getFutureDevRisk(String postalCode, double distance){
        System.out.println("Starting future development risk flow for postal code " + postalCode + " with distance " + distance);
        return calFutureDevRisk(postalCode, distance);
    }

    private JSONObject calFutureDevRisk(String postalCode){
        return landUseChkDev.calFutureDevRisk(postalCode);
    }

    private JSONObject calFutureDevRisk(String postalCode, double distance){
        return landUseChkDev.calFutureDevRisk(postalCode, distance);
    }

}
