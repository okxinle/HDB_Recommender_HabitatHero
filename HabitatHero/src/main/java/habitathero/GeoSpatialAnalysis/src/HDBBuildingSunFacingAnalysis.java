package habitathero.GeoSpatialAnalysis.src;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.JSONArray;
import org.json.JSONObject;

public class HDBBuildingSunFacingAnalysis extends SQLDbConnect {
    private static HDBBuildingSunFacingAnalysis instance;
    private static final double DEFAULT_EAST_AZIMUTH = 90.0;
    private static final double DEFAULT_WEST_AZIMUTH = 270.0;
    private static final double DEFAULT_FULL_SWEEP_STEP_DEGREES = 1.0;
    private static final double DEFAULT_DAY_ARC_STEP_DEGREES = 10.0;
    private static final double MIN_STEP_DEGREES = 0.5;

    private HDBBuildingSunFacingAnalysis() {
        super();
    }

    public static HDBBuildingSunFacingAnalysis getInstance() {
        if (instance == null) {
            instance = new HDBBuildingSunFacingAnalysis();
        }
        return instance;
    }

    public JSONObject calSunFacing(String postalCode) {
        System.out.println("Preparing default azimuth analysis for postal code " + postalCode);
        // default base case uses traditional east / west azimuths
        return calSunFacing(postalCode, DEFAULT_EAST_AZIMUTH, DEFAULT_WEST_AZIMUTH);
    }

    public JSONObject calSunFacing(String postalCode, double sunAzimuth) {
        System.out.println("Preparing sun azimuth analysis for postal code " + postalCode + " with sun azimuth " + sunAzimuth);
        double eastAzimuth = normalizeAzimuth(sunAzimuth);
        double westAzimuth = normalizeAzimuth(sunAzimuth + 180.0);
        JSONObject ringMeta = calSunFacing(postalCode, eastAzimuth, westAzimuth);
        ringMeta.put("sunAzimuth", sunAzimuth);
        ringMeta.put("sunAzimuthOpposite", westAzimuth);
        return ringMeta;
    }

    public JSONObject calSunFacing(String postalCode, double eastAzimuth, double westAzimuth) {
        return calSunFacing(postalCode, eastAzimuth, westAzimuth,
                DEFAULT_FULL_SWEEP_STEP_DEGREES, DEFAULT_DAY_ARC_STEP_DEGREES);
    }

