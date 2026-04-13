package habitathero.control;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import habitathero.entity.AmenityLocation;
import habitathero.entity.Coordinates;
import habitathero.entity.HDBBlock;
import habitathero.entity.PointOfInterest;
import habitathero.entity.UserSavedResults;
import habitathero.repository.ResaleTransactionRepository;
import habitathero.repository.PoiRepository;
import habitathero.repository.UserSavedResultsRepository;

@Service
public class UserProfileDbManager {

    private final UserSavedResultsRepository userSavedResultsRepository;
    private final ResaleTransactionRepository resaleTransactionRepository;
    private final PoiRepository poiRepository;
    private final ObjectMapper objectMapper;

    public UserProfileDbManager(UserSavedResultsRepository userSavedResultsRepository,
                                ResaleTransactionRepository resaleTransactionRepository,
                                PoiRepository poiRepository,
                                ObjectMapper objectMapper) {
        this.userSavedResultsRepository = userSavedResultsRepository;
        this.resaleTransactionRepository = resaleTransactionRepository;
        this.poiRepository = poiRepository;
        this.objectMapper = objectMapper;
    }

    public void saveLatestResults(int userId, List<HDBBlock> rankedBlocks) {
        try {
            String json = objectMapper.writeValueAsString(rankedBlocks == null ? List.of() : rankedBlocks);
            UserSavedResults savedResults = new UserSavedResults(userId, json);
            userSavedResultsRepository.save(savedResults);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to persist member results.", e);
        }
    }

    public List<HDBBlock> getLatestResults(int userId) {
        try {
            return userSavedResultsRepository.findById(userId)
                .map(UserSavedResults::getResultsJson)
                .map(json -> {
                    try {
                        List<HDBBlock> results = objectMapper.readValue(json, new TypeReference<List<HDBBlock>>() {});
                        enrichMissingFloorArea(results);
                        enrichTownBenchmarks(results);
                        enrichNearestAmenities(results);
                        return results;
                    } catch (Exception ex) {
                        return Collections.<HDBBlock>emptyList();
                    }
                })
                .orElse(Collections.emptyList());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to retrieve member results.", e);
        }
    }

    private void enrichMissingFloorArea(List<HDBBlock> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        List<Integer> blockIdsNeedingArea = results.stream()
            .filter(block -> block != null && block.getBlockId() > 0 && block.getFloorAreaSqm() <= 0.0)
            .map(HDBBlock::getBlockId)
            .distinct()
            .collect(Collectors.toList());

        if (blockIdsNeedingArea.isEmpty()) {
            return;
        }

        List<ResaleTransactionRepository.BlockFloorAreaView> areaRows =
            resaleTransactionRepository.findAverageFloorAreaByBlockIds(blockIdsNeedingArea);

        if (areaRows == null || areaRows.isEmpty()) {
            return;
        }

        Map<Integer, Double> averageByBlockId = new HashMap<>();
        for (ResaleTransactionRepository.BlockFloorAreaView row : areaRows) {
            if (row == null || row.getBlockId() == null || row.getAvgFloorAreaSqm() == null) {
                continue;
            }
            averageByBlockId.put(row.getBlockId(), row.getAvgFloorAreaSqm());
        }

        if (averageByBlockId.isEmpty()) {
            return;
        }

        for (HDBBlock block : results) {
            if (block == null || block.getBlockId() <= 0 || block.getFloorAreaSqm() > 0.0) {
                continue;
            }

            Double avgArea = averageByBlockId.get(block.getBlockId());
            if (avgArea != null && avgArea > 0.0) {
                block.setFloorAreaSqm(avgArea);
            }
        }
    }

    private void enrichTownBenchmarks(List<HDBBlock> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        List<String> towns = results.stream()
            .map(HDBBlock::getTown)
            .map(this::normalizeTownKey)
            .filter(town -> town != null && !town.isBlank())
            .distinct()
            .collect(Collectors.toList());

        if (towns.isEmpty()) {
            return;
        }

        Map<String, ResaleTransactionRepository.TownPsfView> benchmarkByTown = resaleTransactionRepository.findAveragePsfByTowns(towns).stream()
            .filter(row -> row != null && row.getTown() != null && row.getAvgPsf() != null)
            .collect(Collectors.toMap(
                row -> normalizeTownKey(row.getTown()),
                row -> row,
                (left, right) -> left,
                LinkedHashMap::new));

        if (benchmarkByTown.isEmpty()) {
            return;
        }

        for (HDBBlock block : results) {
            if (block == null) {
                continue;
            }

            ResaleTransactionRepository.TownPsfView benchmark = benchmarkByTown.get(normalizeTownKey(block.getTown()));
            if (benchmark == null || benchmark.getAvgPsf() == null || benchmark.getAvgPsf() <= 0.0) {
                continue;
            }

            block.setTownAveragePsf(benchmark.getAvgPsf());
            block.setTownTransactionCount(benchmark.getTransactionCount() == null ? 0L : benchmark.getTransactionCount());
        }
    }

