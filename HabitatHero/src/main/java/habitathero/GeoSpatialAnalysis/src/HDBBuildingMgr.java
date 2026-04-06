package habitathero.GeoSpatialAnalysis.src;

import org.json.JSONObject;

public class HDBBuildingMgr {
    public static HDBBuildingMgr instance;
    private HDBPostalToCoordinate hdbPostalToCoordinate;
    private HDBBuildingSunFacingAnalysis hdbSunFacingAnalysis;
    private HDBBuildingSunFacingResultSQLHandler hdbSunFacingResultSQLHandler;

    public static void main(String[] args) {
        HDBBuildingMgr.getInstance().getSunFacing("670180000");
    }

    // singleton initalization
    private HDBBuildingMgr() {
        hdbPostalToCoordinate = HDBPostalToCoordinate.getInstance();
        hdbSunFacingAnalysis = HDBBuildingSunFacingAnalysis.getInstance();
        hdbSunFacingResultSQLHandler = HDBBuildingSunFacingResultSQLHandler.getInstance();
    }

    public static HDBBuildingMgr getInstance() {
        if (instance == null) {
            instance = new HDBBuildingMgr();
        }
        return instance;
    }

    public JSONObject getSunFacing(String postalCode) {
        System.out.println("Starting sun-facing flow for postal code " + postalCode);
        JSONObject storedResult = hdbSunFacingResultSQLHandler.retrieveSunFacingAnalysis(postalCode);
        if (isUsableStoredResult(storedResult)) {
            System.out.println("Using stored sun-facing result");
            return storedResult;
        }

        JSONObject computedResult = calSunFacing(postalCode);
        if (isInvalidAnalysisResult(computedResult)) {
            return computedResult;
        }
        hdbSunFacingResultSQLHandler.saveSunFacingAnalysis(computedResult);
        return computedResult;
    }

    public JSONObject getSunFacing(String postalCode, double sunAzimuth) {
        System.out.println("Starting sun-facing flow for postal code " + postalCode + " with sun azimuth " + sunAzimuth);
        JSONObject storedResult = hdbSunFacingResultSQLHandler.retrieveSunFacingAnalysis(postalCode);
        double expectedEast = normalizeAzimuth(sunAzimuth);
        double expectedWest = normalizeAzimuth(sunAzimuth + 180.0);
        if (isUsableStoredResult(storedResult) && matchesAzimuths(storedResult, expectedEast, expectedWest)) {
            System.out.println("Using stored sun-facing result");
            return storedResult;
        }

        JSONObject computedResult = calSunFacing(postalCode, sunAzimuth);
        if (isInvalidAnalysisResult(computedResult)) {
            return computedResult;
        }
        hdbSunFacingResultSQLHandler.saveSunFacingAnalysis(computedResult);
        return computedResult;
    }

    public JSONObject getSunFacing(String postalCode, double eastAzimuth, double westAzimuth) {
        System.out.println("Starting sun-facing flow for postal code " + postalCode + " with east azimuth " + eastAzimuth + " and west azimuth " + westAzimuth);
        JSONObject storedResult = hdbSunFacingResultSQLHandler.retrieveSunFacingAnalysis(postalCode);
        double expectedEast = normalizeAzimuth(eastAzimuth);
        double expectedWest = normalizeAzimuth(westAzimuth);
        if (isUsableStoredResult(storedResult) && matchesAzimuths(storedResult, expectedEast, expectedWest)) {
            System.out.println("Using stored sun-facing result");
            return storedResult;
        }

        JSONObject computedResult = calSunFacing(postalCode, eastAzimuth, westAzimuth);
        if (isInvalidAnalysisResult(computedResult)) {
            return computedResult;
        }
        hdbSunFacingResultSQLHandler.saveSunFacingAnalysis(computedResult);
        return computedResult;
    }

    public Coordinate postalCodeToCoordinate(String postalCode) {
        return hdbPostalToCoordinate.postalToCoordinate(postalCode);
    }

    private JSONObject calSunFacing(String postalCode) {
        return hdbSunFacingAnalysis.calSunFacing(postalCode);
    }

    private JSONObject calSunFacing(String postalCode, double sunAzimuth) {
        return hdbSunFacingAnalysis.calSunFacing(postalCode, sunAzimuth);
    }

    private JSONObject calSunFacing(String postalCode, double eastAzimuth, double westAzimuth) {
        return hdbSunFacingAnalysis.calSunFacing(postalCode, eastAzimuth, westAzimuth);
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

    private boolean matchesAzimuths(JSONObject result, double expectedEast, double expectedWest) {
        if (!result.has("eastAzimuth") || !result.has("westAzimuth")) {
            return false;
        }
        double storedEast = normalizeAzimuth(result.optDouble("eastAzimuth", Double.NaN));
        double storedWest = normalizeAzimuth(result.optDouble("westAzimuth", Double.NaN));
        return Double.isFinite(storedEast) && Double.isFinite(storedWest)
                && Math.abs(storedEast - expectedEast) < 1e-9
                && Math.abs(storedWest - expectedWest) < 1e-9;
    }

    private double normalizeAzimuth(double azimuth) {
        double normalized = azimuth % 360.0;
        if (normalized < 0) {
            normalized += 360.0;
        }
        return normalized;
    }

}