    public JSONObject calSunFacing(String postalCode, double eastAzimuth, double westAzimuth,
            double fullSweepStepDegrees, double dayArcStepDegrees) {
        System.out.println("Running geometry-based sun-facing computation for postal code " + postalCode + " with east azimuth " + eastAzimuth + " and west azimuth " + westAzimuth);
        // Geometry source of truth for this block. All downstream calculations use this polygon.
        JSONObject geomJson = getHDBBuildingGeom(postalCode);
        JSONObject output = new JSONObject();

        double normalizedFullSweepStep = normalizeStep(fullSweepStepDegrees, DEFAULT_FULL_SWEEP_STEP_DEGREES);
        double normalizedDayArcStep = normalizeStep(dayArcStepDegrees, DEFAULT_DAY_ARC_STEP_DEGREES);

        if (isInvalidPostalCode(geomJson)) {
            return invalidPostalCodeResult(postalCode);
        }

        JSONArray coordinates = geomJson.optJSONArray("coordinates");
        if (coordinates == null || coordinates.length() == 0) {
            output.put("status", "ERROR");
            output.put("message", "No coordinates available");
            return output;
        }

        // Outer ring only (Polygon geography). If MultiPolygon, use first polygon outer ring.
        JSONArray ring = getOuterRing(coordinates);
        if (ring == null || ring.length() < 4) {
            output.put("status", "ERROR");
            output.put("message", "Polygon ring must have at least 4 points");
            return output;
        }

        double perimeter = computePerimeter(ring);

        double eastScore = computeSunFacingScore(ring, eastAzimuth);
        double westScore = computeSunFacingScore(ring, westAzimuth);

        output.put("postalCode", postalCode);
        output.put("status", "OK");
        output.put("message", "NIL");
        output.put("perimeter", perimeter);
        output.put("eastAzimuth", eastAzimuth);
        output.put("westAzimuth", westAzimuth);
        output.put("eastScore", eastScore);
        output.put("westScore", westScore);
        output.put("eastRatio", perimeter > 0.0 ? eastScore / perimeter : 0.0);
        output.put("westRatio", perimeter > 0.0 ? westScore / perimeter : 0.0);

        if (eastScore > westScore) {
            output.put("dominant", "EAST");
        } else if (westScore > eastScore) {
            output.put("dominant", "WEST");
        } else {
            output.put("dominant", "BALANCED");
        }

        // Reuse same ring/perimeter to avoid extra DB access and repeated geometry parsing.
        JSONObject fullSweep = calSunFacingRange(ring, perimeter, 0.0, 360.0, normalizedFullSweepStep);
        double minScoreAbsolute = fullSweep.optDouble("minScore", 0.0);
        double maxScoreAbsolute = fullSweep.optDouble("maxScore", 0.0);

        // Daylight arc integration between east and west azimuths.
        JSONObject rangeIndex = calSunFacingRange(ring, perimeter, eastAzimuth, westAzimuth, normalizedDayArcStep);
        double sunlightIndex = rangeIndex.optDouble("sunlightIndex", 0.0);
        double sunAverage = rangeIndex.optDouble("averageScore", 0.0);

        output.put("sunlightIndex", sunlightIndex);
        output.put("sunlightAverage", sunAverage);
        output.put("sunlightSteps", rangeIndex.optInt("steps", 0));
        output.put("absoluteMinScore", minScoreAbsolute);
        output.put("absoluteMaxScore", maxScoreAbsolute);

        // Relative percentages are normalized against absolute best/worst from full 360° sweep.
        // High score means higher direct sun exposure.
        output.put("eastScoreRelativeExposurePct", computePercentFromBest(eastScore, minScoreAbsolute, maxScoreAbsolute));
        output.put("westScoreRelativeExposurePct", computePercentFromBest(westScore, minScoreAbsolute, maxScoreAbsolute));

        double minIdx = perimeter > 0.0 ? minScoreAbsolute / perimeter : 0.0;
        double maxIdx = perimeter > 0.0 ? maxScoreAbsolute / perimeter : 0.0;
        output.put("sunlightIndexRelativeExposurePct", computePercentFromBest(sunlightIndex, minIdx, maxIdx));

        return output;
    }

    // Range integration with step degrees granularity
    private JSONObject calSunFacingRange(String postalCode, double startAzimuth, double endAzimuth, double stepDegrees) {
        JSONObject geomJson = getHDBBuildingGeom(postalCode);
        JSONObject output = new JSONObject();

        if (geomJson == null || geomJson.isEmpty()) {
            output.put("status", "ERROR");
            output.put("message", "Building geometry not found");
            return output;
        }

        JSONArray coordinates = geomJson.optJSONArray("coordinates");
        if (coordinates == null || coordinates.length() == 0) {
            output.put("status", "ERROR");
            output.put("message", "No coordinates available");
            return output;
        }

        JSONArray ring = getOuterRing(coordinates);
        if (ring == null || ring.length() < 4) {
            output.put("status", "ERROR");
            output.put("message", "Polygon ring must have at least 4 points");
            return output;
        }

        double perimeter = computePerimeter(ring);
        if (perimeter <= 0.0) {
            output.put("status", "ERROR");
            output.put("message", "Invalid polygon perimeter");
            return output;
        }

        return calSunFacingRange(ring, perimeter, startAzimuth, endAzimuth, stepDegrees);
    }

