package habitathero.control;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import habitathero.entity.Coordinates;

@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final String ONEMAP_URL_TEMPLATE =
            "https://www.onemap.gov.sg/api/common/elastic/search?searchVal=%s&returnGeom=Y&getAddrDetails=N&pageNum=1";
    private static final String ONEMAP_ADDRESS_URL_TEMPLATE =
            "https://www.onemap.gov.sg/api/common/elastic/search?searchVal=%s&returnGeom=Y&getAddrDetails=Y&pageNum=1";
    private static final long ONEMAP_MIN_INTERVAL_MS = 250L;
    private static final long ONEMAP_COOLDOWN_ON_429_MS = 60_000L;

    private static final Map<String, String> STREET_TOKEN_CANONICAL = buildStreetTokenCanonicalMap();

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final String oneMapApiToken;
    private final Object oneMapLock = new Object();
    private volatile long oneMapLastRequestEpochMs = 0L;
    private volatile long oneMapCooldownUntilEpochMs = 0L;

    public GeocodingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = new RestTemplate();
        this.oneMapApiToken = System.getenv("ONEMAP_API_TOKEN");
    }

    public Optional<Coordinates> getCoordinates(String postalCode) {
        String normalizedPostal = normalizePostal(postalCode);
        if (normalizedPostal == null) {
            return Optional.empty();
        }

        ensureExternalLookupTable();

        Optional<Coordinates> local = lookupInLocalSources(normalizedPostal);
        if (local.isPresent()) {
            return local;
        }

        Optional<Coordinates> fromOneMap = lookupFromOneMap(normalizedPostal);
        fromOneMap.ifPresent(coords -> cacheExternalLookup(normalizedPostal, coords, "ONEMAP"));
        return fromOneMap;
    }

    public Optional<AddressGeocodeResult> getGeocodeByAddress(String blockNumber, String streetName) {
        String normalizedBlock = normalizeBlock(blockNumber);
        String normalizedStreet = normalizeStreet(streetName);
        if (normalizedBlock == null || normalizedStreet == null) {
            return Optional.empty();
        }

        String addressKey = normalizedBlock + "|" + normalizedStreet;
        ensureExternalAddressLookupTable();

        Optional<AddressGeocodeResult> cached = jdbcTemplate.query(
                """
                SELECT postal_code, latitude, longitude
                FROM external_address_lookup
                WHERE address_key = ?
                LIMIT 1
                """,
                rs -> rs.next()
                        ? Optional.of(new AddressGeocodeResult(
                                rs.getString("postal_code"),
                                rs.getDouble("latitude"),
                                rs.getDouble("longitude")))
                        : Optional.empty(),
                addressKey);

        if (cached.isPresent()) {
            return cached;
        }

        String searchVal = blockNumber.trim() + " " + streetName.trim();
        String encoded = URLEncoder.encode(searchVal, StandardCharsets.UTF_8);
        String url = String.format(ONEMAP_ADDRESS_URL_TEMPLATE, encoded);

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                awaitOneMapAvailability();
                String body = fetchOneMap(url);
                if (body == null || body.isBlank()) {
                    return Optional.empty();
                }

                JSONObject response = new JSONObject(body);
                JSONArray results = response.optJSONArray("results");
                if (results == null || results.isEmpty()) {
                    return Optional.empty();
                }

                for (int i = 0; i < results.length(); i++) {
                    JSONObject row = results.optJSONObject(i);
                    if (row == null) {
                        continue;
                    }

                    String blkNo = normalizeBlock(row.optString("BLK_NO", ""));
                    String roadName = normalizeStreet(row.optString("ROAD_NAME", ""));
                    String postal = normalizePostal(row.optString("POSTAL", ""));
                    String latText = row.optString("LATITUDE", "").trim();
                    String lngText = row.optString("LONGITUDE", "").trim();

                    if (blkNo == null || roadName == null || postal == null || latText.isEmpty() || lngText.isEmpty()) {
                        continue;
                    }

                    if (!normalizedBlock.equals(blkNo) || !normalizedStreet.equals(roadName)) {
                        continue;
                    }

                    AddressGeocodeResult result = new AddressGeocodeResult(
                            postal,
                            Double.parseDouble(latText),
                            Double.parseDouble(lngText));

                    cacheExternalAddressLookup(addressKey, result);
                    return Optional.of(result);
                }

                return Optional.empty();
            } catch (RestClientResponseException ex) {
                boolean shouldRetry = handleOneMapHttpException(ex, "block=" + blockNumber + " street=" + streetName);
                if (!shouldRetry || attempt == 1) {
                    return Optional.empty();
                }
            } catch (RestClientException | NumberFormatException ex) {
                log.warn("OneMap address lookup failed for block={} street={}: {}", blockNumber, streetName, ex.getMessage());
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private Optional<Coordinates> lookupInLocalSources(String postalCode) {
        Optional<Coordinates> cachedExternal = queryCoordinates(
                """
                SELECT latitude, longitude
                FROM external_postal_lookup
                WHERE postal_code = ?
                LIMIT 1
                """,
                postalCode);

        if (cachedExternal.isPresent()) {
            return cachedExternal;
        }

        Optional<Coordinates> fromBlocks = queryCoordinates(
                """
                SELECT
                    (coordinates->>'lat')::double precision AS latitude,
                    (coordinates->>'lng')::double precision AS longitude
                FROM hdb_blocks
                WHERE postal_code = ?
                  AND coordinates IS NOT NULL
                LIMIT 1
                """,
                postalCode);

        if (fromBlocks.isPresent()) {
            return fromBlocks;
        }

        Optional<Coordinates> fromLookup = queryCoordinates(
                """
                SELECT latitude, longitude
                FROM hdb_building_lookup
                WHERE postal_cod = ?
                LIMIT 1
                """,
                postalCode);

        if (fromLookup.isPresent()) {
            return fromLookup;
        }

        // Best effort for PostGIS installations. If table/functions are unavailable, ignore and continue.
        return queryCoordinatesSafe(
                """
                SELECT
                    ST_Y(ST_Centroid(ST_Collect(geom))) AS latitude,
                    ST_X(ST_Centroid(ST_Collect(geom))) AS longitude
                FROM hdb_building
                WHERE postal_cod = ?
                GROUP BY postal_cod
                LIMIT 1
                """,
                postalCode);
    }

    private Optional<Coordinates> lookupFromOneMap(String postalCode) {
        String encoded = URLEncoder.encode(postalCode, StandardCharsets.UTF_8);
        String url = String.format(ONEMAP_URL_TEMPLATE, encoded);

        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                awaitOneMapAvailability();
                String body = fetchOneMap(url);
                if (body == null || body.isBlank()) {
                    return Optional.empty();
                }

                JSONObject response = new JSONObject(body);
                JSONArray results = response.optJSONArray("results");
                if (results == null || results.isEmpty()) {
                    return Optional.empty();
                }

                JSONObject first = results.optJSONObject(0);
                if (first == null) {
                    return Optional.empty();
                }

                String latText = first.optString("LATITUDE", "").trim();
                String lngText = first.optString("LONGITUDE", "").trim();
                if (latText.isEmpty() || lngText.isEmpty()) {
                    return Optional.empty();
                }

                return Optional.of(new Coordinates(Double.parseDouble(latText), Double.parseDouble(lngText)));
            } catch (RestClientResponseException ex) {
                boolean shouldRetry = handleOneMapHttpException(ex, "postalCode=" + postalCode);
                if (!shouldRetry || attempt == 1) {
                    return Optional.empty();
                }
            } catch (RestClientException | NumberFormatException ex) {
                log.warn("OneMap lookup failed for postalCode={}: {}", postalCode, ex.getMessage());
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private void awaitOneMapAvailability() {
        waitForCooldownIfNeeded();
        throttleOneMap();
    }

    private String fetchOneMap(String url) {
        if (oneMapApiToken == null || oneMapApiToken.isBlank()) {
            return restTemplate.getForObject(url, String.class);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", oneMapApiToken.trim());
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
        return response.getBody();
    }

    private void throttleOneMap() {
        synchronized (oneMapLock) {
            long now = System.currentTimeMillis();
            long waitMs = ONEMAP_MIN_INTERVAL_MS - (now - oneMapLastRequestEpochMs);
            if (waitMs > 0) {
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
            oneMapLastRequestEpochMs = System.currentTimeMillis();
        }
    }

    private void waitForCooldownIfNeeded() {
        long waitMs = oneMapCooldownUntilEpochMs - System.currentTimeMillis();
        if (waitMs <= 0) {
            return;
        }

        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean handleOneMapHttpException(RestClientResponseException ex, String context) {
        int statusCode = ex.getStatusCode().value();

        if (statusCode == 429) {
            oneMapCooldownUntilEpochMs = Math.max(
                    oneMapCooldownUntilEpochMs,
                    System.currentTimeMillis() + ONEMAP_COOLDOWN_ON_429_MS);
            log.warn("OneMap throttled (429). Cooling down for {}s. context={}",
                    ONEMAP_COOLDOWN_ON_429_MS / 1000,
                    context);
            return true;
        }

        log.warn("OneMap HTTP error {}. context={}, message={}",
            statusCode,
                context,
                ex.getMessage());
        return false;
    }

    private Optional<Coordinates> queryCoordinates(String sql, String postalCode) {
        return jdbcTemplate.query(sql,
                rs -> rs.next()
                        ? Optional.of(new Coordinates(rs.getDouble("latitude"), rs.getDouble("longitude")))
                        : Optional.empty(),
                postalCode);
    }

    private Optional<Coordinates> queryCoordinatesSafe(String sql, String postalCode) {
        try {
            return queryCoordinates(sql, postalCode);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private void cacheExternalLookup(String postalCode, Coordinates coords, String source) {
        jdbcTemplate.update(
                """
                INSERT INTO external_postal_lookup (postal_code, latitude, longitude, source, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (postal_code) DO UPDATE SET
                    latitude = EXCLUDED.latitude,
                    longitude = EXCLUDED.longitude,
                    source = EXCLUDED.source,
                    updated_at = EXCLUDED.updated_at
                """,
                postalCode,
                coords.getLat(),
                coords.getLng(),
                source,
                LocalDateTime.now());
    }

    private void ensureExternalLookupTable() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS external_postal_lookup (
                    postal_code VARCHAR(20) PRIMARY KEY,
                    latitude DOUBLE PRECISION NOT NULL,
                    longitude DOUBLE PRECISION NOT NULL,
                    source VARCHAR(32) NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
    }

    private void ensureExternalAddressLookupTable() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS external_address_lookup (
                    address_key VARCHAR(256) PRIMARY KEY,
                    postal_code VARCHAR(20) NOT NULL,
                    latitude DOUBLE PRECISION NOT NULL,
                    longitude DOUBLE PRECISION NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
    }

    private void cacheExternalAddressLookup(String addressKey, AddressGeocodeResult result) {
        jdbcTemplate.update(
                """
                INSERT INTO external_address_lookup (address_key, postal_code, latitude, longitude, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (address_key) DO UPDATE SET
                    postal_code = EXCLUDED.postal_code,
                    latitude = EXCLUDED.latitude,
                    longitude = EXCLUDED.longitude,
                    updated_at = EXCLUDED.updated_at
                """,
                addressKey,
                result.postalCode(),
                result.latitude(),
                result.longitude(),
                LocalDateTime.now());
    }

    private String normalizePostal(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value;
    }

    private String normalizeBlock(String raw) {
        if (raw == null) {
            return null;
        }

        String value = raw.trim().toUpperCase().replace(" ", "");
        return value.isEmpty() ? null : value;
    }

    private String normalizeStreet(String raw) {
        if (raw == null) {
            return null;
        }

        String cleaned = raw.toUpperCase().replaceAll("[^A-Z0-9 ]", " ").trim();
        if (cleaned.isEmpty()) {
            return null;
        }

        String[] tokens = cleaned.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            String canonical = STREET_TOKEN_CANONICAL.getOrDefault(token, token);
            if (!canonical.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(canonical);
            }
        }

        return builder.length() == 0 ? null : builder.toString();
    }

    private static Map<String, String> buildStreetTokenCanonicalMap() {
        Map<String, String> map = new HashMap<>();
        map.put("AVENUE", "AVE");
        map.put("AVE", "AVE");
        map.put("AVEN", "AVE");

        map.put("ROAD", "RD");
        map.put("RD", "RD");

        map.put("STREET", "ST");
        map.put("ST", "ST");
        map.put("SAINT", "ST");

        map.put("DRIVE", "DR");
        map.put("DR", "DR");

        map.put("CENTRE", "CTR");
        map.put("CTR", "CTR");

        map.put("CENTRAL", "CTRL");
        map.put("CTRL", "CTRL");

        map.put("CRESCENT", "CRES");
        map.put("CRES", "CRES");

        map.put("TERRACE", "TER");
        map.put("TER", "TER");

        map.put("PLACE", "PL");
        map.put("PL", "PL");

        map.put("NORTH", "NTH");
        map.put("NTH", "NTH");

        map.put("SOUTH", "STH");
        map.put("STH", "STH");

        map.put("UPPER", "UPP");
        map.put("UPP", "UPP");

        map.put("LORONG", "LOR");
        map.put("LOR", "LOR");

        map.put("MOUNT", "MT");
        map.put("MT", "MT");

        map.put("JALAN", "JLN");
        map.put("JLN", "JLN");

        map.put("BUKIT", "BT");
        map.put("BT", "BT");
        return map;
    }

    public record AddressGeocodeResult(String postalCode, double latitude, double longitude) {
    }
}
