import org.json.JSONObject;

public class MainSpatialMgr {

    public static void main(String[] args) {
        MainSpatialMgr mgr = new MainSpatialMgr();
        Coordinate coords = mgr.postalCodeToCoordinate("670180");
        System.out.printf("Coordinate: %f, %f\n", coords.getLatitude(), coords.getLongitude());
    }

    public JSONObject calNoiseLevel(String postalCode) {
        // convert postal code to coordinate
        Coordinate coords = this.postalCodeToCoordinate(postalCode);
        return this.calNoiseLevel(coords);
    }

    public JSONObject calNoiseLevel(Coordinate coords){
        return TransportLineMgr.getInstance().calNoiseLevel(coords);
    }

    public JSONObject calSunFacing(String postal_code) {
        // Get complete sun facing analysis with all metrics
        return HDBBuildingMgr.getInstance().calSunFacing(postal_code);
    }

    public JSONObject calSunFacing(String postal_code, double customAzimuth) {
        // Get sun facing analysis for custom azimuth
        return HDBBuildingMgr.getInstance().calSunFacing(postal_code,customAzimuth);
    }

    public JSONObject calSunFacing(String postalCode, double eastAzimuth, double westAzimuth) {
        // Get sun facing analysis for two custom azimuth
        return HDBBuildingMgr.getInstance().calSunFacing(postalCode, eastAzimuth, westAzimuth);
    }

    public JSONObject calFutureDevelopmentRisk(Coordinate coords, double distance) {
        return LandUseMgr.getInstance().calFutureDevRisk(coords, distance);
    }

    public JSONObject calFutureDevelopmentRisk(String postalCode, double distance){
        // convert postal code to coordinate
        return LandUseMgr.getInstance().calFutureDevRisk(postalCode, distance);
    }

    //only used internally within class
    private Coordinate postalCodeToCoordinate(String postal_code) {
        return HDBBuildingMgr.getInstance().postalCodeToCoordinate(postal_code);
    }

    // Comprehensive analysis integrating all managers and analyzers
    public JSONObject getComprehensiveLocationAnalysis(String postal_code) {
        JSONObject comprehensive = new JSONObject();
        
        try {
            // Building info
            comprehensive.put("postalCode", postal_code);
            Coordinate coords = postalCodeToCoordinate(postal_code);
            comprehensive.put("latitude", coords.getLatitude());
            comprehensive.put("longitude", coords.getLongitude());
            
            // Noise level analysis (TransportLineMgr integration)
            JSONObject noiseResult = calNoiseLevel(postal_code);
            if (noiseResult.has("error")) {
                comprehensive.put("noiseLevel_dBA", "Error: " + noiseResult.getString("error"));
            } else {
                comprehensive.put("noiseLevel_dBA", noiseResult.optDouble("noise_level_db", 0.0));
            }
            
            // Sun exposure analysis (HDBBuildingSunFacingAnalysis integration)
            JSONObject sunAnalysis = calSunFacing(postal_code);
            comprehensive.put("sunExposure", sunAnalysis);
            
            // Future development risk (LandUseMgr integration)
            JSONObject devRisk = calFutureDevelopmentRisk(postal_code, 500.0);  // 500m radius
            comprehensive.put("futureDevRisk_500m", devRisk);
            
            comprehensive.put("status", "OK");
            
        } catch (Exception e) {
            comprehensive.put("status", "ERROR");
            comprehensive.put("error", e.getMessage());
            e.printStackTrace();
        }
        
        return comprehensive;
    }

}
