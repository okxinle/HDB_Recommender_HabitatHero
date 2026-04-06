package habitathero.control;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import habitathero.boundary.RecommendationRequest;
import habitathero.entity.AmenityLocation;
import habitathero.entity.Coordinates;
import habitathero.entity.HDBBlock;
import habitathero.entity.PointOfInterest;
import habitathero.repository.PoiRepository;

@Service
public class ConvenienceScoringService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double POI_MATCH_DISTANCE_KM = 0.5;
    private static final double POI_NEAREST_DISTANCE_KM = 3.0;
    private static final double PARENTS_MAX_DISTANCE_KM = 4.0;
    private static final String AMENITY_PARENTS_ADDRESS = "parentsAddress";
    private static final String CATEGORY_SCHOOL = "SCHOOL";
    private static final String CATEGORY_HAWKER_CENTRE = "HAWKER_CENTRE";
    private static final String CATEGORY_SUPERMARKET = "SUPERMARKET";
    private static final String CATEGORY_PARK = "PARK";
    private static final String CATEGORY_HOSPITAL = "HOSPITAL";
    private static final String CATEGORY_PLAYGROUND = "PLAYGROUND";
    private static final String PARENTS_HOME_WITHIN_4KM = "Parents' Home (Within 4km)";

    private final PoiRepository poiRepository;
    private final GeocodingService geocodingService;

    public ConvenienceScoringService(PoiRepository poiRepository, GeocodingService geocodingService) {
        this.poiRepository = poiRepository;
        this.geocodingService = geocodingService;
    }

    public double scoreBlock(HDBBlock block, RecommendationRequest request) {
        return evaluateBlock(block, request).getScore();
    }

    public ConvenienceEvaluation evaluateBlock(HDBBlock block, RecommendationRequest request) {
        if (block == null || request == null || block.getCoordinates() == null) {
            return new ConvenienceEvaluation(0.0, Map.of(), Map.of(), Map.of());
        }

        List<String> selectedAmenities = request.getSelectedAmenities() == null
                ? List.of()
                : request.getSelectedAmenities();

        if (selectedAmenities.isEmpty()) {
            return new ConvenienceEvaluation(0.0, Map.of(), Map.of(), Map.of());
        }

        int matched = 0;
        int total = 0;
        Map<String, Boolean> amenityMatches = new LinkedHashMap<>();
        Map<String, List<String>> matchedAmenities = new LinkedHashMap<>();
        Map<String, AmenityLocation> nearestAmenities = new LinkedHashMap<>();

        for (String amenityRaw : selectedAmenities) {
            String amenity = normalizeFrontendAmenity(amenityRaw);
            if (amenity == null) {
                continue;
            }

            total++;
            AmenityMatchDetail matchDetail = getAmenityMatchDetail(
                    block.getCoordinates(),
                    amenity,
                    request.getParentsPostalCode());
            boolean satisfied = matchDetail.isMatched();
            amenityMatches.put(amenity, satisfied);
            if (matchDetail.getNearestAmenity() != null) {
                nearestAmenities.put(amenity, matchDetail.getNearestAmenity());
            }
            if (satisfied) {
                matched++;
                matchedAmenities.put(amenity, matchDetail.getPlaceNames());
            }
        }

        if (total == 0) {
            return new ConvenienceEvaluation(0.0, amenityMatches, matchedAmenities, nearestAmenities);
        }

        return new ConvenienceEvaluation((double) matched / (double) total, amenityMatches, matchedAmenities, nearestAmenities);
    }

    private AmenityMatchDetail getAmenityMatchDetail(Coordinates blockCoordinates, String amenity, String parentsPostalCode) {
        if (AMENITY_PARENTS_ADDRESS.equals(amenity)) {
            boolean parentsMatched = isParentsAddressSatisfied(blockCoordinates, parentsPostalCode);
            if (parentsMatched) {
                return new AmenityMatchDetail(
                        true,
                        List.of(PARENTS_HOME_WITHIN_4KM),
                        new AmenityLocation(PARENTS_HOME_WITHIN_4KM, null, null, null));
            }
            return new AmenityMatchDetail(false, List.of(), null);
        }

        String category = mapAmenityToCategory(amenity);
        if (category == null) {
            return new AmenityMatchDetail(false, List.of(), null);
        }

        List<PointOfInterest> nearbyPois = poiRepository.findNearbyPOIs(
                blockCoordinates.getLat(),
                blockCoordinates.getLng(),
                category,
            POI_NEAREST_DISTANCE_KM);

        List<PointDistanceRecord> poiDistanceRecords = nearbyPois.stream()
            .map(poi -> {
                Coordinates poiCoordinates = new Coordinates(poi.getLatitude(), poi.getLongitude());
                double distanceKm = haversineDistanceKm(blockCoordinates, poiCoordinates);
                return new PointDistanceRecord(poi, distanceKm);
            })
            .collect(Collectors.toList());

        List<String> placeNames = poiDistanceRecords.stream()
            .filter(record -> record.distanceKm <= POI_MATCH_DISTANCE_KM)
            .map(record -> record.poi.getName())
                .filter(name -> name != null && !name.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        AmenityLocation nearestAmenity = poiDistanceRecords.stream()
            .min((a, b) -> Double.compare(a.distanceKm, b.distanceKm))
            .map(record -> new AmenityLocation(
                record.poi.getName(),
                record.poi.getLatitude(),
                record.poi.getLongitude(),
                record.distanceKm * 1000.0))
            .orElse(null);

        return new AmenityMatchDetail(!placeNames.isEmpty(), placeNames, nearestAmenity);
    }

    private boolean isParentsAddressSatisfied(Coordinates blockCoordinates, String parentsPostalCode) {
        if (parentsPostalCode == null || parentsPostalCode.trim().isEmpty()) {
            return false;
        }

        Optional<Coordinates> parentsCoordinates = geocodingService.getCoordinates(parentsPostalCode.trim());
        if (parentsCoordinates.isEmpty()) {
            return false;
        }

        return haversineDistanceKm(blockCoordinates, parentsCoordinates.get()) <= PARENTS_MAX_DISTANCE_KM;
    }

    private String mapAmenityToCategory(String amenity) {
        return switch (amenity) {
            case "school" -> CATEGORY_SCHOOL;
            case "hawkerCentre" -> CATEGORY_HAWKER_CENTRE;
            case "supermarket" -> CATEGORY_SUPERMARKET;
            case "park" -> CATEGORY_PARK;
            case "hospital" -> CATEGORY_HOSPITAL;
            case "playground" -> CATEGORY_PLAYGROUND;
            default -> null;
        };
    }

    private String normalizeFrontendAmenity(String rawAmenity) {
        if (rawAmenity == null) {
            return null;
        }

        return switch (rawAmenity.trim()) {
            case "parentsAddress" -> "parentsAddress";
            case "school" -> "school";
            case "hawkerCentre" -> "hawkerCentre";
            case "supermarket" -> "supermarket";
            case "park" -> "park";
            case "hospital" -> "hospital";
            case "playground" -> "playground";
            default -> null;
        };
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

    public static final class ConvenienceEvaluation {
        private final double score;
        private final Map<String, Boolean> amenityMatches;
        private final Map<String, List<String>> matchedAmenities;
        private final Map<String, AmenityLocation> nearestAmenities;

        private ConvenienceEvaluation(double score,
                                      Map<String, Boolean> amenityMatches,
                                      Map<String, List<String>> matchedAmenities,
                                      Map<String, AmenityLocation> nearestAmenities) {
            this.score = score;
            this.amenityMatches = amenityMatches;
            this.matchedAmenities = matchedAmenities;
            this.nearestAmenities = nearestAmenities;
        }

        public double getScore() {
            return score;
        }

        public Map<String, Boolean> getAmenityMatches() {
            return amenityMatches;
        }

        public Map<String, List<String>> getMatchedAmenities() {
            return matchedAmenities;
        }

        public Map<String, AmenityLocation> getNearestAmenities() {
            return nearestAmenities;
        }
    }

    private static final class AmenityMatchDetail {
        private final boolean matched;
        private final List<String> placeNames;
        private final AmenityLocation nearestAmenity;

        private AmenityMatchDetail(boolean matched, List<String> placeNames, AmenityLocation nearestAmenity) {
            this.matched = matched;
            this.placeNames = placeNames;
            this.nearestAmenity = nearestAmenity;
        }

        private boolean isMatched() {
            return matched;
        }

        private List<String> getPlaceNames() {
            return placeNames;
        }

        private AmenityLocation getNearestAmenity() {
            return nearestAmenity;
        }
    }

    private static final class PointDistanceRecord {
        private final PointOfInterest poi;
        private final double distanceKm;

        private PointDistanceRecord(PointOfInterest poi, double distanceKm) {
            this.poi = poi;
            this.distanceKm = distanceKm;
        }
    }
}
