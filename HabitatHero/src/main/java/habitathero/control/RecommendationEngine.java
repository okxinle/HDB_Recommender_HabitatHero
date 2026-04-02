package habitathero.control;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import habitathero.entity.CommuterProfile;
import habitathero.entity.HDBBlock;
import habitathero.entity.HDBDataConstants;
import habitathero.entity.StructuralConstraints;
import habitathero.entity.UserProfile;
import habitathero.entity.WeightedPreference;
import habitathero.exception.ZeroMatchesException;
import habitathero.repository.IHDBRepository;

@Service
public class RecommendationEngine {

    private IHDBRepository  dbRepository;
    private IRoutingService routingService;

    public RecommendationEngine(IHDBRepository dbRepository, IRoutingService routingService) {
        this.dbRepository   = dbRepository;
        this.routingService = routingService;
    }

    // ── Public entry point ────────────────────────────────────────────────

    public List<HDBBlock> generateRecommendations(UserProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("Request body is missing user profile data.");
        }

        StructuralConstraints constraints = profile.getStructuralConstraints();
        if (constraints == null) {
            throw new IllegalArgumentException("Missing structuralConstraints in request payload.");
        }

        // Lab 3: Validate user inputs against official HDB data
        validateStructuralConstraints(constraints);

        // Step 1 & 2: fetch all blocks and apply hard filters
        List<HDBBlock> allBlocks      = dbRepository.findAll();
        List<HDBBlock> filteredBlocks = applyHardFilters(allBlocks, constraints);

        List<WeightedPreference> softConstraints =
                profile.getSoftConstraints() == null ? List.of() : profile.getSoftConstraints();

        // Pre-compute the maximum possible livability score (sum of all priority weights)
        // so we can normalise it to the range [0.0, 1.0] before combining with commuteScore.
        double totalWeight = softConstraints.stream()
                .mapToDouble(WeightedPreference::getPriorityWeight)
                .sum();

        // Step 3: score every block that passed the hard filters
        for (HDBBlock block : filteredBlocks) {

            // Step 4 & 5: compute both sub-scores
            double livabilityScore = calculateLivabilityScore(block, softConstraints);
            float  commuteScore    = calculateCommuteScore(block, profile.getCommuterProfile());

            // Normalise livability to [0.0, 1.0]
            double normalisedLivability = (totalWeight > 0) ? livabilityScore / totalWeight : 0.0;

            // Step 6: combine scores
            double combinedScore;
            if (commuteScore == -1.0f || commuteScore == 0.0f) {
                // Commute is disabled or the routing API returned an error —
                // fall back to livability alone.
                combinedScore = normalisedLivability;
            } else {
                // Both signals are valid: blend them with equal weight (50 / 50).
                combinedScore = (normalisedLivability + commuteScore) / 2.0;
            }

            // Step 7: scale to a percentage [0.0, 100.0] and persist on the block
            block.setGlobalMatchIndex(combinedScore * 100.0);
        }

        // Step 8 & 9: sort descending and return
        sortBlocksByMatchIndex(filteredBlocks);
        return filteredBlocks;
    }

    private List<HDBBlock> applyHardFilters(List<HDBBlock> allBlocks, StructuralConstraints constraints) {
        double maxBudget = constraints.getMaxBudget() > 0 ? constraints.getMaxBudget() : Double.MAX_VALUE;
        int minLeaseYears = Math.max(0, constraints.getMinLeaseYears());
        List<String> preferredTowns = constraints.getPreferredTowns();
        boolean hasTownFilter = preferredTowns != null && !preferredTowns.isEmpty();

        List<HDBBlock> result = allBlocks.stream()
            .filter(Objects::nonNull)
            .filter(b -> b.getEstimatedPrice() <= maxBudget)
            .filter(b -> b.getRemainingLeaseYears() >= minLeaseYears)
            .filter(b -> !hasTownFilter || preferredTowns.contains(b.getTown()))
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            throw new ZeroMatchesException();
        }
        return result;
    }

    // ── Soft scoring (stubs) ─────────────────────────────────────────────

    private double calculateLivabilityScore(HDBBlock block, List<WeightedPreference> preferences) {
        double totalScore = 0.0;

        for (WeightedPreference preference : preferences) {
            double factorScore;

            switch (preference.getFactorName()) {
                case "Solar Orientation":
                    factorScore = block.isWestSunStatus() ? 0.0 : 1.0;
                    break;

                case "Acoustic Comfort":
                    factorScore = "Low".equalsIgnoreCase(block.getNoiseRiskLevel()) ? 1.0 : 0.0;
                    break;

                case "Convenience":
                    // TODO: implement convenience scoring logic
                    factorScore = 0.0;
                    break;

                default:
                    factorScore = 0.0;
                    break;
            }
            totalScore += factorScore * preference.getPriorityWeight();
        }
        return totalScore;
    }

    private float calculateCommuteScore(HDBBlock block, CommuterProfile commuteProfile) {
        if (commuteProfile == null || !commuteProfile.isEnabled()) {
            return 0.0f;
        }

        if (commuteProfile.getDestinationA() == null || commuteProfile.getDestinationB() == null) {
            return -1.0f;
        }

        int timeA = routingService.getTravelTime(block.getCoordinates(), commuteProfile.getDestinationA());
        int timeB = routingService.getTravelTime(block.getCoordinates(), commuteProfile.getDestinationB());

        return calculateFairnessIndex(timeA, timeB);
    }

    private float calculateFairnessIndex(int timeA, int timeB) {
        // Return -1.0f to signal invalid data (e.g. API timeout or error)
        if (timeA < 0 || timeB < 0) {
            return -1.0f;
        }
        return 1.0f - ((float) Math.abs(timeA - timeB) / (timeA + timeB + 1));
    }

    private void sortBlocksByMatchIndex(List<HDBBlock> blocks) {
        blocks.sort(Comparator.comparingDouble(HDBBlock::getGlobalMatchIndex).reversed());
    }

    /**
     * Validates structural constraints against official HDB data.
     * Lab 3: Encapsulation & Input Validation
     */
    private void validateStructuralConstraints(StructuralConstraints constraints) {
        // Validate flat type if provided
        if (constraints.getPreferredFlatType() != null && !constraints.getPreferredFlatType().isEmpty()) {
            if (!HDBDataConstants.isValidFlatType(constraints.getPreferredFlatType())) {
                throw new IllegalArgumentException(
                    "Invalid flat type: " + constraints.getPreferredFlatType() +
                    ". Valid options are: " + HDBDataConstants.VALID_FLAT_TYPES
                );
            }
        }

        // Validate towns if provided
        if (constraints.getPreferredTowns() != null && !constraints.getPreferredTowns().isEmpty()) {
            for (String town : constraints.getPreferredTowns()) {
                if (!HDBDataConstants.isValidTown(town)) {
                    throw new IllegalArgumentException(
                        "Invalid town: " + town +
                        ". Valid towns are: " + HDBDataConstants.getAllValidTowns()
                    );
                }
            }
        }
    }
}