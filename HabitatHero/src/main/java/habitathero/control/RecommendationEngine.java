package habitathero.control;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import habitathero.GeoSpatialAnalysis.src.Coordinate;
import habitathero.GeoSpatialAnalysis.src.HDBBuildingMgr;
import habitathero.GeoSpatialAnalysis.src.TransportLineMgr;
import habitathero.boundary.RecommendationRequest;
import habitathero.entity.CommuterProfile;
import habitathero.entity.HDBBlock;
import habitathero.entity.HDBDataConstants;
import habitathero.entity.StructuralConstraints;
import habitathero.entity.WeightedPreference;
import habitathero.exception.ZeroMatchesException;
import habitathero.repository.ResaleTransactionRepository;

@Service
public class RecommendationEngine {

    private static final String FACTOR_SOLAR = "Solar Orientation";
    private static final String FACTOR_ACOUSTIC = "Acoustic Comfort";
    private static final String FACTOR_CONVENIENCE = "Convenience";
    private static final double STRICT_NOISE_DISTANCE_METERS = 100.0;
    private static final double QUIET_NOISE_DB = 55.0;
    private static final double LOUD_NOISE_DB = 85.0;

    private final ResaleTransactionRepository resaleTransactionRepository;
    private final MultiCommuterService multiCommuterService;
    private final HDBBuildingMgr hdbBuildingMgr;
    private final TransportLineMgr transportLineMgr;
    private final ConvenienceScoringService convenienceScoringService;

    public RecommendationEngine(ResaleTransactionRepository resaleTransactionRepository,
                                MultiCommuterService multiCommuterService,
                                HDBBuildingMgr hdbBuildingMgr,
                                TransportLineMgr transportLineMgr,
                                ConvenienceScoringService convenienceScoringService) {
        this.resaleTransactionRepository = resaleTransactionRepository;
        this.multiCommuterService = multiCommuterService;
        this.hdbBuildingMgr = hdbBuildingMgr;
        this.transportLineMgr = transportLineMgr;
        this.convenienceScoringService = convenienceScoringService;
    }

    // ── Public entry point ────────────────────────────────────────────────

    public List<HDBBlock> generateRecommendations(RecommendationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is missing user profile data.");
        }

        StructuralConstraints constraints = request.getStructuralConstraints();
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

        multiCommuterService.annotateCommuteScores(candidateList, request.getCommuterProfile());

        List<WeightedPreference> softConstraints = request.getSoftConstraints() == null
                ? List.of()
            : request.getSoftConstraints();

        FactorConfig solarConfig = resolveFactorConfig(softConstraints, FACTOR_SOLAR);
        FactorConfig acousticConfig = resolveFactorConfig(softConstraints, FACTOR_ACOUSTIC);
        FactorConfig convenienceConfig = resolveConvenienceConfig(request, softConstraints);

        boolean hasValidCommuterPair = hasValidCommuterPair(request.getCommuterProfile());
        List<HDBBlock> rankedBlocks = new ArrayList<>();

        for (BlockCandidateView candidate : candidateList) {
            HDBBlock block = candidate.getBlock();

            JSONObject sunFacingResult = getSunFacingResult(block);
            JSONObject noiseLevelResult = getNoiseLevelResult(block);

            double solarScore = scoreSolarOrientation(block, sunFacingResult);
            double acousticScore = scoreAcousticComfort(block, noiseLevelResult);
            double convenienceScore = convenienceScoringService.scoreBlock(block, request);

            if (!passesStrictConstraints(
                    block,
                    solarConfig,
                    acousticConfig,
                    convenienceConfig,
                    sunFacingResult,
                    noiseLevelResult,
                    convenienceScore)) {
                continue;
            }

            WeightedScore weightedScore = calculateWeightedLivabilityScore(
                    solarConfig,
                    acousticConfig,
                    convenienceConfig,
                    solarScore,
                    acousticScore,
                    convenienceScore);

            // Keep match score neutral when all factors are ignored (instead of forcing 0%).
            double normalizedLivability = weightedScore.totalWeight > 0.0
                    ? weightedScore.weightedScore / weightedScore.totalWeight
                    : 1.0;
            normalizedLivability = clamp01(normalizedLivability);

            double combinedScore = hasValidCommuterPair
                    ? clamp01((normalizedLivability + candidate.getCommuteScore()) / 2.0)
                    : normalizedLivability;

            block.setEstimatedPrice(candidate.getAverageResalePrice());
            block.setRemainingLeaseYears((int) Math.round(candidate.getAverageRemainingLease()));
            block.setGlobalMatchIndex(100.0 * combinedScore);
            rankedBlocks.add(block);
        }

