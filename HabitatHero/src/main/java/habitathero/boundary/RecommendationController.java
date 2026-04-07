package habitathero.boundary;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import habitathero.GeoSpatialAnalysis.src.LandUseMgr;
import habitathero.control.GeocodingService;
import habitathero.control.RecommendationEngine;
import habitathero.control.UserProfileDbManager;
import habitathero.entity.CommuterProfile;
import habitathero.entity.Coordinates;
import habitathero.entity.HDBBlock;
import habitathero.entity.UserAccount;
import habitathero.entity.UserProfile;
import habitathero.exception.ZeroMatchesException;

@RestController
@RequestMapping("/api/hdb")
@CrossOrigin
public class RecommendationController {

    /**
     * The `RecommendationController` class in Java handles POST requests to recommend HDBBlocks based on a
     * UserProfile using a RecommendationEngine.
     */
    private final RecommendationEngine engine;
    private final UserProfileDbManager userProfileDbManager;
    private final GeocodingService geocodingService;
    private final LandUseMgr landUseMgr;

    public RecommendationController(RecommendationEngine engine,
                                    UserProfileDbManager userProfileDbManager,
                                    GeocodingService geocodingService) {
        this.engine = engine;
        this.userProfileDbManager = userProfileDbManager;
        this.geocodingService = geocodingService;
        this.landUseMgr = LandUseMgr.getInstance();
    }

    /**
     * Accepts a UserProfile as JSON in the request body,
     * runs the recommendation engine, and returns a ranked
     * list of HDBBlocks or a descriptive error response.
     *
     * @param  profile the user's constraints and preferences
     * @return 200 OK        + {status, results}
     *         400 BAD REQ   + {status, message}       if payload is invalid
     *         404 NOT FOUND + {status, message}       if no blocks match
     *         500 ISE       + {status, message}       on unexpected error
     */
    @PostMapping("/recommend")
    public ResponseEntity<?> recommend(@RequestBody RecommendationRequest request, Authentication authentication) {
        try {
            applyPostalCodeDestinations(request, request);

            List<HDBBlock> recommendedBlocks = engine.generateRecommendations(request);
            boolean resultsPersisted = false;

            // Persist only for authenticated members.
            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserAccount userAccount) {
                userProfileDbManager.saveLatestResults(userAccount.getUserId(), recommendedBlocks);
                resultsPersisted = true;
            }

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "pipeline", "REAL_DB_MULTI_COMMUTER_HAVERSINE",
                "resultsPersisted", resultsPersisted,
                "results", recommendedBlocks
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
                ));

        } catch (ZeroMatchesException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "status", "no_matches",
                    "message", e.getMessage()
                ));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "error",
                    "message", "An unexpected error occurred."
                ));
        }
    }

    @GetMapping("/future-development-risk")
    public ResponseEntity<?> getFutureDevelopmentRisk(
            @RequestParam("postalCode") String postalCode,
            @RequestParam(value = "distance", required = false) Double distance,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude) {
        try {
            if (!hasText(postalCode)) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "status", "ERROR",
                                "message", "postalCode is required"
                        ));
            }

            String normalizedPostalCode = postalCode.trim();
            JSONObject result = queryFutureRisk(normalizedPostalCode, distance);

            // Geospatial datasets commonly store postal codes in 9-digit format.
            // Retry with a trailing "000" for 6-digit inputs to match that schema.
            if (isInvalidPostalCodeResult(result)) {
                String alternativePostalCode = toGeoSpatialPostalCode(normalizedPostalCode);
                if (!alternativePostalCode.equals(normalizedPostalCode)) {
                    result = queryFutureRisk(alternativePostalCode, distance);
                }
            }

            if (isInvalidPostalCodeResult(result) && isValidCoordinate(latitude, longitude)) {
                double effectiveDistance = distance != null ? distance : 500.0;
                result = landUseMgr.getFutureDevRiskByCoordinate(latitude, longitude, effectiveDistance);
            }

            if (result == null || result.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "status", "ERROR",
                                "message", "Future development risk lookup failed"
                        ));
            }

            return ResponseEntity.status(resolveHttpStatus(result)).body(result.toMap());
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Unable to retrieve future development risk"
                    ));
        }
    }

    private HttpStatus resolveHttpStatus(JSONObject result) {
        String status = result.optString("status", "");
        if ("OK".equalsIgnoreCase(status)) {
            return HttpStatus.OK;
        }

        String message = result.optString("message", "").toLowerCase();
        if (message.contains("invalid postal code")) {
            return HttpStatus.BAD_REQUEST;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private JSONObject queryFutureRisk(String postalCode, Double distance) {
        return distance != null
                ? landUseMgr.getFutureDevRisk(postalCode, distance)
                : landUseMgr.getFutureDevRisk(postalCode);
    }

    private String toGeoSpatialPostalCode(String postalCode) {
        String trimmed = postalCode == null ? "" : postalCode.trim();
        if (trimmed.matches("\\d{6}")) {
            return trimmed + "000";
        }
        return trimmed;
    }

    private boolean isInvalidPostalCodeResult(JSONObject result) {
        if (result == null || result.isEmpty()) {
            return true;
        }

        String status = result.optString("status", "");
        String message = result.optString("message", "").toLowerCase();
        return "ERROR".equalsIgnoreCase(status) && message.contains("invalid postal code");
    }

    private boolean isValidCoordinate(Double latitude, Double longitude) {
        return latitude != null
                && longitude != null
                && Double.isFinite(latitude)
                && Double.isFinite(longitude)
                && latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    private void applyPostalCodeDestinations(RecommendationRequest request, UserProfile profile) {
        if (request == null || profile == null) {
            return;
        }

        CommuterProfile commuterProfile = profile.getCommuterProfile();
        if (commuterProfile == null) {
            return;
        }

        if (hasText(request.getPostalCodeA())) {
            commuterProfile.setDestinationA(resolveDestination(request.getPostalCodeA(), "postalCodeA"));
        }

        if (hasText(request.getPostalCodeB())) {
            commuterProfile.setDestinationB(resolveDestination(request.getPostalCodeB(), "postalCodeB"));
        }
    }

    private Coordinates resolveDestination(String postalCode, String fieldName) {
        java.util.Optional<Coordinates> coords = geocodingService.getCoordinates(postalCode);

        if (coords.isEmpty()) {
            throw new IllegalArgumentException(
                "Invalid " + fieldName + ": unable to resolve coordinates for postal code " + postalCode
                + " from local datasets or OneMap API."
            );
        }

        return coords.get();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
