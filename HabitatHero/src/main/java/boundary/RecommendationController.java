package boundary;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import control.RecommendationEngine;
import entity.HDBBlock;
import entity.UserProfile;
import exception.ZeroMatchesException;

@RestController
@RequestMapping("/api/hdb")
@CrossOrigin
public class RecommendationController {

    private final RecommendationEngine engine;

    public RecommendationController(RecommendationEngine engine) {
        this.engine = engine;
    }

    /**
     * Accepts a UserProfile as JSON in the request body,
     * runs the recommendation engine, and returns a ranked
     * list of HDBBlocks or a descriptive error response.
     *
     * @param  profile the user's constraints and preferences
     * @return 200 OK        + ranked List<HDBBlock>  on success
     *         404 NOT FOUND + error message           if no blocks match
     *         500 ISE       + generic message         on unexpected error
     */
    @PostMapping("/recommend")
    public ResponseEntity<?> recommend(@RequestBody UserProfile profile) {
        try {
            List<HDBBlock> recommendedBlocks = engine.generateRecommendations(profile);
            return ResponseEntity.ok(recommendedBlocks);

        } catch (ZeroMatchesException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }
}