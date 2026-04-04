package habitathero.GeoSpatialAnalysis.src;

import org.json.JSONObject;

public class TransportLineMgr {
    private static TransportLineMgr instance;
    private TransportLineCalMinDist tlCalMinDist;
    private TransportLineCalNoiseLevel tlCalNoiseLevel;
    private TransportLineCalResultSQLHandler tlCalResultSQLHandler;

    public static void main(String[] args) {
        TransportLineMgr tlMgr = TransportLineMgr.getInstance();
        tlMgr.getNoiseLevel("670180");
    }

    // singleton initalization of TransportLineMgr
    private TransportLineMgr() {
        tlCalMinDist = TransportLineCalMinDist.getInstance();
        tlCalNoiseLevel = TransportLineCalNoiseLevel.getInstance();
        tlCalResultSQLHandler = TransportLineCalResultSQLHandler.getInstance();
    }

    // call to create TransportLineMgr
    public static TransportLineMgr getInstance() {
        if (instance == null) {
            instance = new TransportLineMgr();
        }
        return instance;
    }

    public JSONObject getNoiseLevel(String postalCode) {
        System.out.println("Starting noise analysis flow for postal code " + postalCode);
        JSONObject storedResult = tlCalResultSQLHandler.retrieveTransportLineCalResult(postalCode);
        
        if (isUsableStoredResult(storedResult)) {
            System.out.println("Using stored noise result for postal code " + postalCode);
            return storedResult;
        }

        System.out.println("Noise result missing for postal code " + postalCode);
        if (isInvalidPostalCode(postalCode)) {
            return invalidPostalCodeResult(postalCode, null);
        }

        return calNoiseLevel(postalCode);
    }

    public JSONObject getNoiseLevel(String postalCode, double radius) {
        System.out.println("Starting noise analysis flow for postal code " + postalCode + " with radius " + radius);
        JSONObject storedResult = tlCalResultSQLHandler.retrieveTransportLineCalResult(postalCode);

        if (isUsableStoredResult(storedResult) && storedResult.has("search_radius")) {
            
            double storedRadius = storedResult.optDouble("search_radius", Double.NaN);

            if (Double.isFinite(storedRadius) && Math.abs(storedRadius - radius) < 1e-9) {
                System.out.println("Using stored noise result for postal code " + postalCode + " with radius " + radius);
                return storedResult;
            }
        }

        System.out.println("Noise result missing for postal code " + postalCode + " with radius " + radius);
        if (isInvalidPostalCode(postalCode)) {
            return invalidPostalCodeResult(postalCode, radius);
        }

        return calNoiseLevel(postalCode, radius);
    }

    private JSONObject calNoiseLevel(String postalCode) {
        JSONObject minDistResult = calMinDistToLine(postalCode);
        if (isInvalidAnalysisResult(minDistResult)) {
            return minDistResult;
        }

        JSONObject noiseResult = tlCalNoiseLevel.calNoiseLevel(minDistResult);
        if (isInvalidAnalysisResult(noiseResult)) {
            return noiseResult;
        }

        tlCalResultSQLHandler.saveTransportLineCalResult(noiseResult);
        return noiseResult;
    }

    private JSONObject calNoiseLevel(String postalCode, double radius) {
        JSONObject minDistResult = calMinDistToLine(postalCode, radius);
        if (isInvalidAnalysisResult(minDistResult)) {
            return minDistResult;
        }

        JSONObject noiseResult = tlCalNoiseLevel.calNoiseLevel(minDistResult);
        if (isInvalidAnalysisResult(noiseResult)) {
            return noiseResult;
        }

        tlCalResultSQLHandler.saveTransportLineCalResult(noiseResult);
        return noiseResult;
    }

    private JSONObject calMinDistToLine(String postalCode) {
        JSONObject result = tlCalMinDist.calMinDist(postalCode);
        result.put("postalCode", postalCode);
        return result;
    }

    private JSONObject calMinDistToLine(String postalCode, double radius) {
        JSONObject result = tlCalMinDist.calMinDist(postalCode, radius);
        result.put("postalCode", postalCode);
        return result;
    }

    private boolean isUsableStoredResult(JSONObject result) {
        return result != null && !result.isEmpty()
                && !"NOT_FOUND".equalsIgnoreCase(result.optString("status", ""))
                && !"INVALID_INPUT".equalsIgnoreCase(result.optString("status", ""));
    }

    private boolean isInvalidPostalCode(String postalCode) {
        Coordinate coords = HDBBuildingMgr.getInstance().postalCodeToCoordinate(postalCode);
        return coords == null || (coords.getLatitude() == -1 && coords.getLongitude() == -1);
    }

    private JSONObject invalidPostalCodeResult(String postalCode, Double radius) {
        System.out.println("ERROR: Invalid postalCode");
        JSONObject result = new JSONObject();
        result.put("status", "INVALID_INPUT");
        result.put("error", "Invalid postal code: unable to resolve coordinates");
        result.put("postalCode", postalCode == null ? "" : postalCode);
        if (radius != null) {
            result.put("search_radius", radius);
        }
        return result;
    }

    private boolean isInvalidAnalysisResult(JSONObject result) {
        if (result == null || result.isEmpty()) {
            return true;
        }

        String status = result.optString("status", "");
        return "INVALID_INPUT".equalsIgnoreCase(status) || result.has("error");
    }

}
