package control;
import java.util.List;
import java.util.stream.Collectors;

import entity.CommuterProfile;
import entity.HDBBlock;
import entity.StructuralConstraints;
import entity.UserProfile;
import entity.WeightedPreference;
import exception.ZeroMatchesException;
import repository.IHDBRepository;


public class RecommendationEngine {

    private IHDBRepository  dbRepository;
    private IRoutingService routingService;

    public RecommendationEngine(IHDBRepository dbRepository, IRoutingService routingService) {
        this.dbRepository   = dbRepository;
        this.routingService = routingService;
    }

    // ── Public entry point ────────────────────────────────────────────────

    public List<HDBBlock> generateRecommendations(UserProfile profile) {
        List<HDBBlock> allBlocks      = dbRepository.getAllBlocks();
        List<HDBBlock> filteredBlocks = applyHardFilters(allBlocks, profile.getStructuralConstraints());
        // TODO: soft scoring (calculateLivabilityScore, calculateCommuteScore)
        sortBlocksByMatchIndex(filteredBlocks);
        return filteredBlocks;
    }

    private List<HDBBlock> applyHardFilters(List<HDBBlock> allBlocks, StructuralConstraints constraints) {
        List<HDBBlock> result = allBlocks.stream()
                .filter(b -> b.getEstimatedPrice()      <= constraints.getMaxBudget())
                .filter(b -> b.getRemainingLeaseYears() >= constraints.getMinLeaseYears())
                .filter(b -> constraints.getPreferredTowns().contains(b.getTown()))
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
        // TODO - implement RecommendationEngine.calculateCommuteScore
        throw new UnsupportedOperationException();
    }

    private float calculateFairnessIndex(int timeA, int timeB) {
        // TODO - implement RecommendationEngine.calculateFairnessIndex
        throw new UnsupportedOperationException();
    }

    private void sortBlocksByMatchIndex(List<HDBBlock> blocks) {
        // TODO - implement RecommendationEngine.sortBlocksByMatchIndex
        // e.g. blocks.sort(Comparator.comparingDouble(HDBBlock::getGlobalMatchIndex).reversed());
    }
}