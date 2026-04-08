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
            System.out.println("Geometry unavailable. Falling back to neutral sun-facing result for postal code " + postalCode);
            computedResult = buildNeutralSunFacingResult(postalCode, DEFAULT_EAST_AZIMUTH, DEFAULT_WEST_AZIMUTH,
                    "DEFAULT_RESULT_NO_GEOMETRY");
        }
        System.out.println("ATTEMPTING TO SAVE CACHE FOR POSTAL: " + postalCode);
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
            System.out.println("Geometry unavailable. Falling back to neutral sun-facing result for postal code " + postalCode);
            computedResult = buildNeutralSunFacingResult(postalCode, expectedEast, expectedWest,
                    "DEFAULT_RESULT_NO_GEOMETRY");
        }
        System.out.println("ATTEMPTING TO SAVE CACHE FOR POSTAL: " + postalCode);
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
            System.out.println("Geometry unavailable. Falling back to neutral sun-facing result for postal code " + postalCode);
            computedResult = buildNeutralSunFacingResult(postalCode, expectedEast, expectedWest,
                    "DEFAULT_RESULT_NO_GEOMETRY");
        }
        System.out.println("ATTEMPTING TO SAVE CACHE FOR POSTAL: " + postalCode);
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

    private JSONObject buildNeutralSunFacingResult(String postalCode, double eastAzimuth, double westAzimuth, String message) {
        JSONObject neutral = new JSONObject();
        neutral.put("postalCode", postalCode == null ? "" : postalCode);
        neutral.put("status", "OK");
        neutral.put("message", message == null ? "DEFAULT_RESULT_NO_GEOMETRY" : message);
        neutral.put("perimeter", 0.0);
        neutral.put("eastAzimuth", normalizeAzimuth(eastAzimuth));
        neutral.put("westAzimuth", normalizeAzimuth(westAzimuth));
        neutral.put("eastScore", 0.0);
        neutral.put("westScore", 0.0);
        neutral.put("eastRatio", 0.0);
        neutral.put("westRatio", 0.0);
        neutral.put("dominant", "NORTH_SOUTH");
        neutral.put("sunlightIndex", 0.0);
        neutral.put("sunlightAverage", 0.0);
        neutral.put("sunlightSteps", 0);
        neutral.put("absoluteMinScore", 0.0);
        neutral.put("absoluteMaxScore", 0.0);
        neutral.put("eastScoreRelativeExposurePct", 0.0);
        neutral.put("westScoreRelativeExposurePct", 0.0);
        neutral.put("sunlightIndexRelativeExposurePct", 0.0);
        return neutral;
    }

    private static final double DEFAULT_EAST_AZIMUTH = 90.0;
    private static final double DEFAULT_WEST_AZIMUTH = 270.0;

}
