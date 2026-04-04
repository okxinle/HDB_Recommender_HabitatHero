package habitathero.control;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import habitathero.entity.Coordinates;

@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final String ONEMAP_URL_TEMPLATE =
            "https://www.onemap.gov.sg/api/common/elastic/search?searchVal=%s&returnGeom=Y&getAddrDetails=N&pageNum=1";

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    public GeocodingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = new RestTemplate();
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

        try {
            String body = restTemplate.getForObject(url, String.class);
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
        } catch (RestClientException | NumberFormatException ex) {
            log.warn("OneMap lookup failed for postalCode={}: {}", postalCode, ex.getMessage());
            return Optional.empty();
        }
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
}
