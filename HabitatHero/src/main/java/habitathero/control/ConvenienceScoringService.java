package habitathero.control;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import habitathero.boundary.RecommendationRequest;
import habitathero.entity.Coordinates;
import habitathero.entity.HDBBlock;
import habitathero.repository.PoiRepository;

@Service
public class ConvenienceScoringService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double POI_MAX_DISTANCE_KM = 0.5;
    private static final double PARENTS_MAX_DISTANCE_KM = 4.0;
    private static final String AMENITY_PARENTS_ADDRESS = "parentsAddress";
    private static final String CATEGORY_SCHOOL = "SCHOOL";
    private static final String CATEGORY_HAWKER_CENTRE = "HAWKER_CENTRE";
    private static final String CATEGORY_SUPERMARKET = "SUPERMARKET";
    private static final String CATEGORY_PARK = "PARK";
    private static final String CATEGORY_HOSPITAL = "HOSPITAL";
    private static final String CATEGORY_PLAYGROUND = "PLAYGROUND";

    private final PoiRepository poiRepository;
    private final GeocodingService geocodingService;

    public ConvenienceScoringService(PoiRepository poiRepository, GeocodingService geocodingService) {
        this.poiRepository = poiRepository;
        this.geocodingService = geocodingService;
    }

    public double scoreBlock(HDBBlock block, RecommendationRequest request) {
        if (block == null || request == null || block.getCoordinates() == null) {
            return 0.0;
        }

        List<String> selectedAmenities = request.getSelectedAmenities() == null
                ? List.of()
                : request.getSelectedAmenities();

        if (selectedAmenities.isEmpty()) {
            return 0.0;
        }

        int matched = 0;
        int total = 0;

        for (String amenityRaw : selectedAmenities) {
            String amenity = normalizeFrontendAmenity(amenityRaw);
            if (amenity == null) {
                continue;
            }

            total++;
            if (isAmenitySatisfied(block.getCoordinates(), amenity, request.getParentsPostalCode())) {
                matched++;
            }
        }

        if (total == 0) {
            return 0.0;
        }

        return (double) matched / (double) total;
    }

    private boolean isAmenitySatisfied(Coordinates blockCoordinates, String amenity, String parentsPostalCode) {
        if (AMENITY_PARENTS_ADDRESS.equals(amenity)) {
            return isParentsAddressSatisfied(blockCoordinates, parentsPostalCode);
        }

        String category = mapAmenityToCategory(amenity);
        if (category == null) {
            return false;
        }

        int count = poiRepository.countNearbyPOIs(
                blockCoordinates.getLat(),
                blockCoordinates.getLng(),
                category,
                POI_MAX_DISTANCE_KM);

        return count >= 1;
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
}