    private JSONObject calSunFacingRange(JSONArray ring, double perimeter, double startAzimuth, double endAzimuth, double stepDegrees) {
        JSONObject output = new JSONObject();

        if (ring == null || ring.length() < 4 || perimeter <= 0.0) {
            output.put("status", "ERROR");
            output.put("message", "Invalid polygon geometry");
            return output;
        }

        double sum = 0.0;
        double minScore = Double.POSITIVE_INFINITY;
        double maxScore = Double.NEGATIVE_INFINITY;
        int steps = 0;

        double az = normalizeAzimuth(startAzimuth);
        double end = normalizeAzimuth(endAzimuth);

        // Check if we want a full 360° sweep
        boolean fullCircle = Math.abs(endAzimuth - startAzimuth) >= 359.9;
        
        // support wrapping across 360
        boolean wrap = end < az && !fullCircle;

        while (true) {
            double score = computeSunFacingScore(ring, az);
            sum += score;
            minScore = Math.min(minScore, score);
            maxScore = Math.max(maxScore, score);
            steps++;

            az = normalizeAzimuth(az + stepDegrees);
            
            if (fullCircle && steps >= 360 / stepDegrees) break;
            if (!wrap && !fullCircle && az > end) break;
            if (wrap && ! (az <= end || az >= startAzimuth)) break;
            if (stepDegrees <= 0.0) break; // avoid infinite
        }

        double averageScore = steps > 0 ? sum / steps : 0;
        double sunlightIndex = averageScore / perimeter;

        output.put("status", "OK");
        output.put("message", "NIL");
        output.put("startAzimuth", startAzimuth);
        output.put("endAzimuth", endAzimuth);
        output.put("stepDegrees", stepDegrees);
        output.put("steps", steps);
        output.put("minScore", minScore == Double.POSITIVE_INFINITY ? 0.0 : minScore);
        output.put("maxScore", maxScore == Double.NEGATIVE_INFINITY ? 0.0 : maxScore);
        output.put("averageScore", averageScore);
        output.put("sunlightIndex", sunlightIndex);

        return output;
    }

