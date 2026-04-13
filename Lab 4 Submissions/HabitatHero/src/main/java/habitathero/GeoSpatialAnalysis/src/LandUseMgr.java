package habitathero.GeoSpatialAnalysis.src;

import org.json.JSONObject;

public class LandUseMgr {
    private static LandUseMgr instance;
    private LandUseCheckDevelopment landUseChkDev;
    private LandUseFutureDevRiskResultSQLHandler landUseResultSQLHandler;

    private LandUseMgr() {
        landUseChkDev = LandUseCheckDevelopment.getInstance();
        landUseResultSQLHandler = LandUseFutureDevRiskResultSQLHandler.getInstance();
    }

    public static LandUseMgr getInstance() {
        if (instance == null) {
            instance = new LandUseMgr();
        }
        return instance;
    }

    public JSONObject getFutureDevRisk(String postalCode){
        System.out.println("Starting future development risk lookup for postal code " + postalCode);
        JSONObject storedResult = landUseResultSQLHandler.retrieveFutureDevRiskResult(postalCode);
        if (isUsableStoredResult(storedResult)) {
            System.out.println("Using cached future development risk result for postal code " + postalCode);
            return storedResult;
        }

        System.out.println("Cache miss for future development risk result for postal code " + postalCode);
        JSONObject computedResult = calFutureDevRisk(postalCode);
        if (isInvalidAnalysisResult(computedResult)) {
            return computedResult;
        }
        landUseResultSQLHandler.saveFutureDevRiskResult(computedResult);
        return computedResult;
    }

    public JSONObject getFutureDevRisk(String postalCode, double distance){
        System.out.println("Starting future development risk lookup for postal code " + postalCode + " with distance " + distance);
        JSONObject storedResult = landUseResultSQLHandler.retrieveFutureDevRiskResult(postalCode);
        if (isUsableStoredResult(storedResult)) {
            double storedDistance = storedResult.optDouble("search_distance", Double.NaN);
            if (Double.isFinite(storedDistance) && Math.abs(storedDistance - distance) < 1e-9) {
                System.out.println("Using cached future development risk result for postal code " + postalCode + " with distance " + distance);
                return storedResult;
            }
        }

        System.out.println("Cache miss for future development risk result for postal code " + postalCode + " with distance " + distance);
        JSONObject computedResult = calFutureDevRisk(postalCode, distance);
        if (isInvalidAnalysisResult(computedResult)) {
            return computedResult;
        }
        landUseResultSQLHandler.saveFutureDevRiskResult(computedResult);
        return computedResult;
    }

    public JSONObject getFutureDevRiskByCoordinate(double latitude, double longitude, double distance) {
        System.out.println("Starting future development risk lookup for coordinates ("
                + latitude + ", " + longitude + ") with distance " + distance);
        return landUseChkDev.calFutureDevRiskByCoordinate(latitude, longitude, distance);
    }

    private JSONObject calFutureDevRisk(String postalCode){
        return landUseChkDev.calFutureDevRisk(postalCode);
    }

    private JSONObject calFutureDevRisk(String postalCode, double distance){
        return landUseChkDev.calFutureDevRisk(postalCode, distance);
    }

    private boolean isUsableStoredResult(JSONObject result) {
        return result != null && !result.isEmpty()
                && "OK".equalsIgnoreCase(result.optString("status", ""));
    }

    private boolean isInvalidAnalysisResult(JSONObject result) {
        if (result == null || result.isEmpty()) {
            return true;
        }

        String status = result.optString("status", "");
        return !"OK".equalsIgnoreCase(status);
    }

}
