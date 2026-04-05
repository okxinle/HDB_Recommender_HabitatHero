package habitathero.control;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import habitathero.entity.CommuterProfile;
import habitathero.entity.Coordinates;

@Service
public class MultiCommuterService {

    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final int API_CANDIDATE_LIMIT = 50;

    private final IRoutingService routingService;

    public MultiCommuterService(IRoutingService routingService) {
        this.routingService = routingService;
    }

    public List<BlockCandidateView> annotateCommuteScores(List<BlockCandidateView> candidates,
                                                          CommuterProfile commuter) {

        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }

        boolean hasValidCommuterPair = commuter != null
                && commuter.isEnabled()
                && commuter.getDestinationA() != null
                && commuter.getDestinationB() != null;

        for (BlockCandidateView candidate : candidates) {
            if (!hasValidCommuterPair) {
                candidate.setDistanceToCommuterAKm(0.0);
                candidate.setDistanceToCommuterBKm(0.0);
                candidate.setTotalDistanceKm(0.0);
                candidate.setFairnessGapKm(0.0);
                candidate.setCommuteScore(0.0);
                continue;
            }

            Coordinates origin = candidate.getBlock() == null ? null : candidate.getBlock().getCoordinates();
            if (origin == null) {
                candidate.setDistanceToCommuterAKm(Double.MAX_VALUE);
                candidate.setDistanceToCommuterBKm(Double.MAX_VALUE);
                candidate.setTotalDistanceKm(Double.MAX_VALUE);
                candidate.setFairnessGapKm(Double.MAX_VALUE);
                candidate.setCommuteScore(0.0);
                continue;
            }

            double distanceA = haversineDistanceKm(origin, commuter.getDestinationA());
            double distanceB = haversineDistanceKm(origin, commuter.getDestinationB());
            double totalDistance = distanceA + distanceB;
            double fairnessGap = Math.abs(distanceA - distanceB);

            candidate.setDistanceToCommuterAKm(distanceA);
            candidate.setDistanceToCommuterBKm(distanceB);
            candidate.setTotalDistanceKm(totalDistance);
            candidate.setFairnessGapKm(fairnessGap);
        }

        if (!hasValidCommuterPair) {
            return candidates;
        }

        // Phase 1: pre-filter by Haversine total distance.
        candidates.sort(Comparator.comparingDouble(BlockCandidateView::getTotalDistanceKm));

        int apiCandidateCount = Math.min(API_CANDIDATE_LIMIT, candidates.size());
        List<BlockCandidateView> apiCandidates = candidates.subList(0, apiCandidateCount);

        double minTotalTransitMinutes = Double.MAX_VALUE;
        double maxTotalTransitMinutes = Double.MIN_VALUE;
        double minFairnessGapMinutes = Double.MAX_VALUE;
        double maxFairnessGapMinutes = Double.MIN_VALUE;

        Map<BlockCandidateView, Double> totalTransitMinutesByCandidate = new HashMap<>();
        Map<BlockCandidateView, Double> fairnessGapMinutesByCandidate = new HashMap<>();

        // Phase 2: fetch OneMap transit times only for top pre-filtered candidates.
        for (BlockCandidateView candidate : apiCandidates) {
            Coordinates origin = candidate.getBlock() == null ? null : candidate.getBlock().getCoordinates();
            if (origin == null) {
                candidate.setCommuteScore(0.0);
                continue;
            }

            double timeToA = routingService.getTravelTime(origin, commuter.getDestinationA());
            double timeToB = routingService.getTravelTime(origin, commuter.getDestinationB());

            if (timeToA == Double.MAX_VALUE || timeToB == Double.MAX_VALUE) {
                candidate.setCommuteScore(0.0);
                continue;
            }

            double totalTransitMinutes = timeToA + timeToB;
            double fairnessGapMinutes = Math.abs(timeToA - timeToB);

            totalTransitMinutesByCandidate.put(candidate, totalTransitMinutes);
            fairnessGapMinutesByCandidate.put(candidate, fairnessGapMinutes);

            minTotalTransitMinutes = Math.min(minTotalTransitMinutes, totalTransitMinutes);
            maxTotalTransitMinutes = Math.max(maxTotalTransitMinutes, totalTransitMinutes);
            minFairnessGapMinutes = Math.min(minFairnessGapMinutes, fairnessGapMinutes);
            maxFairnessGapMinutes = Math.max(maxFairnessGapMinutes, fairnessGapMinutes);
        }

        // Candidates outside top N are de-prioritized.
        for (int i = apiCandidateCount; i < candidates.size(); i++) {
            candidates.get(i).setCommuteScore(0.0);
        }

        for (BlockCandidateView candidate : apiCandidates) {
            Double totalTransitMinutes = totalTransitMinutesByCandidate.get(candidate);
            Double fairnessGapMinutes = fairnessGapMinutesByCandidate.get(candidate);
            if (totalTransitMinutes == null || fairnessGapMinutes == null) {
                candidate.setCommuteScore(0.0);
                continue;
            }

            double fairnessScore = 1.0 - normalize(fairnessGapMinutes, minFairnessGapMinutes, maxFairnessGapMinutes);
            double efficiencyScore = 1.0 - normalize(totalTransitMinutes, minTotalTransitMinutes, maxTotalTransitMinutes);
            double commuteScore = (0.6 * fairnessScore) + (0.4 * efficiencyScore);

            candidate.setCommuteScore(clamp01(commuteScore));
        }

        return candidates;
    }

    private double haversineDistanceKm(Coordinates a, Coordinates b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }

        double lat1 = Math.toRadians(a.getLat());
        double lon1 = Math.toRadians(a.getLng());
        double lat2 = Math.toRadians(b.getLat());
        double lon2 = Math.toRadians(b.getLng());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double hav = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double centralAngle = 2 * Math.atan2(Math.sqrt(hav), Math.sqrt(1 - hav));
        return EARTH_RADIUS_KM * centralAngle;
    }

    private double normalize(double value, double min, double max) {
        if (max <= min) {
            return 0.0;
        }
        return (value - min) / (max - min);
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
}