    private JSONObject getHDBBuildingGeom(String postalCode) {
        System.out.println("Fetching building geometry from database");
        String sql = "SELECT ST_AsGeoJSON(geom) AS geojson FROM hdb_building_dataset WHERE postal_cod = ? LIMIT 1";
        try {
            connectSQL();

            if (conn == null) {
                System.out.println("Unable to obtain DB connection for sun-facing analysis.");
                return null;
            }

            if (!hasGeomColumn()) {
                System.out.println("hdb_building_dataset.geom is unavailable; cannot run polygon sun-facing computation.");
                closeConnection();
                return null;
            }

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, postalCode);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String geojson = rs.getString("geojson");
                rs.close();
                ps.close();
                closeConnection();

                if (geojson == null || geojson.isEmpty()) {
                    return null;
                }
                JSONObject geomObj = new JSONObject(geojson);
                return geomObj;
            } else {
                rs.close();
                ps.close();
                closeConnection();
                return null;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            closeConnection();
            return null;
        }
    }

    private boolean hasGeomColumn() {
        if (conn == null) {
            return false;
        }

        String sql = """
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                                    AND table_name = 'hdb_building_dataset'
                  AND column_name = 'geom'
                LIMIT 1
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean isInvalidPostalCode(JSONObject geomJson) {
        return geomJson == null || geomJson.isEmpty();
    }

    private JSONObject invalidPostalCodeResult(String postalCode) {
        System.out.println("ERROR: Invalid postal code");
        JSONObject output = new JSONObject();
        output.put("postalCode", postalCode);
        output.put("status", "ERROR");
        output.put("message", "Invalid postal code: unable to resolve coordinates");
        return output;
    }

    private JSONArray getOuterRing(JSONArray coordinates) {
        // Either Polygon format [ [ring], ... ] or MultiPolygon [[ [ring], ...], ...]
        if (coordinates.length() == 0) {
            return null;
        }
        if (coordinates.get(0) instanceof JSONArray && ((JSONArray) coordinates.get(0)).length() > 0
                && ((JSONArray) coordinates.get(0)).get(0) instanceof JSONArray) {
            // likely Polygon: coordinates = [ ring, ...]
            return coordinates.getJSONArray(0);
        }
        // MultiPolygon: coordinates = [ [ ring, ... ], ... ]
        JSONArray first = coordinates.optJSONArray(0);
        if (first != null && first.length() > 0) {
            return first.getJSONArray(0);
        }

        return null;
    }

    private double computePerimeter(JSONArray ring) {
        double perimeter = 0.0;
        for (int i = 0; i < ring.length() - 1; i++) {
            JSONArray p1 = ring.getJSONArray(i);
            JSONArray p2 = ring.getJSONArray(i + 1);
            double dx = p2.getDouble(0) - p1.getDouble(0);
            double dy = p2.getDouble(1) - p1.getDouble(1);
            perimeter += Math.hypot(dx, dy);
        }
        return perimeter;
    }

    private double computeSunFacingScore(JSONArray ring, double azimuth) {
        double signedArea = computeSignedArea(ring);
        // Ring orientation determines outward normal direction.
        boolean ccw = signedArea > 0;

        double sunX = Math.sin(Math.toRadians(normalizeAzimuth(azimuth)));
        double sunY = Math.cos(Math.toRadians(normalizeAzimuth(azimuth)));

        double score = 0.0;
        for (int i = 0; i < ring.length() - 1; i++) {
            JSONArray p1 = ring.getJSONArray(i);
            JSONArray p2 = ring.getJSONArray(i + 1);
            double x1 = p1.getDouble(0);
            double y1 = p1.getDouble(1);
            double x2 = p2.getDouble(0);
            double y2 = p2.getDouble(1);

            double dx = x2 - x1;
            double dy = y2 - y1;
            double segmentLen = Math.hypot(dx, dy);
            if (segmentLen <= 0) {
                continue;
            }

            double nx = ccw ? dy : -dy;
            double ny = ccw ? -dx : dx;
            double nlen = Math.hypot(nx, ny);
            if (nlen <= 0.0) {
                continue;
            }

            double ux = nx / nlen;
            double uy = ny / nlen;
            // Back-face culling: only faces oriented toward the sun contribute.
            double dot = Math.max(0.0, ux * sunX + uy * sunY); // Dot product
            double exposureFactor = dot;

            // Visibility attenuation from local self-blocking along sun rays.
            double shadingFactor = computeShadingFactor(ring, ux, uy, sunX, sunY, i);
            
            score += segmentLen * exposureFactor * shadingFactor;
        }

        return score;
    }

    // Compute shading factor (0-1) for an edge based on blocking from other edges
    // 1.0 = no shading (fully exposed), 0.0 = fully shaded
    private double computeShadingFactor(JSONArray ring, double outwardNormalX, double outwardNormalY,
                                        double sunX, double sunY, int currentEdgeIndex) {
        JSONArray p1 = ring.getJSONArray(currentEdgeIndex);
        JSONArray p2 = ring.getJSONArray(currentEdgeIndex + 1);
        double x1 = p1.getDouble(0);
        double y1 = p1.getDouble(1);
        double x2 = p2.getDouble(0);
        double y2 = p2.getDouble(1);

        // Sample along full edge so we estimate partial shading, not just midpoint shading.
        int numSamples = 10;
        int exposedSamples = 0;

        for (int sample = 0; sample <= numSamples; sample++) {
            double t = (double) sample / numSamples;
            double sampleX = x1 + (x2 - x1) * t;
            double sampleY = y1 + (y2 - y1) * t;

            // Small outward offset reduces boundary-touching false intersections.
            double offset = 1e-6;
            sampleX += outwardNormalX * offset;
            sampleY += outwardNormalY * offset;

            double rayStartX = sampleX;
            double rayStartY = sampleY;
            // Long finite ray toward sun direction for intersection checks.
            double rayEndX = sampleX + sunX * 1000;
            double rayEndY = sampleY + sunY * 1000;

            boolean isBlocked = false;

            for (int j = 0; j < ring.length() - 1; j++) {
                // Skip same and adjacent edges to avoid trivial/self intersections.
                if (j == currentEdgeIndex || j == (currentEdgeIndex + 1) % (ring.length() - 1)) {
                    continue;
                }

                JSONArray blockP1 = ring.getJSONArray(j);
                JSONArray blockP2 = ring.getJSONArray(j + 1);
                double bx1 = blockP1.getDouble(0);
                double by1 = blockP1.getDouble(1);
                double bx2 = blockP2.getDouble(0);
                double by2 = blockP2.getDouble(1);

                if (doLinesIntersect(rayStartX, rayStartY, rayEndX, rayEndY, bx1, by1, bx2, by2)) {
                    isBlocked = true;
                    break;
                }
            }

            if (!isBlocked) {
                exposedSamples++;
            }
        }

        // Fraction of edge samples with clear line-of-sight to sun direction.
        return (double) exposedSamples / (numSamples + 1);
    }

    // Check if two line segments intersect (ray casting for shading detection)
    private boolean doLinesIntersect(double x1, double y1, double x2, double y2,
                                     double x3, double y3, double x4, double y4) {
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (Math.abs(denom) < 1e-10) {
            return false;  // Parallel lines
        }
        
        double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
        double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
        
        // Intersection exists if both parameters are in [0, 1]
        return (ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1);
    }

    private double computeSignedArea(JSONArray ring) {
        double area = 0.0;
        for (int i = 0; i < ring.length() - 1; i++) {
            JSONArray p1 = ring.getJSONArray(i);
            JSONArray p2 = ring.getJSONArray(i + 1);
            double x1 = p1.getDouble(0);
            double y1 = p1.getDouble(1);
            double x2 = p2.getDouble(0);
            double y2 = p2.getDouble(1);
            area += (x1 * y2 - x2 * y1);
        }
        return area / 2.0;
    }

    private double normalizeAzimuth(double az) {
        double v = az % 360.0;
        if (v < 0) {
            v += 360.0;
        }
        return v;
    }

    private double normalizeStep(double stepDegrees, double fallback) {
        if (!Double.isFinite(stepDegrees) || stepDegrees < MIN_STEP_DEGREES) {
            return fallback;
        }
        return stepDegrees;
    }

    private double computePercentFromBest(double value, double best, double worst) {
        // Compute percentage as distance from best (minimum) towards worst (maximum)
        // best (lowest score) = 0%, worst (highest score) = 100%
        double range = worst - best;
        if (range <= 0) {
            return 0.0;  // No valid range or value is already at best
        }
        double pct = 100.0 * (value - best) / range;
        if (pct < 0.0) {
            pct = 0.0;
        }
        if (pct > 100.0) {
            pct = 100.0;
        }
        return pct;
    }

    // Direct azimuth point score without range integration (to diagnose orientation issues)
    private JSONObject calSunFacingPointOnly(String postalCode, double azimuth) {
        JSONObject geomJson = getHDBBuildingGeom(postalCode);
        JSONObject output = new JSONObject();

        if (geomJson == null || geomJson.isEmpty()) {
            output.put("postalCode", postalCode);
            output.put("status", "ERROR");
            output.put("message", "Building geometry not found");
            return output;
        }

        JSONArray coordinates = geomJson.optJSONArray("coordinates");
        if (coordinates == null || coordinates.length() == 0) {
            output.put("status", "ERROR");
            output.put("message", "No coordinates available");
            return output;
        }

        JSONArray ring = getOuterRing(coordinates);
        if (ring == null || ring.length() < 4) {
            output.put("status", "ERROR");
            output.put("message", "Polygon ring must have at least 4 points");
            return output;
        }

        double perimeter = computePerimeter(ring);
        double score = computeSunFacingScore(ring, azimuth);
        double ratio = perimeter > 0.0 ? score / perimeter : 0.0;

        output.put("postalCode", postalCode);
        output.put("status", "OK");
        output.put("message", "NIL");
        output.put("azimuth", normalizeAzimuth(azimuth));
        output.put("perimeter", perimeter);
        output.put("score", score);
        output.put("ratio", ratio);

        return output;
    }


}