        if (rankedBlocks.isEmpty()) {
            throw new ZeroMatchesException();
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

    private WeightedScore calculateWeightedLivabilityScore(FactorConfig solarConfig,
                                                           FactorConfig acousticConfig,
                                                           FactorConfig convenienceConfig,
                                                           double solarScore,
                                                           double acousticScore,
                                                           double convenienceScore) {
        double weightedScore = 0.0;
        double totalWeight = 0.0;

        if (solarConfig.mode == FactorMode.WEIGHTED && solarConfig.weight > 0.0) {
            weightedScore += solarScore * solarConfig.weight;
            totalWeight += solarConfig.weight;
        }

        if (acousticConfig.mode == FactorMode.WEIGHTED && acousticConfig.weight > 0.0) {
            weightedScore += acousticScore * acousticConfig.weight;
            totalWeight += acousticConfig.weight;
        }

        if (convenienceConfig.mode == FactorMode.WEIGHTED && convenienceConfig.weight > 0.0) {
            weightedScore += clamp01(convenienceScore) * convenienceConfig.weight;
            totalWeight += convenienceConfig.weight;
        }

        return new WeightedScore(weightedScore, totalWeight);
    }

    private boolean passesStrictConstraints(HDBBlock block,
                                            FactorConfig solarConfig,
                                            FactorConfig acousticConfig,
                                            FactorConfig convenienceConfig,
                                            JSONObject sunFacingResult,
                                            JSONObject noiseLevelResult,
                                            double convenienceScore) {
        if (solarConfig.mode == FactorMode.STRICT && !passesStrictSolar(block, sunFacingResult)) {
            return false;
        }

        if (acousticConfig.mode == FactorMode.STRICT && !passesStrictAcoustic(block, noiseLevelResult)) {
            return false;
        }

        if (convenienceConfig.mode == FactorMode.STRICT && clamp01(convenienceScore) < 1.0) {
            return false;
        }

        return true;
    }

    private FactorConfig resolveConvenienceConfig(RecommendationRequest request, List<WeightedPreference> preferences) {
        String explicitMode = normalizeMode(request.getConvenienceMode());
        if (explicitMode != null) {
            FactorMode mode = FactorMode.valueOf(explicitMode);
            if (mode == FactorMode.WEIGHTED) {
                return new FactorConfig(mode, Math.max(0.0, request.getConvenienceWeight()));
            }
            return new FactorConfig(mode, 0.0);
        }

        return resolveFactorConfig(preferences, FACTOR_CONVENIENCE);
    }

    private FactorConfig resolveFactorConfig(List<WeightedPreference> preferences, String factorName) {
        Optional<WeightedPreference> preferenceOpt = preferences.stream()
                .filter(preference -> preference.getFactorName() != null
                        && factorName.equalsIgnoreCase(preference.getFactorName().trim()))
                .findFirst();

        if (preferenceOpt.isEmpty()) {
            return new FactorConfig(FactorMode.IGNORE, 0.0);
        }

        WeightedPreference preference = preferenceOpt.get();
        if (preference.isStrict()) {
            return new FactorConfig(FactorMode.STRICT, 0.0);
        }

        if (preference.getPriorityWeight() > 0.0) {
            return new FactorConfig(FactorMode.WEIGHTED, preference.getPriorityWeight());
        }

        return new FactorConfig(FactorMode.IGNORE, 0.0);
    }

    private JSONObject getSunFacingResult(HDBBlock block) {
        String postalCode = normalized(block.getPostalCode());
        if (postalCode == null) {
            return null;
        }

        try {
            return hdbBuildingMgr.calSunFacing(postalCode);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JSONObject getNoiseLevelResult(HDBBlock block) {
        String postalCode = normalized(block.getPostalCode());

        try {
            if (postalCode != null) {
                return transportLineMgr.calNoiseLevel(postalCode);
            }

            if (block.getCoordinates() != null) {
                Coordinate coords = new Coordinate(block.getCoordinates().getLat(), block.getCoordinates().getLng());
                return transportLineMgr.calNoiseLevel(coords);
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private double scoreSolarOrientation(HDBBlock block, JSONObject sunFacingResult) {
        if (sunFacingResult != null && "OK".equalsIgnoreCase(sunFacingResult.optString("status"))) {
            double westExposurePct = sunFacingResult.optDouble("westScoreRelativeExposurePct", Double.NaN);
            if (!Double.isNaN(westExposurePct)) {
                return clamp01(1.0 - (westExposurePct / 100.0));
            }

            String dominant = sunFacingResult.optString("dominant", "");
            if ("WEST".equalsIgnoreCase(dominant)) {
                return 0.0;
            }
            if ("EAST".equalsIgnoreCase(dominant)) {
                return 1.0;
            }
            if ("BALANCED".equalsIgnoreCase(dominant)) {
                return 0.5;
            }
        }

        // Fallback to persisted block field if geospatial output is unavailable.
        return block.isWestSunStatus() ? 0.0 : 1.0;
    }

    private boolean passesStrictSolar(HDBBlock block, JSONObject sunFacingResult) {
        if (sunFacingResult != null && "OK".equalsIgnoreCase(sunFacingResult.optString("status"))) {
            String dominant = sunFacingResult.optString("dominant", "");
            if (!dominant.isBlank()) {
                return !"WEST".equalsIgnoreCase(dominant);
            }

            return scoreSolarOrientation(block, sunFacingResult) >= 0.5;
        }

        return !block.isWestSunStatus();
    }

    private double scoreAcousticComfort(HDBBlock block, JSONObject noiseLevelResult) {
        if (noiseLevelResult != null && !noiseLevelResult.has("error")) {
            double noiseDb = noiseLevelResult.optDouble("noise_level_db", Double.NaN);
            if (!Double.isNaN(noiseDb)) {
                return scoreFromNoiseDb(noiseDb);
            }
        }

        // Fallback to persisted block field if geospatial output is unavailable.
        return "Low".equalsIgnoreCase(block.getNoiseRiskLevel()) ? 1.0 : 0.0;
    }

    private boolean passesStrictAcoustic(HDBBlock block, JSONObject noiseLevelResult) {
        if (noiseLevelResult != null && !noiseLevelResult.has("error")) {
            double distanceMeters = noiseLevelResult.optDouble("distance_meters", Double.NaN);
            if (!Double.isNaN(distanceMeters)) {
                return distanceMeters >= STRICT_NOISE_DISTANCE_METERS;
            }

            double noiseDb = noiseLevelResult.optDouble("noise_level_db", Double.NaN);
            if (!Double.isNaN(noiseDb)) {
                return scoreFromNoiseDb(noiseDb) >= 0.5;
            }
        }

        return "Low".equalsIgnoreCase(block.getNoiseRiskLevel());
    }

    private double scoreFromNoiseDb(double noiseDb) {
        if (noiseDb <= QUIET_NOISE_DB) {
            return 1.0;
        }
        if (noiseDb >= LOUD_NOISE_DB) {
            return 0.0;
        }

        double normalized = 1.0 - ((noiseDb - QUIET_NOISE_DB) / (LOUD_NOISE_DB - QUIET_NOISE_DB));
        return clamp01(normalized);
    }

    private String normalized(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeMode(String rawMode) {
        if (rawMode == null || rawMode.trim().isEmpty()) {
            return null;
        }

        String normalized = rawMode.trim().toUpperCase(Locale.ROOT);
        if ("IGNORE".equals(normalized) || "STRICT".equals(normalized) || "WEIGHTED".equals(normalized)) {
            return normalized;
        }

        return null;
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

    private static final class WeightedScore {
        private final double weightedScore;
        private final double totalWeight;

        private WeightedScore(double weightedScore, double totalWeight) {
            this.weightedScore = weightedScore;
            this.totalWeight = totalWeight;
        }
    }

    private enum FactorMode {
        IGNORE,
        STRICT,
        WEIGHTED
    }

    private static final class FactorConfig {
        private final FactorMode mode;
        private final double weight;

        private FactorConfig(FactorMode mode, double weight) {
            this.mode = mode;
            this.weight = weight;
        }
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