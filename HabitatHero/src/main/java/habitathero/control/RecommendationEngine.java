package habitathero.control;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import habitathero.entity.CommuterProfile;
import habitathero.entity.HDBBlock;
import habitathero.entity.HDBDataConstants;
import habitathero.entity.StructuralConstraints;
import habitathero.entity.UserProfile;
import habitathero.entity.WeightedPreference;
import habitathero.exception.ZeroMatchesException;
import habitathero.repository.ResaleTransactionRepository;

@Service
public class RecommendationEngine {

    private final ResaleTransactionRepository resaleTransactionRepository;
    private final MultiCommuterService multiCommuterService;

    public RecommendationEngine(ResaleTransactionRepository resaleTransactionRepository,
                                MultiCommuterService multiCommuterService) {
        this.resaleTransactionRepository = resaleTransactionRepository;
        this.multiCommuterService = multiCommuterService;
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

        List<BlockCandidateView> candidateList = fetchCandidates(constraints);
        System.out.println("DEBUG: Candidates found in DB: " + candidateList.size());
        if (candidateList.isEmpty()) {
            throw new ZeroMatchesException();
        }

        multiCommuterService.annotateCommuteScores(candidateList, profile.getCommuterProfile());

        List<WeightedPreference> softConstraints = profile.getSoftConstraints() == null
                ? List.of()
                : profile.getSoftConstraints();
        double totalWeight = softConstraints.stream()
                .mapToDouble(WeightedPreference::getPriorityWeight)
                .sum();

        boolean hasValidCommuterPair = hasValidCommuterPair(profile.getCommuterProfile());
        List<HDBBlock> rankedBlocks = new ArrayList<>();

        for (BlockCandidateView candidate : candidateList) {
            HDBBlock block = candidate.getBlock();

            double livabilityScore = calculateLivabilityScore(block, softConstraints);
            double normalizedLivability = totalWeight > 0 ? livabilityScore / totalWeight : 0.0;
            normalizedLivability = clamp01(normalizedLivability);

            double combinedScore = hasValidCommuterPair
                    ? clamp01((normalizedLivability + candidate.getCommuteScore()) / 2.0)
                    : normalizedLivability;

            block.setEstimatedPrice(candidate.getAverageResalePrice());
            block.setRemainingLeaseYears((int) Math.round(candidate.getAverageRemainingLease()));
            block.setGlobalMatchIndex(combinedScore * 100.0);
            rankedBlocks.add(block);
        }

        rankedBlocks.sort(Comparator.comparingDouble(HDBBlock::getGlobalMatchIndex).reversed());
        return rankedBlocks;
    }

    private List<BlockCandidateView> fetchCandidates(StructuralConstraints constraints) {
        double maxBudget = constraints.getMaxBudget() > 0 ? constraints.getMaxBudget() : Double.MAX_VALUE;
        int minLeaseYears = Math.max(0, constraints.getMinLeaseYears());
        String preferredFlatType = constraints.getPreferredFlatType();

        boolean townFilterDisabled = constraints.getPreferredTowns() == null || constraints.getPreferredTowns().isEmpty();
        List<String> preferredTowns = townFilterDisabled
                ? List.of("__TOWN_FILTER_DISABLED__")
            : constraints.getPreferredTowns().stream()
                .filter(town -> town != null && !town.isBlank())
                .map(town -> town.trim().toUpperCase())
                .collect(Collectors.toList());

        return resaleTransactionRepository.findCandidateBlocks(
                maxBudget,
                minLeaseYears,
                preferredFlatType,
                preferredTowns,
                townFilterDisabled);
    }

    private boolean hasValidCommuterPair(CommuterProfile commuterProfile) {
        return commuterProfile != null
                && commuterProfile.isEnabled()
                && commuterProfile.getDestinationA() != null
                && commuterProfile.getDestinationB() != null;
    }

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
                    // MVP: keeps convenience neutral until amenity-proximity scoring is implemented.
                    factorScore = 0.5;
                    break;

                default:
                    factorScore = 0.0;
                    break;
            }
            totalScore += factorScore * preference.getPriorityWeight();
        }
        return totalScore;
    }

    private double clamp01(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
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