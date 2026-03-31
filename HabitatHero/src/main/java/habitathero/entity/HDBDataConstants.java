package habitathero.entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central repository for official HDB data.
 * Ensures traceability between frontend and backend.
 * Lab 3: BCE Architecture Alignment
 */
public class HDBDataConstants {

    // ── FLAT TYPES (Sentence Case) ──────────────────────────────────────────

    public static final List<String> VALID_FLAT_TYPES = Collections.unmodifiableList(Arrays.asList(
        "1 Room",
        "2 Room",
        "3 Room",
        "4 Room",
        "5 Room",
        "Executive",
        "Multi-generation"
    ));

    // ── REGIONS ────────────────────────────────────────────────────────────────

    public static final List<String> VALID_REGIONS = Collections.unmodifiableList(Arrays.asList(
        "North",
        "North-East",
        "East",
        "West",
        "Central"
    ));

    // ── TOWNS BY REGION (Sentence Case) ────────────────────────────────────────

    public static final Map<String, List<String>> REGION_TOWN_MAP;

    static {
        Map<String, List<String>> map = new HashMap<>();
        
        map.put("North", Collections.unmodifiableList(Arrays.asList(
            "Sembawang",
            "Woodlands",
            "Yishun"
        )));
        
        map.put("North-East", Collections.unmodifiableList(Arrays.asList(
            "Ang Mo Kio",
            "Bishan",
            "Hougang",
            "Punggol",
            "Sengkang",
            "Serangoon"
        )));
        
        map.put("East", Collections.unmodifiableList(Arrays.asList(
            "Bedok",
            "Marine Parade",
            "Pasir Ris",
            "Tampines"
        )));
        
        map.put("West", Collections.unmodifiableList(Arrays.asList(
            "Bukit Batok",
            "Bukit Panjang",
            "Bukit Timah",
            "Choa Chu Kang",
            "Clementi",
            "Jurong East",
            "Jurong West"
        )));
        
        map.put("Central", Collections.unmodifiableList(Arrays.asList(
            "Bukit Merah",
            "Central Area",
            "Geylang",
            "Kallang/Whampoa",
            "Queenstown",
            "Toa Payoh"
        )));
        
        REGION_TOWN_MAP = Collections.unmodifiableMap(map);
    }

    // ── VALIDATION METHODS ────────────────────────────────────────────────────

    /**
     * Validates that a flat type is in the official HDB list.
     */
    public static boolean isValidFlatType(String flatType) {
        return flatType != null && VALID_FLAT_TYPES.contains(flatType);
    }

    /**
     * Validates that a region is in the official list.
     */
    public static boolean isValidRegion(String region) {
        return region != null && VALID_REGIONS.contains(region);
    }

    /**
     * Validates that a town belongs to the official HDB towns.
     */
    public static boolean isValidTown(String town) {
        if (town == null) return false;
        return REGION_TOWN_MAP.values().stream()
            .anyMatch(towns -> towns.contains(town));
    }

    /**
     * Validates that a town belongs to a specific region.
     */
    public static boolean isTownInRegion(String town, String region) {
        if (town == null || region == null) return false;
        List<String> towns = REGION_TOWN_MAP.get(region);
        return towns != null && towns.contains(town);
    }

    /**
     * Gets all valid towns across all regions.
     */
    public static List<String> getAllValidTowns() {
        List<String> allTowns = new java.util.ArrayList<>();
        REGION_TOWN_MAP.values().forEach(allTowns::addAll);
        return allTowns;
    }
}