    private void enrichNearestAmenities(List<HDBBlock> results) {
        if (results == null || results.isEmpty()) {
            return;
        }

        for (HDBBlock block : results) {
            if (block == null || block.getCoordinates() == null) {
                continue;
            }

            Map<String, AmenityLocation> nearestAmenities = block.getNearestAmenities() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(block.getNearestAmenities());

            Map<String, Boolean> convenienceFactors = block.getConvenienceFactors();
            Map<String, List<String>> matchedAmenities = block.getMatchedAmenities();
            Coordinates coordinates = block.getCoordinates();

            Set<String> amenityKeysToEnrich = new LinkedHashSet<>();
            if (convenienceFactors != null && !convenienceFactors.isEmpty()) {
                amenityKeysToEnrich.addAll(convenienceFactors.keySet());
            }
            if (matchedAmenities != null && !matchedAmenities.isEmpty()) {
                amenityKeysToEnrich.addAll(matchedAmenities.keySet());
            }

            if (!amenityKeysToEnrich.isEmpty()) {
                for (String amenityKey : amenityKeysToEnrich) {
                    if (nearestAmenities.containsKey(amenityKey)) {
                        continue;
                    }

                    String category = mapAmenityToCategory(amenityKey);
                    if (category == null) {
                        continue;
                    }

                    List<PointOfInterest> nearbyPois = poiRepository.findNearbyPOIs(
                        coordinates.getLat(),
                        coordinates.getLng(),
                        category,
                        3.0);

                    if (nearbyPois == null || nearbyPois.isEmpty()) {
                        continue;
                    }

                    PointOfInterest nearestPoi = nearbyPois.get(0);
                    if (nearestPoi == null) {
                        continue;
                    }

                    double distanceMeters = haversineMeters(
                        coordinates.getLat(),
                        coordinates.getLng(),
                        nearestPoi.getLatitude(),
                        nearestPoi.getLongitude());

                    nearestAmenities.put(amenityKey, new AmenityLocation(
                        nearestPoi.getName(),
                        nearestPoi.getLatitude(),
                        nearestPoi.getLongitude(),
                        distanceMeters));
                }
            }

            if (!nearestAmenities.containsKey("mrtStation")) {
                AmenityLocation mrtFromMatched = getNearestMrtFromMatchedAmenities(block);
                if (mrtFromMatched != null) {
                    nearestAmenities.put("mrtStation", mrtFromMatched);
                }
            }

            if (!nearestAmenities.isEmpty()) {
                block.setNearestAmenities(nearestAmenities);
            }
        }
    }

    private AmenityLocation getNearestMrtFromMatchedAmenities(HDBBlock block) {
        if (block == null || block.getMatchedAmenities() == null) {
            return null;
        }

        List<String> mrtNames = block.getMatchedAmenities().get("mrtStation");
        if (mrtNames == null || mrtNames.isEmpty()) {
            mrtNames = block.getMatchedAmenities().get("mrt");
        }
        if (mrtNames == null || mrtNames.isEmpty()) {
            mrtNames = block.getMatchedAmenities().get("train");
        }

        if (mrtNames == null || mrtNames.isEmpty()) {
            return null;
        }

        String name = mrtNames.stream()
            .filter(item -> item != null && !item.isBlank())
            .map(String::trim)
            .findFirst()
            .orElse("Nearest MRT");

        return new AmenityLocation(name, null, null, null);
    }

    private String mapAmenityToCategory(String amenity) {
        if (amenity == null) {
            return null;
        }

        return switch (amenity.trim()) {
            case "school" -> "SCHOOL";
            case "hawkerCentre" -> "HAWKER_CENTRE";
            case "supermarket" -> "SUPERMARKET";
            case "park" -> "PARK";
            case "hospital" -> "HOSPITAL";
            case "playground" -> "PLAYGROUND";
            default -> null;
        };
    }

    private String normalizeTownKey(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
