package habitathero.control;

import java.util.List;

import org.springframework.stereotype.Service;

import habitathero.entity.CommuterProfile;
import habitathero.entity.Coordinates;

@Service
public class MultiCommuterService {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    public List<BlockCandidateView> annotateCommuteScores(List<BlockCandidateView> candidates,
                                                          CommuterProfile commuter) {

        if (candidates == null || candidates.isEmpty()) {
            return candidates;
        }

        boolean hasValidCommuterPair = commuter != null
                && commuter.isEnabled()
                && commuter.getDestinationA() != null
                && commuter.getDestinationB() != null;

        double minTotalDistance = Double.MAX_VALUE;
        double maxTotalDistance = Double.MIN_VALUE;
        double minFairnessGap = Double.MAX_VALUE;
        double maxFairnessGap = Double.MIN_VALUE;

        for (BlockCandidateView candidate : candidates) {
            if (!hasValidCommuterPair) {
                candidate.setDistanceToCommuterAKm(0.0);
                candidate.setDistanceToCommuterBKm(0.0);
                candidate.setTotalDistanceKm(0.0);
                candidate.setFairnessGapKm(0.0);
                candidate.setCommuteScore(0.0);
                continue;
            }

            Coordinates origin = candidate.getBlock().getCoordinates();
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

            minTotalDistance = Math.min(minTotalDistance, totalDistance);
            maxTotalDistance = Math.max(maxTotalDistance, totalDistance);
            minFairnessGap = Math.min(minFairnessGap, fairnessGap);
            maxFairnessGap = Math.max(maxFairnessGap, fairnessGap);
        }

        for (BlockCandidateView candidate : candidates) {
            if (!hasValidCommuterPair) {
                candidate.setCommuteScore(0.0);
                continue;
            }

            if (candidate.getTotalDistanceKm() == Double.MAX_VALUE || candidate.getFairnessGapKm() == Double.MAX_VALUE) {
                candidate.setCommuteScore(0.0);
                continue;
            }

            double fairnessScore = 1.0 - normalize(candidate.getFairnessGapKm(), minFairnessGap, maxFairnessGap);
            double efficiencyScore = 1.0 - normalize(candidate.getTotalDistanceKm(), minTotalDistance, maxTotalDistance);
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
