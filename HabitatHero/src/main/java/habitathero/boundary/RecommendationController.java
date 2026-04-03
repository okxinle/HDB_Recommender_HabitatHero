package habitathero.boundary;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import habitathero.GeoSpatialAnalysis.src.Coordinate;
import habitathero.GeoSpatialAnalysis.src.HDBBuildingMgr;
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

    public RecommendationController(RecommendationEngine engine, UserProfileDbManager userProfileDbManager) {
        this.engine = engine;
        this.userProfileDbManager = userProfileDbManager;
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
            UserProfile profile = request;
            applyPostalCodeDestinations(request, profile);

            List<HDBBlock> recommendedBlocks = engine.generateRecommendations(profile);

            // Persist only for authenticated members.
            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserAccount userAccount) {
                userProfileDbManager.saveLatestResults(userAccount.getUserId(), recommendedBlocks);
            }

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "pipeline", "REAL_DB_MULTI_COMMUTER_HAVERSINE",
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
        Coordinate geospatialCoordinate = HDBBuildingMgr.getInstance().postalCodeToCoordinate(postalCode);

        if (geospatialCoordinate == null ||
            (Double.compare(geospatialCoordinate.getLatitude(), -1.0) == 0 &&
             Double.compare(geospatialCoordinate.getLongitude(), -1.0) == 0)) {
            throw new IllegalArgumentException(
                "Invalid " + fieldName + ": unable to resolve coordinates for postal code " + postalCode + "."
            );
        }

        return new Coordinates(geospatialCoordinate.getLatitude(), geospatialCoordinate.getLongitude());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
