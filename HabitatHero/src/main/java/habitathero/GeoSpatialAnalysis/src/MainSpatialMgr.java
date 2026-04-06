package habitathero.GeoSpatialAnalysis.src;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainSpatialMgr {
    public static void main(String[] args) {
        MainSpatialMgr mgr = MainSpatialMgr.getInstance();
        Coordinate coords = mgr.postalCodeToCoordinate("670180");
        System.out.printf("Coordinate: %f, %f\n", coords.getLatitude(), coords.getLongitude());
    }
    
    private static MainSpatialMgr instance;
    private HDBBuildingMgr hdbBuildingMgr;
    private TransportLineMgr transportLineMgr;
    private LandUseMgr landUseMgr;

    private MainSpatialMgr() {
        this.hdbBuildingMgr = HDBBuildingMgr.getInstance();
        this.transportLineMgr = TransportLineMgr.getInstance();
        this.landUseMgr = LandUseMgr.getInstance();
    }

    public static MainSpatialMgr getInstance() {
        if (instance == null) {
            instance = new MainSpatialMgr();
        }
        return instance;
    }

    public JSONObject getNoiseLevel(String postalCode) {
        return transportLineMgr.getNoiseLevel(postalCode);
    }

    public JSONObject getNoiseLevel(String postalCode, double radius) {
        return transportLineMgr.getNoiseLevel(postalCode, radius);
    }

    public JSONObject getSunFacing(String postal_code) {
        // Get complete sun facing analysis with all metrics
        return hdbBuildingMgr.getSunFacing(postal_code);
    }

    public JSONObject getSunFacing(String postal_code, double customAzimuth) {
        // Get sun facing analysis for custom azimuth
        return hdbBuildingMgr.getSunFacing(postal_code, customAzimuth);
    }

    public JSONObject getSunFacing(String postalCode, double eastAzimuth, double westAzimuth) {
        // Get sun facing analysis for two custom azimuth
        return hdbBuildingMgr.getSunFacing(postalCode, eastAzimuth, westAzimuth);
    }

    public JSONObject getFutureDevelopmentRisk(String postalCode, double distance){
        return landUseMgr.getFutureDevRisk(postalCode, distance);
    }

    public JSONObject getFutureDevelopmentRisk(String postalCode){
        return landUseMgr.getFutureDevRisk(postalCode);
    }

    //only used internally within class
    private Coordinate postalCodeToCoordinate(String postal_code) {
        return hdbBuildingMgr.postalCodeToCoordinate(postal_code);
    }

    // Comprehensive analysis integrating all managers and analyzers
    public JSONObject getComprehensiveLocationAnalysis(String postal_code) {
        JSONObject comprehensive = new JSONObject();
        JSONArray issues = new JSONArray();
        
        try {
            // Building info
            comprehensive.put("postalCode", postal_code);
            Coordinate coords = postalCodeToCoordinate(postal_code);
            comprehensive.put("latitude", coords.getLatitude());
            comprehensive.put("longitude", coords.getLongitude());
            
            // Noise level analysis (TransportLineMgr integration)
            JSONObject noiseResult = getNoiseLevel(postal_code);
            if ("ERROR".equalsIgnoreCase(noiseResult.optString("status", ""))) {
                comprehensive.put("noiseLevel_dBA", JSONObject.NULL);
                issues.put("Noise: " + noiseResult.optString("message", "Unknown error"));
            } else {
                comprehensive.put("noiseLevel_dBA", noiseResult.optDouble("noise_level_db", 0.0));
            }
            
            // Sun exposure analysis (HDBBuildingSunFacingAnalysis integration)
            JSONObject sunAnalysis = getSunFacing(postal_code);
            comprehensive.put("sunExposure", sunAnalysis);
            if ("ERROR".equalsIgnoreCase(sunAnalysis.optString("status", ""))) {
                issues.put("Sun exposure: " + sunAnalysis.optString("message", "Unknown error"));
            }
            
            // Future development risk (LandUseMgr integration)
            JSONObject devRisk = getFutureDevelopmentRisk(postal_code, 500.0);  // 500m radius
            comprehensive.put("futureDevRisk_500m", devRisk);
            if ("ERROR".equalsIgnoreCase(devRisk.optString("status", ""))) {
                issues.put("Future development risk: " + devRisk.optString("message", "Unknown error"));
            }
            
            if (issues.length() > 0) {
                comprehensive.put("status", "ERROR");
                comprehensive.put("message", issues.join(" | "));
                comprehensive.put("issues", issues);
            } else {
                comprehensive.put("status", "OK");
                comprehensive.put("message", "NIL");
            }
            
        } catch (Exception e) {
            comprehensive.put("status", "ERROR");
            comprehensive.put("message", e.getMessage());
            e.printStackTrace();
        }
        
        return comprehensive;
    }

}
