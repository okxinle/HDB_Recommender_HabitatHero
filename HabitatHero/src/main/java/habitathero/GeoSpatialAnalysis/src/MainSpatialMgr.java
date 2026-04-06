package habitathero.GeoSpatialAnalysis.src;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainSpatialMgr {
    /**
     * Demo entry point for quick coordinate lookup by postal code.
     *
     * @param args optional CLI args (unused in current flow)
     */
    public static void main(String[] args) {
        MainSpatialMgr mgr = MainSpatialMgr.getInstance();
        Coordinate coords = mgr.postalCodeToCoordinate("670180");
        System.out.printf("Coordinate: %f, %f\n", coords.getLatitude(), coords.getLongitude());
    }
    
    private static MainSpatialMgr instance;
    private HDBBuildingMgr hdbBuildingMgr;
    private TransportLineMgr transportLineMgr;
    private LandUseMgr landUseMgr;

    /**
     * Initializes manager dependencies used by the spatial facade.
     */
    private MainSpatialMgr() {
        this.hdbBuildingMgr = HDBBuildingMgr.getInstance();
        this.transportLineMgr = TransportLineMgr.getInstance();
        this.landUseMgr = LandUseMgr.getInstance();
    }

    /**
     * Returns the singleton instance of MainSpatialMgr.
     *
     * @return singleton MainSpatialMgr instance
     */
    public static MainSpatialMgr getInstance() {
        if (instance == null) {
            instance = new MainSpatialMgr();
        }
        return instance;
    }

    /**
     * Returns transport noise analysis for a postal code using default radius logic.
     *
     * @param postalCode target postal code
     * @return JSONObject fields:
     *         status: OK|ERROR
     *         message: NIL when status=OK, otherwise error detail
     *         postalCode
     *         noise_level_db
     *         objectId
     *         rail_type
     *         distance_meters
     *         hdb_latitude
     *         hdb_longitude
     *         search_radius (when available)
     *         noiseStatus/noiseMessage/noiseResultJson/createdAt/updatedAt (when loaded from cache)
     */
    public JSONObject getNoiseLevel(String postalCode) {
        return transportLineMgr.getNoiseLevel(postalCode);
    }

    /**
     * Returns transport noise analysis for a postal code with a custom search radius.
     *
     * @param postalCode target postal code
     * @param radius search radius in meters
     * @return JSONObject fields:
     *         status: OK|ERROR
     *         message: NIL when status=OK, otherwise error detail
     *         postalCode
     *         noise_level_db
     *         objectId
     *         rail_type
     *         distance_meters
     *         hdb_latitude
     *         hdb_longitude
     *         search_radius
     *         noiseStatus/noiseMessage/noiseResultJson/createdAt/updatedAt (when loaded from cache)
     */
    public JSONObject getNoiseLevel(String postalCode, double radius) {
        return transportLineMgr.getNoiseLevel(postalCode, radius);
    }

    /**
     * Returns default sun-facing analysis for a postal code.
     *
     * @param postal_code target postal code
     * @return JSONObject fields:
     *         status: OK|ERROR
     *         message: NIL when status=OK, otherwise error detail
     *         postalCode
     *         perimeter
     *         eastAzimuth
     *         westAzimuth
     *         eastScore
     *         westScore
     *         eastRatio
     *         westRatio
     *         dominant
     *         sunlightIndex
     *         sunlightAverage
     *         sunlightSteps
     *         absoluteMinScore
     *         absoluteMaxScore
     *         eastScoreRelativeExposurePct
     *         westScoreRelativeExposurePct
     *         sunlightIndexRelativeExposurePct
     *         createdAt (when loaded from cache)
     */
    public JSONObject getSunFacing(String postal_code) {
        return hdbBuildingMgr.getSunFacing(postal_code);
    }

    /**
     * Returns sun-facing analysis using one custom azimuth.
     *
     * @param postal_code target postal code
     * @param customAzimuth base azimuth; opposite side is computed as +180 deg
     * @return JSONObject fields:
     *         all fields documented in getSunFacing(postal_code), plus:
     *         sunAzimuth
     *         sunAzimuthOpposite
     */
    public JSONObject getSunFacing(String postal_code, double customAzimuth) {
        return hdbBuildingMgr.getSunFacing(postal_code, customAzimuth);
    }

    /**
     * Returns sun-facing analysis using explicit east/west azimuths.
     *
     * @param postalCode target postal code
     * @param eastAzimuth custom east-facing azimuth
     * @param westAzimuth custom west-facing azimuth
     * @return JSONObject fields:
     *         same fields documented in getSunFacing(postal_code)
     */
    public JSONObject getSunFacing(String postalCode, double eastAzimuth, double westAzimuth) {
        return hdbBuildingMgr.getSunFacing(postalCode, eastAzimuth, westAzimuth);
    }

    /**
     * Returns future development risk analysis within a custom distance.
     *
     * @param postalCode target postal code
     * @param distance search radius in meters
     * @return JSONObject fields:
     *         status: OK|ERROR
     *         message: NIL when status=OK, otherwise error detail
     *         postalCode
     *         search_distance
     *         latitude
     *         longitude
     *         developmentCount
     *         developments (JSONArray of {objectId, gpr, distance_meters, geom})
     *         createdAt/updatedAt (when loaded from cache)
     */
    public JSONObject getFutureDevelopmentRisk(String postalCode, double distance){
        return landUseMgr.getFutureDevRisk(postalCode, distance);
    }

    /**
     * Returns future development risk analysis using default distance.
     *
     * @param postalCode target postal code
     * @return JSONObject fields:
     *         same schema as getFutureDevelopmentRisk(postalCode, distance)
     */
    public JSONObject getFutureDevelopmentRisk(String postalCode){
        return landUseMgr.getFutureDevRisk(postalCode);
    }

    /**
     * Resolves a postal code into coordinates.
     *
     * @param postal_code target postal code
     * @return Coordinate with latitude/longitude; invalid lookups may return sentinel values from downstream resolver
     */
    private Coordinate postalCodeToCoordinate(String postal_code) {
        return hdbBuildingMgr.postalCodeToCoordinate(postal_code);
    }

    /**
     * Returns combined location analysis (noise + sun exposure + development risk).
     *
     * @param postal_code target postal code
     * @return JSONObject fields:
     *         status: OK|ERROR
     *         message: NIL when status=OK, otherwise aggregated error text
     *         postalCode
     *         latitude
     *         longitude
     *         noiseLevel_dBA (Double or null on noise error)
     *         sunExposure (JSONObject from getSunFacing)
     *         futureDevRisk_500m (JSONObject from getFutureDevelopmentRisk)
     *         issues (JSONArray, present only when status=ERROR)
     */
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
